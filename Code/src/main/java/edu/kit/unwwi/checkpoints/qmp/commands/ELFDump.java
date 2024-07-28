package edu.kit.unwwi.checkpoints.qmp.commands;

import edu.kit.unwwi.checkpoints.qemu.models.memory.MemorySegment;
import edu.kit.unwwi.checkpoints.qmp.Command;
import edu.kit.unwwi.checkpoints.qmp.Event;
import edu.kit.unwwi.checkpoints.qmp.EventHandler;
import edu.kit.unwwi.checkpoints.qmp.QMPInterface;
import net.fornwall.jelf.ElfFile;
import net.fornwall.jelf.ElfSegment;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

/**
 * This class dumps the current memory data from a QEMU-instance to disk in ELF format.
 */
public class ELFDump extends Command implements EventHandler {

	/**
	 * This gets used to generate names for the temporary files.
	 */
	private static final Random NAME_GENERATOR = new Random();
	/**
	 * Where to store the temporary files.
	 */
	private static Path TEMPORARY_PATH = Paths.get(System.getProperty("java.io.tmpdir"));

	/**
	 * Where to store the dump to.
	 */
	private final Path target;
	/**
	 * The instance to wait for.
	 */
	private final QMPInterface instance;
	/**
	 * Lock used to await the completion of the dump.
	 */
	private final Lock completionLock = new ReentrantLock();
	/**
	 * Condition to await the completion of the dump.
	 */
	private final Condition awaitCompletion = completionLock.newCondition();

	/**
	 * Whether the command was executed.
	 */
	private boolean done;
	/**
	 * The size of the dump in bytes.
	 */
	private long size;
	/**
	 * The memory segments extracted from the dump.
	 */
	private MemorySegment[] result;

	/**
	 * This sets the path where to store the temporary dump files.
	 * Keep in mind they may get very large depending on the virtual machine.
	 * @param temp The Path to use for temporary files.
	 */
	public static void setTemp(@NotNull Path temp) {
		TEMPORARY_PATH = temp;
	}

	/**
	 * Creates a new ELFDump object which can be executed later.
	 * Needs the QMPInterface because this is an asynchronous request,
	 * which means the object needs to receive information from the Interface at some point.
	 *
	 * @param instance The QMPinterface to receive the event from.
	 */
	public ELFDump(@NotNull QMPInterface instance) {
		String filename = NAME_GENERATOR.nextLong() + ".dmp";
		this.target = TEMPORARY_PATH.resolve(filename);
		this.instance = instance;
	}

	/**
	 * Whether the dump operation has completed.
	 *
	 * @return Whether the dump is done.
	 */
	public boolean isDone() {
		return this.done;
	}

	/**
	 * The size of the dump in bytes.
	 *
	 * @return The size of the dump.
	 * @throws IllegalStateException Gets thrown when the dump operation has not yet completed.
	 */
	public long getSize() throws IllegalStateException {
		if (done) return this.size;
		else throw new IllegalStateException("The operation has not yet completed");
	}

	/**
	 * In case this dump has not yet completed, this method waits until the dump has been completed.
	 *
	 * @throws InterruptedException If this thread got interrupted during wait.
	 */
	public void awaitCompletion() throws InterruptedException {
		if (isDone()) return;
		completionLock.lock();
		if (!isDone()) awaitCompletion.await();
		completionLock.unlock();
	}

	public MemorySegment[] getSegments() throws IllegalStateException {
		if (!done) throw new IllegalStateException("The operation has not yet completed");
		else return this.result;
	}

	@Override
	protected @NotNull String toJson() {
		instance.registerEventHandler(this);
		return "{ \"execute\": \"dump-guest-memory\", \"arguments\": { \"paging\": true, \"protocol\": \"file:" + StringEscapeUtils.escapeJson(target.toAbsolutePath().toString()) + "\", \"format\": \"elf\" } }";
	}

	@Override
	protected void processResult(@NotNull Object result) {
		assert "{}".equals(result); // This should just be an empty JSON-object
	}

	@Override
	public void handleEvent(@NotNull Event event) {
		instance.unregisterEventHandler(this);
		JSONObject data = event.getData().getJSONObject("result");
		if (data.getString("status").equals("completed")) {
			this.size = data.getLong("total");
			completionLock.lock();
			try (InputStream fileInput = Files.newInputStream(target)) {
				ElfFile elf = ElfFile.from(fileInput);

				// This is split into 2 stream because performing parallel operations on the ElfFile-object heavily corrupts the used InputStream.
				BasicSegmentData[] segments = IntStream.range(0, elf.e_phnum).filter(x -> elf.getProgramHeader(x).p_type == ElfSegment.PT_LOAD)
						.mapToObj(x -> {
							ElfSegment programHeader = elf.getProgramHeader(x);
							return new BasicSegmentData(programHeader.p_offset, programHeader.p_filesz, programHeader.p_paddr, programHeader.p_vaddr);
						}).toArray(BasicSegmentData[]::new);
				this.result = Arrays.stream(segments).parallel().map(segment -> {
					try (InputStream segmentStream = Files.newInputStream(target)) {
						segmentStream.skipNBytes(segment.offset);
						return new MemorySegment(segment.pAddress, segment.vAddress, segment.size, segmentStream);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}).toArray(MemorySegment[]::new);

			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} finally {
				this.done = true;
				awaitCompletion.signalAll();
				completionLock.unlock();
			}
		}
	}

	@Override
	public @NotNull String eventName() {
		return "DUMP_COMPLETED";
	}

	/**
	 * This record gets used to store address data from ELF which later on gets used to actually create the segment objects.
	 *
	 * @param offset   The offset inside the ELF-File.
	 * @param size     The size of the segment in the file.
	 * @param pAddress The physical address of the segment.
	 * @param vAddress The virtual address of the segment.
	 */
	private record BasicSegmentData(long offset, long size, long pAddress, long vAddress) {
	}
}

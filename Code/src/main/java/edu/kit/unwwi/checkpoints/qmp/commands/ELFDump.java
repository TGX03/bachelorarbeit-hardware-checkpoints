package edu.kit.unwwi.checkpoints.qmp.commands;

import edu.kit.unwwi.checkpoints.qmp.Command;
import edu.kit.unwwi.checkpoints.qmp.Event;
import edu.kit.unwwi.checkpoints.qmp.EventHandler;
import edu.kit.unwwi.checkpoints.qmp.QMPInterface;
import net.fornwall.jelf.ElfFile;
import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class dumps the current memory data from a QEMU-instance to disk in ELF format.
 */
public class ELFDump extends Command implements EventHandler, AutoCloseable {

	private static final Random NAME_GENERATOR = new Random();
	private static Path TEMPORARY_PATH = Paths.get(System.getProperty("java.io.tmpdir"));

	/**
	 * Where to store the dump to.
	 */
	private final Path target;
	/**
	 * The instance to wait for.
	 */
	private final QMPInterface instance;
	private final Lock completionLock = new ReentrantLock();
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
	 * The path of the ELF file created during dumping.
	 */
	private ElfFile resultingFile;
	/**
	 * The InputStream of the file. Required to close it later on.
	 */
	private InputStream fileInput;

	public static void setTemp(Path temp) {
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

	public ElfFile getELF() throws IllegalStateException {
		if (done) return this.resultingFile;
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

	@Override
	protected @NotNull String toJson() {
		instance.registerEventHandler(this);
		return "{ \"execute\": \"dump-guest-memory\", \"arguments\": { \"paging\": false, \"protocol\": \"file:" + StringEscapeUtils.escapeJson(target.toAbsolutePath().toString()) + "\", \"format\": \"elf\" } }";
	}

	@Override
	protected void processResult(@NotNull Object result) {
		assert "{}".equals(result); // This should just be an empty JSON-object
	}

	@Override
	public void handleEvent(Event event) {
		instance.unregisterEventHandler(this);
		JSONObject data = event.getData().getJSONObject("result");
		if (data.getString("status").equals("completed")) {
			this.size = data.getLong("total");
			completionLock.lock();
			try {
				fileInput = Files.newInputStream(target);
				this.resultingFile = ElfFile.from(fileInput);
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
	public String eventName() {
		return "DUMP_COMPLETED";
	}

	@Override
	public void close() {
		try {
			fileInput.close();
			Files.deleteIfExists(target);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}

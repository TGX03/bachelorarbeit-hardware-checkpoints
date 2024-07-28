package edu.kit.unwwi.checkpoints;

import edu.kit.unwwi.checkpoints.qemu.models.Blockdevice;
import edu.kit.unwwi.checkpoints.qemu.models.CPU;
import edu.kit.unwwi.checkpoints.qemu.models.memory.MemorySegment;
import edu.kit.unwwi.checkpoints.qmp.Event;
import edu.kit.unwwi.checkpoints.qmp.EventHandler;
import edu.kit.unwwi.checkpoints.qmp.QMPInterface;
import edu.kit.unwwi.checkpoints.qmp.commands.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

/**
 * A class representing a checkpoint of a running QEMU-instance.
 */
public class Checkpoint {

	/**
	 * The location where this checkpoint is stored.
	 */
	private final Path location;
	/**
	 * The location where the JSON holding all information is stored.
	 */
	private final Path config;
	/**
	 * The timestamp from QEMU when this was created.
	 */
	private final long timestamp;
	/**
	 * The JSON in memory of this checkpoint.
	 */
	private final JSONObject json;

	private Checkpoint(Path location, Path config, long timestamp, JSONObject json) {
		this.location = location;
		this.config = config;
		this.timestamp = timestamp;
		this.json = json;
	}

	/**
	 * Create a new checkpoint at the given location containing all data that could be queried on the given interface.
	 *
	 * @param location     Where to store the data to.
	 * @param qmpInterface The interface connected to QEMU to query.
	 * @return A checkpoint object representing all the data found.
	 * @throws IOException          An IO-error occurred while writing to disk or while communicating with QEMU.
	 * @throws InterruptedException A thread was interrupted while waiting for data from QEMU.
	 * @throws ExecutionException   An exception occurred while waiting for data from QEMU.
	 */
	public static Checkpoint createCheckpoint(Path location, QMPInterface qmpInterface) throws IOException, InterruptedException, ExecutionException {
		assert Files.isDirectory(location);

		// This block just stops the VM synchronously.
		Lock stopLock = new ReentrantLock();
		final LongContainer container = new LongContainer();
		stopLock.lock();
		Condition stopCondition = stopLock.newCondition();
		qmpInterface.registerEventHandler(new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				stopLock.lock();
				container.value = event.getTimestamp() * 1000000 + event.getTimestampMicroseconds();
				stopCondition.signalAll();
				stopLock.unlock();
			}

			@Override
			public String eventName() {
				return "STOP";
			}
		});
		qmpInterface.executeCommand(Stop.INSTANCE);
		stopCondition.awaitUninterruptibly();
		stopLock.unlock();
		long timestamp = container.value;

		// Parse the registers
		FutureTask<JSONArray> futureCPUs = new FutureTask<>(() -> parseCPU(qmpInterface));
		Thread.ofPlatform().start(futureCPUs);

		// Create the subfolder for storing all checkpoint data
		Path subfolder = location.resolve(Long.toUnsignedString(timestamp));
		Files.createDirectory(subfolder);

		// Parse memory and blockdevices. Not yet sure whether virtual Threads are really a good idea here.
		FutureTask<JSONArray> futureBlocks = new FutureTask<>(() -> parseBlock(qmpInterface, subfolder));
		FutureTask<JSONArray> futureMemory = new FutureTask<>(() -> parseMemory(qmpInterface, subfolder));
		Thread.ofVirtual().start(futureBlocks);
		Thread.ofVirtual().start(futureMemory);

		// Create the descriptor file
		Path descriptorFile = subfolder.resolve("descriptor.json");
		JSONObject fullJSON = new JSONObject();
		fullJSON.put("timestamp", timestamp);

		// Put the results into JSON
		fullJSON.put("cpu", futureCPUs.get());
		fullJSON.put("memory", futureMemory.get());
		fullJSON.put("blockdevice", futureBlocks.get());

		qmpInterface.executeCommand(Continue.INSTANCE);
		Files.writeString(descriptorFile, fullJSON.toString());
		return new Checkpoint(subfolder, descriptorFile, timestamp, fullJSON);
	}

	/**
	 * Parses all information about the CPU on the given QMP instance.
	 *
	 * @param inter The QMP interface to parse.
	 * @return All collected information about the CPU.
	 * @throws IOException An error while reading from QEMU occurred.
	 */
	private static JSONArray parseCPU(QMPInterface inter) throws IOException {
		CPU[] cpus = getCPUs(inter);

		// Write CPU information to the JSON file
		JSONArray cpuArray = new JSONArray();
		for (CPU cpu : cpus) {
			cpuArray.put(cpu.toJSON());
		}
		return cpuArray;
	}

	/**
	 * Queries all existing CPU cores and their registers.
	 *
	 * @param inter The interface to query on.
	 * @return The CPU cores that were found.
	 * @throws IOException An error occurred while communicating with QEMU.
	 */
	private static CPU[] getCPUs(QMPInterface inter) throws IOException {
		QueryCPU query = new QueryCPU();
		inter.executeCommand(query);
		JSONArray cpus = query.getResult();
		final CPU[] result = new CPU[cpus.length()];
		IntStream.range(0, cpus.length()).parallel().forEach(i -> {
			JSONObject cpu = cpus.getJSONObject(i);
			int index = cpu.getInt("cpu-index");
			QueryRegisters registers = new QueryRegisters(index);
			try {
				inter.executeCommand(registers);
				result[i] = new CPU(index, cpu.getString("target"), cpu.getInt("thread-id"), registers.getResult(), registers.flags());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
		return result;
	}

	/**
	 * Parse all blockdevices connected to the running QEMU-instance and return a JSON Array containing their data.
	 * Also copies the images behind the blockdevices to the checkpoint folder.
	 *
	 * @param inter     The interface to query on.
	 * @param directory The directory where data about the running instance gets stored.
	 * @return The JSON array containing the information about the blockdevices.
	 * @throws IOException An error occurred while communicating with QEMU.
	 */
	private static JSONArray parseBlock(QMPInterface inter, Path directory) throws IOException {
		JSONArray result = new JSONArray();
		Path subfolder = directory.resolve("blocks");
		Files.createDirectory(subfolder);
		Blockdevice[] devices = getBlockdevices(inter);
		for (Blockdevice device : devices) {
			JSONObject deviceJSON = device.toJSON();
			if (device.hasMedia()) {
				assert device.getPath() != null;
				Path target = subfolder.resolve(device.getPath().getFileName());
				deviceJSON.put("storageLocation", target.toAbsolutePath().toString());
				Files.copy(device.getPath(), target);
				result.put(deviceJSON);
			}
		}
		return result;
	}

	/**
	 * Requests all blockdevices currently registered with QEMU.
	 *
	 * @param inter The interface to query on.
	 * @return The blockdevices which were found.
	 */
	private static Blockdevice[] getBlockdevices(QMPInterface inter) {
		QueryBlock blocks = new QueryBlock();
		try {
			inter.executeCommand(blocks);
			return blocks.getResult();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Parse the memory contents of the VM and write them to disk.
	 *
	 * @param inter     The interface to query on.
	 * @param directory The directory to store the dumps to.
	 * @return A JSON array holding information about the queried data.
	 * @throws IOException          An error occurred while communicating with QEMU.
	 * @throws InterruptedException This thread was interrupted while waiting for the QEMU-dump to finish.
	 */
	private static JSONArray parseMemory(QMPInterface inter, Path directory) throws IOException, InterruptedException {
		JSONArray segments = new JSONArray();
		ELFDump elf = new ELFDump(inter);
		inter.executeCommand(elf);
		elf.awaitCompletion();
		Path segmentStorage = directory.resolve("memory");
		Files.createDirectory(segmentStorage);
		for (MemorySegment segment : elf.getSegments()) {
			JSONObject segmentJSON = segment.toJSON();
			Path segmentLocation = segmentStorage.resolve(Long.toUnsignedString(segment.getStartPhysicalAddress()) + ".dmp");
			segmentJSON.put("location", segmentLocation.toAbsolutePath().toString());
			segment.getInputStream().transferTo(Files.newOutputStream(segmentLocation));
			segments.put(segmentJSON);
		}
		return segments;
	}

	/**
	 * Simply stores a long that can be updated at will.
	 */
	private static final class LongContainer {
		/**
		 * The stored long.
		 */
		public long value;
	}
}

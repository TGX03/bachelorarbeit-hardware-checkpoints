package edu.kit.unwwi.checkpoints;

import edu.kit.unwwi.checkpoints.qemu.models.Blockdevice;
import edu.kit.unwwi.checkpoints.qemu.models.CPU;
import edu.kit.unwwi.checkpoints.qemu.models.memory.MemorySegment;
import edu.kit.unwwi.checkpoints.qmp.Event;
import edu.kit.unwwi.checkpoints.qmp.EventHandler;
import edu.kit.unwwi.checkpoints.qmp.QMPInterface;
import edu.kit.unwwi.checkpoints.qmp.commands.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A class representing a checkpoint of a running QEMU-instance.
 */
public class Checkpoint {

	/**
	 * The JSON key for the hash of a given object.
	 */
	private static final String HASH = "hash";
	/**
	 * The JSON key for the location of files related to a given object.
	 */
	private static final String STORAGE_LOCATION = "storageLocation";
	/**
	 * The JSON key for CPU data.
	 */
	private static final String CPU = "cpu";
	/**
	 * The JSON key and directory name for blockdevices.
	 */
	private static final String BLOCK = "blockdevice";
	/**
	 * The JSON key and directory name for memory segments.
	 */
	private static final String MEMORY = "memory";
	/**
	 * The file ending used for memory files.
	 */
	private static final String MEMORY_FILE_TYPE = ".dmp";
	/**
	 * The JSON key used for the timestamp of a checkpoint.
	 */
	private static final String TIMESTAMP = "timestamp";
	/**
	 * The filename of the main JSON file.
	 */
	private static final String JSON_FILE = "checkpoint.json";

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
	/**
	 * Stores hashes and paths of all the blockdevices in this checkpoint for later use.
	 */
	private final Map<String, Path> blockHashes = new HashMap<>();
	/**
	 * Stores hashes and paths of all the memory segments in this checkpoint for later use.
	 */
	private final Map<String, Path> segmentHashes = new HashMap<>();

	/**
	 * Internal constructor to create a new checkpoint after necessary actions were completed.
	 *
	 * @param location  The folder where this checkpoint gets stored.
	 * @param config    The path to the json-file containing metadata about this checkpoint.
	 * @param timestamp The timestamp QEMU reported when this checkpoint was created.
	 * @param json      The json structure contained in the file.
	 */
	private Checkpoint(@NotNull Path location, @NotNull Path config, long timestamp, @NotNull JSONObject json) {
		this.location = location;
		this.config = config;
		this.timestamp = timestamp;
		this.json = json;
		JSONArray blockdevices = json.getJSONArray(BLOCK);
		for (Object current : blockdevices) {
			JSONObject device = (JSONObject) current;
			if (device.has(STORAGE_LOCATION)) {
				blockHashes.put(device.getString(HASH), Paths.get(device.getString(STORAGE_LOCATION)));
			}
		}
		JSONArray segments = json.getJSONArray(MEMORY);
		for (Object current : segments) {
			JSONObject segment = (JSONObject) current;
			segmentHashes.put(segment.getString(HASH), Paths.get(segment.getString(STORAGE_LOCATION)));
		}
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
	public static Checkpoint createCheckpoint(@NotNull Path location, @NotNull QMPInterface qmpInterface) throws IOException, InterruptedException, ExecutionException {
		assert Files.isDirectory(location);
		long timestamp = stopExecution(qmpInterface);

		// Parse the registers
		FutureTask<JSONArray> futureCPUs = new FutureTask<>(() -> parseCPU(qmpInterface));
		Thread.ofPlatform().name(CPU).start(futureCPUs);

		// Create the subfolder for storing all checkpoint data
		Path subfolder = location.resolve(Long.toUnsignedString(timestamp));
		Files.createDirectory(subfolder);

		// Parse memory and blockdevices. Not yet sure whether virtual Threads are really a good idea here.
		FutureTask<JSONArray> futureBlocks = new FutureTask<>(() -> parseAndCopyBlock(qmpInterface, subfolder));
		FutureTask<JSONArray> futureMemory = new FutureTask<>(() -> parseMemory(qmpInterface, subfolder));
		Thread.ofPlatform().name(MEMORY).start(futureMemory);
		Thread.ofVirtual().name(BLOCK).start(futureBlocks);

		// Create the descriptor file
		Path descriptorFile = subfolder.resolve(JSON_FILE);
		JSONObject fullJSON = new JSONObject();
		fullJSON.put(TIMESTAMP, timestamp);

		// Put the results into JSON
		fullJSON.put(CPU, futureCPUs.get());
		fullJSON.put(MEMORY, futureMemory.get());
		fullJSON.put(BLOCK, futureBlocks.get());

		qmpInterface.executeCommand(Continue.INSTANCE);
		Files.writeString(descriptorFile, fullJSON.toString());
		return new Checkpoint(subfolder, descriptorFile, timestamp, fullJSON);
	}

	/**
	 * This method stops the execution of the provided QEMU-instance and returns the timestamp provided by QEMU.
	 *
	 * @param inter The QEMU-instance to stop.
	 * @return The timestamp returned by QEMU.
	 * @throws IOException Something went wrong while communicating with QEMU.
	 */
	private static long stopExecution(@NotNull QMPInterface inter) throws IOException {

		Status status = new Status();
		inter.executeCommand(status);

		if (status.isRunning()) {
			Lock stopLock = new ReentrantLock();
			final LongContainer container = new LongContainer();
			stopLock.lock();
			Condition stopCondition = stopLock.newCondition();
			inter.registerEventHandler(new EventHandler() {
				@Override
				public void handleEvent(@NotNull Event event) {
					stopLock.lock();
					container.value = event.getTimestamp() * 1000000 + event.getTimestampMicroseconds();
					stopCondition.signalAll();
					stopLock.unlock();
				}

				@Override
				public @NotNull String eventName() {
					return "STOP";
				}
			});
			inter.executeCommand(Stop.INSTANCE);
			stopCondition.awaitUninterruptibly();
			stopLock.unlock();
			return container.value;
		} else return -1L;
	}

	/**
	 * Parses all information about the CPU on the given QMP instance.
	 *
	 * @param inter The QMP interface to parse.
	 * @return All collected information about the CPU.
	 * @throws IOException An error while reading from QEMU occurred.
	 */
	private static JSONArray parseCPU(@NotNull QMPInterface inter) throws IOException {
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
	private static CPU[] getCPUs(@NotNull QMPInterface inter) throws IOException {
		QueryCPURegisters query = new QueryCPURegisters(inter);
		inter.executeCommand(query);
		return query.getResult();
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
	private static JSONArray parseAndCopyBlock(@NotNull QMPInterface inter, @NotNull Path directory) throws IOException {
		JSONArray result = new JSONArray();
		Path subfolder = directory.resolve(BLOCK);
		Files.createDirectory(subfolder);
		Blockdevice[] devices = getBlockdevices(inter);
		for (Blockdevice device : devices) {
			JSONObject deviceJSON = device.toJSON();
			if (device.hasMedia()) {
				assert device.getPath() != null;
				Path target = subfolder.resolve(device.getPath().getFileName());
				deviceJSON.put(STORAGE_LOCATION, target.toAbsolutePath().toString());
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
	private static Blockdevice[] getBlockdevices(@NotNull QMPInterface inter) {
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
	private static JSONArray parseMemory(@NotNull QMPInterface inter, @NotNull Path directory) throws IOException, InterruptedException {
		JSONArray segments = new JSONArray();
		ELFDump elf = new ELFDump(inter);
		inter.executeCommand(elf);
		elf.awaitCompletion();
		Path segmentStorage = directory.resolve(MEMORY);
		Files.createDirectory(segmentStorage);
		for (MemorySegment segment : elf.getSegments()) {
			JSONObject segmentJSON = segment.toJSON();
			Path segmentLocation = segmentStorage.resolve(Long.toUnsignedString(segment.getStartPhysicalAddress()) + MEMORY_FILE_TYPE);
			segmentJSON.put(STORAGE_LOCATION, segmentLocation.toAbsolutePath().toString());
			segment.getInputStream().transferTo(Files.newOutputStream(segmentLocation));
			segments.put(segmentJSON);
		}
		return segments;
	}

	/**
	 * Returns the directory containing all data from this checkpoint.
	 *
	 * @return Where this checkpoint is stored.
	 */
	@NotNull
	public Path getContainingPath() {
		return this.location;
	}

	/**
	 * The path of the JSON-File containing all the data equivalent to getJson().
	 *
	 * @return The path of the JSON-file of this checkpoint.
	 */
	@NotNull
	public Path getJsonFile() {
		return this.config;
	}

	/**
	 * The timestamp from QEMU of when this checkpoint was created.
	 *
	 * @return When this checkpoint was created.
	 */
	public long getTimestamp() {
		return this.timestamp;
	}

	/**
	 * All data included in this checkpoint as JSON.
	 *
	 * @return All data included in this checkpoint as JSON.
	 */
	@NotNull
	public JSONObject getJson() {
		return this.json;
	}

	/**
	 * Create a new Checkpoint that is a successor to this checkpoint.
	 * It checks whether memory regions or blockdevices are still identical to preserve space,
	 * however for this it only tracks the full file, so if a single bit changes, the complete file gets saved again.
	 *
	 * @param qmpInterface The interface to query the current VM on.
	 * @return The newly created checkpoint.
	 * @throws IOException          When something went wrong during IO or while communicating with QEMU.
	 * @throws InterruptedException If this thread got interrupted for some reason.
	 * @throws ExecutionException   When an exception occurred in another thread affecting this thread.
	 */
	public Checkpoint createFollowUp(@NotNull QMPInterface qmpInterface) throws IOException, InterruptedException, ExecutionException {
		long timestamp = stopExecution(qmpInterface);

		// Parse the registers
		FutureTask<JSONArray> futureCPUs = new FutureTask<>(() -> parseCPU(qmpInterface));
		Thread.ofPlatform().name(CPU).start(futureCPUs);

		Path subfolder = location.getParent().resolve(Long.toUnsignedString(timestamp));
		Files.createDirectory(subfolder);

		// Parse memory and blockdevices. Not yet sure whether virtual Threads are really a good idea here.
		FutureTask<JSONArray> futureBlocks = new FutureTask<>(() -> parseBlocksCheckDuplicates(qmpInterface, subfolder));
		FutureTask<JSONArray> futureMemory = new FutureTask<>(() -> parseMemoryCheckDuplicates(qmpInterface, subfolder));
		Thread.ofPlatform().name(MEMORY).start(futureMemory);
		Thread.ofVirtual().name(BLOCK).start(futureBlocks);

		// Create the descriptor file
		Path descriptorFile = subfolder.resolve(JSON_FILE);
		JSONObject fullJSON = new JSONObject();
		fullJSON.put(TIMESTAMP, timestamp);

		// Put the results into JSON
		fullJSON.put(CPU, futureCPUs.get());
		fullJSON.put(MEMORY, futureMemory.get());
		fullJSON.put(BLOCK, futureBlocks.get());

		qmpInterface.executeCommand(Continue.INSTANCE);
		Files.writeString(descriptorFile, fullJSON.toString());
		return new Checkpoint(subfolder, descriptorFile, timestamp, fullJSON);
	}

	/**
	 * Parses block devices and checks whether those have changed. If no changes were detected,
	 * the reference points to the already existing file.
	 *
	 * @param inter     The interface to query QEMU on.
	 * @param directory Where this checkpoint gets stored.
	 * @return A JSON Array containing the metadata about the block devices.
	 * @throws IOException When something went wrong during IO or while communicating with QEMU.
	 */
	@NotNull
	private JSONArray parseBlocksCheckDuplicates(@NotNull QMPInterface inter, @NotNull Path directory) throws IOException {
		JSONArray result = new JSONArray();
		Path subfolder = directory.resolve(BLOCK);
		Files.createDirectory(subfolder);
		Blockdevice[] devices = getBlockdevices(inter);
		for (Blockdevice device : devices) {
			JSONObject deviceJSON = device.toJSON();
			if (device.hasMedia()) {
				assert device.getPath() != null;
				if (blockHashes.containsKey(deviceJSON.getString(HASH))) {
					deviceJSON.put(STORAGE_LOCATION, blockHashes.get(deviceJSON.getString(HASH)).toAbsolutePath().toString());
				} else {
					Path target = subfolder.resolve(device.getPath().getFileName());
					deviceJSON.put(STORAGE_LOCATION, target.toAbsolutePath().toString());
					Files.copy(device.getPath(), target);
				}
				result.put(deviceJSON);
			}
		}
		return result;
	}

	/**
	 * Parses memory and checks whether its segments have changed. If no changes are detected,
	 * the reference points to the already existing files.
	 *
	 * @param inter     The interface to query QEMU on.
	 * @param directory Where this checkpoint gets stored.
	 * @return A JSON Array containing the metadata about the memory segments.
	 * @throws IOException When something went wrong during IO or while communicating with QEMU.
	 */
	@NotNull
	private JSONArray parseMemoryCheckDuplicates(@NotNull QMPInterface inter, @NotNull Path directory) throws IOException, InterruptedException {
		JSONArray segments = new JSONArray();
		ELFDump elf = new ELFDump(inter);
		inter.executeCommand(elf);
		elf.awaitCompletion();
		Path segmentStorage = directory.resolve(MEMORY);
		Files.createDirectory(segmentStorage);
		for (MemorySegment segment : elf.getSegments()) {
			JSONObject segmentJSON = segment.toJSON();
			if (segmentHashes.containsKey(segmentJSON.getString(HASH))) {
				segmentJSON.put(STORAGE_LOCATION, segmentHashes.get(segmentJSON.getString(HASH)).toAbsolutePath().toString());
			} else {
				Path segmentLocation = segmentStorage.resolve(Long.toUnsignedString(segment.getStartPhysicalAddress()) + MEMORY_FILE_TYPE);
				segmentJSON.put(STORAGE_LOCATION, segmentLocation.toAbsolutePath().toString());
				segment.getInputStream().transferTo(Files.newOutputStream(segmentLocation));
			}
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

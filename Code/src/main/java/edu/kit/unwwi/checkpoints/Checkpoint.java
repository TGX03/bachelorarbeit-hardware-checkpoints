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

public class Checkpoint {

	private final Path location;
	private final Path config;
	private final long timestamp;
	private final JSONObject json;

	private Checkpoint(Path location, Path config, long timestamp, JSONObject json) {
		this.location = location;
		this.config = config;
		this.timestamp = timestamp;
		this.json = json;
	}

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

		fullJSON.put("cpu", futureCPUs.get());
		fullJSON.put("memory", futureMemory.get());
		fullJSON.put("blockdevice", futureBlocks.get());

		qmpInterface.executeCommand(Continue.INSTANCE);
		Files.writeString(descriptorFile, fullJSON.toString());
		return new Checkpoint(subfolder, descriptorFile, timestamp, fullJSON);
	}

	private static JSONArray parseCPU(QMPInterface inter) throws IOException {
		CPU[] cpus = getCPUs(inter);

		// Write CPU information to the JSON file
		JSONArray cpuArray = new JSONArray();
		for (CPU cpu : cpus) {
			cpuArray.put(cpu.toJSON());
		}
		return cpuArray;
	}

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

	private static Blockdevice[] getBlockdevices(QMPInterface inter) {
		QueryBlock blocks = new QueryBlock();
		try {
			inter.executeCommand(blocks);
			return blocks.getResult();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

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

	private static final class LongContainer {
		public long value;
	}
}

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class Checkpoint {

	private final Path location;
	private final Path config;
	private final long timestamp;

	private Checkpoint(Path location, Path config, long timestamp) {
		this.location = location;
		this.config = config;
		this.timestamp = timestamp;
	}

	public static Checkpoint createCheckpoint(Path location, QMPInterface qmpInterface) throws IOException, InterruptedException {
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

		// Create a thread for parsing blockdevices
		final AtomicReference<Blockdevice[]> blockdevices = new AtomicReference<>();
		final Thread blockParser = Thread.ofVirtual().start(() -> blockdevices.set(getBlockdevices(qmpInterface)));

		// Create a thread for dumping memory
		ELFDump elf = new ELFDump(qmpInterface);
		qmpInterface.executeCommand(elf);

		// Create a thread for parsing CPU information
		final AtomicReference<CPU[]> cpus = new AtomicReference<>();
		Thread cpuParser = Thread.ofVirtual().start(() -> cpus.set(getCPUs(qmpInterface)));

		// Create the subfolder for storing all checkpoint data
		Path subfolder = location.resolve(Long.toUnsignedString(timestamp));
		Files.createDirectory(subfolder);

		// Create the descriptor file
		Path descriptorFile = subfolder.resolve("descriptor.json");
		JSONObject fullJSON = new JSONObject();
		fullJSON.put("timestamp", timestamp);

		// Write CPU information to the JSON file
		JSONArray cpuArray = new JSONArray();
		cpuParser.join();
		for (CPU cpu : cpus.get()) {
			cpuArray.put(cpu.toJSON());
		}
		fullJSON.put("CPUs", cpuArray);

		// Create directory for storing copies of the blockdevices
		final Path blockStorage = subfolder.resolve("blocks");
		Files.createDirectory(blockStorage);

		// Copy the blockdevices
		Thread fileCopier = Thread.ofVirtual().start(() -> {
			try {
				blockParser.join();
			} catch (InterruptedException _) {}

			Blockdevice[] devices = blockdevices.get();
			for (Blockdevice device : devices) {
				JSONObject deviceJSON = device.toJSON();
				if (device.hasMedia()) {
					assert device.getPath() != null;
					Path target = subfolder.resolve(device.getPath().getFileName());
					deviceJSON.put("storageLocation", target.toAbsolutePath().toString());
					try {
						Files.copy(device.getPath(), target);
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
			}
		});

		// Write the memory regions to disk
		elf.awaitCompletion();
		JSONArray segments = new JSONArray();
		Path segmentStorage = subfolder.resolve("memory");
		Files.createDirectory(segmentStorage);
		for (MemorySegment segment : elf.getSegments()) {
			JSONObject segmentJSON = segment.toJSON();
			Path segmentLocation = segmentStorage.resolve(Long.toUnsignedString(segment.getStartPhysicalAddress()) + ".dmp");
			segmentJSON.put("location", segmentLocation.toAbsolutePath().toString());
			segment.getInputStream().transferTo(Files.newOutputStream(segmentLocation));
			segments.put(segmentJSON);
		}
		fullJSON.put("memory", segments);

		fileCopier.join();
		qmpInterface.executeCommand(Continue.INSTANCE);
		Files.writeString(descriptorFile, fullJSON.toString());
		return new Checkpoint(subfolder, descriptorFile, timestamp);
	}

	private static CPU[] getCPUs(QMPInterface inter) {
		QueryCPU query = new QueryCPU();
		try {
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
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
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

	private static final class LongContainer {
		public long value;
	}
}

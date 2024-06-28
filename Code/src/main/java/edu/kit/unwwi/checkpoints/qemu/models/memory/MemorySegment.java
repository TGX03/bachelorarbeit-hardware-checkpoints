package edu.kit.unwwi.checkpoints.qemu.models.memory;

import com.sun.jna.Memory;

import java.io.InputStream;
import java.lang.ref.ReferenceQueue;

/**
 * This class represents a segment of memory.
 * This is the generic class, for specific platforms further implementations are required.
 */
public abstract class MemorySegment implements AutoCloseable {

	/**
	 * The queue for phantom references that went out of scope,
	 * to make sure no Memory objects stay behind and cause a memory leak.
	 */
	private static final ReferenceQueue<MemorySegment> memoryQueue = new ReferenceQueue<>();

	static {
		Thread collector = new Thread(() -> {
			while (true) {
				try {
					MemoryReference<?> current = (MemoryReference<?>) memoryQueue.remove();
					current.free();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
		collector.setDaemon(true);
		collector.start();
	}

	/**
	 * The memory region used to read the data into.
	 */
	public final Memory resultBuffer;
	/**
	 * The process ID of the process to read from.
	 */
	protected final int process;
	/**
	 * The address of the other process to read from.
	 */
	protected final long address;

	/**
	 * Create a new Memory segment. Its memory gets allocated directly in the constructor.
	 *
	 * @param processID The process ID to read from.
	 * @param offset    The address to read from.
	 * @param size      The number of bytes to read from the other process.
	 */
	public MemorySegment(int processID, long offset, long size) {
		resultBuffer = new Memory(size);
		this.process = processID;
		this.address = offset;
		new MemoryReference<>(resultBuffer, this, memoryQueue);
	}

	/**
	 * Start the reading process.
	 */
	public abstract void read();

	/**
	 * Create an InputStream from this memory object to read from.
	 * As the max size of a byte array is ~2GB, this is probably a better way.
	 * Make sure to actually read memory beforehand, as this method does not check.
	 *
	 * @return The input stream from read memory.
	 */
	public InputStream getInputStream() {
		return new MemoryInputStream(resultBuffer);
	}

	/**
	 * Reads a single byte from the memory backing this object.
	 * Make sure to actually read memory beforehand, as this method does not check.
	 * The address gets treated as a global address,
	 * meaning it subtracts the start address from this object from the provided address.
	 *
	 * @param address The address to read from.
	 * @return The byte read from the specified address.
	 */
	public byte getByte(long address) {
		return resultBuffer.getByte(address - this.address);
	}

	/**
	 * Close the memory behind this object.
	 */
	public void close() {
		resultBuffer.close();
	}
}

package edu.kit.unwwi.checkpoints.qemu.models.memory;

import com.sun.jna.Memory;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

/**
 * An Input Stream made from a JNA memory object.
 */
class MemoryInputStream extends InputStream {

	/**
	 * The backing memory.
	 */
	private final Memory memory;
	/**
	 * The address to be read from next.
	 */
	private long currentAddress = 0;

	/**
	 * Create a new InputStream from a memory object.
	 * Start index is 0.
	 *
	 * @param memory The memory object to use.
	 */
	public MemoryInputStream(Memory memory) {
		this.memory = memory;
	}

	@Override
	public int available() {
		long result = memory.size() - currentAddress;
		if (result > Integer.MAX_VALUE) return Integer.MAX_VALUE;
		else return (int) result;
	}

	@Override
	public int read() {
		if (memory.size() >= currentAddress) return -1;
		else {
			int result = memory.getByte(currentAddress);
			this.currentAddress++;
			return result;
		}
	}

	@Override
	public int read(byte @NotNull [] buffer) {
		long size = memory.size() - currentAddress;
		if (buffer.length > size) {
			this.memory.read(currentAddress, buffer, 0, buffer.length);
			this.currentAddress += buffer.length;
			return buffer.length;
		} else {
			this.memory.read(currentAddress, buffer, 0, (int) size);
			this.currentAddress += size;
			return (int) size;
		}
	}

	@Override
	public int read(byte @NotNull [] buffer, int offset, int length) {
		long size = memory.size() - currentAddress;
		if (length > size) {
			this.memory.read(currentAddress, buffer, offset, (int) size);
			this.currentAddress += size;
			return (int) size;
		} else {
			this.memory.read(currentAddress, buffer, offset, length);
			this.currentAddress += length;
			return length;
		}
	}

	@Override
	public long skip(long n) {
		long result = currentAddress + n;
		if (result < memory.size()) {
			this.currentAddress = result;
			return n;
		} else {
			result = memory.size() - currentAddress;
			currentAddress = memory.size();
			return result;
		}
	}
}

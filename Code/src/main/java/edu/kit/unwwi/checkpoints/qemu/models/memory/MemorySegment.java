package edu.kit.unwwi.checkpoints.qemu.models.memory;

import edu.kit.unwwi.collections.big.BigByteArrayInputStream;
import it.unimi.dsi.fastutil.BigArrays;
import it.unimi.dsi.fastutil.bytes.ByteBigArrays;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

/**
 * A class representing a memory segment read from an ELF dump
 */
public class MemorySegment implements Serializable {

	/**
	 * The start address of the contents in this segment in actual memory.
	 */
	private final long startAddress;
	/**
	 * The size of this segment.
	 */
	private final long size;
	/**
	 * The actual content of this segment.
	 * 2D-array because ints.
	 */
	private final byte[][] content;

	/**
	 * Create a new memory segment from an already existing 2D array.
	 *
	 * @param startAddress The start address of the segment in actual memory.
	 * @param size         The size of this segment.
	 * @param content      The contents to copy to this segment.
	 */
	public MemorySegment(long startAddress, long size, byte[][] content) {
		this.startAddress = startAddress;
		this.size = size;
		this.content = BigArrays.copy(content, 0, size);
	}

	/**
	 * Create a new segment which reads data from an Input Stream.
	 *
	 * @param startAddress The start address of this segment in actual memory.
	 * @param size         The size of this segment.
	 * @param input        The stream to write to this segment.
	 * @throws IOException If any read error occurs while reading.
	 */
	public MemorySegment(long startAddress, long size, InputStream input) throws IOException {
		this.startAddress = startAddress;
		this.size = size;
		this.content = ByteBigArrays.newBigArray(size);
		int bufferSize = Integer.MAX_VALUE;
		if (size < bufferSize) bufferSize = (int) size;
		byte[] buffer = new byte[bufferSize];
		int read;
		long position = 0;
		do {
			read = input.read(buffer);
			if (read != -1) {
				BigArrays.copyToBig(buffer, 0, this.content, position, read);
			}
			position = position + read;
		} while (read != -1);
	}

	/**
	 * Returns the address where this segment starts in physical memory.
	 *
	 * @return The address where this segment starts.
	 */
	public long getStartAddress() {
		return startAddress;
	}

	/**
	 * The size of this segment.
	 *
	 * @return The size of this segment.
	 */
	public long getSize() {
		return size;
	}

	/**
	 * Returns a copy of the contents of this segment.
	 * Changes to the returned array are isolated from this object.
	 *
	 * @return The contents of this array as a 2D-array.
	 */
	public byte[][] getContent() {
		return BigArrays.copy(this.content);
	}

	/**
	 * Returns a byte from an actual given address.
	 *
	 * @param address The address to read from.
	 * @return The byte at given position.
	 */
	public byte getByAddress(long address) {
		return BigArrays.get(this.content, address - this.startAddress);
	}

	/**
	 * Returns a byte from an offset relative to the start of this segment.
	 *
	 * @param offset The offset in this segment to read from.
	 * @return The byte from the given offset.
	 */
	public byte getByOffset(long offset) {
		return BigArrays.get(this.content, offset);
	}

	public InputStream getInputStream() {
		return new BigByteArrayInputStream(this.content);
	}
}
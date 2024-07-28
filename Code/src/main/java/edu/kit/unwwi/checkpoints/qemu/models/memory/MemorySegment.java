package edu.kit.unwwi.checkpoints.qemu.models.memory;

import edu.kit.unwwi.JSONable;
import edu.kit.unwwi.collections.big.BigByteArrayInputStream;
import it.unimi.dsi.fastutil.BigArrays;
import it.unimi.dsi.fastutil.bytes.ByteBigArrays;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * A class representing a memory segment read from an ELF dump
 */
public class MemorySegment implements Serializable, JSONable {

	/**
	 * The digest to use when computing the hash of a memory segment.
	 * SHA256 by default, can be changed using static method.
	 */
	private static MessageDigest DIGEST = DigestUtils.getSha256Digest();

	/**
	 * The start address of the contents in this segment in physical  memory.
	 */
	private final long startPhysicalAddress;
	/**
	 * The start address of the contents in this segment in virtual memory.
	 */
	private final long startVirtualAddress;
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
	 * The hash of this segment.
	 */
	private final byte[] hash;

	/**
	 * Create a new memory segment from an already existing 2D array.
	 *
	 * @param startPhysicalAddress The start address of the segment in physical memory.
	 * @param startVirtualAddress  The start address of the segment in virtual memory.
	 * @param size                 The size of this segment.
	 * @param content              The contents to copy to this segment.
	 */
	public MemorySegment(long startPhysicalAddress, long startVirtualAddress, long size, byte[][] content) {
		this.startPhysicalAddress = startPhysicalAddress;
		this.startVirtualAddress = startVirtualAddress;
		this.size = size;
		this.content = BigArrays.copy(content, 0, size);
		try {
			this.hash = DigestUtils.digest(DIGEST, this.getInputStream());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Create a new segment which reads data from an Input Stream.
	 *
	 * @param startPhysicalAddress The start address of this segment in actual memory.
	 * @param startVirtualAddress  The start address of the segment in virtual memory.
	 * @param size                 The size of this segment.
	 * @param input                The stream to write to this segment.
	 * @throws IOException If any read error occurs while reading.
	 */
	public MemorySegment(long startPhysicalAddress, long startVirtualAddress, long size, @NotNull InputStream input) throws IOException {
		this.startPhysicalAddress = startPhysicalAddress;
		this.startVirtualAddress = startVirtualAddress;
		this.size = size;
		this.content = ByteBigArrays.newBigArray(size);
		int bufferSize = Integer.MAX_VALUE;
		if (size < bufferSize) bufferSize = (int) size;
		byte[] buffer = new byte[bufferSize];
		int read;
		long position = 0;
		do {
			read = input.read(buffer);
			BigArrays.copyToBig(buffer, 0, this.content, position, read);
			position = position + read;
		} while (read != -1 && position < size);
		try {
			this.hash = DigestUtils.digest(DIGEST, this.getInputStream());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Set the hash algorithm to use for segment comparison.
	 *
	 * @param digest The digest to use.
	 */
	public static void setHashAlgorithm(@NotNull MessageDigest digest) {
		DIGEST = digest;
	}

	/**
	 * Returns the address where this segment starts in physical memory.
	 * If this is the same as the virtual address,
	 * this means QEMU does not resolve virtual addresses for the used architecture and this is not actually the physical address.
	 *
	 * @return The address where this segment starts.
	 */
	public long getStartPhysicalAddress() {
		return startPhysicalAddress;
	}

	/**
	 * Returns the supposed virtual address.
	 * If this is the same value as the physical address,
	 * it means QEMU does not resolve virtual into physical addresses.
	 *
	 * @return The first virtual address of this segment.
	 */
	public long getStartVirtualAddress() {
		return startVirtualAddress;
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
	public byte @NotNull [] @NotNull [] getContent() {
		return BigArrays.copy(this.content);
	}

	/**
	 * Returns a byte from an actual given address.
	 *
	 * @param address The address to read from.
	 * @return The byte at given position.
	 */
	public byte getByAddress(long address) {
		return BigArrays.get(this.content, address - this.startPhysicalAddress);
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

	/**
	 * Return the hash of this memory region according to the provided digest.
	 *
	 * @return The hash of this segment.
	 */
	public byte[] getHash() {
		return Arrays.copyOf(hash, hash.length);
	}

	/**
	 * Returns an input stream to the contents of this segment.
	 * The returned input stream is not guaranteed to be thread-safe.
	 *
	 * @return InputStream to the contents of this segment.
	 */
	@NotNull
	public InputStream getInputStream() {
		return new BigByteArrayInputStream(this.content);
	}

	@Override
	public @NotNull JSONObject toJSON() {
		String hash = Base64.getEncoder().encodeToString(this.hash);
		JSONObject result = new JSONObject();
		result.put("startPhysicalAddress", startPhysicalAddress);
		result.put("startVirtualAddress", startVirtualAddress);
		result.put("size", size);
		result.put("hash", hash);
		return result;
	}
}
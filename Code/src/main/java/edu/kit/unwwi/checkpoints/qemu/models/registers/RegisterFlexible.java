package edu.kit.unwwi.checkpoints.qemu.models.registers;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * A class representing a register of any size.
 */
public class RegisterFlexible extends Register {

	/**
	 * The number of bits this register holds.
	 */
	private final int size;
	/**
	 * The byte array storing the value.
	 */
	private final byte[] contents;

	/**
	 * The constructor to create a register from bytes.
	 * Bounds checks are conducted somewhat.
	 *
	 * @param name    The name of the register.
	 * @param size    The number of bits in this register.
	 * @param content The bytes representing the content of this register.
	 * @throws IllegalArgumentException If the input array is larger than what's specified by size.
	 */
	public RegisterFlexible(String name, int size, byte... content) throws IllegalArgumentException {
		super(name);
		this.size = size;
		int byteSize = Math.ceilDiv(size, 8);
		if (byteSize < content.length)
			throw new IllegalArgumentException("Content does not fit in specified register width");
		this.contents = new byte[byteSize];
		System.arraycopy(content, 0, this.contents, byteSize - content.length, content.length);
	}

	/**
	 * The constructor to create a register from bytes.
	 * Bounds checks are conducted somewhat.
	 *
	 * @param name           The name of the register.
	 * @param size           The number of bits in this register.
	 * @param content        The bytes representing the content of this register.
	 * @param registerNumber The number of this register to be used when ordering same-name registers.
	 * @throws IllegalArgumentException If the input array is larger than what's specified by size.
	 */
	public RegisterFlexible(String name, int size, int registerNumber, byte... content) throws IllegalArgumentException {
		super(name, registerNumber);
		this.size = size;
		int byteSize = Math.ceilDiv(size, 8);
		if (byteSize < content.length)
			throw new IllegalArgumentException("Content does not fit in specified register width");
		this.contents = new byte[byteSize];
		System.arraycopy(content, 0, this.contents, byteSize - content.length, content.length);
	}

	/**
	 * Create a new Register from an int.
	 * Bounds checks are conducted.
	 *
	 * @param name    The name of the register.
	 * @param size    The number of bits this register holds.
	 * @param content The int representing the contents.
	 * @throws IllegalArgumentException If the value of the int would not fit in such an array.
	 */
	public RegisterFlexible(String name, int size, int content) throws IllegalArgumentException {
		super(name);
		this.size = size;
		if (content >= (1 << (size + 1)) || (size < 32 && content < 0))
			throw new IllegalArgumentException("Content does not fit in specified register width");
		int byteSize = Math.ceilDiv(size, 8);
		switch (byteSize) {
			case 0 -> throw new IllegalArgumentException("Register width mustn't be zero");
			case 1 -> this.contents = new byte[]{(byte) content};
			case 2 -> this.contents = new byte[]{(byte) (content >>> 8), (byte) content};
			case 3 -> this.contents = new byte[]{(byte) (content >>> 16), (byte) (content >>> 8), (byte) content};
			case 4 ->
					this.contents = new byte[]{(byte) (content >>> 24), (byte) (content >>> 16), (byte) (content >>> 8), (byte) content};
			default -> {
				this.contents = new byte[byteSize];
				this.contents[this.contents.length - 1] = (byte) content;
				this.contents[this.contents.length - 2] = (byte) ((content >>> 8));
				this.contents[this.contents.length - 3] = (byte) ((content >>> 16));
				this.contents[this.contents.length - 4] = (byte) ((content >>> 24));
			}
		}
	}

	/**
	 * Create a new Register from an int.
	 * Bounds checks are conducted.
	 *
	 * @param name           The name of the register.
	 * @param size           The number of bits this register holds.
	 * @param content        The int representing the contents.
	 * @param registerNumber The number of this register to be used when ordering same-name registers.
	 * @throws IllegalArgumentException If the value of the int would not fit in such an array.
	 */
	public RegisterFlexible(String name, int size, int registerNumber, int content) throws IllegalArgumentException {
		super(name, registerNumber);
		this.size = size;
		if (content >= (1 << (size + 1)) || (size < 32 && content < 0))
			throw new IllegalArgumentException("Content does not fit in specified register width");
		int byteSize = Math.ceilDiv(size, 8);
		switch (byteSize) {
			case 0 -> throw new IllegalArgumentException("Register width mustn't be zero");
			case 1 -> this.contents = new byte[]{(byte) content};
			case 2 -> this.contents = new byte[]{(byte) (content >>> 8), (byte) content};
			case 3 -> this.contents = new byte[]{(byte) (content >>> 16), (byte) (content >>> 8), (byte) content};
			case 4 ->
					this.contents = new byte[]{(byte) (content >>> 24), (byte) (content >>> 16), (byte) (content >>> 8), (byte) content};
			default -> {
				this.contents = new byte[byteSize];
				this.contents[this.contents.length - 1] = (byte) content;
				this.contents[this.contents.length - 2] = (byte) ((content >>> 8));
				this.contents[this.contents.length - 3] = (byte) ((content >>> 16));
				this.contents[this.contents.length - 4] = (byte) ((content >>> 24));
			}
		}
	}

	/**
	 * Create a new array from a long.
	 * Bounds checks are conducted.
	 *
	 * @param name    The name of the register.
	 * @param size    The size of the register in bits.
	 * @param content The long representing the content of the register.
	 * @throws IllegalArgumentException When the value of the long would not fit in such a register.
	 */
	public RegisterFlexible(String name, int size, long content) throws IllegalArgumentException {
		super(name);
		this.size = size;
		if (content >= (1L << (size + 1)) || (size < 64 && content < 0))
			throw new IllegalArgumentException("Content does not fit in specified register width");
		int byteSize = Math.ceilDiv(size, 8);
		this.contents = new byte[byteSize];
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.putLong(0, content);
		int start = 8 - byteSize;
		buffer.position(start);
		for (int i = 0; i < size; i++) {
			this.contents[this.contents.length - 8 + i] = buffer.get();
		}
	}

	/**
	 * Create a new array from a long.
	 * Bounds checks are conducted.
	 *
	 * @param name           The name of the register.
	 * @param size           The size of the register in bits.
	 * @param content        The long representing the content of the register.
	 * @param registerNumber The number of this register to be used when ordering same-name registers.
	 * @throws IllegalArgumentException When the value of the long would not fit in such a register.
	 */
	public RegisterFlexible(String name, int size, int registerNumber, long content) throws IllegalArgumentException {
		super(name, registerNumber);
		this.size = size;
		if (content >= (1L << (size + 1)) || (size < 64 && content < 0))
			throw new IllegalArgumentException("Content does not fit in specified register width");
		int byteSize = Math.ceilDiv(size, 8);
		this.contents = new byte[byteSize];
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.putLong(0, content);
		int start = 8 - byteSize;
		buffer.position(start);
		for (int i = 0; i < size; i++) {
			this.contents[this.contents.length - 8 + i] = buffer.get();
		}
	}

	/**
	 * The contents of this register as a byte array.3
	 * Values get returned in big-endian order
	 *
	 * @return The contents of this register.
	 */
	@Override
	public byte[] contents() {
		return Arrays.copyOf(contents, contents.length);
	}

	/**
	 * The size in bits of this register.
	 *
	 * @return The size in bits of this register.
	 */
	@Override
	public int size() {
		return this.size;
	}

	/**
	 * Formats the contents of this register to a Hex-String that gets correctly padded to the length of the array.
	 *
	 * @return The contents of this register as hex.
	 */
	@Override
	public String toHexString() {
		boolean first = (size % 8) >= 4;
		StringBuilder builder = new StringBuilder(Math.ceilDiv(this.contents.length, 4) + 2);
		builder.append("0x");
		for (byte current : this.contents) {
			if (first) {
				builder.append(String.format("%1$01x", current));
				first = false;
			} else builder.append(String.format("%1$02x", current));
		}
		return builder.toString();
	}
}

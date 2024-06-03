package edu.kit.unwwi.checkpoints.qemu.models.registers;

import java.nio.ByteBuffer;

/**
 * A class representing an 80-bit register.
 * These registers usually get used for Floating-Point numbers.
 */
public class Register80Bit extends Register {

	/**
	 * The first 64bit of the register.
	 * These are usually the relevant part and get kept in memory.
	 */
	private final long base;
	/**
	 * The latter 16bit of the register.
	 * These often get discarded when writing the value to memory.
	 */
	private final short extension;

	/**
	 * Create a new 80-bit register from its name, the base and the extension.
	 *
	 * @param name      The name of the register.
	 * @param base      The first 64 bit of the register.
	 * @param extension The latter 16 bit of the register.
	 */
	public Register80Bit(String name, long base, short extension) {
		super(name);
		this.base = base;
		this.extension = extension;
	}

	/**
	 * Create a new 80-bit register from its name and its content as a byte array.
	 *
	 * @param name    The name of the register.
	 * @param content The bytes of this array. Length must be 10.
	 */
	public Register80Bit(String name, byte[] content) {
		super(name);
		assert content.length == 10;
		ByteBuffer buffer = ByteBuffer.wrap(content);
		base = buffer.getLong();
		extension = buffer.getShort();
	}

	/**
	 * Create a new 80 bit register from its name, base and extension as an int.3
	 * The bounds of the int get checked.
	 *
	 * @param name      The name of the register.
	 * @param base      The first 64 bit of the register.
	 * @param extension The last 16 bit of the register as an int.
	 * @throws IllegalArgumentException When the int is out of bounds.
	 */
	public Register80Bit(String name, long base, int extension) throws IllegalArgumentException {
		super(name);
		this.base = base;
		if (extension >= (1 << 17)) throw new IllegalArgumentException("Extension is too big for this register");
		else this.extension = (short) extension;
	}

	/**
	 * The contents of this register as a byte array.3
	 * Values get returned in big-endian order
	 *
	 * @return The contents of this register.
	 */
	@Override
	public byte[] contents() {
		ByteBuffer buffer = ByteBuffer.allocate(10);
		buffer.putLong(base);
		buffer.putShort(extension);
		return buffer.array();
	}

	/**
	 * The size in bits of this register.
	 *
	 * @return The size in bits of this register.
	 */
	@Override
	public int size() {
		return 80;
	}

	/**
	 * Formats the contents of this register to a Hex-String that gets correctly padded to the length of the array.
	 *
	 * @return The contents of this register as hex.
	 */
	@Override
	public String toHexString() {
		return String.format("0x%1$016x%2$04x", base, extension);
	}
}

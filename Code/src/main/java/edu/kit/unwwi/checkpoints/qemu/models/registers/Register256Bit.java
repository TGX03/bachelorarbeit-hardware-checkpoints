package edu.kit.unwwi.checkpoints.qemu.models.registers;

import java.nio.ByteBuffer;

/**
 * A class representing a 256-bit register.
 */
public class Register256Bit extends Register {

	/**
	 * The upper 64 bit of the register.
	 */
	private final long upper;
	/**
	 * The second quarter of the register.
	 */
	private final long middleUpper;
	/**
	 * The third quarter of the register.
	 */
	private final long middleLower;
	/**
	 * The last 64bit of the register.
	 */
	private final long lower;

	/**
	 * Create a new 256-bit register.
	 *
	 * @param name   The name of the register.
	 * @param values The longs representing the contents of the register.
	 */
	public Register256Bit(String name, long... values) {
		super(name);
		assert values.length == 4;
		upper = values[0];
		middleUpper = values[1];
		middleLower = values[2];
		lower = values[3];
	}

	/**
	 * The contents of this register as a byte array.3
	 * Values get returned in big-endian order
	 *
	 * @return The contents of this register.
	 */
	@Override
	public byte[] contents() {
		ByteBuffer buf = ByteBuffer.allocate(32);
		buf.putLong(upper);
		buf.putLong(middleUpper);
		buf.putLong(middleLower);
		buf.putLong(lower);
		return buf.array();
	}

	/**
	 * The size in bits of this register.
	 *
	 * @return The size in bits of this register.
	 */
	@Override
	public int size() {
		return 256;
	}

	/**
	 * Formats the contents of this register to a Hex-String that gets correctly padded to the length of the array.
	 *
	 * @return The contents of this register as hex.
	 */
	@Override
	public String toHexString() {
		return String.format("0x%1$016x%2$016x%3$016x%4$016x", upper, middleUpper, middleLower, lower);
	}
}

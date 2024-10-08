package edu.kit.unwwi.checkpoints.qemu.models.registers;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * A class representing a 512-bit register.
 */
public class Register512Bit extends Register {

	/**
	 * The array holding the longs representing the register.
	 */
	private final long[] contents = new long[8];

	/**
	 * Creates a new 512-bit Register from 8 longs
	 *
	 * @param name   The name of the register.
	 * @param values The 8 longs representing the register.
	 */
	public Register512Bit(String name, long @NotNull ... values) {
		super(name);
		assert values.length == 8;
		System.arraycopy(values, 0, contents, 0, 8);
	}

	/**
	 * The contents of this register as a byte array.3
	 * Values get returned in big-endian order
	 *
	 * @return The contents of this register.
	 */
	@Override
	public byte @NotNull [] contents() {
		ByteBuffer buffer = ByteBuffer.allocate(64);
		for (long current : contents) buffer.putLong(current);
		return buffer.array();
	}

	/**
	 * The size in bits of this register.
	 *
	 * @return The size in bits of this register.
	 */
	@Override
	public int size() {
		return 512;
	}

	/**
	 * Formats the contents of this register to a Hex-String that gets correctly padded to the length of the array.
	 *
	 * @return The contents of this register as hex.
	 */
	@Override
	public @NotNull String toHexString() {
		StringBuilder builder = new StringBuilder(128);
		for (long current : contents) builder.append(String.format("0x%1$016x", current));
		return builder.toString();
	}
}

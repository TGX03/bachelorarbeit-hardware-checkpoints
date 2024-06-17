package edu.kit.unwwi.checkpoints.qemu.models.registers;

import org.jetbrains.annotations.NotNull;

/**
 * A class representing a 32-bit register.
 */
public class Register32Bit extends Register {

	/**
	 * The contents of this register.
	 */
	private final int contents;

	/**
	 * Create a new 32-bit register from the given value.
	 *
	 * @param name           The name of the register.
	 * @param content        The content of the new register.
	 * @param registerNumber The number of this register to be used when ordering same-name registers.
	 */
	public Register32Bit(@NotNull String name, int content, int registerNumber) {
		super(name, registerNumber);
		this.contents = content;
	}

	/**
	 * The contents of this register as a byte array.
	 * Values get returned in big-endian order
	 *
	 * @return The contents of this register.
	 */
	@Override
	public byte @NotNull [] contents() {
		return new byte[]{(byte) (contents >>> 24), (byte) (contents >>> 16), (byte) (contents >>> 8), (byte) contents};
	}

	/**
	 * The size in bits of this register.
	 *
	 * @return The size in bits of this register.
	 */
	@Override
	public int size() {
		return 32;
	}

	/**
	 * Formats the contents of this register to a Hex-String that gets correctly padded to the length of the array.
	 *
	 * @return The contents of this register as hex.
	 */
	@Override
	@NotNull
	public String toHexString() {
		return String.format("0x%1$08x", contents);
	}
}

package edu.kit.unwwi.checkpoints.qemu.models.registers;

import org.jetbrains.annotations.NotNull;

/**
 * A class representing an 8bit Register
 */
public class Register8Bit extends Register {

	/**
	 * The contents of this register.
	 */
	private final byte contents;

	/**
	 * Create a new 8-bit register with the given value.
	 *
	 * @param name           The name of the register.
	 * @param content        The content of this register.
	 * @param registerNumber The number of this register to be used when ordering same-name registers.
	 */
	public Register8Bit(@NotNull String name, byte content, int registerNumber) {
		super(name, registerNumber);
		this.contents = content;
	}

	/**
	 * Create a new 8-bit Register with the given value.
	 * Make sure the int actually fits in 8 bits.
	 *
	 * @param name           The name of the register.
	 * @param content        The value of this register.
	 * @param registerNumber The number of this register to be used when ordering same-name registers.
	 * @throws IllegalArgumentException Gets thrown when the value does not fit in this register.
	 */
	public Register8Bit(@NotNull String name, int content, int registerNumber) throws IllegalArgumentException {
		super(name, registerNumber);
		if (content >= (1 << 9) || content < 0)
			throw new IllegalArgumentException("Value too big for an 8bit Register");
		this.contents = (byte) content;
	}

	/**
	 * The contents of this register as a byte array.
	 *
	 * @return The contents of this register.
	 */
	@Override
	public byte @NotNull [] contents() {
		return new byte[]{contents};
	}

	/**
	 * The size in bits of this register.
	 *
	 * @return The size in bits of this register.
	 */
	@Override
	public int size() {
		return 8;
	}

	/**
	 * The content of this register.
	 *
	 * @return The content of this register.
	 */
	public byte getContents() {
		return contents;
	}

	/**
	 * Formats the contents of this register to a Hex-String that gets correctly padded to the length of the array.
	 *
	 * @return The contents of this register as hex.
	 */
	@Override
	@NotNull
	public String toHexString() {
		return String.format("0x%1$02x", contents);
	}
}

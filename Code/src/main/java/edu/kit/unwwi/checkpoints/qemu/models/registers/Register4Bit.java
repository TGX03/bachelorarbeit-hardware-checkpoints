package edu.kit.unwwi.checkpoints.qemu.models.registers;

/**
 * A class for representing a 4bit register.
 */
public class Register4Bit extends Register {

	/**
	 * The content of this register.
	 */
	private final byte contents;

	/**
	 * Create a new register.
	 *
	 * @param name           The name of this register.
	 * @param content        The content of this register as a byte.
	 * @param registerNumber The number of this register to be used when ordering same-name registers.
	 * @throws IllegalArgumentException When the provided value does not fit into 4 bit, as bytes are the smallest unit in Java.
	 */
	public Register4Bit(String name, byte content, int registerNumber) throws IllegalArgumentException {
		super(name, registerNumber);
		if (content >= (1 << 5) || content < 0) throw new IllegalArgumentException("Value too big for register");
		this.contents = content;
	}

	/**
	 * Create a new register.
	 *
	 * @param name           The name of this register.
	 * @param content        The content of this register as a byte.
	 * @param registerNumber The number of this register to be used when ordering same-name registers.
	 * @throws IllegalArgumentException When the provided value does not fit into 4 bit.
	 */
	public Register4Bit(String name, int content, int registerNumber) throws IllegalArgumentException {
		super(name, registerNumber);
		if (content >= (1 << 5) || content < 0) throw new IllegalArgumentException("Value too big for register");
		this.contents = (byte) content;
	}

	/**
	 * Returns the contents of this register as a byte.
	 * @return The contents of this register.
	 */
	public byte getContents() {
		return this.contents;
	}

	/**
	 * The contents of this register as a byte array.3
	 * Values get returned in big-endian order
	 *
	 * @return The contents of this register.
	 */
	@Override
	public byte[] contents() {
		return new byte[]{contents};
	}

	/**
	 * The size in bits of this register.
	 *
	 * @return The size in bits of this register.
	 */
	@Override
	public int size() {
		return 4;
	}

	/**
	 * Formats the contents of this register to a Hex-String that gets correctly padded to the length of the array.
	 *
	 * @return The contents of this register as hex.
	 */
	@Override
	public String toHexString() {
		return String.format("0x%1$01x", contents);
	}
}

package edu.kit.unwwi.checkpoints.qemu.models.registers;

/**
 * A class representing a 32-bit register.
 */
public class Register32Bit extends Register{

	/**
	 * The contents of this register.
	 */
	private final int contents;

	/**
	 * Create a new 32-bit register from the given value.
	 * @param name The name of the register.
	 * @param content The content of the new register.
	 */
	public Register32Bit(String name, int content) {
		super(name);
		this.contents = content;
	}

	/**
	 * The contents of this register as a byte array.
	 * Values get returned in big-endian order
	 *
	 * @return The contents of this register.
	 */
	@Override
	public byte[] contents() {
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

	@Override
	public String toString() {
		return name + ": " + String.format("0x%1$08x", contents);
	}
}

package edu.kit.unwwi.checkpoints.qemu.models.registers;

/**
 * A class representing a 64-bit register.
 */
public class Register64Bit extends Register {

	/**
	 * The contents of this register.
	 */
	private final long contents;

	/**
	 * Create a new 64-bit register from the given value.
	 * @param name The name of the register.
	 * @param content The content of the new register.
	 */
	public Register64Bit(String name, long content) {
		super(name);
		this.contents = content;
	}

	/**
	 * The contents of this register as a byte array.3
	 * Values get returned in big-endian order
	 *
	 * @return The contents of this register.
	 */
	@Override
	public byte[] contents() {
		return new byte[]{(byte) (contents >>> 56), (byte) (contents >>> 48), (byte) (contents >>> 40), (byte) (contents >>> 32), (byte) (contents >>> 24), (byte) (contents >>> 16), (byte) (contents >>> 8), (byte) contents};
	}

	/**
	 * The size in bits of this register.
	 *
	 * @return The size in bits of this register.
	 */
	@Override
	public int size() {
		return 64;
	}

	/**
	 * Formats the contents of this register to a Hex-String that gets correctly padded to the length of the array.
	 *
	 * @return The contents of this register as hex.
	 */
	@Override
	public String toHexString() {
		return String.format("0x%1$016x", contents);
	}
}

package edu.kit.unwwi.checkpoints.qemu.models.registers;

/**
 * A class representing a 16-bit register.
 */
public class Register16Bit extends Register {

	/**
	 * The contents of this register.
	 */
	private final short contents;

	/**
	 * Create a new 16-bit register from the given value.
	 *
	 * @param name    The name of the register.
	 * @param content The content of the new register.
	 * @param registerNumber The number of this register to be used when ordering same-name registers.
	 */
	public Register16Bit(String name, short content, int registerNumber) {
		super(name, registerNumber);
		this.contents = content;
	}

	/**
	 * Create a new 16-bit integer from the given value.
	 * Make sure the value fits in 16 bits.
	 *
	 * @param name    The name of the register.
	 * @param content The content of the new register.
	 * @param registerNumber The number of this register to be used when ordering same-name registers.
	 * @throws IllegalArgumentException If the value does not fit.
	 */
	public Register16Bit(String name, int content, int registerNumber) throws IllegalArgumentException {
		super(name, registerNumber);
		if (content >= (1 << 17) || content < 0) throw new IllegalArgumentException("Value does not fit in 16 bit");
		this.contents = (short) content;
	}

	/**
	 * The contents of this register.
	 *
	 * @return The contents of this register.
	 */
	public short getContents() {
		return this.contents;
	}

	/**
	 * The contents of this register as a byte array.
	 * Values get returned in big-endian order
	 *
	 * @return The contents of this register.
	 */
	@Override
	public byte[] contents() {
		return new byte[]{(byte) (contents >>> 8), (byte) contents};
	}

	/**
	 * The size in bits of this register.
	 *
	 * @return The size in bits of this register.
	 */
	@Override
	public int size() {
		return 16;
	}

	/**
	 * Formats the contents of this register to a Hex-String that gets correctly padded to the length of the array.
	 *
	 * @return The contents of this register as hex.
	 */
	@Override
	public String toHexString() {
		return String.format("0x%1$04x", contents);
	}
}

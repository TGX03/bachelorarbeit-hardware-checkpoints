package edu.kit.unwwi.checkpoints.qemu.models.registers;

/**
 * A class representing a 128-bit register.
 */
public class Register128Bit extends Register {

	/**
	 * The upper 64 bits of the 128 bits
	 */
	private final long upper;
	/**
	 * The lower 64 bits of the 128 bits
	 */
	private final long lower;

	/**
	 * Create a new 128 bit Register.
	 * Expects exactly 2 longs.
	 * @param name The name of the register.
	 * @param values The 2 longs representing the value.
	 */
	public Register128Bit(String name, long ...values) {
		super(name);
		this.upper = values[0];
		this.lower = values[1];
	}


	/**
	 * The contents of this register as a byte array.3
	 * Values get returned in big-endian order
	 *
	 * @return The contents of this register.
	 */
	@Override
	public byte[] contents() {
		byte[] out = new byte[16];
		for (int i = 0; i < 8; i++) {
			out[i] = (byte) (upper >>> (i * 8));
		}
		for (int i = 0; i < 8; i++) {
			out[i + 8] = (byte) (lower >>> (i * 8));
		}
		return out;
	}

	/**
	 * The size in bits of this register.
	 *
	 * @return The size in bits of this register.
	 */
	@Override
	public int size() {
		return 128;
	}

	/**
	 * Formats the contents of this register to a Hex-String that gets correctly padded to the length of the array.
	 *
	 * @return The contents of this register as hex.
	 */
	@Override
	public String toHexString() {
		return String.format("0x%1$016x%2$016x", upper, lower);
	}
}

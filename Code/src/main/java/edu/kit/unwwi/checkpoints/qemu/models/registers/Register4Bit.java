package edu.kit.unwwi.checkpoints.qemu.models.registers;

public class Register4Bit extends Register{

	private final byte contents;

	public Register4Bit(String name, byte content) {
		super(name);
		if (content >= (1 << 5)) throw new IllegalArgumentException("Value too big for register");
		this.contents = content;
	}

	public Register4Bit(String name, int content) {
		super(name);
		if (content >= (1 << 5)) throw new IllegalArgumentException("Value too big for register");
		this.contents = (byte) content;
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
		return 0;
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

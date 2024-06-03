package edu.kit.unwwi.checkpoints.qemu.models.registers;

/**
 * A class representing a single flag register.
 */
public class FlagRegister extends Register{

	/**
	 * The value of this register.
	 */
	private final boolean flag;

	/**
	 * Create a new register from a boolean value.
	 * @param name The name of the register.
	 * @param content The value of this register as a boolean.
	 */
	public FlagRegister(String name, boolean content) {
		super(name);
		this.flag = content;
	}

	/**
	 * Create a new register from its name and its content as an int.
	 * Currently, bounds checks are done, but maybe that will change.
	 * @param name The name of the register.
	 * @param content The content of the register.
	 * @throws IllegalArgumentException When the value is out of bounds.
	 */
	public FlagRegister(String name, int content) throws IllegalArgumentException {
		super(name);
		if (content == 0) flag = false;
		else if (content == 1) flag = true;
		else throw new IllegalArgumentException("Value out of bounds"); //TODO: In C any value not equal to 0 is true, maybe use that variant instead.
	}

	/**
	 * The contents of this register as a byte array.3
	 * Values get returned in big-endian order
	 *
	 * @return The contents of this register.
	 */
	@Override
	public byte[] contents() {
		if (flag) return new byte[]{(byte) 1};
		else return new byte[]{(byte) 0};
	}

	/**
	 * The size in bits of this register.
	 *
	 * @return The size in bits of this register.
	 */
	@Override
	public int size() {
		return 1;
	}

	/**
	 * Formats the contents of this register to a Hex-String that gets correctly padded to the length of the array.
	 *
	 * @return The contents of this register as hex.
	 */
	@Override
	public String toHexString() {
		if (flag) return "0x1";
		else return "0x0";
	}
}

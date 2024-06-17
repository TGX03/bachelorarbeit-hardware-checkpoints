package edu.kit.unwwi.checkpoints.qemu.models.registers;

import java.io.Serializable;

/**
 * A class representing a register, which's contents can be queried.
 */
public abstract class Register implements Serializable, Comparable<Register> {

	/**
	 * The name of this register.
	 */
	protected final String name;
	private final int registerNumber;

	/**
	 * Create a new register.
	 *
	 * @param name The name of the new register.
	 */
	public Register(String name) {
		this.name = name;
		this.registerNumber = -1;
	}

	/**
	 * Create a new register
	 * A register includes a hidden value which can later on be used to order registers which have the same name,
	 * e.g. the DPL-Registers of x86.
	 *
	 * @param name           The name of the new register.
	 * @param registerNumber The number which can be used to order registers of the same name.
	 */
	public Register(String name, int registerNumber) {
		this.name = name;
		this.registerNumber = registerNumber;
	}

	/**
	 * The contents of this register as a byte array.3
	 * Values get returned in big-endian order
	 *
	 * @return The contents of this register.
	 */
	public abstract byte[] contents();

	/**
	 * The size in bits of this register.
	 *
	 * @return The size in bits of this register.
	 */
	public abstract int size();

	@Override
	public final String toString() {
		return name + ": " + toHexString();
	}

	/**
	 * Formats the contents of this register to a Hex-String that gets correctly padded to the length of the array.
	 *
	 * @return The contents of this register as hex.
	 */
	public abstract String toHexString();

	/**
	 * Allows for 2 registers with the same name to be put in some order, so that registers with the same name can later be restored in the correct order.
	 *
	 * @param register The other register to compare against.
	 * @return Which register comes first.
	 * @throws IllegalArgumentException When the registers do not have the same name and can therefore not be put into any meaningful order.
	 * @throws IllegalStateException    When either register doesn't have enough date to be ordered.
	 */
	@Override
	public int compareTo(Register register) throws IllegalArgumentException, IllegalStateException {
		if (!register.name.equals(this.name)) throw new IllegalArgumentException("Registers do not have the same name");
		else if (this.registerNumber == -1 || register.registerNumber == -1)
			throw new IllegalStateException("One of the registers was not assigned an order");
		else return this.registerNumber - register.registerNumber;
	}
}

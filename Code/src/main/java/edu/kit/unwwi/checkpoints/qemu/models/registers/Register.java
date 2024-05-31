package edu.kit.unwwi.checkpoints.qemu.models.registers;

import java.io.Serializable;

/**
 * A class representing a register, which's contents can be queried.
 */
public abstract class Register implements Serializable {

	/**
	 * The name of this register.
	 */
	protected final String name;

	/**
	 * Create a new register.
	 * @param name The name of the new register.
	 */
	public Register(String name) {
		this.name = name;
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
	public String toString() {
		return name;
	}
}

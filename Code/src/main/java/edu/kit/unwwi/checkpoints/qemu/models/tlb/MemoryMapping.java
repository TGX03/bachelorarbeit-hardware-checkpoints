package edu.kit.unwwi.checkpoints.qemu.models.tlb;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A class representing a single memory mapping inside the TLB.
 * The equals- and hashCode-methods are create in a way to allow for searching through a hashtable by the virtual address,
 * meaning, when testing for equality, only the virtual address gets tested.
 */
public class MemoryMapping implements Comparable<MemoryMapping>, Serializable {

	/**
	 * The starting address of the virtual page of this mapping.
	 */
	final long virtualAddress;
	/**
	 * The starting address of the physical frame of this mapping.
	 */
	private final long physicalAddress;
	/**
	 * How large this page/frame is.
	 */
	private final long size;
	/**
	 * The flags set for this region.
	 */
	private final char[] flags; // TODO: What the fuck does this actually do?

	/**
	 * Creates a new memory mapping from the provided data.
	 *
	 * @param virtualAddress  The starting address of the virtual page of this mapping.
	 * @param physicalAddress The starting address of the physical frame of this mapping.
	 * @param size            The size of the mapped space.
	 * @param flags           The flags set for this space.
	 */
	public MemoryMapping(long virtualAddress, long physicalAddress, long size, char[] flags) {
		this.virtualAddress = virtualAddress;
		this.physicalAddress = physicalAddress;
		this.size = size;
		this.flags = flags;
		for (int i = 0; i < flags.length; i++) {
			if (flags[i] == '-') flags[i] = 0;
		}
	}

	/**
	 * Test whether this mapping contains the provided address.
	 *
	 * @param address The address to check.
	 * @return Whether it's in this memory space.
	 */
	public boolean containsAddress(long address) {
		return address >= virtualAddress && address < virtualAddress + size;
	}

	/**
	 * Converts a virtual address to a physical address.
	 *
	 * @param address The virtual address.
	 * @return The physical address.
	 */
	public long translateAddress(long address) {
		if (!containsAddress(address))
			throw new IllegalArgumentException("Mapping does not contain the provided address");
		return (address - virtualAddress) + physicalAddress;
	}

	/**
	 * Check whether a flag is set.
	 *
	 * @param flag The flag to test.
	 * @return Whether it's set.
	 */
	public boolean flagSet(char flag) {
		for (char current : flags) if (current == flag) return true;
		return false;
	}

	/**
	 * Return all flags set for this mapping.
	 * A 0-char means the flag at that position is not set.
	 *
	 * @return The flags set for this mapping.
	 */
	public char[] getFlags() {
		return Arrays.copyOf(flags, flags.length);
	}

	@Override
	public int compareTo(@NotNull MemoryMapping mapping) {
		return Long.compare(this.virtualAddress, mapping.virtualAddress);
	}

	@Override
	public String toString() {
		return virtualAddress + ": " + physicalAddress + "[" + new String(flags).replaceAll("\0", "-") + "]";
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.virtualAddress);
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (o instanceof MemoryMapping m) return this.virtualAddress == m.virtualAddress;
		else return false;
	}
}

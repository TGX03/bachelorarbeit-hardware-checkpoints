package edu.kit.unwwi.checkpoints.qemu.models.memory;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

/**
 * A class representing a TLB able to convert addresses from the guest memory space to the QEMU-internal memory space.
 */
public class TLB implements Serializable {

	/**
	 * The map used for finding the memory mappings.
	 */
	private final HashMap<Long, MemoryMapping> mappings = new HashMap<>();

	/**
	 * Create a new TLB from the mappings provided.
	 *
	 * @param mappings The mappings.
	 */
	public TLB(@NotNull MemoryMapping... mappings) {
		for (MemoryMapping mapping : mappings) this.mappings.put(mapping.virtualAddress, mapping);
	}

	/**
	 * Translates a virtual address from the guest to a QEMU-internal address.
	 *
	 * @param address The virtual address to translate.
	 * @return The physical address.
	 * @throws IllegalArgumentException If this address is not contained in the TLB.
	 */
	public long translateAddress(final long address) throws IllegalArgumentException {
		long shifted = address;
		long shifter = -1;
		while (!mappings.containsKey(shifted) && shifter != 0) {    //TODO: This will always return the zero mapping if nothing gets found.
			shifter = shifter << 1;
			shifted = address & shifter;
		}
		return mappings.get(shifted).translateAddress(address);
	}

	/**
	 * Returns the current mappings of the TLB as an array.
	 *
	 * @return The current TLB mappings.
	 */
	@NotNull
	public MemoryMapping @NotNull [] getMappings() {
		MemoryMapping[] result = mappings.values().toArray(new MemoryMapping[0]);
		Arrays.sort(result);
		return result;
	}
}

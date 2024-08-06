package edu.kit.unwwi.checkpoints.qmp.commands.qhm;

import edu.kit.unwwi.checkpoints.qemu.models.memory.MemoryMapping;
import edu.kit.unwwi.checkpoints.qemu.models.memory.TLB;
import edu.kit.unwwi.checkpoints.qmp.commands.QHMCommand;
import org.jetbrains.annotations.NotNull;

import java.util.stream.IntStream;

/**
 * The command to query the QEMU-TLB and allow for finding of memory regions.
 */
public class QueryTLB extends QHMCommand {

	/**
	 * Storage for the result after the execution of the command.
	 */
	private TLB tlb;

	/**
	 * Return the TLB that was created from querying QEMU.
	 *
	 * @return The created TLB.
	 */
	@NotNull
	public TLB getResult() {
		if (executed) return tlb;
		else throw new IllegalStateException("Command has not been executed");
	}

	@Override
	protected @NotNull String commandName() {
		return "info tlb";
	}

	/**
	 * Used by subclasses to parse the received result.
	 *
	 * @param result The result to parse.
	 */
	@Override
	protected void receiveResult(@NotNull String result) {
		String[] mappings = ((String) result).split(System.lineSeparator());
		this.tlb = new TLB(IntStream.range(0, mappings.length).parallel().mapToObj(x -> {
			String[] split = mappings[x].split(" ");
			long virtualAddress = Long.parseUnsignedLong(split[0].replace(":", ""), 16);
			long physicalAddress = Long.parseUnsignedLong(split[1], 16);
			char[] flags = split[2].toCharArray();
			long size;
			if (x == mappings.length - 1) {
				size = virtualAddress - Long.parseUnsignedLong(mappings[x - 1].split(" ")[0].replace(":", ""), 16); // This is just a hack under the assumption that the size of this page is the same as before.
			} else {
				size = Long.parseUnsignedLong(mappings[x + 1].split(" ")[0].replace(":", ""), 16) - virtualAddress;
			}
			return new MemoryMapping(virtualAddress, physicalAddress, size, flags);
		}).toList().toArray(new MemoryMapping[0]));
	}
}

package edu.kit.unwwi.checkpoints.qmp.commands.qhm;

import edu.kit.unwwi.checkpoints.qmp.commands.QHMCommand;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Represents the pmemsave command of QEMU,
 * which dumps the contents of physical guest memory to disk.
 */
public class PMemsave extends QHMCommand {

	/**
	 * The first byte to dump.
	 */
	private final long startAddress;
	/**
	 * How many bytes to dump.
	 */
	private final long size;
	/**
	 * The path of the file to dump to.
	 */
	private final Path path;

	/**
	 * Create a new instance of a pmemsave command.
	 * @param address The first byte to dump.
	 * @param size How many bytes to dump.
	 * @param path The path of the file to dump to.
	 */
	public PMemsave(long address, long size, Path path) {
		this.startAddress = address;
		this.size = size;
		this.path = path;
	}

	@Override
	protected @NotNull String commandName() {
		return "pmemsave " + startAddress + " " + size + " " + path.toAbsolutePath();
	}

	@Override
	protected void receiveResult(@NotNull String result) {
		// ignored in hope the dump just works
	}
}
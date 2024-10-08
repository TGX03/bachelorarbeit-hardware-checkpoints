package edu.kit.unwwi.checkpoints.qmp.commands;

import edu.kit.unwwi.checkpoints.qmp.Command;
import org.jetbrains.annotations.NotNull;

/**
 * The command telling QEMU to continue execution.
 */
public class Continue implements Command {

	/**
	 * The instance to be used, as there is no reason to have multiple instances of this class.
	 */
	public static final Continue INSTANCE = new Continue();

	/**
	 * Private constructor because singleton.
	 */
	private Continue() {
	}

	@Override
	public @NotNull String toJson() {
		return "{ \"execute\": \"cont\" }";
	}
}

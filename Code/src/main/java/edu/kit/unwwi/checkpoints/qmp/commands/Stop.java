package edu.kit.unwwi.checkpoints.qmp.commands;

import edu.kit.unwwi.checkpoints.qmp.Command;
import org.jetbrains.annotations.NotNull;

/**
 * The command telling QEMU to pause execution.
 */
public class Stop extends Command {

	/**
	 * The instance to use as there is no reason to use multiple instances of this class.
	 */
	public static final Stop INSTANCE = new Stop();

	/**
	 * Private because singleton.
	 */
	private Stop() {
	}

	@Override
	protected @NotNull String toJson() {
		return "{ \"execute\": \"stop\" }";
	}

	@Override
	protected void processResult(@NotNull Object result) {
		assert "{}".equals(result);
	}
}

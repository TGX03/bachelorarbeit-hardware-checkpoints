package edu.kit.unwwi.checkpoints.qmp.commands;

import edu.kit.unwwi.checkpoints.qmp.Command;
import org.jetbrains.annotations.NotNull;

abstract class StatefulCommand implements Command {

	/**
	 * Boolean to make sure the command is only executed once.
	 */
	protected volatile boolean executed = false;

	@Override
	public void receiveResult(@NotNull Object result) {
		if (executed) throw new IllegalStateException("This command was already executed");
		else {
			processResult(result);
			executed = true;
		}
	}

	protected abstract void processResult(@NotNull Object result);
}

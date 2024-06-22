package edu.kit.unwwi.checkpoints.qmp;

import org.jetbrains.annotations.NotNull;

/**
 * An interface representing a command.
 * Such a command can be given to the interface to transmission and will then receive a result,
 * which each command then can parse by itself.
 */
public abstract class Command {

	/**
	 * Boolean to make sure the command is only executed once.
	 */
	protected volatile boolean executed = false;

	/**
	 * Turn this command into JSON which can then directly be sent to a running QMP server.
	 *
	 * @return JSON representation of this command.
	 */
	@NotNull
	protected abstract String toJson();

	/**
	 * This method takes the received JSON and then may or may not parse it in any way fit to its purpose.
	 * It may also fully discard it if not needed.
	 * Should probably not be used outside QMPInterface.
	 *
	 * @param result The result received from QEMU.
	 */
	void receiveResult(@NotNull Object result) {
		if (executed) throw new IllegalStateException("This command was already executed");
		else {
			processResult(result);
			executed = true;
		}
	}

	/**
	 * Used by subclasses to parse the received result.
	 *
	 * @param result The result to parse.
	 */
	protected abstract void processResult(@NotNull Object result);
}

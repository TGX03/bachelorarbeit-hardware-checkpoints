package edu.kit.unwwi.checkpoints.qmp;

import org.jetbrains.annotations.NotNull;

/**
 * An interface representing a command.
 * Such a command can be given to the interface to transmission and will then receive a result,
 * which each command then can parse by itself.
 */
public interface Command {

	/**
	 * Turn this command into JSON which can then directly be sent to a running QMP server.
	 *
	 * @return JSON representation of this command.
	 */
	@NotNull
	String toJson();

	/**
	 * This method takes the received JSON and then may or may not parse it in any way fit to its purpose.
	 * It may also fully discard it if not needed.
	 * Should probably not be used outside QMPInterface.
	 *
	 * @param result The result received from QEMU.
	 * @throws IllegalStateException Gets thrown when this command may only be executed once.
	 */
	default void receiveResult(@NotNull Object result) throws IllegalStateException {
		// Nothing ever happens
	}
}

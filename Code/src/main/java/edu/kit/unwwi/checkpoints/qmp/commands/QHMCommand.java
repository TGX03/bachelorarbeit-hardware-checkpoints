package edu.kit.unwwi.checkpoints.qmp.commands;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A generic class representing a QHM-command.
 * QHM commands have a different syntax for the command sent to the server, so this gets split.
 */
public abstract class QHMCommand extends StatefulCommand {

	@Override
	protected void processResult(@NotNull Object result) {
		assert result instanceof String;
		receiveResult((String) result);
	}

	@Override
	public @NotNull String toJson() {
		//return "{ \"execute\": \"human-monitor-command\", \"arguments\": { \"command-line\": \"" + commandName() +"\" }}";
		StringBuilder command = new StringBuilder();
		command.append("{ \"execute\": \"human-monitor-command\", \"arguments\\\": { \"command-line\": \"");
		command.append(commandName());
		for (Map.Entry<String, String> set : additionalArguments().entrySet()) {
			command.append(String.format(", \"%s\": \"%s\"", set.getKey(), set.getValue()));
		}
		command.append("\" }}");
		return command.toString();
	}

	/**
	 * The internal name to be sent to the QEMU instance for this command.
	 *
	 * @return The name of this QHM command.
	 */
	@NotNull
	protected abstract String commandName();

	/**
	 * If this command requires additional arguments,
	 * here a map of these argument can be supplied.
	 * Currently only arguments with additional specifiers are used,
	 * as I'm currently not aware for a need of others.
	 *
	 * @return A map of additional arguments to attach to the request.
	 */
	@NotNull
	protected Map<String, String> additionalArguments() {
		return Map.of();
	}

	protected abstract void receiveResult(@NotNull String result);
}

package edu.kit.unwwi.checkpoints.qmp.commands;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

/**
 * The command to query basic status information about the running VM.
 */
public class Status extends StatefulCommand {

	/**
	 * The current state of the VM.
	 */
	private State state;
	/**
	 * Whether the VM is currently running in Single-Step-mode
	 */
	private boolean singlestep;
	/**
	 * Whether the VM is currently in some kind of running state,
	 * as there are multiple states which mean the VM is running.
	 */
	private boolean running;

	/**
	 * Whether the VM was running while this was queried.
	 * This function exists as states different from "running" still mean the VM is running,
	 * like "Watchdog".
	 *
	 * @return Whether the VM is currently running.
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Whether this VM is currently running in single-step-mode.
	 *
	 * @return Whether this VM is currently running in single-step-mode.
	 */
	public boolean isSinglestep() {
		return singlestep;
	}

	/**
	 * Which detailed state the VM is currently in.
	 * Remember that there are states different from "running" which may still mean the VM is currently executing.
	 *
	 * @return Which state the VM is currently in.
	 */
	public State getState() {
		return state;
	}

	/**
	 * Turn this command into JSON which can then directly be sent to a running QMP server.
	 * Is always { "execute": "query-status" }.
	 *
	 * @return JSON representation of this command.
	 */
	@Override
	public @NotNull String toJson() {
		return "{ \"execute\": \"query-status\" }";
	}

	/**
	 * This method takes the received JSON and then may or may not parse it in any way fit to its purpose.
	 * It may also fully discard it if not needed.
	 * Should probably not be used outside QMPInterface.
	 *
	 * @param result The result received from QEMU.
	 */
	@Override
	protected void processResult(@NotNull Object result) {
		assert result instanceof JSONObject;
		JSONObject json = (JSONObject) result;
		state = State.fromString(json.getString("status"));
		singlestep = json.getBoolean("singlestep");
		running = json.getBoolean("running");   //TODO: Apparently not always
	}

	/**
	 * An enum representing all states currently defined in the QMP protocol.f
	 * Simply copied from <a href="URL#https://qemu-project.gitlab.io/qemu/interop/qemu-qmp-ref.html#qapidoc-83">https://qemu-project.gitlab.io/qemu/interop/qemu-qmp-ref.html#qapidoc-83</a>
	 */
	public enum State {
		DEBUG("debug"),
		FINISH_MIGRATE("finish-migrate"),
		INMIGRATE("inmigrate"),
		INTERNAL_ERROR("internal-error"),
		IO_ERROR("io-error"),
		PAUSED("paused"),
		POSTMIGRATE("postmigrate"),
		PRELAUNCH("prelaunch"),
		RESTORE_VM("restore-vm"),
		RUNNING("running"),
		SAVE_VM("save-vm"),
		SHUTDOWN("shutdown"),
		SUSPENDED("suspended"),
		WATCHDOG("watchdog"),
		GUEST_PANICKED("guest-panicked"),
		COLO("colo");

		private final String type;

		State(String type) {
			this.type = type;
		}

		private static State fromString(String state) {
			State result;
			switch (state) {
				case "debug" -> result = State.DEBUG;
				case "finish-migrate" -> result = State.FINISH_MIGRATE;
				case "inmigrate" -> result = State.INMIGRATE;
				case "internal-error" -> result = State.INTERNAL_ERROR;
				case "io-error" -> result = State.IO_ERROR;
				case "paused" -> result = State.PAUSED;
				case "postmigrate" -> result = State.POSTMIGRATE;
				case "prelaunch" -> result = State.PRELAUNCH;
				case "restore-vm" -> result = State.RESTORE_VM;
				case "running" -> result = State.RUNNING;
				case "save-vm" -> result = State.SAVE_VM;
				case "shutdown" -> result = State.SHUTDOWN;
				case "suspended" -> result = State.SUSPENDED;
				case "watchdog" -> result = State.WATCHDOG;
				case "guest-panicked" -> result = State.GUEST_PANICKED;
				case "colo" -> result = State.COLO;
				default -> throw new IllegalArgumentException("Unknown state");
			}
			return result;
		}

		public String toString() {
			return this.type;
		}
	}
}

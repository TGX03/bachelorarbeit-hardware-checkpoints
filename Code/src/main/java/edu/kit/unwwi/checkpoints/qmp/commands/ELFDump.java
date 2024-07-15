package edu.kit.unwwi.checkpoints.qmp.commands;

import edu.kit.unwwi.checkpoints.qmp.Command;
import edu.kit.unwwi.checkpoints.qmp.Event;
import edu.kit.unwwi.checkpoints.qmp.EventHandler;
import edu.kit.unwwi.checkpoints.qmp.QMPInterface;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.nio.file.Path;

/**
 * This class dumps the current memory data from a QEMU-instance to disk in ELF format.
 */
public class ELFDump extends Command implements EventHandler {

	/**
	 * Where to store the dump to.
	 */
	private final Path target;
	/**
	 * The instance to wait for.
	 */
	private final QMPInterface instance;

	/**
	 * Whether the command was executed.
	 */
	private boolean done;
	/**
	 * The size of the dump in bytes.
	 */
	private long size;

	/**
	 * Creates a new ELFDump object which can be executed later.
	 * Needs the QMPInterface because this is an asynchronous request,
	 * which means the object needs to receive information from the Interface at some point.
	 * @param target The path where the dump shall be stored.
	 * @param instance The QMPinterface to receive the event from.
	 */
	public ELFDump(@NotNull final Path target, @NotNull QMPInterface instance) {
		this.target = target;
		this.instance = instance;
	}

	/**
	 * Whether the dump operation has completed.
	 *
	 * @return Whether the dump is done.
	 */
	public boolean isDone() {
		return this.done;
	}

	/**
	 * The size of the dump in bytes.
	 *
	 * @return The size of the dump.
	 * @throws IllegalStateException Gets thrown when the dump operation has not yet completed.
	 */
	public long getSize() throws IllegalStateException {
		if (done) return this.size;
		else throw new IllegalStateException("The operation has not yet completed");
	}

	@Override
	protected @NotNull String toJson() {
		instance.registerEventHandler(this);
		return "{ \"execute\": \"dump-guest-memory\", \"arguments\": { \"paging\": false, \"protocol\": \"file: " + target.toAbsolutePath() + "\", \"format\": \"elf\" } }";
	}

	@Override
	protected void processResult(@NotNull Object result) {
		assert "{}".equals(result); // This should just be an empty JSON-object
	}

	@Override
	public void handleEvent(Event event) {
		instance.unregisterEventHandler(this);
		JSONObject data = event.getData().getJSONObject("result");
		if (data.getString("status").equals("completed")) {
			size = data.getLong("total");
			done = true;
		}
	}

	@Override
	public String eventName() {
		return "DUMP_COMPLETED";
	}
}

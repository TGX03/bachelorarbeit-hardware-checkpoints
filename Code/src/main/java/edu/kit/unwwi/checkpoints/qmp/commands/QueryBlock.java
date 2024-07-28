package edu.kit.unwwi.checkpoints.qmp.commands;

import edu.kit.unwwi.checkpoints.qemu.models.Blockdevice;
import edu.kit.unwwi.checkpoints.qmp.Command;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * A command used to query QEMU for all available block devices.
 */
public class QueryBlock extends Command {

	/**
	 * The block devices found after execution.
	 */
	private Blockdevice[] result;

	/**
	 * Turn this command into JSON which can then directly be sent to a running QMP server.
	 *
	 * @return JSON representation of this command.
	 */
	@Override
	protected @NotNull String toJson() {
		return "{ \"execute\": \"query-block\" }";
	}

	/**
	 * Used by subclasses to parse the received result.
	 *
	 * @param result The result to parse.
	 */
	@Override
	protected void processResult(@NotNull Object result) {
		assert result instanceof JSONArray;
		JSONArray array = (JSONArray) result;
		this.result = new Blockdevice[array.length()];
		IntStream.range(0, array.length()).parallel().forEach(i -> {
			JSONObject current = array.getJSONObject(i);
			String name = current.getString("device");
			String qdev = current.getString("qdev");
			if (current.has("inserted") && current.getJSONObject("inserted").has("image")) {
				JSONObject insert = current.getJSONObject("inserted").getJSONObject("image");
				long virtualSize = insert.getLong("virtual-size");
				long actualSize = insert.getLong("actual-size");
				Path path = Paths.get(insert.getString("filename"));
				this.result[i] = new Blockdevice(name, qdev, path, virtualSize, actualSize);
			} else {
				this.result[i] = new Blockdevice(name, qdev, null, 0L, 0L);
			}
		});
	}

	/**
	 * Gets the blockdevices this commands found.
	 *
	 * @return The blockdevices currently registered with QEMU.
	 * @throws IllegalStateException If the command was not yet executed.
	 */
	@NotNull
	public Blockdevice @NotNull[] getResult() throws IllegalStateException {
		if (executed) return Arrays.copyOf(result, result.length);
		else throw new IllegalStateException("Command has not yet been queried");
	}
}

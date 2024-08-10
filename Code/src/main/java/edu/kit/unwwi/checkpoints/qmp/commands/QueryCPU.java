package edu.kit.unwwi.checkpoints.qmp.commands;

import edu.kit.unwwi.checkpoints.qemu.models.CPU;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.stream.IntStream;

/**
 * This command queries basic CPU information, like core count or architecture.
 * For more details see <a href="URL#https://qemu-project.gitlab.io/qemu/interop/qemu-qmp-ref.html#qapidoc-2533">the command documentation.</a>
 * This command does not query for registers, those must be fetched another way if required.
 */
public class QueryCPU extends StatefulCommand {

	/**
	 * The result of the query.
	 */
	protected CPU[] result;

	/**
	 * Returns how many CPUs are instantiated in this QEMU instance.
	 *
	 * @return How many CPUs are instantiated.
	 */
	public int getCPUCount() {
		if (executed) return result.length;
		else throw new IllegalStateException("Command hasn't been queried");
	}

	/**
	 * Returns the queried CPU objects.
	 * Keep in mind the CPUs returned from this class do not hold registers.
	 *
	 * @return JSON representing CPU data.
	 */
	public @NotNull CPU[] getResult() {
		if (executed) return result;
		else throw new IllegalStateException("Command hasn't been queried");
	}

	/**
	 * Turn this command into JSON which can then directly be sent to a running QMP server.
	 *
	 * @return JSON representation of this command.
	 */
	@Override
	public @NotNull String toJson() {
		return "{ \"execute\": \"query-cpus-fast\"}";   // For some reason they removed the normal "query-cpus"-command and didn't tell anyone
	}

	/**
	 * Used by subclasses to parse the received result.
	 *
	 * @param result The result to parse.
	 */
	@Override
	protected void processResult(@NotNull Object result) {
		assert result instanceof JSONArray;
		JSONArray cpus = (JSONArray) result;
		this.result = new CPU[cpus.length()];
		IntStream.range(0, cpus.length()).parallel().forEach(i -> {
			JSONObject cpu = cpus.getJSONObject(i);
			this.result[i] = new CPU(cpu.getInt("cpu-index"), cpu.getString("target"), cpu.getInt("thread-id"), null, null);
		});
	}
}

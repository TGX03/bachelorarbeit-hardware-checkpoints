package edu.kit.unwwi.checkpoints.qmp.commands;

import edu.kit.unwwi.checkpoints.qmp.Command;
import org.json.JSONArray;

/**
 * This command queries basic CPU information, like core count or architecture.
 * For more details see <a href="URL#https://qemu-project.gitlab.io/qemu/interop/qemu-qmp-ref.html#qapidoc-2533">the command documentation.</a>
 */
public class QueryCPU extends Command {

	/**
	 * The result of the query.
	 */
	private JSONArray result;

	/**
	 * Returns how many CPUs are instantiated in this QEMU instance.
	 *
	 * @return How many CPUs are instantiated.
	 */
	public int getCPUCount() {
		if (executed) return result.length();
		else throw new IllegalStateException("Command hasn't been queried");
	}

	/**
	 * Returns the data from querying the command as a JSONArray containing all the CPUs.
	 * See the official documentation for the format.
	 *
	 * @return JSON representing CPU data.
	 */
	public JSONArray getResult() {
		if (executed) return result;
		else throw new IllegalStateException("Command hasn't been queried");
	}

	/**
	 * Turn this command into JSON which can then directly be sent to a running QMP server.
	 *
	 * @return JSON representation of this command.
	 */
	@Override
	protected String toJson() {
		return "{ \"execute\": \"query-cpus-fast\"}";   // For some reason they removed the normal "query-cpus"-command and didn't tell anyone
	}

	/**
	 * Used by subclasses to parse the received result.
	 *
	 * @param result The result to parse.
	 */
	@Override
	protected void processResult(Object result) {
		assert result instanceof JSONArray;
		this.result = (JSONArray) result;
	}
}

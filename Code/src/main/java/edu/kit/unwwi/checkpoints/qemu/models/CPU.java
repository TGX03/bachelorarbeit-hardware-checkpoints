package edu.kit.unwwi.checkpoints.qemu.models;

import edu.kit.unwwi.JSONable;
import edu.kit.unwwi.checkpoints.qemu.models.registers.Register;
import edu.kit.unwwi.checkpoints.qmp.commands.QueryCPU;
import edu.kit.unwwi.checkpoints.qmp.commands.QueryRegisters;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Arrays;

/**
 * A class representing a CPU core of a QEMU instance.
 */
public class CPU implements Serializable, JSONable {

	/**
	 * The ID of this CPU/core.
	 */
	private final int id;
	/**
	 * The ID of the Thread emulating this CPU.
	 */
	private final int hostId;
	/**
	 * The architecture this CPU uses.
	 */
	private final String architecture;
	/**
	 * The registers of this CPU.
	 */
	private final Register[] registers;
	/**
	 * Any flags set for this CPU
	 */
	private final char[] flags;

	/**
	 * Create a new CPU from the given data.
	 *
	 * @param id           The ID of this CPU (core).
	 * @param architecture The architecture of this CPU.
	 * @param hostId       The thread of the host emulating this CPU.
	 * @param registers    The registers present in this CPU.
	 * @param flags        The flags set in this CPU
	 */
	public CPU(int id, String architecture, int hostId, Register[] registers, char[] flags) {
		this.id = id;
		this.architecture = architecture;
		this.hostId = hostId;
		this.registers = Arrays.copyOf(registers, registers.length);
		this.flags = Arrays.copyOf(flags, flags.length);
	}

	/**
	 * A factory method for turning QMP queries into CPU-objects.
	 *
	 * @param id        The ID of this CPU.
	 * @param cpu       The result of the "query CPU" command.
	 * @param registers The result of the "Query Registers" command.
	 * @return The CPU object constructed from the given data.
	 */
	public static CPU createFromQueries(int id, @NotNull QueryCPU cpu, @NotNull QueryRegisters registers) {
		JSONObject jsonCPU = cpu.getResult().getJSONObject(id);
		String architecture = jsonCPU.getString("target");
		int hostId = jsonCPU.getInt("thread-id");
		return new CPU(id, architecture, hostId, registers.getResult(), registers.flags());
	}

	@Override
	public @NotNull JSONObject toJSON() {
		JSONArray registers = new JSONArray();
		for (Register current : this.registers) {
			registers.put(current.toJSON());
		}
		JSONObject result = new JSONObject();
		result.put("id", id);
		result.put("architecture", architecture);
		result.put("hostId", hostId);
		result.put("registers", registers);
		if (flags != null) result.put("flags", new String(flags).replaceAll("\0", "-"));
		return result;
	}
}

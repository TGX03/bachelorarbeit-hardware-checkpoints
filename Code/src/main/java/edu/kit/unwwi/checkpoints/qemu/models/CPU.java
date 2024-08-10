package edu.kit.unwwi.checkpoints.qemu.models;

import edu.kit.unwwi.JSONable;
import edu.kit.unwwi.checkpoints.qemu.models.registers.Register;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
	public CPU(int id, String architecture, int hostId, @NotNull Register @Nullable [] registers, char @Nullable [] flags) {
		this.id = id;
		this.architecture = architecture;
		this.hostId = hostId;
		if (registers == null) this.registers = new Register[0];
		else this.registers = Arrays.copyOf(registers, registers.length);
		if (flags == null ) this.flags = null;
		else this.flags = Arrays.copyOf(flags, flags.length);
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

	/**
	 * Returns the ID of this CPU.
	 *
	 * @return ID of this CPU.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Returns the ID of the host thread emulating this CPU.
	 *
	 * @return The thread emulating this CPU.
	 */
	public int getHostThreadId() {
		return hostId;
	}

	/**
	 * Returns the name of the architecture of this CPU.
	 * Must be done for each core because modern CPUs sometimes carry multiple architectures.
	 *
	 * @return The architecture of this CPU.
	 */
	public String getArchitecture() {
		return architecture;
	}

	/**
	 * The registers of this CPU.
	 * Returns an empty array if registers were not queried for this CPU.
	 *
	 * @return The registers of this CPU.
	 */
	public @NotNull Register @NotNull [] getRegisters() {
		if (this.registers == null) return new Register[0];
		else return Arrays.copyOf(this.registers, this.registers.length);
	}

	/**
	 * The flags associated with this CPU.
	 * Returns an empty array if flags were not queried
	 * or flags are not supported on this architecture.
	 * @return Flags set for this CPU.
	 */
	public char @NotNull [] getFlags() {
		if (this.flags == null) return new char[0];
		else return Arrays.copyOf(this.flags, this.flags.length);
	}
}

package edu.kit.unwwi.checkpoints.qmp.commands;

import edu.kit.unwwi.checkpoints.qemu.models.registers.*;
import edu.kit.unwwi.checkpoints.qmp.Command;

import java.util.Arrays;

/**
 * This command uses the "info registers" command from the human-monitor-interface to read the data from the registers
 * and returns them as the objects.
 */
public class QueryRegisters extends Command {

	/**
	 * The ID of the CPU to query.
	 */
	private final int id;
	/**
	 * The registers after querying.
	 */
	private Register[] registers;

	/**
	 * Create a new command with an IT to query.
	 *
	 * @param id The CPU ID to query.
	 */
	public QueryRegisters(int id) {
		this.id = id;
	}

	/**
	 * Creates a Register from a String matching "name=content"
	 * @param input The string to parse.
	 * @return The created Register object.
	 */
	private static Register parseRegister(String input) {
		String[] split = input.split("=");
		String name = split[0];
		String value = split[1];
		int length = value.length() * 4;

		switch (length) {
			case 4 -> {
				return new Register4Bit(name, Integer.parseUnsignedInt(value, 16));
			}
			case 8 -> {
				return new Register8Bit(name, Integer.parseUnsignedInt(value, 16));
			}
			case 16 -> {
				return new Register16Bit(name, Integer.parseUnsignedInt(value, 16));
			}
			case 32 -> {
				return new Register32Bit(name, Integer.parseUnsignedInt(value, 16));
			}
			case 64 -> {
				return new Register64Bit(name, Long.parseUnsignedLong(value, 16));
			}
			case 80 -> {
				String base = value.substring(0, 16);
				String extension = value.substring(16);
				return new Register80Bit(name, Long.parseUnsignedLong(base, 16), Integer.parseUnsignedInt(extension, 16));
			}
			case 128 -> {
				String upper = value.substring(0, 16);
				String lower = value.substring(16, 32);
				return new Register128Bit(name, Long.parseUnsignedLong(upper, 16), Long.parseUnsignedLong(lower, 16));
			}
			case 256 -> {
				String upper = value.substring(0, 16);
				String upperMiddle = value.substring(16, 32);
				String lowerMiddle = value.substring(32, 48);
				String lower = value.substring(48, 64);
				return new Register256Bit(name, Long.parseUnsignedLong(upper, 16), Long.parseUnsignedLong(upperMiddle, 16), Long.parseUnsignedLong(lowerMiddle, 16), Long.parseUnsignedLong(lower, 16));
			}
			case 512 -> {
				long[] content = new long[8];
				for (int i = 0; i < 8; i++) {
					String current = value.substring(i * 16, (i + 1) * 16);
					content[i] = Long.parseUnsignedLong(current, 16);
				}
				return new Register512Bit(name, content);
			}
			default -> {
				return new FlagRegister("Invalid", false);  //TODO: Find out why this gets used
			}
		}
	}

	/**
	 * Get the Registers after querying.
	 *
	 * @return The registers.
	 */
	public Register[] getResult() {
		if (executed) return registers;
		else throw new IllegalStateException("Command hasn't been queried");
	}

	/**
	 * Turn this command into JSON which can then directly be sent to a running QMP server.
	 *
	 * @return JSON representation of this command.
	 */
	@Override
	protected String toJson() {
		return "{ \"execute\": \"human-monitor-command\", \"arguments\": { \"command-line\": \"info registers\", \"cpu-index\": " + id + " } }";
	}

	/**
	 * Used by subclasses to parse the received result.
	 *
	 * @param result The result to parse.
	 */
	@Override
	protected void processResult(Object result) {
		assert result instanceof String;
		String input = (String) result;
		input = input.replaceAll(System.lineSeparator(), " ");
		input = input.replaceFirst("CPU#\\d", "");
		input = input.replaceAll("\\s+", " ");
		input = input.replaceAll("\\s*=\\s*", "=");
		input = input.replaceAll("\\b([0-9,a-f]+)\\s+(?=[0-9,a-f])", "$1");
		input = input.trim();
		String[] registers = input.split(" ");

		this.registers = Arrays.stream(registers).parallel().filter(x -> x.matches(".*=[0-9,a-f]+")).map(QueryRegisters::parseRegister).toList().toArray(new Register[0]);
	}
}

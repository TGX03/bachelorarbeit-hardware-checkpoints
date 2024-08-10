package edu.kit.unwwi.checkpoints.qmp.commands.qhm;

import edu.kit.unwwi.checkpoints.qemu.models.registers.*;
import edu.kit.unwwi.checkpoints.qmp.commands.QHMCommand;
import org.jetbrains.annotations.NotNull;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * This command uses the "info registers" command from the human-monitor-interface to read the data from the registers
 * and returns them as the objects.
 */
public class QueryRegisters extends QHMCommand {

	/**
	 * The ID of the CPU to query.
	 */
	private final int id;
	/**
	 * The registers after querying.
	 */
	private Register[] registers;
	/**
	 * The flags returned from this register, if they exist.
	 */
	private char[] flags;

	/**
	 * Create a new command with an ID to query.
	 *
	 * @param id The CPU ID to query.
	 */
	public QueryRegisters(int id) {
		this.id = id;
	}

	/**
	 * Creates a Register from a String matching "name=content"
	 *
	 * @param input The string to parse.
	 * @return The created Register object.
	 */
	@NotNull
	private static Register parseRegister(String input, int registerNumber) {
		String[] split = input.split("=");
		String name = split[0];
		String value = split[1];
		int length = value.length() * 4;

		switch (length) {
			case 4 -> {
				return new Register4Bit(name, Integer.parseUnsignedInt(value, 16), registerNumber);
			}
			case 8 -> {
				return new Register8Bit(name, Integer.parseUnsignedInt(value, 16), registerNumber);
			}
			case 16 -> {
				return new Register16Bit(name, Integer.parseUnsignedInt(value, 16), registerNumber);
			}
			case 32 -> {
				return new Register32Bit(name, Integer.parseUnsignedInt(value, 16), registerNumber);
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
				BigInteger number = new BigInteger(value, 16);
				byte[] bytes = number.toByteArray();
				if (number.bitLength() % 8 == 1) bytes = Arrays.copyOfRange(bytes, 1, bytes.length - 1);
				return new RegisterFlexible(name, length, bytes);
			}
		}
	}

	/**
	 * Get the Registers after querying.
	 *
	 * @return The registers.
	 */
	public @NotNull Register @NotNull [] getResult() {
		if (executed) return registers;
		else throw new IllegalStateException("Command hasn't been queried");
	}

	/**
	 * Whether any flags are associated with the queried CPU core.
	 *
	 * @return Whether any flags are associated with the queried CPU core.
	 */
	public boolean hasFlags() {
		return this.flags != null;
	}

	/**
	 * Returns an array of all the set flags of this CPU.
	 * Flags which aren't set are represented as a null-char.
	 *
	 * @return The flags of this register if set.
	 * @throws IllegalStateException When no flags were found for this CPU.
	 */
	public char @NotNull [] flags() throws IllegalStateException {
		if (hasFlags()) return Arrays.copyOf(this.flags, this.flags.length);
		else throw new IllegalStateException("This CPU has no flags.");
	}

	@Override
	public @NotNull String commandName() {
		return "info registers";
	}

	@Override
	protected @NotNull Map<String, Object> additionalArguments() {
		return Map.of("cpu-index", id);
	}

	/**
	 * Used by subclasses to parse the received result.
	 *
	 * @param input The result to parse.
	 */
	@Override
	protected void receiveResult(@NotNull String input) {

		/*
		The goal of this block is to reformat the data of all registers into a uniform way, as QEMU formats all the registers rather randomly.
		The desired target is to have an array of strings, in which all string match the format %a=%b, with %a being the name of the register
		and %b being the data stored in said register.
		This works for ARM and x86, other architectures may need further testing (and perhaps changes).
		 */
		input = input.replaceAll(System.lineSeparator(), " ");
		input = input.replaceFirst("CPU#\\d", "");  // Remove the CPU identifier
		input = input.replaceAll("\\s+", " ");  // Turn any kind and amount of whitespace into a single whitespace
		input = input.replaceAll("\\s*=\\s*", "="); // Place data around equals-sign directly next to it.
		input = input.replaceAll("\\b([0-9a-f]+)\\s+(?=[0-9a-f])", "$1");   // Remove whitespace inside hex-numbers
		input = input.trim();
		String[] registers = input.split(" ");  // Split the input so each datapoint is its own string.

		Thread flagger = new Thread(() -> {
			for (String current : registers) {
				if (current.matches("\\[[A-Za-z-]*]")) {
					current = current.replaceAll("-", "\0");
					char[] flags = current.toCharArray();
					this.flags = new char[flags.length - 2];
					System.arraycopy(flags, 1, this.flags, 0, flags.length - 2);
					break;
				}
			}
		});
		flagger.start();
		this.registers = IntStream.range(0, registers.length).parallel().filter(x -> registers[x].matches(".*=[0-9,a-f]+")).mapToObj(x -> parseRegister(registers[x], x)).toArray(Register[]::new);
		try {
			flagger.join();
		} catch (InterruptedException _) {
		}
	}
}

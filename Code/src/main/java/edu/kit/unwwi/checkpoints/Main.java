package edu.kit.unwwi.checkpoints;

import edu.kit.unwwi.checkpoints.qmp.QMPInterface;
import edu.kit.unwwi.checkpoints.qmp.commands.ELFDump;
import org.apache.commons.cli.*;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

/**
 * This is an example usage of the checkpoint mechanisms that can be used outside of Java.
 * When wanting to exit from automatic or manual mode, type "exit".
 * It provides three modes:<p>
 * - Automatic mode in which the program halts the QEMU-instance after a certain amount of milliseconds has elapsed.<p>
 * - Manual mode in which the program executes everytime the word "Check" is read from System.IN<p>
 * - Single-shot mode when neither automatic nor manual mode are specified. Here the tool creates a single checkpoint and then exits.
 */
public final class Main {

	/**
	 * This boolean gets used to exit a running program.
	 */
	private static volatile boolean running = true;

	/**
	 * Runs the program.
	 *
	 * @param args The args to this program. Run the tool with -h for explanation.
	 * @throws Exception Mostly exceptions form asynchronous execution and that weird buffer index I don't understand.
	 */
	public static void main(String[] args) throws Exception {
		CommandLine cmd = parseArgs(args);
		if (cmd == null) return;

		Path target = Paths.get(cmd.getOptionValue("d"));
		QMPInterface inter = new QMPInterface(cmd.getOptionValue("h"), Integer.parseInt(cmd.getOptionValue("p")));
		if (cmd.hasOption("t")) ELFDump.setTemp(Paths.get(cmd.getOptionValue("t")));

		if (!cmd.hasOption("a") && !cmd.hasOption("m")) singleCheckpoint(inter, target);
		else if (cmd.hasOption("a") && !cmd.hasOption("m")) {
			long timeout = Long.parseLong(cmd.getOptionValue("a"));
			automaticMode(inter, target, timeout, cmd.hasOption("i"));
		} else if (cmd.hasOption("m") && !cmd.hasOption("a")) {
			manualMode(inter, target, cmd.hasOption("i"));
		} else throw new IllegalArgumentException("-a and -m are not allowed at the same time!");
	}

	/**
	 * Parses Command-line arguments provided to this tool.
	 * Returns null if the help screen was printed.
	 *
	 * @param args The arguments to parse.
	 * @return The resulting CommandLine object.
	 * @throws ParseException When commons-cli doesn't like the input.
	 */
	@Nullable
	private static CommandLine parseArgs(String[] args) throws ParseException {
		Options options = new Options();

		options.addOption("t", "temp", true, "Specify a temp directory in case the System-default does not work. E.g. Linux /tmp is often too small to be used.");
		options.addOption("d", "directory", true, "Specifies the directory where checkpoints should be stored.");
		options.addOption("m", "manual", false, "Manual mode, meaning checkpoints are only created when explicitly requested on the command line.");
		options.addOption("a", "automatic", true, "Specifies automatic mode, in which checkpoints get created every amount of milliseconds specified here.");
		options.addOption("i", "ignoreDuplicates", false, "If this flag is set, the program will not check for duplicates, but create a completely new checkpoint everytime.");
		options.addOption("h", "host", true, "The hostname of the targeted QEMU-instance.");
		options.addOption("p", "port", true, "The port of the targeted QEMU-instance.");
		options.addOption("h", "help", false, "Print this message.");

		CommandLine result = new DefaultParser().parse(options, args);
		if (result.hasOption("h")) {
			new HelpFormatter().printHelp("Checkpoint -x", options);
			return null;
		} else return result;
	}

	/**
	 * Creates a single checkpoint at the provided location from the provided interface.
	 *
	 * @param inter  The QMPInterface to query.
	 * @param target The directory to write the checkpoint to.
	 * @throws IOException          When something went wrong during communication with QEMU.
	 * @throws ExecutionException   Something went wrong in another thread, usually happens when an error occurs while trying to access the target or temporary directory.
	 * @throws InterruptedException Shouldn't occur.
	 */
	private static void singleCheckpoint(QMPInterface inter, Path target) throws IOException, ExecutionException, InterruptedException {
		Checkpoint.createCheckpoint(target, inter);
	}

	/**
	 * Runs this tool in automatic mode.
	 * When any kind of exception occurs, the tool simply exists.
	 *
	 * @param inter          The interface to query against.
	 * @param target         The target directory to store the checkpoint at.
	 * @param pause          How long to wait between checkpoints.
	 * @param keepDuplicates Whether new checkpoints should check for duplicates.
	 */
	private static void automaticMode(QMPInterface inter, Path target, long pause, boolean keepDuplicates) {
		Thread.ofPlatform().name("Runner").start(() -> {
			try {
				if (keepDuplicates) {
					do {
						Checkpoint.createCheckpoint(target, inter);
						Thread.sleep(pause);
					} while (running);
				} else {
					Checkpoint innerCheckpoint = Checkpoint.createCheckpoint(target, inter);
					while (running) {
						Thread.sleep(pause);
						innerCheckpoint = innerCheckpoint.createFollowUp(inter);
					}
				}
			} catch (Exception e) {
				running = false;
				throw new RuntimeException(e);
			}
		});
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		do {
			try {
				String line = reader.readLine();
				if ("exit".equalsIgnoreCase(line)) running = false;
			} catch (IOException e) {
				running = false;
				throw new RuntimeException(e);
			}
		} while (running);
	}

	/**
	 * Runs the tool in manual mode.
	 *
	 * @param inter          The interface to query.
	 * @param target         The directory to store the checkpoints at.
	 * @param keepDuplicates Whether secondary checkpoints should check for duplicates.
	 * @throws IOException          Usually occurs when the target directory could not be accessed or is not empty.
	 * @throws ExecutionException   Something happened in another thread. Often a weird OutOfBoundsException.
	 * @throws InterruptedException Should not occur.
	 */
	private static void manualMode(QMPInterface inter, Path target, boolean keepDuplicates) throws IOException, ExecutionException, InterruptedException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		Checkpoint checkpoint = null;
		do {
			String line = reader.readLine();
			switch (line) {
				case "exit" -> running = false;
				case "check" -> {
					if (keepDuplicates || checkpoint == null) checkpoint = Checkpoint.createCheckpoint(target, inter);
					else checkpoint = checkpoint.createFollowUp(inter);
				}
			}
		} while (running);
	}
}

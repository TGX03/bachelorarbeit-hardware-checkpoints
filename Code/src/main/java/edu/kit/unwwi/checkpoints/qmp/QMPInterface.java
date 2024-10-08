package edu.kit.unwwi.checkpoints.qmp;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The class representing the connection to the QMP server.
 * Sends requests and handles all incoming data, including asynchronous events which may occur.
 * For this, it uses a pure socket which uses Telnet to communicate with the host.
 */
public class QMPInterface {

	/**
	 * The socket used for the Telnet-connection.
	 * Is kept for closing the connection.
	 */
	private final Socket socket;
	/**
	 * The OutputStream for sending commands to the server.
	 */
	private final OutputStream out;
	/**
	 * The lock used to make sure only one command accesses the OutputStream at a time.
	 */
	private final Lock accessLock = new ReentrantLock();
	/**
	 * The lock used to wait for an answer from QEMU.
	 */
	private final Lock answerLock = new ReentrantLock();
	/**
	 * Condition used for waiting on server replies.
	 */
	private final Condition awaitAnswer = answerLock.newCondition();
	/**
	 * All handlers currently registered to listen for events.
	 */
	private final Collection<EventHandler> handlers = new Vector<>();

	/**
	 * The last result which was received.
	 */
	private volatile Object lastResult;
	/**
	 * Whether the client is to be terminated.
	 */
	private volatile boolean exit = false;

	/**
	 * @param host The host where the QMP server runs. Is probably localhost in most cases.
	 * @param port The port the host listens on.
	 * @throws IOException When the connection couldn't be established.
	 */
	public QMPInterface(@NotNull String host, int port) throws IOException {
		socket = new Socket(host, port);
		out = socket.getOutputStream();
		out.write("{ \"execute\": \"qmp_capabilities\" }".getBytes(StandardCharsets.UTF_8));
		answerLock.lock();
		Thread listener = new Thread(new Reader(socket.getInputStream()));
		listener.setDaemon(true);
		listener.start();
		awaitAnswer.awaitUninterruptibly();
		answerLock.unlock();
	}

	/**
	 * Execute the given command. The command itself will contain the result once this method finishes executing.
	 * This method itself doesn't check for errors in the data, that's up to the implementation of a command.
	 *
	 * @param command The command to execute.
	 * @throws IOException When something went wrong during transmission.
	 */
	public void executeCommand(@NotNull Command command) throws IOException {
		byte[] request = command.toJson().getBytes(StandardCharsets.UTF_8);
		accessLock.lock();
		answerLock.lock();
		out.write(request);
		awaitAnswer.awaitUninterruptibly();
		Object result = lastResult;
		answerLock.unlock();
		accessLock.unlock();
		command.receiveResult(result);
	}

	/**
	 * Informs this Interface to shut down.
	 * Calling this method will lead this interface to kill the connections, no matter what kind of data is being transmitted.
	 */
	public void exit() {
		exit = true;
		try {
			socket.close();
		} catch (IOException _) {
		}
	}

	/**
	 * Add a handler which receives specific asynchronous events from the QEMU-instance.
	 *
	 * @param handler The receiving handler.
	 */
	public void registerEventHandler(@NotNull EventHandler handler) {
		this.handlers.add(handler);
	}

	/**
	 * Remove a previously added handler from this QEMU-instance.
	 *
	 * @param handler The handler to remove.
	 */
	public void unregisterEventHandler(@NotNull EventHandler handler) {
		this.handlers.remove(handler);
	}

	/**
	 * Reads the input asynchronously to receive asynchronous events sent by QEMU.
	 */
	private class Reader implements Runnable {

		/**
		 * The buffer to read from input.
		 */
		private final BufferedReader in;

		/**
		 * Constructs a new asynchronous reader, which then must be started.
		 *
		 * @param in The InputStream to read from.
		 */
		Reader(@NotNull InputStream in) {
			this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
		}

		/**
		 * Keeps reading data from the socket until exit gets set.
		 */
		@Override
		public void run() {
			while (!exit) {
				try {
					String response = in.readLine();
					answerLock.lock();
					JSONObject json = new JSONObject(response);
					if (json.has("return")) {
						lastResult = json.get("return");
						awaitAnswer.signalAll();
					} else if (json.has("event")) {
						String name = json.getString("event");
						JSONObject data = null;
						if (json.has("data")) data = json.getJSONObject("data");
						long seconds = json.getJSONObject("timestamp").getLong("seconds");
						int microseconds = json.getJSONObject("timestamp").getInt("microseconds");
						Event event = new Event(name, data, seconds, microseconds);
						new Thread(() -> handleEvent(event)).start();
					} else if (json.has("error")) {
						System.err.println(json.get("error"));
					}
					answerLock.unlock();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}

		private void handleEvent(Event event) {
			handlers.stream().filter(x -> x.eventName().equals(event.getName())).forEach(handler -> Thread.ofVirtual().start(() -> handler.handleEvent(event)));
		}
	}
}
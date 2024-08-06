package edu.kit.unwwi.checkpoints.qmp;

import org.jetbrains.annotations.NotNull;

/**
 * This interface allows for an object to receive events from a running QMPInterface instance.
 * Since QEMU receives some data asynchronously, this is how these messages get treated internally.
 */
public interface EventHandler {

	/**
	 * The method that gets called when the specified event gets received.
	 *
	 * @param event The data of the event.
	 */
	void handleEvent(@NotNull Event event);

	/**
	 * This method returns the type of Event QEMU reports in its message.
	 * QMPInterface only calls the handleEvent method for EventHandlers where the reported name from QEMU
	 * and the return of this method match.
	 *
	 * @return The name of the events this class waits for.
	 */
	@NotNull
	String eventName();
}

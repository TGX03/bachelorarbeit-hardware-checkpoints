package edu.kit.unwwi.checkpoints.qmp;

import org.json.JSONObject;

/**
 * QEMU may send events, usually if an error occurred during execution.
 * This class aims to represent these events on a best-effort basis.
 * Probably not all event will be handled correctly.
 */
public class Event {

	/**
	 * The name of the event.
	 */
	private final String name;
	/**
	 * The data included in the event.
	 */
	private final JSONObject data;
	/**
	 * The timestamp in seconds when the event occurred.
	 */
	private final long timestampSeconds;
	/**
	 * Microseconds for additional precision.
	 */
	private final int timestampMicroseconds;

	/**
	 * @param name The name of the event.
	 * @param data The data included in the event.
	 * @param timestampSeconds The timestamp in seconds when the event occurred.
	 * @param timestampMicroseconds Microseconds for additional precision.
	 */
	Event(String name, JSONObject data, long timestampSeconds, int timestampMicroseconds) {
		this.name = name;
		this.data = data;
		this.timestampSeconds = timestampSeconds;
		this.timestampMicroseconds = timestampMicroseconds;
	}

	@Override
	public String toString() {
		return name + " at " +timestampSeconds;
	}

	/**
	 * @return The internal Name of QEMU for this event.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns the data included in this event.
	 * This class makes no effort to parse that data in any way and just includes the raw json.
	 * @return The data included with this event.
	 */
	public JSONObject getData() {
		return this.data;
	}

	/**
	 * The timestamp of this event in seconds.
	 * @return The timestamp of this event.
	 */
	public long getTimestamp() {
		return this.timestampSeconds;
	}

	/**
	 * Additional microseconds of when this event occurred.
	 * These are in addition to the normal timestamp, so they must be added to get the complete timestamp.
	 * @return The additional microseconds of this timestamp.
	 */
	public int getTimestampMicroseconds() {
		return this.timestampMicroseconds;
	}
}

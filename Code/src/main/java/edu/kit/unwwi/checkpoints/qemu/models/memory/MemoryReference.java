package edu.kit.unwwi.checkpoints.qemu.models.memory;

import com.sun.jna.Memory;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

/**
 * This class gets used to make sure memory objects get cleared at some point, to prevent memory leaks.
 * This is mainly a backup in case the AutoCloseable in MemorySegment turns out to be cumbersome,
 * but maybe this class gets removed at some point.
 *
 * @param <T> The type of object the backing phantom reference stores, even though it doesn't make any kind of difference.
 */
class MemoryReference<T> extends PhantomReference<T> {

	/**
	 * The memory object to close once this reference goes out of scope.
	 * Just like the main object of this reference, this should never be accessed.
	 */
	private final Memory memory;

	/**
	 * Create a new memory reference to free the memory region once no longer required.
	 *
	 * @param region   The memory region to free.
	 * @param referent The Segment to track.
	 * @param q        The queue to push this to once cleared.
	 */
	public MemoryReference(Memory region, T referent, ReferenceQueue<? super T> q) {
		super(referent, q);
		this.memory = region;
	}

	/**
	 * Free the memory object behind this reference.
	 */
	public void free() {
		memory.close();
	}
}

package edu.kit.unwwi.collections.big;

import it.unimi.dsi.fastutil.BigArrays;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An InputStream backed by a BigArray aka a 2D-array
 */
public class BigByteArrayInputStream extends InputStream {

	/**
	 * The array backing this stream.
	 */
	private final byte[][] array;
	/**
	 * The current position of this stream.
	 */
	private long position;

	/**
	 * Create a new input stream from a provided array.
	 * @param array The 2D array backing this stream.
	 */
	public BigByteArrayInputStream(byte[][] array) {
		position = 0L;
		this.array = array;
	}

	@Override
	public int available() {
		long result = BigArrays.length(array) - position;
		if (result > Integer.MAX_VALUE) return Integer.MAX_VALUE;
		else return (int) result;
	}

	@Override
	public int read() throws IOException {
		return 0;
	}

	@Override
	public int read(byte @NotNull [] buffer) {
		int copy = buffer.length;
		if ((long) copy > BigArrays.length(this.array) - position) copy = (int) (BigArrays.length(this.array) - position);
		BigArrays.copyFromBig(this.array, position, buffer, 0, copy);
		position += copy;
		return copy;
	}

	@Override
	public int read(byte @NotNull [] buffer, int offset, int length) {
		int copy = length;
		if ((long) copy > BigArrays.length(this.array) - position) copy = (int) (BigArrays.length(this.array) - position);
		BigArrays.copyFromBig(this.array, position, buffer, offset, copy);
		position += copy;
		return copy;
	}

	@Override
	public byte[] readAllBytes() {
		long remaining = BigArrays.length(this.array) - position;
		int size;
		if (remaining > Integer.MAX_VALUE) size = Integer.MAX_VALUE;
		else size = (int) remaining;
		byte[] result = new byte[size];
		BigArrays.copyFromBig(this.array, position, result, 0, size);
		return result;
	}

	@Override
	public long skip(long n) {
		if (position + n > BigArrays.length(this.array)) {
			long result = BigArrays.length(this.array) - position;
			position = BigArrays.length(this.array);
			return result;
		} else {
			position = position + n;
			return n;
		}
	}

	@Override
	public long transferTo(@NotNull OutputStream out) throws IOException {
		byte[] result = readAllBytes();
		long counter = 0;
		do {
			out.write(result);
			counter = counter + result.length;
			result = readAllBytes();
		} while (result.length > 0);
		return counter;
	}
}

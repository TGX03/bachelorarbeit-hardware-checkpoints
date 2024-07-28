package edu.kit.unwwi.checkpoints.qemu.models;

import edu.kit.unwwi.JSONable;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A class representing any kind of blockdevice,
 * which usually are any form of drive backed by a file on the host.
 */
public class Blockdevice implements Serializable, JSONable {

	/**
	 * The digest to use when computing the hash of the backing file.
	 * SHA256 by default, can be changed using static method.
	 */
	private static MessageDigest DIGEST = DigestUtils.getSha256Digest();

	/**
	 * The name of this device in QEMU.
	 */
	private final String device;
	/**
	 * The qdev-ID or QOM-Path, depending on internal assignment.
	 */
	private final String qdev;
	/**
	 * The path to the file on the host backing this device.
	 * NULL if no media is inserted.
	 */
	private final Path path;
	/**
	 * The size shown to the guest OS.
	 */
	private final long virtualSize;
	/**
	 * The size currently taken up on disk.
	 */
	private final long actualSize;
	/**
	 * Whether any media is inserted in this device.
	 */
	private final boolean hasMedia;
	/**
	 * The lock used to wait for the hash to be created.
	 */
	private final Lock hashLock;
	/**
	 * The hash of the media in this device.
	 */
	private byte[] hash;

	/**
	 * Create a new Blockdevice.
	 *
	 * @param device      The QEMU-name of the device.
	 * @param qdev        The qdev-ID or QOM-path, depending on QEMU internal assignment.
	 * @param path        The path to the file backing the device if available.
	 * @param virtualSize The size of the device shown to the guest.
	 * @param actualSize  The size currently taken up on the host disk.
	 */
	public Blockdevice(@NotNull String device, @NotNull String qdev, @Nullable Path path, long virtualSize, long actualSize) {

		//TODO: Is it really a good idea to always compute the hash?
		this.hasMedia = path != null && Files.exists(path);
		ReadWriteLock lock = new ReentrantReadWriteLock();
		this.hashLock = lock.readLock();
		if (this.hasMedia) {
			new Thread(() -> {
				lock.writeLock().lock();
				try {
					this.hash = DigestUtils.digest(DIGEST, Files.newInputStream(path));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				} finally {
					lock.writeLock().unlock();
				}
			}).start();
		}

		this.device = device;
		this.qdev = qdev;
		this.path = path;
		this.virtualSize = virtualSize;
		this.actualSize = actualSize;
	}

	/**
	 * Set the hash algorithm to use for segment comparison.
	 *
	 * @param digest The digest to use.
	 */
	public static void setHashAlgorithm(@NotNull MessageDigest digest) {
		DIGEST = digest;
	}

	/**
	 * Returns the name of this blockdevice.
	 *
	 * @return The name of this blockdevice.
	 */
	@NotNull
	public String getDevice() {
		return device;
	}

	/**
	 * Returns the qdev-ID or QOM-path of this device.
	 *
	 * @return The qdev-ID or QOM-path of this device.
	 */
	@NotNull
	public String getQdev() {
		return qdev;
	}

	/**
	 * Returns the path of the file backing this device,
	 * if available.
	 *
	 * @return The path of the file backing this device.
	 */
	@Nullable
	public Path getPath() {
		return path;
	}

	/**
	 * The total size of this device shown to the guest system.
	 *
	 * @return The size shown to the guest.
	 */
	public long getVirtualSize() {
		return virtualSize;
	}

	/**
	 * The size the backing file is taking up on the host's disks.
	 *
	 * @return The size on the host.
	 */
	public long getActualSize() {
		return actualSize;
	}

	/**
	 * Whether this blockdevice has any medium associated with it,
	 * e.g. an ISO-File of a drive or a qcow-file.
	 *
	 * @return Whether this device has media.
	 */
	public boolean hasMedia() {
		return this.hasMedia;
	}

	/**
	 * Returns a hash of the medium associated with this blockdevice.
	 * The hashing method can be set using setHashAlgorithm.
	 * All instances of Blockdevice share the same algorithm,
	 * as one probably wants to compare the files later on.
	 *
	 * @return The hash of the associated medium
	 * @throws IllegalStateException Gets thrown if this device doesn't have any medium associated with it, and therefore doesn't have a hash.
	 */
	public byte[] getHash() throws IllegalStateException {
		if (this.hasMedia) {
			hashLock.lock();
			byte[] result = Arrays.copyOf(this.hash, this.hash.length);
			hashLock.unlock();
			return result;
		} else throw new IllegalStateException("This blockdevice has no media, and therefore no associated hash");
	}

	@Override
	@NotNull
	public String toString() {
		return device;
	}

	@Override
	public JSONObject toJSON() {
		String hash = Base64.getEncoder().encodeToString(this.hash);
		JSONObject result = new JSONObject();
		result.put("deviceName", this.device);
		result.put("qdevID", this.qdev);
		result.put("virtualSize", this.virtualSize);
		result.put("actualSize", this.actualSize);
		result.put("hasMedia", this.hasMedia);
		result.put("hash", hash);
		return result;
	}
}

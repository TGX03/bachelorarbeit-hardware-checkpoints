package edu.kit.unwwi.checkpoints.qemu.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * A class representing any kind of blockdevice,
 * which usually are any form of drive backed by a file on the host.
 */
public class Blockdevice {

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
	 * Create a new Blockdevice.
	 *
	 * @param device      The QEMU-name of the device.
	 * @param qdev        The qdev-ID or QOM-path, depending on QEMU internal assignment.
	 * @param path        The path to the file backing the device if available.
	 * @param virtualSize The size of the device shown to the guest.
	 * @param actualSize  The size currently taken up on the host disk.
	 */
	public Blockdevice(@NotNull String device, @NotNull String qdev, @Nullable Path path, long virtualSize, long actualSize) {
		this.device = device;
		this.qdev = qdev;
		this.path = path;
		this.virtualSize = virtualSize;
		this.actualSize = actualSize;
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


	@Override
	@NotNull
	public String toString() {
		return device;
	}
}

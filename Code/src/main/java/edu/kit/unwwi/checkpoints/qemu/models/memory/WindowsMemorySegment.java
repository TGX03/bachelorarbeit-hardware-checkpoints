package edu.kit.unwwi.checkpoints.qemu.models.memory;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;
import com.sun.jna.ptr.IntByReference;

/**
 * This class allows to read a specified address from another process.
 * As Windows randomizes addresses somewhat, one needs to find the actual address using the win32-API.
 * As a process consists of multiple modules, the main .exe and other .dlls, one needs to select the module.
 * Currently, this class only uses the main .exe, as in QEMU other modules are not required.
 */
public class WindowsMemorySegment extends MemorySegment {

	static {
		Native.load(Kernel32.class);
	}

	/**
	 * Create a new Memory segment in Windows.
	 *
	 * @param processID The ID of the process to read memory from.
	 * @param offset    The address to start reading from.
	 * @param size      How many bytes to read.
	 */
	public WindowsMemorySegment(int processID, long offset, long size) {
		super(processID, offset, size);
	}

	/**
	 * Read the data from the specified process.
	 *
	 * @throws Win32Exception When something went wrong in the underlying Win32-API.
	 */
	public void read() throws Win32Exception {

		// Get handle to process
		WinNT.HANDLE processHandle = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_VM_READ | WinNT.PROCESS_QUERY_INFORMATION, false, this.process);
		if (processHandle == null) throw new Win32Exception(Kernel32.INSTANCE.GetLastError());

		// Get offset for main module
		WinDef.HMODULE[] hmodules = new WinDef.HMODULE[16];
		boolean success = Psapi.INSTANCE.EnumProcessModules(processHandle, hmodules, hmodules.length, new IntByReference());
		if (!success) throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
		Pointer offset = hmodules[0].getPointer();

		// Actually read the memory
		for (long current = 0; current < this.resultBuffer.size(); current = current + Integer.MAX_VALUE) {
			int readBytes;
			if (this.resultBuffer.size() - current <= Integer.MAX_VALUE)
				readBytes = (int) (this.resultBuffer.size() - current);
			else readBytes = Integer.MAX_VALUE;
			Pointer actualAddress = new Pointer(Pointer.nativeValue(offset) + current + this.address);
			success = Kernel32.INSTANCE.ReadProcessMemory(processHandle, actualAddress, this.resultBuffer, readBytes, null);
			if (!success) throw new Win32Exception(Kernel32.INSTANCE.GetLastError());
		}
	}
}

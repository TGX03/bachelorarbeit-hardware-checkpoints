package edu.kit.unwwi.checkpoints.qmp.commands;

import edu.kit.unwwi.checkpoints.qemu.models.CPU;
import edu.kit.unwwi.checkpoints.qmp.QMPInterface;
import edu.kit.unwwi.checkpoints.qmp.commands.qhm.QueryRegisters;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;

/**
 * This class queries all the CPUs existing in a QEMU-instance as well as all the registers associated with it.
 */
public class QueryCPURegisters extends QueryCPU {

	/**
	 * The interface to query against.
	 * Is required for the later queries asking for registers.
	 */
	private final QMPInterface inter;

	/**
	 * Create a new query.
	 * Requires the QMPInterface to query for registers later on.
	 *
	 * @param qmpInterface The QMPInterface to query against.
	 */
	public QueryCPURegisters(@NotNull QMPInterface qmpInterface) {
		this.inter = qmpInterface;
	}

	@Override
	public void processResult(@NotNull Object Result) {
		super.processResult(Result);
		super.result = Arrays.stream(super.result).parallel().map(cpu -> {
			QueryRegisters query = new QueryRegisters(cpu.getId());
			try {
				inter.executeCommand(query);
				return new CPU(cpu.getId(), cpu.getArchitecture(), cpu.getHostThreadId(), query.getResult(), query.flags());
			} catch (IOException e) {
				return cpu;
			}
		}).toArray(CPU[]::new);
	}
}

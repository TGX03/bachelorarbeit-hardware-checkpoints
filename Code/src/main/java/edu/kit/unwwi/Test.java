package edu.kit.unwwi;

import edu.kit.unwwi.checkpoints.Checkpoint;
import edu.kit.unwwi.checkpoints.qmp.QMPInterface;

import java.nio.file.Paths;

public class Test {

	public static void main(String[] args) throws Exception {
		QMPInterface inter = new QMPInterface("localhost", 4444);
		Checkpoint checkpoint = Checkpoint.createCheckpoint(Paths.get("E:\\Lars\\Downloads\\out"), inter);
		Thread.sleep(2000);
		Checkpoint second = checkpoint.createFollowUp(inter);
		System.out.println(second);
	}
}

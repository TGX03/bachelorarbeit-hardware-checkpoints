import edu.kit.unwwi.checkpoints.qmp.QMPInterface;
import edu.kit.unwwi.checkpoints.qmp.commands.Status;
import org.json.JSONObject;

import java.io.IOException;

public class Test {

	public static void main(String[] args) throws IOException {
		QMPInterface inter = new QMPInterface("localhost", 4444);
		Status status = new Status();
		inter.executeCommand(status);
		System.out.println(status);
	}
}

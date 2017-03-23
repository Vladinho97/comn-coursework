/* Isabella Chan s1330027 */

import java.io.IOException;

public class Receiver2a {

	public static void main(String[] args) throws IOException {
		if (args.length != 2) { // ignoring WindowSize parameter, exit code 1 if missing arguments
			System.err.println("Usage: Receiver2a <Port> <Filename> [WindowSize]");
			System.exit(1);
		}
		int portNo = Integer.parseInt(args[0]);
		String filename = args[1];
		
		Server2a server2a = new Server2a(portNo, filename);
		
		while (!server2a.doneACK) {
			server2a.ackPacket();
		}
	}
}

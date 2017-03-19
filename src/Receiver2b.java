/* Isabella Chan s1330027 */

import java.io.IOException;

public class Receiver2b {
	
	public static void main(String[] args) throws IOException {
		if (args.length != 3) {
			System.err.println("Usage: Receiver1a <Port> <Filename> [WindowSize]");
			System.exit(1);
		}
		int portNo = Integer.parseInt(args[0]);
		String filename = args[1];
		int windowSize = Integer.parseInt(args[2]);
		
		Server2b server2b = new Server2b(portNo, filename, windowSize);
		
		while (!server2b.doneACK) {
			server2b.ackPacket();
		}
	}
}

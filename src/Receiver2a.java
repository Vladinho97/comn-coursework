/* Isabella Chan s1330027 */

import java.io.IOException;

//mount -t vboxsf dummynetshared /mnt/shared
//ipfw add pipe 100 in
//ipfw add pipe 200 out
//ipfw pipe 100 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
//ipfw pipe 200 config delay 5/25/100ms plr 0.005 bw 10Mbits/s

public class Receiver2a {

	public static void main(String[] args) throws IOException {
		if (args.length != 2) { // ignoring WindowSize parameter, exit code 1 if missing arguments
			System.err.println("Usage: Receiver1a <Port> <Filename> [WindowSize]");
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

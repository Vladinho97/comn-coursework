/* Isabella Chan s1330027 */

import java.io.IOException;

/*
mount -t vboxsf dummynetshared /mnt/shared
ipfw add pipe 100 in
ipfw add pipe 200 out
ipfw pipe 100 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
ipfw pipe 200 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
*/

public class Sender2a {

	public static void main(String[] args) throws IOException {

		// ------------------------- Read arguments -------------------------
		if (args.length != 5) { // ignoring WindowSize parameter, exit code 1 if missing arguments
			System.err.println("Usage: java Sender1a localhost <Port> <Filename> [RetryTimeout] [WindowSize]");
			System.exit(1);
		}
		String localhost = args[0];
		int portNo = Integer.parseInt(args[1]);
		String filename = args[2];
		int retryTimeout = Integer.parseInt(args[3]);
		int windowSize = Integer.parseInt(args[4]);

		Client2a client2a = new Client2a(localhost, portNo, filename, retryTimeout, windowSize);
		client2a.openFile(); // opens image file

		// ------------------------ creates threads ------------------------------
		Thread rcvtt = new Thread(new RcvThread(client2a));
		Thread sendtt = new Thread(new SendThread(client2a));
		
		// --------------------------- run threads --------------------------------
		rcvtt.start();
		sendtt.start();
	}
}

class RcvThread implements Runnable {
	private Client2a client2a;
	public RcvThread(Client2a client2a) {
		this.client2a = client2a;
	}
	@Override
	public void run() {
		while (!client2a.doneACK) {
			try {
				client2a.ackPacket();
			} catch (IOException e) {
//				e.printStackTrace();
			}
		}
		client2a.printOutputs();
		// System.exit(0);
		return;
	}
}

class SendThread implements Runnable {
	private Client2a client2a;
	public SendThread(Client2a client2a) {
		this.client2a = client2a;
	}
	@Override
	public void run() {
		while (!client2a.doneSEND) {
			try {
				client2a.sendPacket();
			} catch (IOException e) {
//				e.printStackTrace();
			}
		}
		return;
	}
}

/* Isabella Chan s1330027 */

import java.io.IOException;


public class Sender2b {
	
	public static void main(String[] args) throws IOException {

		// ================ Read arguments ===============
		if (args.length != 5) { // ignoring WindowSize parameter, exit code 1 if missing arguments
			System.err.println("Usage: java Sender1a localhost <Port> <Filename> [RetryTimeout] [WindowSize]");
			System.exit(1);
		}
		String localhost = args[0];
		int portNo = Integer.parseInt(args[1]);
		String filename = args[2];
		int retryTimeout = Integer.parseInt(args[3]);
		int windowSize = Integer.parseInt(args[4]);

		Client2b client2b = new Client2b(localhost, portNo, filename, retryTimeout, windowSize);
		client2b.openFile(); // opens image file

		Thread rcvtt = new Thread(new RcvThread2b(client2b));
		Thread sendtt = new Thread(new SendThread2b(client2b));
		rcvtt.start();
		sendtt.start();
	}
	
}

class RcvThread2b implements Runnable {
	private Client2b client2b;
	public RcvThread2b(Client2b client2b) {
		this.client2b = client2b;
	}
	@Override
	public void run() {
		while (!client2b.doneACK) {
			try {
				client2b.ackPacket();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		client2b.printOutputs();
		return;
	}
}

class SendThread2b implements Runnable {
	private Client2b client2b;
	public SendThread2b(Client2b client2b) {
		this.client2b = client2b;
	}
	@Override
	public void run() {
		while (!client2b.doneSEND) {
			try {
				client2b.sendPacket();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return;
	}
}


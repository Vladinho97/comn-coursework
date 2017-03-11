import java.io.IOException;

// mount -t vboxsf dummynetshared /mnt/shared
// ipfw add pipe 100 in
// ipfw add pipe 200 out
// ipfw pipe 100 config delay 5/25/100ms plr 0.05 bw 10Mbits/s
// ipfw pipe 200 config delay 5/25/100ms plr 0.05 bw 10Mbits/s
public class Sender2a {
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
		
		Client client = new Client(localhost, portNo, filename, retryTimeout, windowSize);
		client.openFile(); // opens image file
		
		Thread rcvtt = new Thread(new RcvThread(client));
		Thread sendtt = new Thread(new SendThread(client));
		rcvtt.start();
		sendtt.start();
	}
}

class RcvThread implements Runnable {
	private Client client;
	public RcvThread(Client client) {
		this.client = client;
	}
	@Override
	public void run() {
		System.out.println("****************** Start Receiving thread ******************");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		try {
			while (!client.doneACK) {
				System.out.println("RcvThread: client not done acking");
				client.ackPacket();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class SendThread implements Runnable {
	private Client client;
	public SendThread(Client client) {
		this.client = client;
	}
	@Override
	public void run() {
		System.out.println("****************** Start Sending thread ******************");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		while (!client.doneACK) {
			System.out.println("SendThread: client not done acking");
			if (client.canSendMore()) {
				System.out.println("SendThread: client can send more");
				try {
					client.sendPacket();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} 
		}
	}
}
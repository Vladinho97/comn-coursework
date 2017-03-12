import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

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
		while (!client.doneACK) {
			try {
				client.ackPacket();
				// Thread.sleep(10);
			} catch (IOException e) {
				e.printStackTrace();
			}
			//  catch (InterruptedException e) {
			// 	e.printStackTrace();
			// }
		}
		try {
			client.bw.write("???????????????????????? finish acking ????????????????????????????????");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class ReceivingThread implements Runnable {
	private DatagramSocket clientSocket;
	private byte[] ackBuffer = new byte[2]; // ACK value from rcvPacket stored here
	private DatagramPacket rcvPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
	private Client client;
	public ReceivingThread( Client client, DatagramSocket socket) {
		this.clientSocket = socket;
		this.client = client;
	}

	public void run() {
		while (!client.doneACK) {
			try {
				rcvPacket.setLength(2);
				clientSocket.receive(rcvPacket);
				client.ackPacket(rcvPacket);
			} catch (Exception e) {
				e.printStackTrace();
			}
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
		while (!client.doneACK) {
			try {
				client.sendPacket();
				// Thread.sleep(10);
			} catch (IOException e) {
				e.printStackTrace();
			}
			//  catch (InterruptedException e) {
			// 	e.printStackTrace();
			// };
		}
	}
}

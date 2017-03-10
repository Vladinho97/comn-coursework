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

class ResendTask extends TimerTask {
	private Client client;
	public ResendTask(Client client) {
		this.client = client;
	}
	@Override
	public void run() {
		try {
			client.resendPackets();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

public class Client {
	
	private String localhost, filename;
	private int portNo, retryTimeout, windowSize;

	// ============= variables related to image file =================
	private byte[] imgBytesArr; 
	private int imgBytesArrLen, imgBytesArrIdx = 0;
	
	// ============= variables related to client socket ==============
	private DatagramSocket clientSocket = new DatagramSocket();
	private InetAddress IPAddress;
	private DatagramPacket sendPacket;
	
	// ============= variables related to sequence no ================
	private int incre = 0, seqNoInt = 0, base = 0, nextseqnum = 0;
	private ArrayList<DatagramPacket> pktsBuffer = new ArrayList<DatagramPacket>();
	private byte endFlag = (byte) 0; // last packet flag
	private int lastPktAttempt = 0; // attempt to send last pkt, to check for lost last ACK
	private Timer timer = new Timer();
	
	// ============= variables related to receiving packets ==========
	private byte[] ackBuffer = new byte[2]; // ACK value from rcvPacket stored here
	private DatagramPacket rcvPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
	private int rcvSeqNoInt; // received sequence no. in integer

	// ============= for logging purpose =============================
	FileWriter fw;
	BufferedWriter bw;

	public Client(String localhost, int portNo, String filename, int retryTimeout, int windowSize) throws IOException {
		this.localhost = localhost;
		this.portNo = portNo;
		this.filename = filename;
		this.retryTimeout = retryTimeout;
		this.windowSize = windowSize;
		this.IPAddress = InetAddress.getByName(localhost);
		this.fw = new FileWriter("output.txt");
		this.bw = new BufferedWriter(fw);
	}
	
	// opens image file and read into bytes array
	public void openFile() throws IOException {
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(file);
		this.imgBytesArrLen = (int) file.length();
		this.imgBytesArr = new byte[imgBytesArrLen];
		fis.read(imgBytesArr);
		fis.close();
	}

	public synchronized boolean canSendMore() {
		if (pktsBuffer.size()<windowSize && imgBytesArrIdx<imgBytesArrLen) {
			return true;
		}
		return false;
	}
	
	// creates and packet with sequence no, increment seq no and send
	public synchronized void sendPacket() throws IOException {
		seqNoInt = incre % 65535;
		incre++; 
		// ------------------------------ create new packet --------------------------------
		int packetIdx = 3, packetSize;
		byte[] buffer;
		if ((imgBytesArrLen - imgBytesArrIdx) >= 1024) {
			packetSize = 1027;
			buffer = new byte[packetSize];
			if ((imgBytesArrLen - imgBytesArrIdx) == 1024)	endFlag = (byte) 1;
			else	endFlag = (byte) 0;
		} else {
			packetSize = 3+imgBytesArrLen - imgBytesArrIdx; // last packet
			buffer = new byte[packetSize];
			endFlag = (byte) 1;
		}
		buffer[0] = (byte) (seqNoInt >>> 8); // store sequence no. value as two byte values
		buffer[1] = (byte) seqNoInt;
		buffer[2] = endFlag;
		while (packetIdx < packetSize) { // write imgBytesArr byte values into packet
			buffer[packetIdx] = imgBytesArr[imgBytesArrIdx];
			packetIdx++;
			imgBytesArrIdx++;
		}
		// ------------------------------ send the packet --------------------------------
		sendPacket = new DatagramPacket(buffer, buffer.length, IPAddress, portNo);
		clientSocket.send(sendPacket);
		// ------------------------------ update values --------------------------------
		if (base == nextseqnum) { // if no unAck'd packets
			timer.schedule(new ResendTask(this), retryTimeout);
		}
		nextseqnum = (nextseqnum+1) % 65535; // increment next available sequence no 
		pktsBuffer.add(sendPacket); // add sent packet to buffer
	}
	
	
	boolean doneACK = false;
	// tries to receive a packet - the packet with base sequence no
	public void ackPacket() throws IOException {
		rcvPacket.setLength(2);
		// ---------------------------- this is last packet -------------------------
		if (endFlag==(byte)1 && pktsBuffer.size()==1) { // last one packet that needs to be ack
			timer.cancel(); // switch to setSoTimeOut instead
			while (true) {
				clientSocket.setSoTimeout(retryTimeout);
				try {
					clientSocket.receive(rcvPacket);
					rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] &0xff));
					if (rcvSeqNoInt == base) { // last packet has been received
						doneACK = true;
						return;
					}
				} catch (SocketTimeoutException e) {
					if (lastPktAttempt >= 50) { // last packet's ACK was assumed lost
						doneACK = true;
						return;
					}
					lastPktAttempt++;
					resendPackets(); // send packets again
				}
			}
		}
		
		// ----------------------------- ordinary packets -------------------------------
		else {
			clientSocket.setSoTimeout(1000); 
			try {
				clientSocket.receive(rcvPacket);
				rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] &0xff));
				if (rcvSeqNoInt == base) {
					update();
				}
			} catch (SocketTimeoutException e) {
			}
		}

	}
	
	
	// method to resend packets in the window
	public synchronized void resendPackets() throws IOException {
		for (int i = 0; i < pktsBuffer.size(); i++) {
			DatagramPacket currPacket = pktsBuffer.get(i);
			clientSocket.send(currPacket);
			System.out.println("Send");
		}
		timer.schedule(new ResendTask(this), retryTimeout);
	}
	
	// updates base, nextseqnum, pktsBuffer
	public synchronized void update() {
		base = (base+1) % 65535; 
		pktsBuffer.remove(0); 
		if (base == nextseqnum) {
			timer.cancel();
			timer = new Timer();
		} else {
			timer.schedule(new ResendTask(this), retryTimeout);
		}
	}
}
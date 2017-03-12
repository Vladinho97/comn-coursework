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


// ============================================================================================================
//
// 												RESEND TASK
//
// ============================================================================================================
class ResendTask extends TimerTask {
	private Client client;
	public ResendTask(Client client) {
		this.client = client;
	}
	@Override
	public void run() {
		System.out.println("++++++++++++++++++++++++++ Run Timer Task +++++++++++++++++++++");
		try {
			client.resendPackets();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

//============================================================================================================
//
//												CLIENT CLASS
//
//============================================================================================================
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

	Object lock = new Object();

	// boolean noMorePackets = false;
	public void sendPacket() throws IOException {
		// bw.write("sendPacket(): calling\n");
		System.out.println("sendPacket(): calling");
		if (imgBytesArrIdx >= imgBytesArrLen) {
			// noMorePackets = true;
			bw.write("sendPacket(): no more packets to be sent\n");
			System.out.println("sendPacket(): no more packets to be sent");
			return;
		}
		if (pktsBuffer.size() >= windowSize) {
			// bw.write("sendPacket(): pktsBuffer.size() = "+pktsBuffer.size()+" equals windowSize "+windowSize+". window is full\n");
			// System.out.println("sendPacket(): pktsBuffer.size() = "+pktsBuffer.size()+" equals windowSize "+windowSize+". window is full");
			return;
		}
		synchronized (lock) {
			bw.write("sendPacket(): locked object\n");
			System.out.println("sendPacket(): locked object.");
			seqNoInt = incre % 65535;
			incre++;
			bw.write("sendPacket(): creating a packet with seqNoInt = "+seqNoInt+"    |    base : "+base+"   |   nextseqnum : "+nextseqnum+"\n");
			System.out.println("sendPacket(): creating a packet with seqNoInt : "+seqNoInt+"    |    base : "+base+"   |   nextseqnum : "+nextseqnum);
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
				System.out.println("sendPacket(): base == nextseqnum, cancel timer, schedule timer!");
				timer = new Timer();
				timer.schedule(new ResendTask(this), retryTimeout);
			}
			nextseqnum = (nextseqnum+1) % 65535; // increment next available sequence no
			pktsBuffer.add(sendPacket); // add sent packet to packets buffer

			bw.write("sendPacket(): sent packet. seqNoInt : "+seqNoInt+"   |   base : "+base+"   |   nextseqnum : "+nextseqnum+"    |   pktsBuffer.Size() : "+pktsBuffer.size()+"\n");
			System.out.println("sendPacket(): sent packet. seqNoInt : "+seqNoInt+"   |   base : "+base+"   |   nextseqnum : "+nextseqnum+"    |   pktsBuffer.Size() : "+pktsBuffer.size());
		}
	}

	boolean doneACK = false;
	// tries to receive a packet - the packet with base sequence no
	public void ackPacket() throws IOException {
		bw.write("ackPacket(): calling\n");
		System.out.println("ackPacket(): calling");
		rcvPacket.setLength(2);
		clientSocket.setSoTimeout(5000);
		try {
			clientSocket.receive(rcvPacket);
			ackBuffer = rcvPacket.getData();
			rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] & 0xff));
			bw.write("ackPacket(): received packet with rcvSeqNoInt : "+rcvSeqNoInt+"\n");
			System.out.println("ackPacket(): received packet with rcvSeqNoInt : "+rcvSeqNoInt);
			synchronized (lock) {
				bw.write("ackPacket(): locked object\nackPacket(): base : "+base+"   |   nextseqnum : "+nextseqnum+"\n");
				System.out.println("ackPacket(): locked object");
				System.out.println("ackPacket(): base : "+base+"   |   nextseqnum : "+nextseqnum);
				if (rcvSeqNoInt == base) {
					if (endFlag == (byte)1 && pktsBuffer.size() == 1) {
						doneACK = true;
					}
					bw.write("ackPacket(): rcvSeqNoInt == base, update variables.\n");
					System.out.println("ackPacket(): rcvSeqNoInt == base, update variables.");
					base = (base+1) % 65535;
					pktsBuffer.remove(0);
					bw.write("ackPacket(): update base : "+base+"   |   pktsBuffer.size() : "+pktsBuffer.size()+"\n");
					if (base == nextseqnum) {
						timer.cancel();
						bw.write("ackPacket(): base == nextseqnum, timer cancelled.\n");
						System.out.println("ackPacket(): base == nextseqnum, timer cancelled.");
					} else {
						timer.cancel();
						timer = new Timer();
						timer.schedule(new ResendTask(this), retryTimeout);
						bw.write("ackPacket(): base != nextseqnum. new timer and scheduled.\n");
					}
				} else {
					bw.write("ackPacket(): rcvSeqNoInt != base. disregard\n");
				}
			}
		} catch (SocketTimeoutException e) {
			bw.write("I am a bitch!\n");
			// resendPackets();
		}
	}
	public void ackPacket(DatagramPacket pkt) throws IOException {
		try {
			this.rcvPacket = pkt;
			ackBuffer = rcvPacket.getData();
			rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] & 0xff));
			bw.write("ackPacket(): received packet with rcvSeqNoInt : "+rcvSeqNoInt+"\n");
			System.out.println("ackPacket(): received packet with rcvSeqNoInt : "+rcvSeqNoInt);
			synchronized (lock) {
				bw.write("ackPacket(): locked object\nackPacket(): base : "+base+"   |   nextseqnum : "+nextseqnum+"\n");
				System.out.println("ackPacket(): locked object");
				System.out.println("ackPacket(): base : "+base+"   |   nextseqnum : "+nextseqnum);
				if (rcvSeqNoInt == base) {
					if (endFlag == (byte)1 && pktsBuffer.size() == 1) {
						doneACK = true;
					}
					bw.write("ackPacket(): rcvSeqNoInt == base, update variables.\n");
					System.out.println("ackPacket(): rcvSeqNoInt == base, update variables.");
					base = (base+1) % 65535;
					pktsBuffer.remove(0);
					bw.write("ackPacket(): update base : "+base+"   |   pktsBuffer.size() : "+pktsBuffer.size()+"\n");
					if (base == nextseqnum) {
						timer.cancel();
						bw.write("ackPacket(): base == nextseqnum, timer cancelled.\n");
						System.out.println("ackPacket(): base == nextseqnum, timer cancelled.");
					} else {
						timer.cancel();
						timer = new Timer();
						timer.schedule(new ResendTask(this), retryTimeout);
						bw.write("ackPacket(): base != nextseqnum. new timer and scheduled.\n");
					}
				} else {
					bw.write("ackPacket(): rcvSeqNoInt != base. disregard\n");
				}
			}
		} catch (SocketTimeoutException e) {
			bw.write("I am a bitch!\n");
			// resendPackets();
		}
	}


	public void resendPackets() throws IOException {
		bw.write("resendPackets(): calling \n");
		System.out.println("================= resendPackets() =============");
		synchronized (lock) {
			bw.write("resendPackets(): lock object\n");
			bw.write("resendPackets(): base : "+base+"    |   nextseqnum : "+nextseqnum+"   |   seqNoInt : "+seqNoInt+"   |    pktsBuffer.size() : "+pktsBuffer.size()+"\n");
			System.out.println("resendPacket(): seqNoInt = "+seqNoInt+"   |   base = "+base+"   |   nextseqnum = "+nextseqnum+"   |   pktsBuffer.size() = "+pktsBuffer.size());
			for (int i = 0; i < pktsBuffer.size(); i++) {
				DatagramPacket currPkt = pktsBuffer.get(i);
				byte[] dataByte = currPkt.getData();
				byte one = dataByte[0];
				byte two = dataByte[1];
				int currSeqNo = (((one & 0xff)<<8) | (two & 0xff));
				bw.write("resendPackets(): resend packet with seqNoInt = "+currSeqNo+"\n");
				System.out.println("pkt size = "+currPkt.getLength());
				clientSocket.send(pktsBuffer.get(i));
			}
			timer = new Timer();
			timer.schedule(new ResendTask(this), retryTimeout);
			bw.write("resendPackets(): scheduled new timer.");
		}
	}

//	public boolean canSendMore() {
//		if (getPktsBufferSize()<windowSize && imgBytesArrIdx<imgBytesArrLen) {
////		if (pktsBuffer.size()<windowSize && imgBytesArrIdx<imgBytesArrLen) {
//			return true;
//		}
//		return false;
//	}
//
//	boolean doneACK = false;
//	// tries to receive a packet - the packet with base sequence no
//	public void ackPacket() throws IOException {
//		System.out.println("================= ackPacket() ==============");
//		rcvPacket.setLength(2);
//		// ---------------------------- this is last packet -------------------------
//		if (endFlag==(byte)1 && getPktsBufferSize()==1) { // last one packet that needs to be ack
//			timer.cancel(); // switch to setSoTimeOut instead
//			while (true) {
//				System.out.println("ackPacket(): trying to receive last ack");
//				clientSocket.setSoTimeout(retryTimeout);
//				try {
//					clientSocket.receive(rcvPacket);
//					rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] &0xff));
//					System.out.println("ackPacket(): trying to receive last ack: received : "+rcvSeqNoInt);
//					if (rcvSeqNoInt == base) { // last packet has been received
//						doneACK = true;
//						System.out.println("ackPacket(): last packet has been ack'd");
//						return;
//					}
//				} catch (SocketTimeoutException e) {
//					System.out.println("ackPacket(): socket timed out!");
//					if (lastPktAttempt >= 50) { // last packet's ACK was assumed lost
//						doneACK = true;
//						System.out.println("ackPacket(): lastPktAttempt >= 50");
//						return;
//					}
//					lastPktAttempt++;
//					System.out.println("ackPacket(): lastPktAttempt = "+lastPktAttempt+". resend packet with seqNo = "+seqNoInt);
//				}
//			}
//		}
//
//		// ----------------------------- ordinary packets -------------------------------
//		else {
//			System.out.println("ackPacket(): trying to receive ordinary packets.");
//			clientSocket.receive(rcvPacket);
//			rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] &0xff));
//			System.out.println("ackPacket(): received: "+rcvSeqNoInt+"   |   base : "+base);
//			if (rcvSeqNoInt == base) {
//				System.out.println("ackPacket(): rcvSeqNoInt == base, update variables.");
//				update();
//			}
//		}
//	}
//
//
//	// method to resend packets in the window
//	public synchronized void resendPackets() throws IOException {
//		System.out.println("SYNC: ================= resendPackets() ==============");
//		System.out.println("resendPacket(): seqno = "+seqNoInt);
//		System.out.println("resendPacket(): resending "+getPktsBufferSize()+" packets.");
//		for (int i = 0; i < getPktsBufferSize(); i++) {
//			clientSocket.send(pktsBuffer.get(i));
//		}
//		timer.schedule(new ResendTask(this), retryTimeout);
//		System.out.println("resendPacket(): scheduled timer.");
//	}
//
//	// updates base, nextseqnum, pktsBuffer
//	public synchronized void update() {
//		System.out.println("SYNC: ================= update() ==============");
//		base = (base+1) % 65535;
//		rmvPktFromBuffer();
//		System.out.println("update(): updated values:   base = "+base+"   |   pktsBuffer.size() = "+getPktsBufferSize());
//		System.out.println("update(): nextseqnum = "+nextseqnum);
//		if (base == nextseqnum) {
//			timer.cancel();
//			timer = new Timer();
//			System.out.println("update(): base == nextseqnum, timer cancelled.");
//		} else {
//			timer.schedule(new ResendTask(this), retryTimeout);
//			System.out.println("update(): base != nextseqnum, timer rescheduled.");
//		}
//	}
}

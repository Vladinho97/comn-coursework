/* Isabella Chan s1330027 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

public abstract class AbstractClient {

	Object lock = new Object(); // for synchronization
	boolean doneSEND = false, doneACK = false; // flags to terminate sending and acking

	// ======================== Sender parameters ==========================
	String localhost, filename;
	int portNo, retryTimeout, windowSize;

	// ================ variables related to image file ===================
	byte[] imgBytesArr; // byte array to store all bytes from a file
	int imgBytesArrLen, imgBytesArrIdx = 0;

	// ================== variables related to client socket ==============
	DatagramSocket clientSocket = new DatagramSocket();
	DatagramPacket sendPacket;
	InetAddress IPAddress; // server's IP address

	// ================== variables related to sequence no. ===============
	int incre = 1, seqNoInt = 0, base = 1, nextseqnum = 1;
	ArrayList<DatagramPacket> pktsBuffer = new ArrayList<DatagramPacket>(); // window
	byte endFlag = (byte) 0; // last packet flag

	// ================== variables related to receiving packets ============
	byte[] ackBuffer = new byte[2]; // ACK value from rcvPacket stored here
	DatagramPacket rcvPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
	int lastSeqNo, rcvSeqNoInt; // received seqno in interger

	// ========================= Program outputs ==========================
	double fileSizeKB, throughput, estimatedTimeInSec;
	long estimatedTimeInNano;
//	int noOfRetransmission = 0;
	boolean isFirstPacket = true;
	Long startTime = null, endTime = null;

	public AbstractClient(String localhost, int portNo, String filename, int retryTimeout, int windowSize) throws IOException {
		this.localhost = localhost;
		this.portNo = portNo;
		this.filename = filename;
		this.retryTimeout = retryTimeout;
		this.windowSize = windowSize;
		this.IPAddress = InetAddress.getByName(localhost);
	}

	/** Opens file and reads bytes into a byte array */
	public void openFile() throws IOException {
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(file);
		this.imgBytesArrLen = (int) file.length();
		this.imgBytesArr = new byte[imgBytesArrLen];
		this.fileSizeKB = ((double)file.length())/1024;
		fis.read(imgBytesArr);
		fis.close();
	}

	/** creates a packet and updates buffer */
	public byte[] createPacket() {
		int packetIdx = 3;
		int packetSize;
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

		if (endFlag == (byte) 1)
			lastSeqNo = seqNoInt;

		buffer[0] = (byte) (seqNoInt >>> 8); // store sequence no. value as two byte values
		buffer[1] = (byte) seqNoInt;
		buffer[2] = endFlag;

		while (packetIdx < packetSize) { // write imgBytesArr byte values into packet
			buffer[packetIdx] = imgBytesArr[imgBytesArrIdx];
			packetIdx++;
			imgBytesArrIdx++;
		}

		return buffer;
	}

	public abstract void sendPacket() throws IOException;
	
	/** Client socket receives a packet and updates ackBuffer and rcvSeqNoInt */
	public void receivePacket() throws IOException {
		rcvPacket.setLength(2);
		clientSocket.setSoTimeout(0);
		clientSocket.receive(rcvPacket);
		ackBuffer = rcvPacket.getData();
		rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] & 0xff));
	}
	
	public abstract void ackPacket() throws IOException;

	public abstract void resendPackets() throws IOException;

	public abstract void printOutputs();

	public void closeAll() throws IOException {
		clientSocket.close();
		endTime = System.nanoTime();
		return;
	}
}

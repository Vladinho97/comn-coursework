/* Isabella Chan s1330027 */

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;


public abstract class AbstractClient {
	
	Object lock = new Object(); 
	
	String localhost, filename;
	int portNo, retryTimeout, windowSize;

	// ================== for logging purpose ============================
	FileWriter fw;
	BufferedWriter bw;
	
	// ================ variables related to image file ===================
	byte[] imgBytesArr;
	int imgBytesArrLen, imgBytesArrIdx = 0;

	// ================== variables related to client socket ==============
	DatagramSocket clientSocket = new DatagramSocket();
	DatagramPacket sendPacket;
	InetAddress IPAddress;
	
	// ================== variables related to sequence no. ===============
	int incre = 0, seqNoInt = 0, base = 0, nextseqnum = 0;
	ArrayList<DatagramPacket> pktsBuffer = new ArrayList<DatagramPacket>(); // window
	byte endFlag = (byte) 0; // last packet flag
	int attempt = 0; // no. of attempts to send the last packet TODO: do i need this?
	
	// ================== variabl related to receiving packets ============
	byte[] ackBuffer = new byte[2]; // ACK value from rcvPacket stored here
	DatagramPacket rcvPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
	int rcvSeqNoInt; // received seqno in interger
	
	// ========================= Program outputs ==========================
	double fileSizeKB, throughput, estimatedTimeInSec;
	long estimatedTimeInNano;
	int noOfRetransmission = 0;
	boolean isFirstPacket = true;
	Long startTime = null, endTime = null;
	
	public AbstractClient(String localhost, int portNo, String filename, int retryTimeout, int windowSize) throws IOException {
		this.localhost = localhost;
		this.portNo = portNo;
		this.filename = filename;
		this.retryTimeout = retryTimeout;
		this.windowSize = windowSize;
		this.IPAddress = InetAddress.getByName(localhost);
		this.fw = new FileWriter("output-sender.txt");
		this.bw = new BufferedWriter(fw);
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
	
	public void printOutputs() {
		System.out.println("================== Part2a: output ==================");
		System.out.println("No of retransmission = "+noOfRetransmission);
		estimatedTimeInNano = endTime - startTime; 
		estimatedTimeInSec = ((double)estimatedTimeInNano)/1000000000.0; // convert from nano-sec to sec
		throughput = fileSizeKB/estimatedTimeInSec;
		System.out.println("Throughput = "+throughput);
		System.out.println("================== Program terminates ==================");
	}
		
	public abstract void sendPacket() throws IOException;
	
	public abstract void ackPacket() throws IOException;
	
	public abstract void resendPacket() throws IOException;
	
	public void closeAll() throws IOException {
		bw.write("ackPacket(): last packet acked. doneACK!\n");
		bw.close();
		fw.close();
		clientSocket.close();
		endTime = System.nanoTime();
		return;
	}
}

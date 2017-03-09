import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/* Isabella Chan s1330027 */

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
		client.openFile(filename); // opens image file
		
		// UpdatingThread ut = ..
		// ReceivingThread rt = ...
		// SendingThread st = ... 
	}
}

class Client {
	
	String localhost, filename;
	int portNo, retryTimeout, windowSize;

	// ============= variables related to image file =================
	byte[] imgBytesArr; 
	int imgBytesArrLen, imgBytesArrIdx = 0;
	
	// ============= variables related to client socket ==============
	DatagramSocket clientSocket = new DatagramSocket();
	InetAddress IPAddress;
	DatagramPacket sendPacket;
	
	// ============= variables related to sequence no ================
	int incre = 0, seqNoInt = 0, base = 0, nextseqnum = 0;
	DatagramPacket[] pktsBuffer; // buffer to store sent-but-not-ack'd packets
	
	// ============= variables related to receiving packets ==========
	byte[] ackBuffer = new byte[2]; // ACK value from rcvPacket stored here
	DatagramPacket rcvPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
	int rcvSeqNoInt; // received sequence no. in integer

	FileWriter fw;
	BufferedWriter bw;

	public Client(String localhost, int portNo, String filename, int retryTimeout, int windowSize) throws IOException {
		this.localhost = localhost;
		this.portNo = portNo;
		this.filename = filename;
		this.retryTimeout = retryTimeout;
		this.windowSize = windowSize;
		this.IPAddress = InetAddress.getByName(localhost);
		this.pktsBuffer = new DatagramPacket[windowSize];
		this.fw = new FileWriter("output.txt");
		this.bw = new BufferedWriter(fw);
	}
	
	public void openFile(String filename) throws IOException {
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(file);
		this.imgBytesArrLen = (int) file.length();
		this.imgBytesArr = new byte[imgBytesArrLen];
		fis.read(imgBytesArr);
		fis.close();
	}

	
	boolean moreToSend() throws IOException {
		if (!(imgBytesArrIdx < imgBytesArrLen)) {
			bw.write("No more packets to be sent!\n");
			return false;
		} 
		bw.write("more packets to be sent!\n");
		return true;
	}
	
	void sendPackets() throws IOException {
		seqNoInt = incre % 65535; // seq no for this packet
		boolean canSendMore = false;
		int windowSizeSeqNo = (base+windowSize) % 65536; 
		if (seqNoInt < (base+windowSize))	canSendMore = true; // check whether this packet will fit in the window
		bw.write("base : "+base+
				"   |   nextseqnum: "+nextseqnum+
				"   |   seq no : "+seqNoInt+
				"   |   max window seq no : "+windowSizeSeqNo+
				"   |   can send more? "+canSendMore+"\n");
		bw.write("current packet buffer = ");
		for (int i = 0; i < pktsBuffer.length; i++) {
			bw.write("["+pktsBuffer[i]+"]   ");
		}
		bw.write("\n");
		
		if (!canSendMore) { // window is full
			return;
		}
		
		// there is space in the window
		incre++; // increment 
		bw.write("Creating packets to be sent....\n");
		// if window is not full AND not imgBytesArrIdx < imgBytesArrLen (?)
		int packetIdx = 3;
		int packetSize;
		byte[] buffer;
		if ((imgBytesArrLen - imgBytesArrIdx) >= 1024) {
			packetSize = 1027;
			buffer = new byte[packetSize];
			if ((imgBytesArrLen - imgBytesArrIdx) == 1024)
				buffer[2] = (byte) 1;
			else
				buffer[2] = (byte) 0; 
		} else {
			packetSize = 3+imgBytesArrLen - imgBytesArrIdx; // last packet
			buffer = new byte[packetSize];
			buffer[2] = (byte) 1;
		}
		
		buffer[0] = (byte) (seqNoInt >>> 8); // store sequence no. value as two byte values
		buffer[1] = (byte) seqNoInt;
		
		// create the packet
		while (packetIdx < packetSize) { // write imgBytesArr byte values into packet
			buffer[packetIdx] = imgBytesArr[imgBytesArrIdx];
			packetIdx++;
			imgBytesArrIdx++;
		}
		
		bw.write("Created packet with sequence no. = "+seqNoInt+"\n");
		
		// send packet!
		sendPacket = new DatagramPacket(buffer, buffer.length, IPAddress, portNo);
		clientSocket.send(sendPacket);
		if (base == nextseqnum) {
			// start timer!
//			timer = scheduleTimer(timer, nextseqnum, base, clientSocket, packetsBuffer);
		}
		nextseqnum = (nextseqnum+1) % 65535;
		pktsBuffer[seqNoInt-base] = sendPacket; // add sent packet to buffer
		bw.write("Sent packet! base : "+base+"   |   nextseqnum: "+nextseqnum+"   |   placed sendPacket in packetsBuffer["+(seqNoInt-base)+"]");
		bw.write("current packet buffer = ");
		for (int i = 0; i < pktsBuffer.length; i++) {
			bw.write("["+pktsBuffer[i]+"]   ");
		}
		bw.write("\n");
	}
	
	void receiveACK() throws IOException {
		rcvPacket.setLength(2);
		clientSocket.receive(rcvPacket);
		rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] &0xff));
		
		bw.write("rcv seq no: "+rcvSeqNoInt+"\n");
		
		if (rcvSeqNoInt == base) { // received expected packet's ACK
			bw.write("ACK received. rcvSeqNoInt = "+rcvSeqNoInt+"calling update()\n");
			update(); // calls update to update variables
		}
	}
	
	// time task:
	// Note: in timer task, if only one packet (last packet to be sent) has to keep retrying
	// assume ACK packt got lost and terminate program!! 
	
	synchronized void update() throws IOException {
		bw.write("update() running...\n");
		base = (base+1) % 65535; // ACK received for the current seq no. update base
		for (int i = 0; i < windowSize-1; i++) {
			pktsBuffer[i] = pktsBuffer[i+1];
		}
		pktsBuffer[windowSize-1] = null;
		
		// --------------------------- for print ---------------------------
		bw.write("incremented base no. and shifted pktsBuffer\n");
		boolean canSendMore = false;
		int windowSizeSeqNo = (base+windowSize) % 65536; 
		if (seqNoInt < (base+windowSize))	canSendMore = true; // check whether this packet will fit in the window
		bw.write("base : "+base+
				"   |   nextseqnum: "+nextseqnum+
				"   |   seq no : "+seqNoInt+
				"   |   max window seq no : "+windowSizeSeqNo+
				"   |   can send more? "+canSendMore+"\n");
		for (int i = 0; i < pktsBuffer.length; i++) {
			DatagramPacket pkt = pktsBuffer[i];
			if (pkt != null) {
				byte[] packetBytes = pkt.getData();
				int printSeqNo = (((packetBytes[0] & 0xff) << 8) | (packetBytes[1] &0xff));						
				bw.write("["+printSeqNo+"]   ");
			} else {
				bw.write("["+pktsBuffer[i]+"]   ");
			}
		}
		bw.write("\n");
		// --------------------------- for print ---------------------------
		
		if (base == nextseqnum) {
//			timer.cancel();
			bw.write("base == nextseqnum, timer cancelled!\n");
		} else {
//			timer = scheduleTimer(timer, nextseqnum, base, clientSocket, packetsBuffer);
			bw.write("timer scheduled for base = "+base+"\n");
		}
		
	}
	// (A) synchronised method to update packet buffer array 
	
	// a synchronised thread to create a packet and send
	
	// (B) a forever loop to received ACK packet, once received, notify (A) to update status
	
	
	
//	String localhost, filename;
//	int portNo, retryTimeout, windowSize;
//	
//	DatagramSocket clientSocket;
//	DatagramPacket sendPacket;
//	DatagramPacket[] packetsBuffer;
//	byte[] buffer = new byte[1027];
//	byte[] ackBuffer = new byte[2]; // ACK value = seq no.
//
//	FileWriter fw;
//	BufferedWriter bw;
//	
//	/** Read arguments and initialise class variables; opens image file and creates an image bytes array
//	 * @param args
//	 * @return imgBytesArr
//	 * @throws IOException
//	 */
//	public static byte[] initialiseVars(String[] args) throws IOException {
//		// ================ Read arguments ===============
//		if (args.length != 5) { // ignoring WindowSize parameter, exit code 1 if missing arguments
//			System.err.println("Usage: java Sender1a localhost <Port> <Filename> [RetryTimeout] [WindowSize]");
//			System.exit(1);
//		}
//		localhost = args[0];
//		portNo = Integer.parseInt(args[1]);
//		filename = args[2];
//		retryTimeout = Integer.parseInt(args[3]);
//		windowSize = Integer.parseInt(args[4]);
//		// ================ Open image file ===============
//		File file = new File(filename);
//		FileInputStream fis = new FileInputStream(file);
//		byte[] imgBytesArr = new byte[(int) file.length()]; // current file in byte array
//		fis.read(imgBytesArr);
//		fis.close();
//		return imgBytesArr;
//	}
//	
//	private static class ACK implements Runnable {
//		public void run() {
//			try {
//				
//			} catch (InterruptedException e) {
//				
//			}
//		}
//	}
//	public static void main(String[] args) throws IOException {
//		Sender2a sender = new Sender2a(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), Integer.parseInt(args[4]));
//		sender.fw = new FileWriter("output.txt");
//		sender.bw = new BufferedWriter(fw);
//		sender.bw.write("============== Starting program ============\n");
//		
//		byte[] imgBytesArr = initialiseVars(args);
//		
//	}
//	
	
//	
//	/** TimerTask to resend all packets in the packetsBuffer */
//	static class ResendTask extends TimerTask {
//		int nextseqnum, base;
//		DatagramSocket clientSocket;
//		DatagramPacket[] packetsBuffer;
//		
//		public ResendTask(int nextseqnum, int base, DatagramSocket clientSocket, DatagramPacket[] packetsBuffer) {
//			this.nextseqnum = nextseqnum;
//			this.base = base;
//			this.clientSocket = clientSocket;
//			this.packetsBuffer = packetsBuffer;
//		}
//		
//		public void run() {
//			for (int i = 0; i < (nextseqnum-base); i++) {
//				try {
//					DatagramPacket currpkt = packetsBuffer[i];
//					byte[] packetBytes = currpkt.getData();
//					int printSeqNo = (((packetBytes[0] & 0xff) << 8) | (packetBytes[1] &0xff));						
//					bw.write("resend packet with index = "+printSeqNo);
//					clientSocket.send(packetsBuffer[i]);
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		}
//	}
//	
//	public static Timer scheduleTimer(Timer timer, int nextseqnum, int base, DatagramSocket clientSocket, DatagramPacket[] packetsBuffer) {
//		ResendTask resendTask = new ResendTask(nextseqnum, base, clientSocket, packetsBuffer);
//		timer.schedule(resendTask, (long) retryTimeout);
//		return timer;
//	}
//	
//	public static void main(String[] args) throws IOException {
//		byte[] imgBytesArr = initialiseVars(args); // read arguments and set variable values
//		Timer timer = new Timer(); // Time object
//
//		fw = new FileWriter("output.txt");
//		bw = new BufferedWriter(fw);
//
//		bw.write("Starting program\n");
//		
//		try {
//			DatagramSocket clientSocket = new DatagramSocket();
//			InetAddress IPAddress = InetAddress.getByName(localhost);
//			DatagramPacket sendPacket; // current packet to be sent 
//			DatagramPacket rcvPacket; // received packet from server
//			byte[] ackBuffer = new byte[2]; // ACK value is the sequence no.
//			int rcvSeqNoInt; // received sequence no. in integer
//
//			// ============== Sequence number related variables ==============
//			int incre = 0; // to increment sequence no.
//			int seqNoInt; // sequence no. in integer
//			int base = 0; // oldest unack'd packet starts with sequence no = 0 
//			int nextseqnum = 0; // smallest unused sequence no initially is 1 
//			DatagramPacket[] packetsBuffer = new DatagramPacket[windowSize]; 
//			
//			// for checking -----------------------------------
//			bw.write("current packet buffer = ");
//			for (int i = 0; i < packetsBuffer.length; i++) {
//				bw.write("["+packetsBuffer[i]+"]   ");
//			}
//			bw.write("\n");
//			// ------------------------------------------------
//			
//			// ============== imgBytesArr and packet idx pointers ==============
//			int imgBytesArrLen = imgBytesArr.length; // total no. of bytes
//			int imgBytesArrIdx = 0; // index pointer for imgBytesArr
//			int packetSize; // current packet size, maximum = 1027
//			int packetIdx; // index pointer for the current packet
//			
//			while (imgBytesArrIdx < imgBytesArrLen) { // while there are bytes left in the image file
//				seqNoInt = incre % 65535; // TODO: check this max number
////				seqNoInt = incre % Integer.MAX_VALUE;
//				incre++;
//				packetIdx = 3; // packet index pointer starts at 3 for image byte values
//				byte[] buffer; // buffer for current packet
//				
//				if ((imgBytesArrLen - imgBytesArrIdx) >= 1024) {
//					packetSize = 1027; // max size of pkt is 1027
//					buffer = new byte[packetSize];
//					if ((imgBytesArrLen - imgBytesArrIdx) == 1024)
//						buffer[2] = (byte) 1; 
//					else 
//						buffer[2] = (byte) 0;
//				} else {
//					packetSize = 3+imgBytesArrLen - imgBytesArrIdx; // last packet
//					buffer = new byte[packetSize];
//					buffer[2] = (byte) 1;
//				}
//				
//				buffer[0] = (byte) (seqNoInt >>> 8); // store sequence no. value as two byte values
//				buffer[1] = (byte) seqNoInt;
//
//				while (packetIdx < packetSize) { // write imgBytesArr byte values into packet
//					buffer[packetIdx] = imgBytesArr[imgBytesArrIdx];
//					packetIdx++;
//					imgBytesArrIdx++;
//				}
//				
//				bw.write("created one packet with seq no. = "+seqNoInt+"\n");
//
//				boolean canSendMore = false;
//				int windowSizeSeqNo = (base+windowSize) % 65536; 
//				if (seqNoInt < (base+windowSize))	canSendMore = true;
//				
//				bw.write("base : "+base+"   |   nextseqnum: "+nextseqnum+"   |   seq no : "+seqNoInt+"   |   max window seq no : "+windowSizeSeqNo+"   |   can send more? "+canSendMore+"\n");
//				
//				while (!canSendMore) {
//					rcvPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
//					rcvPacket.setLength(2);
//					clientSocket.receive(rcvPacket);
//					rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] &0xff));
//					
//					bw.write("rcv seq no : "+rcvSeqNoInt+"\n");
//					
//					if (rcvSeqNoInt == base) { // received expected packet's ACK
//						base = (base+1) % 65535;
//						canSendMore = true;
//						for (int i = 0; i < windowSize-1; i++) {
//							packetsBuffer[i] = packetsBuffer[i+1];
//						}
//						bw.write("incremented base no. and shifted packetsBuffer\n");
//						packetsBuffer[windowSize-1] = null;
//						bw.write("base : "+base+"   |   nextseqnum: "+nextseqnum+"   |   seq no : "+seqNoInt+"   |   max window seq no : "+windowSizeSeqNo+"   |   can send more? "+canSendMore+"\n");
//						bw.write("current packet buffer = ");
//						for (int i = 0; i < packetsBuffer.length; i++) {
//							DatagramPacket pkt = packetsBuffer[i];
//							if (pkt != null) {
//								byte[] packetBytes = pkt.getData();
//								int printSeqNo = (((packetBytes[0] & 0xff) << 8) | (packetBytes[1] &0xff));						
//								bw.write("["+printSeqNo+"]   ");
//							} else {
//								bw.write("["+packetsBuffer[i]+"]   ");
//							}
//						}
//						bw.write("\n");
//						if (base == nextseqnum) {
//							timer.cancel();
//							bw.write("base == nextseqnum, timer cancelled!\n");
//						} else {
//							timer = scheduleTimer(timer, nextseqnum, base, clientSocket, packetsBuffer);
//							bw.write("timer scheduled for base = "+base+"\n");
//						}
//					}
//				}
//				
//				sendPacket = new DatagramPacket(buffer, buffer.length, IPAddress, portNo);
//				clientSocket.send(sendPacket);
//				bw.write("packet sent+\n");
//				if (base == nextseqnum) {
//					bw.write("base == nextseqnum: schedule timer with base!\n");
//					timer = scheduleTimer(timer, nextseqnum, base, clientSocket, packetsBuffer);
//				}
//				nextseqnum = (nextseqnum+1) % 65535;
//				packetsBuffer[seqNoInt-base] = sendPacket; // add sent packet to buffer
//				bw.write("base : "+base+"   |   nextseqnum: "+nextseqnum+"   |   placed sendPacket in packetsBuffer["+(seqNoInt-base)+"]");
//				bw.write("current packet buffer = ");
//				for (int i = 0; i < packetsBuffer.length; i++) {
//					bw.write("["+packetsBuffer[i]+"]   ");
//				}
//				bw.write("\n");
//				// -----------------------------------------------------
//				System.out.println("base : "+base+"   |   nextseqnum : "+nextseqnum+"   |   packet saved in sentPackets["+(seqNoInt-base)+"]");
//			}
//		} catch (Exception e) {
//			System.err.println("Error: "+e.getMessage());
//			e.printStackTrace();
//		} finally {
//			bw.close();
//			fw.close();
//		}
//	}
	
}

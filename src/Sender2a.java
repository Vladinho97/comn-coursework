import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

/* Isabella Chan s1330027 */

public class Sender2a {
	
	public static void main(String[] args) throws IOException {
		if (args.length != 5) { // ignoring WindowSize parameter, exit code 1 if missing arguments
			System.err.println("Usage: java Sender1a localhost <Port> <Filename> [RetryTimeout] [WindowSize]");
			System.exit(1);
		}
		
		// ============== input arguments ==============
		String localhost = args[0];
		int portNo = Integer.parseInt(args[1]);
		String filename = args[2];
		int retryTimeout = Integer.parseInt(args[3]);
		int windowSize = Integer.parseInt(args[4]);


		Timer timer = new Timer();

		try {
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName(localhost);
			DatagramPacket sendPacket; // current packet to be sent 
			DatagramPacket rcvPacket; // received packet from server
			byte[] ackBuffer = new byte[2]; // ACK value is the sequence no.
			int rcvSeqNoInt;
			
			// ============== Sequence number related variables ==============
			int incre = 0; // to increment sequence no.
			int seqNoInt; // sequence no. in integer
			int base = 0; // oldest unack'd packet starts with sequence no = 0 
			int nextseqnum = 0; // smallest unused sequence no initially is 1 
			DatagramPacket[] sentPackets = new DatagramPacket[windowSize]; 
			
			class ResendTask extends TimerTask {
				private int nextseqnum, base;
				private DatagramSocket clientSocket;
				private DatagramPacket[] sentPackets;
				public ResendTask(int nextseqnum, int base, DatagramSocket clientSocket, DatagramPacket[] sentPackets) {
					this.nextseqnum = nextseqnum;
					this.base = base;
					this.clientSocket = clientSocket;
					this.sentPackets = sentPackets;
				}
				public void run() {
					for (int i = 0; i < (nextseqnum-base); i++) {
						try {
							clientSocket.send(sentPackets[i]);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}					

			// ============== Open file image ==============
			File file = new File(filename);
			FileInputStream fis = new FileInputStream(file);
			byte[] imgBytesArr = new byte[(int) file.length()]; // current file in byte array
			fis.read(imgBytesArr);
			fis.close();
			
			// ============== imgBytesArr and packet idx pointers ==============
			int imgBytesArrLen = imgBytesArr.length; // total no. of bytes
			int imgBytesArrIdx = 0; // index pointer for imgBytesArr
			int packetSize; // current packet size, maximum = 1027
			int packetIdx; // index pointer for the current packet
			
			while (imgBytesArrIdx < imgBytesArrLen) { // while there are bytes left in the image file
				seqNoInt = incre % 65535; // TODO: check this max number
//				seqNoInt = incre % Integer.MAX_VALUE;
				incre++;
				packetIdx = 3; // packet index pointer starts at 3 for image byte values
				byte[] buffer; // buffer for current packet
				
				if ((imgBytesArrLen - imgBytesArrIdx) >= 1024) {
					packetSize = 1027; // max size of pkt is 1027
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

				while (packetIdx < packetSize) { // write imgBytesArr byte values into packet
					buffer[packetIdx] = imgBytesArr[imgBytesArrIdx];
					packetIdx++;
					imgBytesArrIdx++;
				}
				
				System.out.println("======================== Created one packet with seq no: "+seqNoInt+" =====================");

				boolean canSendMore = false;
				int windowSizeSeqNo = (base+windowSize) % 65536; 
				if (seqNoInt < (base+windowSize))	canSendMore = true;
				
				System.out.println("base : "+base+"   |   nextseqnum: "+nextseqnum+"   |   seq no : "+seqNoInt+"   |   max window seq no : "+windowSizeSeqNo+"   |   can send more? "+canSendMore);
				
				while (!canSendMore) {
					rcvPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
					rcvPacket.setLength(2);
					clientSocket.receive(rcvPacket);
					rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] &0xff));
					if (rcvSeqNoInt == base) { // received expected packet's ACK
						base = (base+1) % 65535;
						canSendMore = true;
						for (int i = 0; i < windowSize-1; i++) {
							sentPackets[i] = sentPackets[i+1];
						}
						if (base == nextseqnum) {
							timer.cancel();
						} else {
							ResendTask resendTask = new ResendTask(nextseqnum, base, clientSocket, sentPackets);
							timer.schedule(resendTask, (long) retryTimeout);
							System.out.println("timer scheduled");
						}
					}
					System.out.println("rcv seq no : "+rcvSeqNoInt+"   |  base : "+base+"   |   nextseqnum : "+nextseqnum);
				}
				
				sendPacket = new DatagramPacket(buffer, buffer.length, IPAddress, portNo);
				clientSocket.send(sendPacket);
				if (base == nextseqnum) {
					ResendTask resendTask = new ResendTask(nextseqnum, base, clientSocket, sentPackets);
					timer.schedule(resendTask, (long) retryTimeout); // delay in milliseconds
					System.out.println("timer scehduled");
				}
				nextseqnum = (nextseqnum+1) % 65535;
				sentPackets[seqNoInt-base] = sendPacket;
				System.out.println("base : "+base+"   |   nextseqnum : "+nextseqnum+"   |   packet saved in sentPackets["+(seqNoInt-base)+"]");
			}
		} catch (Exception e) {
			System.err.println("Error: "+e.getMessage());
			e.printStackTrace();
		}
	}
	
}

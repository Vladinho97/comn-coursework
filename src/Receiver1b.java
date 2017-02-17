import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Receiver1b {

	public static void main(String[] args) {
		if (args.length != 2) { // ignoring WindowSize parameter for just now 
			System.err.println("Usage: Receiver1a <Port> <Filename> [WindowSize]");
			System.exit(1);
		}
		
		int portNo = Integer.parseInt(args[0]);
		String filename = args[1];
//		 int windowSize = Integer.parseInt(args[2]);
				
		try {
			int rcvSeqNo;
			int expectedSeqNo = 0;
			byte endFlag;
			
			byte[] buffer = new byte[1027];
			DatagramSocket serverSocket = new DatagramSocket(portNo);
			DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
			
			InetAddress IPAddress;
			DatagramPacket ackPacket; 
			byte[] ackBuffer = new byte[2];
			
			int check = 0;
			
			int packetSize;
			OutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
			System.out.println("================ Start receiver ==============");
			while (true) {
				serverSocket.setSoTimeout(0);
				receivePacket.setLength(1027);
//				System.out.println("packet length : "+receivePacket.getLength());
				serverSocket.receive(receivePacket);
				check++;
				System.out.println("received Packet : #"+check);
				packetSize = receivePacket.getLength();
//				System.out.println("packetSize : "+packetSize);
				IPAddress = receivePacket.getAddress();
				portNo = receivePacket.getPort();
				
				rcvSeqNo = (((buffer[0] & 0xff) << 8) | (buffer[1] & 0xff));
				ackBuffer[0] = buffer[0];
				ackBuffer[1] = buffer[1];
				endFlag = buffer[2];
				
				if (rcvSeqNo == expectedSeqNo) {
					byte[] currentBuffer = new byte[packetSize-3];
					int currIdx = 0;
					for (int i = 3; i < packetSize; i++) {
						currentBuffer[currIdx] = buffer[i];
						currIdx++;
					}
					out.write(currentBuffer);
					System.out.println("expected sequence no : " + expectedSeqNo);
					System.out.println("received sequence no : " + rcvSeqNo);
					// send ack packet
					ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, IPAddress, portNo);
					serverSocket.send(ackPacket);
					
					if (expectedSeqNo == 0) {
						expectedSeqNo = 1;
					} else {
						expectedSeqNo = 0;
					}
					System.out.println("sent ack packet");
					System.out.println("updated expectedSeqNo : " + expectedSeqNo);
					System.out.println();
					System.out.println();
					if (endFlag == ((byte) 1)) {
						out.close();
						serverSocket.close();
						System.out.println("Is last packet. Finish");
						break;
					}
				} else {
					System.out.println("Duplicate packet. send again.");
					System.out.println("expected : "+expectedSeqNo);
					System.out.println("received : "+rcvSeqNo);
				}
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}	
	
}

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class Receiver2a {

	static public int portNo, windowSize;
	static public String filename;
	
	public static void readArgs(String[] args) {
		if (args.length != 2) { // ignoring WindowSize parameter, exit code 1 if missing arguments
			System.err.println("Usage: Receiver1a <Port> <Filename> [WindowSize]");
			System.exit(1);
		}
		portNo = Integer.parseInt(args[0]); // read arguments
		filename = args[1];
//		windowSize = Integer.parseInt(args[2]);
	}
	
	public static void main(String[] args) {
		readArgs(args);
		
		try {
			byte[] buffer = new byte[1027]; // received packet buffer: 3 bytes header and 1024 bytes payload
			DatagramSocket serverSocket = new DatagramSocket(portNo);
			DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
			int clientPortNo;
			InetAddress IPAddress; // IP address from received packet

			int packetSize; // current received packet size 
			OutputStream out = new BufferedOutputStream(new FileOutputStream(filename)); // write image to file

			DatagramPacket ackPacket; // ACK packet to be sent to client
			byte[] ackBuffer = new byte[2]; // 2 byte for ACK 
			int rcvSeqNo; // sequence number received from client
			int expectedSeqNo = 0 % 65535; // expected value of received sequence number, begins with 0

			while (true) {
				receivePacket.setLength(1027);
				System.out.println("size = "+receivePacket.getLength());
				serverSocket.setSoTimeout(1000); // do nothing until a packet is received

				try { 
					serverSocket.receive(receivePacket); // receive packet from client
					packetSize = receivePacket.getLength(); // obtain information of client
					IPAddress = receivePacket.getAddress();
					clientPortNo = receivePacket.getPort();
					
					rcvSeqNo = (((buffer[0] & 0xff) << 8) | (buffer[1] & 0xff)); // received packet's sequence no.
					ackBuffer[0] = buffer[0]; // ackBuffer contains the value of the received sequence no.
					ackBuffer[1] = buffer[1];
					
					System.out.println("expected : "+expectedSeqNo+"   |   received : "+rcvSeqNo);
					System.out.println("packetSize = "+packetSize);
	//				System.out.println("packetSize = "+ packetSize);
					if (rcvSeqNo == expectedSeqNo && packetSize > 2) { // received packet is the right packet
						byte[] currBuff = new byte[packetSize-3]; // to extract image file byte values
						int currIdx = 0; // index pointer for currBuff
						for (int i = 3; i < packetSize; i++) { // write received packet's byte values into currBuff
							currBuff[currIdx] = buffer[i];
							currIdx++;
						}
						out.write(currBuff); // write into file
						
						ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, IPAddress, clientPortNo);
						serverSocket.send(ackPacket); // send ACK to client
						System.out.println("sent ackPacket for seqno  = "+rcvSeqNo);
						expectedSeqNo = (expectedSeqNo+1) % 65535; // update expected sequence no by incrementing it
						
						if (buffer[2] == ((byte) 1)) { // terminates if last packet
							out.close();
							serverSocket.close();
							break;
						}
					} else { // ACK packet lost
						ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, IPAddress, portNo);
						serverSocket.send(ackPacket); // resend ACK packet
					}
				} catch (SocketTimeoutException e) {
					
				}
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}	
}

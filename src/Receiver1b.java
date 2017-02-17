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
		int portNo = Integer.parseInt(args[0]); // read arguments
		String filename = args[1];
//		 int windowSize = Integer.parseInt(args[2]);
				
		try {			
			byte[] buffer = new byte[1027]; // 3 bytes header and 1024 bytes payload
			byte endFlag; // endFlag = 1 for last packet, 0 otherwise
			DatagramSocket serverSocket = new DatagramSocket(portNo);
			DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
			InetAddress IPAddress; // IP address from received packet
			
			DatagramPacket ackPacket; // ACK packet to be received from client
			byte[] ackBuffer = new byte[2]; // 2 byte for ACK 
			int rcvSeqNo; // sequence number received from client
			int expectedSeqNo = 0; // expected value of received sequence number
			
			int packetSize; // to keep track of current packet size 
			OutputStream out = new BufferedOutputStream(new FileOutputStream(filename)); // write image to file
			
			while (true) {
				receivePacket.setLength(1027);
				serverSocket.setSoTimeout(0); 

				serverSocket.receive(receivePacket); // receive packet from client
				packetSize = receivePacket.getLength();
				IPAddress = receivePacket.getAddress();
				portNo = receivePacket.getPort();
				
				// extract sequence number and end flag information
				// ackBuffer contains the value of the received sequence number
				rcvSeqNo = (((buffer[0] & 0xff) << 8) | (buffer[1] & 0xff));
				ackBuffer[0] = buffer[0];
				ackBuffer[1] = buffer[1];
				endFlag = buffer[2];
				
				if (rcvSeqNo == expectedSeqNo) { 
					// write bytes into file
					byte[] currentBuffer = new byte[packetSize-3];
					int currIdx = 0;
					for (int i = 3; i < packetSize; i++) {
						currentBuffer[currIdx] = buffer[i];
						currIdx++;
					}
					out.write(currentBuffer); 
					
					// send ACK to client
					ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, IPAddress, portNo);
					serverSocket.send(ackPacket);
					
					if (expectedSeqNo == 0) // update expected value of received sequence number
						expectedSeqNo = 1;
					else
						expectedSeqNo = 0;

					if (endFlag == ((byte) 1)) { // terminates if last packet
						out.close();
						serverSocket.close();
						break;
					}
				} else { // ACK packet lost
					ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, IPAddress, portNo);
					serverSocket.send(ackPacket); // resend ACK packet
				}
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}	
}

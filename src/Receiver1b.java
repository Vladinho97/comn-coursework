/* Isabella Chan s1330027 */

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Receiver1b {

	public static void main(String[] args) {
		
		if (args.length != 2) { // ignoring WindowSize parameter, exit code 1 if missing arguments
			System.err.println("Usage: Receiver1a <Port> <Filename> [WindowSize]");
			System.exit(1);
		}
		int portNo = Integer.parseInt(args[0]); // read arguments
		String filename = args[1];
//		 int windowSize = Integer.parseInt(args[2]);
				
		try {			
			byte[] buffer = new byte[1027]; // received packet buffer: 3 bytes header and 1024 bytes payload
//			byte endFlag; // endFlag = 1 for last packet, 0 otherwise
			DatagramSocket serverSocket = new DatagramSocket(portNo);
			DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
			InetAddress IPAddress; // IP address from received packet
			
			int packetSize; // current received packet size 
			OutputStream out = new BufferedOutputStream(new FileOutputStream(filename)); // write image to file
			
			DatagramPacket ackPacket; // ACK packet to be sent to client
			byte[] ackBuffer = new byte[2]; // 2 byte for ACK 
			int rcvSeqNo; // sequence number received from client
			int expectedSeqNo = 0; // expected value of received sequence number, begins with 0
			
//			int packetReceived = 0; // TODO: remove this!!!! 
//			int byteWritten = 0; // TODO: remove this!!!!
			
			while (true) {
				receivePacket.setLength(1027);
				serverSocket.setSoTimeout(0); // do nothing until a packet is received

				serverSocket.receive(receivePacket); // receive packet from client
				packetSize = receivePacket.getLength(); // obtain information of client
				IPAddress = receivePacket.getAddress();
				portNo = receivePacket.getPort();
				
				rcvSeqNo = (((buffer[0] & 0xff) << 8) | (buffer[1] & 0xff)); // received packet's sequence no.
				ackBuffer[0] = buffer[0]; // ackBuffer contains the value of the received sequence no.
				ackBuffer[1] = buffer[1];
				
//				packetReceived++; // TODO: remove this!!!
//				System.out.println("============== Received Packet no. "+packetReceived+" =============");
//				System.out.println("rcvSeqNo = "+rcvSeqNo);
				
				if (rcvSeqNo == expectedSeqNo) { // received packet is the right packet
					byte[] currBuff = new byte[packetSize-3]; // to extract image file byte values
					int currIdx = 0; // index pointer for currBuff
					for (int i = 3; i < packetSize; i++) { // write received packet's byte values into currBuff
						currBuff[currIdx] = buffer[i];
						currIdx++;
//						byteWritten++; // TODO remove this!!! 
					}
					out.write(currBuff); // write into file
					
					ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, IPAddress, portNo);
					serverSocket.send(ackPacket); // send ACK to client
					
//					System.out.println("======== sent ackPacket for packet no. "+packetReceived+" ======");
					if (expectedSeqNo == 0) // update expected value of received sequence number
						expectedSeqNo = 1;
					else
						expectedSeqNo = 0;
					
//					System.out.println("updated expected SeqNo = "+ expectedSeqNo);
//					System.out.println("no of bytes written = "+byteWritten);
					
					if (buffer[2] == ((byte) 1)) { // terminates if last packet
						out.close();
						serverSocket.close();
						break;
					}
				} else { // ACK packet lost
					ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, IPAddress, portNo);
					serverSocket.send(ackPacket); // resend ACK packet

//					packetReceived--; // TODO remove this!! 
//					System.out.println("==== Duplicate packet. Resent ackPacket for packet no. "+packetReceived+" ====");
				}
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}	
}

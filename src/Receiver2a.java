/* Isabella Chan s1330027 */

import java.io.IOException;

public class Receiver2a {

	public static void main(String[] args) throws IOException {
		if (args.length != 2) { // ignoring WindowSize parameter, exit code 1 if missing arguments
			System.err.println("Usage: Receiver1a <Port> <Filename> [WindowSize]");
			System.exit(1);
		}
		int portNo = Integer.parseInt(args[0]);
		String filename = args[1];
		
		Server server = new Server(portNo, filename);
		
		while (server.endFlag != (byte)1) {
			server.receivePacket();
		}
//		System.out.println("done receiving!");
	}
}
//	static public int portNo;
//	static public String filename;
//	static Object lock = new Object();
//
//	public static void main(String[] args) throws IOException {
//		// ================= read arguments ==================
//		if (args.length != 2) { // ignoring WindowSize parameter, exit code 1 if missing arguments
//			System.err.println("Usage: Receiver1a <Port> <Filename> [WindowSize]");
//			System.exit(1);
//		}
//		portNo = Integer.parseInt(args[0]);
//		filename = args[1];
//		
//		FileWriter fw = new FileWriter("output-receiver.txt");
//		BufferedWriter bw = new BufferedWriter(fw);
//		try {
//			DatagramSocket serverSocket = new DatagramSocket(portNo);
//
//			// ==================== for receiving ==================== 
//			byte[] buffer = new byte[1027]; // received packet buffer: 3 bytes header and 1024 bytes payload
//			DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
//			int packetSize; // current received packet size
//			int clientPortNo;
//			InetAddress clientIPAddress; // IP address from received packet
//			OutputStream out = new BufferedOutputStream(new FileOutputStream(filename)); // write image to file
//			
//			bw.write("created serverSocket : portNo : "+serverSocket.getPort()+"   |   IPAddress : "+serverSocket.getInetAddress()+"\n");
//
//			// ==================== for sending ack's ==================== 
//			byte[] ackBuffer = new byte[2]; // 2 byte for ACK
//			DatagramPacket ackPacket; // ACK packet to be sent to client
//			int rcvSeqNo; // sequence number received from client
//			int expectedSeqNo = 0 % 65535; // expected value of received sequence number, begins with 0
//
//			while (true) {
//				receivePacket.setLength(1027);
//				serverSocket.setSoTimeout(0); // do nothing until a packet is received
//				// ---------------- receiving a packet! ----------------------
//				serverSocket.receive(receivePacket); // receive packet from client
//				packetSize = receivePacket.getLength(); // obtain information of client
//				clientPortNo = receivePacket.getPort();
//				clientIPAddress = receivePacket.getAddress();
//				
//				bw.write("serverSocket : portNo : "+serverSocket.getPort()+"   |   IPAddress : "+serverSocket.getInetAddress()+"\n");
//				bw.write("packet received: packetSize : "+packetSize+"   |   clientPortNo : "+clientPortNo+"   |   clientIPAddress : "+clientIPAddress+"\n");
//				// TODO: remove this?! 
//				if (clientPortNo == portNo) { // make sure its not sending to itself????
//					System.out.println("You are sendig to yourself stupid !!!");
//				}
//				
//				rcvSeqNo = (((buffer[0] & 0xff) << 8) | (buffer[1] & 0xff)); // received packet's sequence no.
//				ackBuffer[0] = buffer[0]; // ackBuffer contains the value of the received sequence no.
//				ackBuffer[1] = buffer[1];
//
//				bw.write("expected : "+expectedSeqNo+"   |   received : "+rcvSeqNo+"\n");
//
//				if (rcvSeqNo == expectedSeqNo) { // received packet is the right packet
//					bw.write("rcvSeqNo == expectedSeqNo!\n");
//					byte[] currBuff = new byte[packetSize-3]; // to extract image file byte values
//					int currIdx = 0; // index pointer for currBuff
//					for (int i = 3; i < packetSize; i++) { // write received packet's byte values into currBuff
//						currBuff[currIdx] = buffer[i];
//						currIdx++;
//					}
//					out.write(currBuff); // write into file
//
//					ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
//					bw.write("send ack packet: rcvseqno : "+rcvSeqNo+"   |   clientIPAddress : "+clientIPAddress+"   |   clientPortNo : "+clientPortNo+"\n");
//					serverSocket.send(ackPacket); // send ACK to client
//					expectedSeqNo = (expectedSeqNo+1) % 65535; // update expected sequence no by incrementing it
//					bw.write("updated expectedSeqNo : "+expectedSeqNo+"\n");
//					if (buffer[2] == ((byte) 1)) { // terminates if last packet
//						out.close();
//						serverSocket.close();
//						fw.close();
//						bw.close();
//						break;
//					}
//
//				} else { // ACK packet lost
//					bw.write("rcvSeqNo != expectedSeqNo!\n");
//					ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
//					serverSocket.send(ackPacket); // resend ACK packet
//					bw.write("send ack packet: rcvseqno : "+rcvSeqNo+"   |   clientIPAddress : "+clientIPAddress+"   |   clientPortNo : "+clientPortNo+"\n");
//				}
//			}
//			
//		} catch (Exception e) {
//			System.err.println("Error: " + e.getMessage());
//			e.printStackTrace();
//		}
//	}
//}

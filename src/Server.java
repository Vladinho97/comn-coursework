import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;


public class Server {

	private int portNo; //, windowSize;
	private String filename;
//	private Object lock = new Object(); 
	FileWriter fw;
	BufferedWriter bw;
	
	// =========== for receiving =============
	byte[] buffer = new byte[1027];
	DatagramSocket serverSocket;
	DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
	int packetSize, clientPortNo;
	byte endFlag = (byte) 0; // for last packet
	InetAddress clientIPAddress;
	OutputStream out;
	
	// =========== for sending ack's =============
	byte[] ackBuffer = new byte[2];
	DatagramPacket ackPacket;
	int rcvSeqNo, expectedSeqNo = 0;
	
	public Server(int portNo, String filename) throws IOException {
		this.portNo = portNo;
		this.filename = filename;
		this.fw = new FileWriter("output-receiver.txt");
		this.bw = new BufferedWriter(fw);
		this.serverSocket = new DatagramSocket(portNo);
		this.out = new BufferedOutputStream(new FileOutputStream(filename)); // write image to file
	}
	
	public void receivePacket() throws IOException {
		receivePacket.setLength(1027);
		serverSocket.setSoTimeout(0);
		// -------------------- receiving a packet! ----------------------
		serverSocket.receive(receivePacket);
		packetSize = receivePacket.getLength();
		clientPortNo = receivePacket.getPort();
		clientIPAddress = receivePacket.getAddress();
		
		bw.write("serverSocket : portNo : "+serverSocket.getPort()+"   |   IPAddress : "+serverSocket.getInetAddress()+"\n");
		bw.write("packet received: packetSize : "+packetSize+"   |   clientPortNo : "+clientPortNo+"   |   clientIPAddress : "+clientIPAddress+"\n");

//		byte[] temp = new byte[2];
//		temp[0] = ackBuffer[0];
//		temp[1] = ackBuffer[1];

		rcvSeqNo = (((buffer[0] & 0xff) << 8) | (buffer[1] & 0xff)); // received packet's sequence no.
		ackBuffer[0] = buffer[0]; // ackBuffer contains the value of the received sequence no.
		ackBuffer[1] = buffer[1];

		System.out.println("expected: "+expectedSeqNo+"   |   received: "+rcvSeqNo);
		if (rcvSeqNo == expectedSeqNo) { // received packet is the right packet
			bw.write("rcvSeqNo == expectedSeqNo!\n");
			byte[] currBuff = new byte[packetSize-3]; // to extract image file byte values
			int currIdx = 0; // index pointer for currBuff
			for (int i = 3; i < packetSize; i++) { // write received packet's byte values into currBuff
				currBuff[currIdx] = buffer[i];
				currIdx++;
			}
			out.write(currBuff); // write into file

			sendACK(); 
			
			if (buffer[2] == ((byte) 1)) { // terminates if last packet
				out.close();
				serverSocket.close();
				bw.close();
				fw.close();
				endFlag = (byte) 1;
				return;
			}
		} else { // ACK packet lost
//			ackBuffer[0] = temp[0];
//			ackBuffer[1] = temp[1];
//			resendACK();
		}
	}
	
	public void sendACK() throws IOException {
		ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
		bw.write("send ack packet: rcvseqno : "+rcvSeqNo+"   |   clientIPAddress : "+clientIPAddress+"   |   clientPortNo : "+clientPortNo+"\n");
		System.out.println("send ack packet: received : "+rcvSeqNo);
		serverSocket.send(ackPacket); // send ACK to client
		expectedSeqNo = (expectedSeqNo+1) % 65535; // update expected sequence no by incrementing it
		bw.write("updated expectedSeqNo : "+expectedSeqNo+"\n");
		return;
	}
	
	public void resendACK() throws IOException {
		bw.write("rcvSeqNo != expectedSeqNo!\n");
		ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
		System.out.println("send ack packet: received : "+rcvSeqNo);
		serverSocket.send(ackPacket); // resend ACK packet
		bw.write("send ack packet: rcvseqno : "+rcvSeqNo+"   |   clientIPAddress : "+clientIPAddress+"   |   clientPortNo : "+clientPortNo+"\n");
		return;
	}
	
}


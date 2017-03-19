/* Isabella Chan s1330027 */

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public abstract class AbstractServer {

	int portNo;
	String filename;

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
	int rcvSeqNo;

	public AbstractServer(int portNo, String filename) throws IOException {
		this.portNo = portNo;
		this.filename = filename;
		this.serverSocket = new DatagramSocket(portNo);
		this.out = new BufferedOutputStream(new FileOutputStream(filename)); // write image to file
	}

	/** receives a packet and update the datagram receivePacket */
	public void receivePacket() throws IOException {
		receivePacket = new DatagramPacket(buffer, buffer.length);
		receivePacket.setLength(1027);
		serverSocket.setSoTimeout(0);
		// -------------------- receiving a packet! ----------------------
		// System.out.println("Trying to receive packet!");
		serverSocket.receive(receivePacket);
		packetSize = receivePacket.getLength();
		clientPortNo = receivePacket.getPort();
		clientIPAddress = receivePacket.getAddress();

		rcvSeqNo = (((buffer[0] & 0xff) << 8) | (buffer[1] & 0xff)); // received packet's sequence no.
		ackBuffer[0] = buffer[0]; // ackBuffer contains the value of the received sequence no.
		ackBuffer[1] = buffer[1];
		endFlag = buffer[2];
		// System.out.println("received rcvSeqNo = "+rcvSeqNo);
		return;
	}

	public abstract void ackPacket() throws IOException;

	public void closeAll() throws IOException {
		out.close();
		serverSocket.close();
		return;
	}


}

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
import java.net.SocketTimeoutException;

public abstract class AbstractServer {
	
	boolean doneACK = false; // terminates if doneACK = true
	
	// =========== Server parameters =========
	int portNo;
	String filename;

	// =========== for receiving =============
	byte[] buffer = new byte[1027];
	DatagramSocket serverSocket;
	DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
	int packetSize, clientPortNo;
	byte endFlag = (byte) 0; // for last packet
	InetAddress clientIPAddress;
	OutputStream out; // to write image file

	// =========== for sending ack's =============
	byte[] ackBuffer = new byte[2]; // two bytes containing the seq no.
	DatagramPacket ackPacket;
	int rcvSeqNo; // received sequence no. from client

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
		return;
	}

	public abstract void ackPacket() throws IOException;

	/** Only terminates if not more packets are arriving from client */
	public void waitBeforeTerminate() throws IOException {
		boolean canTerminate = false;
		int attempts = 0;
		while (!canTerminate) { // can only terminate if no more packets are arriving
			receivePacket = new DatagramPacket(buffer, buffer.length);
			receivePacket.setLength(1027);
			serverSocket.setSoTimeout(1000); // wait for one second
			try {
				serverSocket.receive(receivePacket);
				attempts = 0;
				clientPortNo = receivePacket.getPort();
				clientIPAddress = receivePacket.getAddress();
				rcvSeqNo = (((buffer[0] & 0xff) << 8) | (buffer[1] & 0xff)); // received packet's sequence no.
				ackBuffer[0] = buffer[0]; // ackBuffer contains the value of the received sequence no.
				ackBuffer[1] = buffer[1];
				ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
				serverSocket.send(ackPacket); // resend ack packet!
			} catch (SocketTimeoutException e) {
				if (attempts >= 3) { // maximum wait is 3 consecutive sec, if no packets are arriving, terminate the program
					canTerminate = true;
				}
				attempts++;
			}
		}
	}
	
	public void closeAll() throws IOException {
		out.close();
		serverSocket.close();
		return;
	}


}

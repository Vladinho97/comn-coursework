/* Isabella Chan s1330027 */

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;


public class Receiver2b {
	
	int portNo, windowSize;
	String filename;
	
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
	ArrayList<Integer> window = new ArrayList<Integer>();
	
	// =========== for sending ack's =============
	byte[] ackBuffer = new byte[2];
	DatagramPacket ackPacket;
	int rcvSeqNo, expectedSeqNo = 0;

}

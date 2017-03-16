/* Isabella Chan s1330027 */

import java.io.IOException;
import java.net.DatagramPacket;

public class Server2a extends Server {

	public Server2a(int portNo, String filename) throws IOException {
		super(portNo, filename);
	}

	public void ack_packets() throws IOException {

		rcv_packet();
		
		// update variables 
//		System.out.println("expected: "+expectedSeqNo+"   |   received: "+rcvSeqNo);
		if (rcvSeqNo == expectedSeqNo) { // received packet is the right packet
			bw.write("rcvSeqNo == expectedSeqNo!\n");
			byte[] currBuff = new byte[packetSize-3]; // to extract image file byte values
			int currIdx = 0; // index pointer for currBuff
			for (int i = 3; i < packetSize; i++) { // write received packet's byte values into currBuff
				currBuff[currIdx] = buffer[i];
				currIdx++;
			}
			out.write(currBuff); // write into file

			ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
			bw.write("send ack packet: rcvseqno : "+rcvSeqNo+"   |   clientIPAddress : "+clientIPAddress+"   |   clientPortNo : "+clientPortNo+"\n");
//			System.out.println("send ack packet: received : "+rcvSeqNo);
			serverSocket.send(ackPacket); // send ACK to client
			expectedSeqNo = (expectedSeqNo+1) % 65535; // update expected sequence no by incrementing it
			bw.write("updated expectedSeqNo : "+expectedSeqNo+"\n");
			
			if (endFlag == ((byte) 1)) { // terminates if last packet
				close_everything();
				return;
			}
		} 
	}
}


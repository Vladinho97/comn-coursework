/* Isabella Chan s1330027 */

import java.io.IOException;
import java.net.DatagramPacket;

//mount -t vboxsf dummynetshared /mnt/shared
//ipfw add pipe 100 in
//ipfw add pipe 200 out
//ipfw pipe 100 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
//ipfw pipe 200 config delay 5/25/100ms plr 0.005 bw 10Mbits/s

public class Server2a extends AbstractServer {

	int expectedSeqNo = 0; // expectedSeqNo = base number for the receiver window
	
	public Server2a(int portNo, String filename) throws IOException {
		super(portNo, filename);
	}

	@Override
	public void ackPacket() throws IOException {
		receivePacket();

		System.out.println("expected: "+expectedSeqNo+"   |   received: "+rcvSeqNo);

		if (rcvSeqNo != expectedSeqNo) 
			return;
		
		// received packet is the right packet, update variables 
		System.out.print("rcvSeqNo == expectedSeqNo!\n");
		byte[] currBuff = new byte[packetSize-3]; // to extract image file byte values
		int currIdx = 0; // index pointer for currBuff
		for (int i = 3; i < packetSize; i++) { // write received packet's byte values into currBuff
			currBuff[currIdx] = buffer[i];
			currIdx++;
		}
		out.write(currBuff); // write into file

		ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
//			System.out.println("send ack packet: received : "+rcvSeqNo);
		serverSocket.send(ackPacket); // send ACK to client
		expectedSeqNo = (expectedSeqNo+1) % 65535; // update expected sequence no by incrementing it
		
		if (endFlag == ((byte) 1)) { // terminates if last packet
			closeAll();
			return;
		}
	}

}


/* Isabella Chan s1330027 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;

/* Dummynet configuration
mount -t vboxsf dummynetshared /mnt/shared
ipfw add pipe 100 in
ipfw add pipe 200 out
ipfw pipe 100 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
ipfw pipe 200 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
*/

public class Server2a extends AbstractServer {

	int expectedSeqNo = 1; // expectedSeqNo = base number for the receiver window
	DatagramPacket lastInOrderPacket = null; // last ack'd packet
	byte[] lastInOrderAckBuffer = new byte[2]; // array to store a copy of the sequence no

	public Server2a(int portNo, String filename) throws IOException {
		super(portNo, filename);
	}

	@Override
	public void ackPacket() throws IOException {
		
		receivePacket(); // updates rcvSeqNo and ackBuffer (which will be the rcvSeqNo no matter what)

		// System.out.println("expected: "+expectedSeqNo+"   |   received: "+rcvSeqNo);

		if (rcvSeqNo != expectedSeqNo) {
			if (lastInOrderPacket != null) {
				serverSocket.send(lastInOrderPacket);
				// byte[] check = lastInOrderPacket.getData();
				// int hihi = (((buffer[0] & 0xff) << 8) | (buffer[1] & 0xff)); // received packet's sequence no.
				// System.out.println("resend : "+hihi);
			}
			return;
		}

		// received packet is the right packet, update variables
//		System.out.print("rcvSeqNo == expectedSeqNo!\n");
		byte[] outBuff = new byte[packetSize-3]; // to extract image file byte values
		int outIdx = 0; // index pointer for currBuff
		for (int i = 3; i < packetSize; i++) { // write received packet's byte values into currBuff
			outBuff[outIdx] = buffer[i];
			outIdx++;
		}
		out.write(outBuff); // write into file

		ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
		lastInOrderAckBuffer[0] = ackBuffer[0];
		lastInOrderAckBuffer[1] = ackBuffer[1];
		lastInOrderPacket = new DatagramPacket(lastInOrderAckBuffer, lastInOrderAckBuffer.length, clientIPAddress, clientPortNo);
		// System.out.println("send ack packet: received : "+rcvSeqNo);
		serverSocket.send(ackPacket); // send ACK to client
		expectedSeqNo = (expectedSeqNo+1) % 65535; // update expected sequence no by incrementing it

		if (endFlag == ((byte) 1)) { // terminates if last packet
			waitBeforeTerminate(); // waits for a grace period
			doneACK = true;
			closeAll();
			return;
		}
	}
}


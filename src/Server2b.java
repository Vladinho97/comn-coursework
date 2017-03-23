/* Isabella Chan s1330027 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/* Dummynet configuration
mount -t vboxsf dummynetshared /mnt/shared
ipfw add pipe 100 in
ipfw add pipe 200 out
ipfw pipe 100 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
ipfw pipe 200 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
*/

public class Server2b extends AbstractServer {
	int windowSize, rcvBase = 1;
	ArrayList<byte[]> windowBuffer = new ArrayList<byte[]>(); // stores byte array from received packet

	public Server2b(int portNo, String filename, int windowSize) throws IOException {
		super(portNo, filename);
		this.windowSize = windowSize;
		for (int i = 0; i < windowSize; i++) {
			windowBuffer.add(i, null); // initialise window
		}
	}

	/** Checks whether a sequence no. is within the window */
	public boolean isWithinWindow(int n) {
		if (n >= rcvBase && n <= (rcvBase+windowSize-1))
			return true;
		return false;
	}

	/** Checks whether a sequence no. is withint [rcvBase-windowSize, rcvBase-1] */
	public boolean isBelowWindow(int n) {
		if (n >= (rcvBase-windowSize) && n <=(rcvBase-1))
			return true;
		return false;
	}

	/** Helper method to check current server window */
	public void printCurrWindow() {
		 System.out.println("current windowBuffer: windowBuffer.size() = "+windowBuffer.size());
		 for (int i = 0; i < windowBuffer.size(); i++) {
		 	byte[] data = windowBuffer.get(i);
		 	if (data == null) {
		 		System.out.print("[   ]   ");
		 	} else {
		 		int currSeqNo = (((data[0] & 0xff) << 8) | (data[1] & 0xff)); // received packet's sequence no.
		 		System.out.print("["+currSeqNo+"]   ");
		 	}
		 }
		 System.out.println();
	}

	@Override
	public void ackPacket() throws IOException {

		receivePacket(); // updates rcvSeqNo and ackBuffer (which will be the rcvSeqNo no matter what)

		if (isBelowWindow(rcvSeqNo)) { // ack must be generated, although this is a packet that the receiver has previously ack'd
			ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
			serverSocket.send(ackPacket);
			return;
		}

		if (isWithinWindow(rcvSeqNo)) { // correctly received
			ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
			serverSocket.send(ackPacket);

			// add packet to windowBuffer if not already exists
			int windowBufferIdx = rcvSeqNo - rcvBase; // index in windowBuffer to store the received packet's data
			if (windowBuffer.get(windowBufferIdx) == null) { // if this packet was not previously received
				byte[] currByteArr = receivePacket.getData();
				int currByteArrLen = receivePacket.getLength();
				byte[] newCurrByteArr = new byte[currByteArrLen]; // create a copy of it
				for (int i = 0; i < currByteArrLen; i++) {
					newCurrByteArr[i] = currByteArr[i];
				}
				windowBuffer.set(windowBufferIdx, newCurrByteArr);
			}
			// printCurrWindow();

			// ----------------------- write packets to file ------------------
			if (rcvSeqNo == rcvBase) {
				while (windowBuffer.get(0) != null) {
					// write datagram packet bytes to image file
					byte[] currPacketBuff = windowBuffer.get(0);
					endFlag = currPacketBuff[2];
					byte[] outBuff = new byte[currPacketBuff.length-3]; // output buffer

					int outIdx = 0;
					for (int j = 3; j < currPacketBuff.length; j++) {
						outBuff[outIdx] = currPacketBuff[j];
						outIdx++;
					}
					out.write(outBuff);

					if (endFlag == (byte) 1) { // last packet has been written
						waitBeforeTerminate(); // waits for a grace period
						doneACK = true;
						closeAll();
						return;
					}	
					rcvBase = (rcvBase+1)%65535;
					windowBuffer.remove(0);
					windowBuffer.add(null);
				}
			}
			
			return;
		}

		// ignore the packet otherwise
	}
}

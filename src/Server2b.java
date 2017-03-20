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
	int windowSize, rcvBase = 0;
	ArrayList<byte[]> windowBuffer = new ArrayList<byte[]>();

	public Server2b(int portNo, String filename, int windowSize) throws IOException {
		super(portNo, filename);
		this.windowSize = windowSize;
		for (int i = 0; i < windowSize; i++) {
			windowBuffer.add(i, null);
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

//	boolean doneACK = false;
	@Override
	public void ackPacket() throws IOException {

		receivePacket();

		// System.out.println("rcvBase = "+rcvBase+"   |   rcvBase+windowSize-1 = "+(rcvBase+windowSize-1)+"   |   rcvSeqNo = "+rcvSeqNo);

		if (isWithinWindow(rcvSeqNo)) { // correctly received
			// System.out.println("Is within window, send ackPacket for rcvSeqNo = "+rcvSeqNo);
			ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
			serverSocket.send(ackPacket);

			// add packet to windowBuffer if not already exists
			int windowBufferIdx = rcvSeqNo - rcvBase;
			if (windowBuffer.get(windowBufferIdx) == null) { // if this packet was not previously received
				// System.out.println("ackPacket is new, add to windowBuffer at idx = "+windowBufferIdx);
				byte[] currByteArr = receivePacket.getData();
				int currByteArrLen = receivePacket.getLength();
//				System.out.println("currByteArrLen = "+currByteArrLen);
				byte[] newCurrByteArr = new byte[currByteArrLen];
				for (int i = 0; i < currByteArrLen; i++) {
					newCurrByteArr[i] = currByteArr[i];
				}
				windowBuffer.set(windowBufferIdx, newCurrByteArr);
			}
			// printCurrWindow();

			// write packets to file
			if (rcvSeqNo == rcvBase) {
			// if (windowBuffer.get(0) != null) { // i.e. rcvSeqNo == rcvBase
				// System.out.println("rcvBase is not null. write packets to file");
				int endingIdx = 0;
				for (int i = 0; i < windowSize; i++) {
					if (windowBuffer.get(i) == null) {
						endingIdx = i;
						// System.out.println("packet in idx = "+i+" is null. Stop writing packets");
						break;
					}
					if (i == (windowSize-1))
						endingIdx = windowSize;

					// write datagram packet bytes to image file
					byte[] currPacketBuff = windowBuffer.get(i);
					byte[] outBuff = new byte[currPacketBuff.length-3]; // output buffer
//					int wroteSeqNo = (((currPacketBuff[0] & 0xff) << 8) | (currPacketBuff[1] & 0xff)); // received packet's sequence no.

					int outIdx = 0;
					for (int j = 3; j < currPacketBuff.length; j++) {
						outBuff[outIdx] = currPacketBuff[j];
						outIdx++;
					}
					out.write(outBuff);
					if (currPacketBuff[2] == (byte) 1) { // last packet has been written

						// in the case of all packets have been ack'ed in the receiver, but not sender..
//						The receiver shouldn't be terminated until those unACKed packets at the sender are acknowledged.
//						Otherwise, the sender will continue to send those unACKed packets while there is no receiver.
//						One way to handle this issue is that the receiver should have some grace period to allow the sender
//						to send unACKed packets. The grace period should be renewed every time a new packet arrives at the receiver.
//
//						Your receiver stays alive for a while (i.e., some grace time period) after it receives the last packet.
//						For instance, your receiver waits for one second after the last packet was received. Within that time,
//						if some packet arrives, the receiver takes an appropriate action depending on the context (e.g., sending ACK)
//						and waits for another second. If there are no arriving packets for say 3 seconds in a row, then terminates
//						the receiver. Then, the problem will not arise any more. Here one assumption is that the RTT is much smaller
//						than 1 sec, which holds true according to the part 2b specification.
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
								if (attempts >= 3) { // maximum wait is 3 sec, if no packets are arriving, terminate the program
									canTerminate = true;
								}
								attempts++;
							}
						}
//						System.out.println("Wrote last packet! I am so done.");
						doneACK = true;

						closeAll();
						return;
					}
					rcvBase = (rcvBase+1)%65535;
					// printCurrWindow();
				}

				for (int j = 0; j < endingIdx; j++) { // remove packets
					windowBuffer.remove(0);
					windowBuffer.add(null);
					// printCurrWindow();
				}
			}
			return;
		}

		if (isBelowWindow(rcvSeqNo)) { // ack must be generated, although this is a packet that the receiver has previously ack'd
			// System.out.println("is below window, resend ack!");
			ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
			serverSocket.send(ackPacket);
			return;
		}
		// ignore the packet otherwise
	}
}

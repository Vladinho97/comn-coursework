/* Isabella Chan s1330027 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.ArrayList;

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

	boolean isDone = false;
	@Override
	public void ackPacket() throws IOException {

		receivePacket();

		// System.out.println("rcvBase = "+rcvBase
		// 		+"   |   rcvBase+windowSize-1 = "+(rcvBase+windowSize-1)
		// 		+"   |   rcvSeqNo = "+rcvSeqNo);

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
				System.out.println("currByteArrLen = "+currByteArrLen);
				byte[] newCurrByteArr = new byte[currByteArrLen];
				for (int i = 0; i < currByteArrLen; i++) {
					newCurrByteArr[i] = currByteArr[i];
				}
				windowBuffer.set(windowBufferIdx, newCurrByteArr);
			}
			// System.out.println("current windowBuffer: windowBuffer.size() = "+windowBuffer.size());
			// for (int i = 0; i < windowBuffer.size(); i++) {
			// 	byte[] data = windowBuffer.get(i);
			// 	if (data == null) {
			// 		System.out.print("[   ]   ");
			// 	} else {
			// 		int currSeqNo = (((data[0] & 0xff) << 8) | (data[1] & 0xff)); // received packet's sequence no.
			// 		System.out.print("["+currSeqNo+"]   ");
			// 	}
			// }
			// System.out.println();

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
					int wroteSeqNo = (((currPacketBuff[0] & 0xff) << 8) | (currPacketBuff[1] & 0xff)); // received packet's sequence no.

					int outIdx = 0;
					for (int j = 3; j < currPacketBuff.length; j++) {
						outBuff[outIdx] = currPacketBuff[j];
						outIdx++;
					}
					out.write(outBuff);
					if (currPacketBuff[2] == (byte) 1) { // last packet has been written
						System.out.println("Wrote last packet! I am so done.");
						isDone = true;
						closeAll();
						return;
					}
					rcvBase = (rcvBase+1)%65535;
					// System.out.println("current windowBuffer: windowBuffer.size() = "+windowBuffer.size());
				// 	for (int j = 0; j < windowBuffer.size(); j++) {
				// 		byte[] data = windowBuffer.get(j);
				// 		if (data == null) {
				// 			System.out.print("[   ]   ");
				// 		} else {
				// 			int currSeqNo = (((data[0] & 0xff) << 8) | (data[1] & 0xff)); // received packet's sequence no.
				// 			System.out.print("["+currSeqNo+"]   ");
				// 		}
				// 	}
				// 	System.out.println();
				}

				// remove packets
				for (int j = 0; j < endingIdx; j++) {
					windowBuffer.remove(0);
					windowBuffer.add(null);
					// System.out.println("window slided: ");
					// System.out.println("current windowBuffer: windowBuffer.size() = "+windowBuffer.size());
					// for (int i = 0; i < windowBuffer.size(); i++) {
					// 	byte[] data = windowBuffer.get(i);
					// 	if (data == null) {
					// 		System.out.print("[   ]   ");
					// 	} else {
					// 		int currSeqNo = (((data[0] & 0xff) << 8) | (data[1] & 0xff)); // received packet's sequence no.
					// 		System.out.print("["+currSeqNo+"]   ");
					// 	}
					// }
					// System.out.println();
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
		// Otherwise, ignore the packet!
	}

//	int windowSize;
//	ArrayList<DatagramPacket> window = new ArrayList<DatagramPacket>();
//
//	public Server2b(int portNo, String filename, int windowSize) throws IOException {
//		super(portNo, filename);
//		this.windowSize = windowSize;
//		for (int i = 0; i < windowSize; i++) {
//			window.add(null);
//		}
//	}
//
//	boolean isDone = false;
//	@Override
//	public void ackPacket() throws IOException {
//
//		System.out.println("Trying to ack packet!");
//
//		receivePacket();
//
//		// update variables based on received packet
//		System.out.println("expected: "+expectedSeqNo+"   |   received: "+rcvSeqNo);
//		if (rcvSeqNo == expectedSeqNo) {
//			System.out.println("rcvSeqNo == expectedSeqNo");
//			window.set(0, receivePacket);
//			for (int i = 0; i < windowSize; i++) {
//				if (window.get(i) == null)
//					break;
//				packetSize = window.get(i).getLength();
//				byte[] currBuff = new byte[packetSize-3];
//				int currIdx = 0;
//				for (int j = 3; j < packetSize; j++) {
//					currBuff[currIdx] = buffer[i];
//					currIdx++;
//				}
//				out.write(currBuff);
//				expectedSeqNo = (expectedSeqNo+1)%65535;
//				window.set(i, null);
//			}
//			ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
//			System.out.print("send ack packet: rcvseqno : "+rcvSeqNo+"   |   clientIPAddress : "+clientIPAddress+"   |   clientPortNo : "+clientPortNo+"\n");
//			serverSocket.send(ackPacket);
//
//			if (endFlag == (byte)1) {  // TODO: check this! does expected has to be the last packet??
//				for (int i = 0; i < windowSize; i++) {
//					if (window.get(i) != null) {
//						isDone = false;
//					}
//				}
//				if (isDone) {
//					closeAll();
//					System.out.println("done receiving packet! endFlag == 1");
//					return;
//				}
//			}
//		}
//		else if (rcvSeqNo < expectedSeqNo) { // already ack'd packet, resend ack!
//			System.out.println("rcvSeqNo < expectedSeqNo");
//			ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
//			serverSocket.send(ackPacket);
//			bw.write("send ack packet: rcvseqno : "+rcvSeqNo+"   |   clientIPAddress : "+clientIPAddress+"   |   clientPortNo : "+clientPortNo+"\n");
//			return;
//		}
//		else if (rcvSeqNo >= ((expectedSeqNo+windowSize) % 65535)) {
//			System.out.println("rcvSeqNo >= ((expectedSeqNo+windowSize) % 65535)");
//			System.out.println("Severe: Should not reach here!!!");
//		}
//		else { // packet received is within window
//			System.out.println("Packet received is within window.");
//			bw.write("packet received is within window\n");
//			DatagramPacket currPacket = receivePacket;
//			int idx = rcvSeqNo - expectedSeqNo;
//			window.set(idx, currPacket);
//		}
//	}
//
////	public void receivePacket() throws IOException {
////		receivePacket.setLength(1027);
////		serverSocket.setSoTimeout(0);
////		// -------------------- receiving a packet! ----------------------
////		serverSocket.receive(receivePacket);
////		packetSize = receivePacket.getLength();
////		clientPortNo = receivePacket.getPort();
////		clientIPAddress = receivePacket.getAddress();
////
////		bw.write("serverSocket : portNo : "+serverSocket.getPort()+"   |   IPAddress : "+serverSocket.getInetAddress()+"\n");
////		bw.write("packet received: packetSize : "+packetSize+"   |   clientPortNo : "+clientPortNo+"   |   clientIPAddress : "+clientIPAddress+"\n");
////
////		rcvSeqNo = (((buffer[0] & 0xff) << 8) | (buffer[1] & 0xff)); // received packet's sequence no.
////		ackBuffer[0] = buffer[0]; // ackBuffer contains the value of the received sequence no.
////		ackBuffer[1] = buffer[1];
////		endFlag = buffer[2];
////
////		if (rcvSeqNo>=rcvBase && rcvSeqNo<((rcvBase+windowSize)%65535)) {
////			if (rcvSeqNo == rcvBase) {
////				// send all the ack'd packets up
////				window.set(0, receivePacket);
////				for (DatagramPacket pkt : window) { // for each packet, (consequentially) write image
////					if (pkt == null)
////						break;
////					packetSize = pkt.getLength();
////					byte[] currBuff = new byte[packetSize-3];
////					int currIdx = 0;
////					for (int i = 3; i < packetSize; i++) {
////						currBuff[currIdx] = buffer[i];
////						currIdx++;
////					}
////					out.write(currBuff);
////					rcvBase = (rcvBase+1) % 65535;
////					window.set(window.indexOf(pkt), null);
////				}
////				ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
////				bw.write("send ack packet: rcvseqno : "+rcvSeqNo+"   |   clientIPAddress : "+clientIPAddress+"   |   clientPortNo : "+clientPortNo+"\n");
////				serverSocket.send(ackPacket); // send ACK to client
////				bw.write("updated expectedSeqNo : "+rcvBase+"\n");
////
////				if (endFlag == (byte) 1) { // is last packet
////					out.close();
////					serverSocket.close();
////					bw.close();
////					fw.close();
////					return;
////				}
////			} else {
////				DatagramPacket currPacket = receivePacket;
////				int idx = rcvSeqNo - rcvBase;
////				window.set(idx, currPacket);
////			}
////		} else if (rcvSeqNo < rcvBase){ // already ack'd packet, resend ack!
////			bw.write("rcvSeqNo is not >= expectedSeqNo!\n");
////			ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length, clientIPAddress, clientPortNo);
////			serverSocket.send(ackPacket);
////			bw.write("send ack packet: rcvseqno : "+rcvSeqNo+"   |   clientIPAddress : "+clientIPAddress+"   |   clientPortNo : "+clientPortNo+"\n");
////			return;
////		} else { // disregard
////			System.out.println("Severe: Should not reach here!!!");
////			bw.write("received a packet that is bigger than window size??\n");
////		}
////	}
}

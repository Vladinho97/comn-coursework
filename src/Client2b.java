/* Isabella Chan s1330027 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Timer;


public class Client2b extends AbstractClient {
	
	private ArrayList<Long> timerBuffer = new ArrayList<Long>();
	private int lastSeqNo;
	
	public Client2b(String localhost, int portNo, String filename, int retryTimeout, int windowSize) throws IOException {
		super(localhost, portNo, filename, retryTimeout, windowSize);
		for (int i = 0; i < windowSize; i++) {
			pktsBuffer.add(null);
			timerBuffer.add(null);
		}
	}
	
	/** Checks whether a sequence no. is within the window */
	public boolean isWithinWindow(int n) {
		if (n >= base && n <= (base+windowSize-1))
			return true;
		return false;
	}

	boolean doneSEND;
	@Override
	public void sendPacket() throws IOException {
		if (imgBytesArrIdx >= imgBytesArrLen) {
			doneSEND = true;
			return;
		}
		if (!isWithinWindow(nextseqnum)) { // next seqNoInt is not within window
			return;
		}
		synchronized (lock) {
			seqNoInt = incre % 65535;
			incre++;
			// ------------------------------ create new packet --------------------------------
			int packetIdx = 3, packetSize;
			byte[] buffer;
			if ((imgBytesArrLen - imgBytesArrIdx) >= 1024) {
				packetSize = 1027;
				buffer = new byte[packetSize];
				if ((imgBytesArrLen - imgBytesArrIdx) == 1024)	endFlag = (byte) 1;
				else	endFlag = (byte) 0;
			} else {
				packetSize = 3+imgBytesArrLen - imgBytesArrIdx; // last packet
				buffer = new byte[packetSize];
				endFlag = (byte) 1;
			}
			buffer[0] = (byte) (seqNoInt >>> 8); // store sequence no. value as two byte values
			buffer[1] = (byte) seqNoInt;
			buffer[2] = endFlag;
			
			if (endFlag == (byte) 1) {
				lastSeqNo = seqNoInt;
			}
			
			while (packetIdx < packetSize) { // write imgBytesArr byte values into packet
				buffer[packetIdx] = imgBytesArr[imgBytesArrIdx];
				packetIdx++;
				imgBytesArrIdx++;
			}
			// ------------------------------ send the packet --------------------------------
			sendPacket = new DatagramPacket(buffer, buffer.length, IPAddress, portNo);
			clientSocket.send(sendPacket);
			// is this first packet?
			if (isFirstPacket) {
				startTime = System.currentTimeMillis();
				isFirstPacket = false;
			}
			// ------------------------------ update values --------------------------------
//			if (base == nextseqnum) { // if no unAck'd packets
//				
//			}
			nextseqnum = (nextseqnum+1) % 65535; // increment next available sequence no
			pktsBuffer.set(seqNoInt-base, sendPacket); // add sent packet to packets buffer
			timerBuffer.set(seqNoInt-base, System.currentTimeMillis());
		}
	}

	boolean doneACK = false;
	@Override
	public void ackPacket() throws IOException {
		
		int oldestTimer = getOldestTimer();
		int setTimeout = 0;
		if (oldestTimer == 0) {
			// no timer has been set?
			System.out.println("oldestTimer == 0... no timer has been set.. should this even occur???");
		} else {
			System.out.println("Found one oldest timer!");
			setTimeout = retryTimeout - oldestTimer;
		}
		
		try {
			rcvPacket.setLength(2);
			clientSocket.setSoTimeout(setTimeout);
			clientSocket.receive(rcvPacket);
			ackBuffer = rcvPacket.getData();
			rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] & 0xff));
			
			System.out.println("base = "+base+"   |   rcvSeqNoInt = "+rcvSeqNoInt+"   |   nextSeqNoInt = "+nextseqnum+"   |   base+windowSize-1 = "+(base+windowSize-1));
			
			synchronized (lock) {
				if (!isWithinWindow(rcvSeqNoInt)) {
					System.out.println("received packet not within window wtf?");
					return; 
				}
				
				int idx = rcvSeqNoInt-base;
				if (pktsBuffer.get(idx) != null) { // packet not yet been ack
					System.out.println("this packet has not been ack. Now ack it");
					pktsBuffer.set(idx, null);
					timerBuffer.set(idx, null);
				}
				
				if (pktsBuffer.get(0) == null) { // move sliding window
					int endingIdx = 0;
					System.out.println("base is now null. base = "+base);
					for (int i = 0; i < pktsBuffer.size(); i++) {
						if (pktsBuffer.get(i) != null) {
							endingIdx = i;
							break;
						}
						System.out.println("increment base = "+base);
						base = (base+1)%65535;
						if (endFlag == (byte)1 && base == lastSeqNo) {
							doneACK = true;
						}
					}
					System.out.println("slide the window");
					for (int i = 0; i < endingIdx; i++) {
						pktsBuffer.remove(i);
						pktsBuffer.add(null);
						timerBuffer.remove(i);
						timerBuffer.add(null);
					}
				}
			}
		} catch (SocketTimeoutException e) {
			System.out.println("Time out occur!!!");
			for (int i = 0; i < timerBuffer.size(); i++) {
				if (timerBuffer.get(i) == oldestTimer) {
					clientSocket.send(pktsBuffer.get(i));
					timerBuffer.set(i, System.currentTimeMillis());
					System.out.println("resend pkt for idx = "+i+" and reset timer.");
				}
			}
		}
	}

	public int getOldestTimer() throws IOException {
		synchronized (lock) {
			long oldest = 0;
			for (int i = 0; i < timerBuffer.size(); i++) {
				if (timerBuffer.get(i) != null && (System.currentTimeMillis()-timerBuffer.get(i)) >= retryTimeout) {
					System.out.println("current timer in buffer time out! resend packet");
					clientSocket.send(pktsBuffer.get(i));
					timerBuffer.set(i, System.currentTimeMillis());
				}
				else if (timerBuffer.get(i) != null && timerBuffer.get(i) >= oldest) {
						oldest = timerBuffer.get(i);
				}
			}
			int	oldestInt = (int) oldest;
			return oldestInt;
		}
	}
	
	@Override
	public void resendPacket() throws IOException {
//		synchronized (lock) {
//			for (int i = 0; i < timerBuffer.size(); i++) {
//				Long timerTime = timerBuffer.get(i);
//				if (timerTime != null && (System.nanoTime() - timerTime) >= retryTimeout) {
//					System.out.println("Timeout occured for packet at pktsBuffer with idx = "+i);
//					DatagramPacket currPacket = pktsBuffer.get(i);
//					clientSocket.send(currPacket);
//					System.out.println("resend the packet!");
//				}
//			}
//		}
	}
	
	
//	/** 
//	 * Even for part 2b, although you need to be more careful about timeout 
//	 * check for each packet, multi-threading is not absolutely necessary. 
//	 * The trick is when a (retransmitted) packet is sent, the absolute time 
//	 * that the packet was sent should be recorded. Then, before the sender 
//	 * calls recvfrom(), it should scan the sent times for all unacked packets, 
//	 * find out the timestamp of the oldest unacked packet and calls 
//	 * setSoTimeout() by adjusting (computing) the timeout value based on the 
//	 * timestamp and the current time.
//	 */
//
//	private ArrayList<Long> timers = new ArrayList<Long>(); // a timer for each packet
//	
//	public Client2b(String localhost, int portNo, String filename,
//			int retryTimeout, int windowSize) throws IOException {
//		super(localhost, portNo, filename, retryTimeout, windowSize);
//	}
//	
//	boolean doneSEND = false;
//	public void sendPacket() throws IOException {
//		if (imgBytesArrIdx >= imgBytesArrLen) {
//			bw.write("sendPacket(): no more packets to be sent\n");
//			doneSEND = true;
//			return;
//		}
//		if (pktsBuffer.size() >= windowSize) {
//			return;
//		}
//		synchronized (lock) {
//			bw.write("sendPacket(): locked object\n");
//			seqNoInt = incre % 65535;
//			incre++;
//			bw.write("sendPacket(): creating a packet with seqNoInt = "+seqNoInt+"    |    base : "+base+"   |   nextseqnum : "+nextseqnum+"\n");
//			// ------------------------------ create new packet --------------------------------
//			int packetIdx = 3, packetSize;
//			byte[] buffer;
//			if ((imgBytesArrLen - imgBytesArrIdx) >= 1024) {
//				packetSize = 1027;
//				buffer = new byte[packetSize];
//				if ((imgBytesArrLen - imgBytesArrIdx) == 1024)	endFlag = (byte) 1;
//				else	endFlag = (byte) 0;
//			} else {
//				packetSize = 3+imgBytesArrLen - imgBytesArrIdx; // last packet
//				buffer = new byte[packetSize];
//				endFlag = (byte) 1;
//			}
//			buffer[0] = (byte) (seqNoInt >>> 8); // store sequence no. value as two byte values
//			buffer[1] = (byte) seqNoInt;
//			buffer[2] = endFlag;
//			while (packetIdx < packetSize) { // write imgBytesArr byte values into packet
//				buffer[packetIdx] = imgBytesArr[imgBytesArrIdx];
//				packetIdx++;
//				imgBytesArrIdx++;
//			}
//			// ------------------------------ send the packet --------------------------------
//			sendPacket = new DatagramPacket(buffer, buffer.length, IPAddress, portNo);
//			clientSocket.send(sendPacket);
//			bw.write("sendPacket(): sent packet to IPAddress : "+IPAddress+"    |    portNo : "+portNo);
//			// is this first packet?
//			if (isFirstPacket) {
//				startTime = System.nanoTime();
//				isFirstPacket = false;
//			}
//			// ------------------------------ update values --------------------------------
//			if (base == nextseqnum) { // if no unAck'd packets
////				timer.cancel();
////				timer = new Timer();
////				timer.schedule(new ResendTask(this), retryTimeout);
//			}
//			nextseqnum = (nextseqnum+1) % 65535; // increment next available sequence no
//			pktsBuffer.add(sendPacket); // add sent packet to packets buffer
//			timers.add(System.nanoTime()); //schedule a timer
//			
//			bw.write("sendPacket(): sent packet. seqNoInt : "+seqNoInt+"   |   base : "+base+"   |   nextseqnum : "+nextseqnum+"    |   pktsBuffer.Size() : "+pktsBuffer.size()+"\n");
//		}
//	}
//	
//	boolean doneACK = false;
//	public void ackPacket() throws IOException { // receives any packet
//		rcvPacket.setLength(2);
//		bw.write("ackPacket(): Acking normal packets\n");
//		clientSocket.setSoTimeout(0);
//		bw.write("ackPacket(): clientSocket.getPort() : "+clientSocket.getPort()+"   |   clientSocket.getInetAddress(): "+clientSocket.getInetAddress()+"\n");
//		synchronized(lock) { // TODO: create a listener!!
//			// resend neceessary packets?! 
//			Long currTime = System.nanoTime();
//			for (int i = 0; i < pktsBuffer.size(); i++) {
//				Long duration = currTime - timers.get(i);
//				if (duration >= retryTimeout) {
//					clientSocket.send(pktsBuffer.get(i));
//					timers.add(i, currTime); // reschedule timer
//				}
//			}
//		}
//		// resend all necessary packets, try to receive packet now!!
//		clientSocket.receive(rcvPacket);
//		ackBuffer = rcvPacket.getData();
//		rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] & 0xff));
//		bw.write("ackPacket(): received packet with rcvSeqNoInt : "+rcvSeqNoInt+"\n");
//		synchronized (lock) {
//			bw.write("ackPacket(): locked object\n"
//					+ "ackPacket(): base : "+base+"   |   nextseqnum : "+nextseqnum+"\n");
//			if (rcvSeqNoInt >= base) {
//				bw.write("ackPacket(): rcvSeqNoInt >= base, update variables.\n");
//				int idx = rcvSeqNoInt-base+1;
//				pktsBuffer.remove(idx);
//				timers.remove(idx);
//				if (endFlag==(byte)1 && pktsBuffer.size()==0) { // acking last packet
//					doneACK = true;
//					closeAll();
//					return;
//				}
//				if (rcvSeqNoInt == base) {
//					base = (base+1) % 65535;
//					bw.write("ackPacket(): update base : "+base+"   |   pktsBuffer.size() : "+pktsBuffer.size()+"\n");
//					if (base == nextseqnum) {
//						System.out.println("base == nextseqnum. pktsBuffer.size() = "+pktsBuffer.size());
//////						System.out.println("base == nextseqnum, cancel timer");
////						timer.cancel();
////						bw.write("ackPacket(): base == nextseqnum, timer cancelled.\n");
//					} else {
////						timer.cancel();
////						timer = new Timer();
////						timer.schedule(new ResendTask(this), retryTimeout);
//////						System.out.println("Schedule new timer");
////						bw.write("ackPacket(): base != nextseqnum. new timer and scheduled.\n");
//					}
//				}
//			} else {
//				bw.write("ackPacket(): rcvSeqNoInt is not >= base. disregard\n");
//			}
//		}
//	}
//	
//	public void resendPacket() {
//		// not implemented
//	}

}

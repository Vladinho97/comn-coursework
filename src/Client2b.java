/* Isabella Chan s1330027 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Timer;

/** Dummynet configuration
mount -t vboxsf dummynetshared /mnt/shared
ipfw add pipe 100 in
ipfw add pipe 200 out
ipfw pipe 100 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
ipfw pipe 200 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
*/

public class Client2b extends AbstractClient {

	private ArrayList<Long> timersBuffer = new ArrayList<Long>(); // to store all timers

	public Client2b(String localhost, int portNo, String filename, int retryTimeout, int windowSize) throws IOException {
		super(localhost, portNo, filename, retryTimeout, windowSize);
		for (int i = 0; i < windowSize; i++) {
			pktsBuffer.add(null);
			timersBuffer.add(null);
		}
	}

	/** Checks whether a sequence no. is within the window */
	public boolean isWithinWindow(int n) {
		if (n >= base && n <= (base+windowSize-1))
			return true;
		return false;
	}

	/** Checks whether the seq no is one of the sequence no. that has been sent */
	public boolean isWithinSent(int n) {
		if (n >= base && n < nextseqnum)
			return true;
		return false;
	}
	
	/** Checks whether a timer has timed out */
	public boolean isTimeout(long timer) {
		if (System.nanoTime()-timer >= retryTimeout) 
			return true;
		return false;
	}
	
	/** Prints current pktsbuffer for checking purposes */
	public void printPktsBuffer() {
		System.out.println("current pktsBuffer: ");
		for (int i = 0; i < pktsBuffer.size(); i++) {
			DatagramPacket currPkt = pktsBuffer.get(i);
			if (currPkt == null) {
				System.out.print("[   ]   ");
			} else {
				byte[] data = currPkt.getData();
				int currSeqNo = (((data[0] & 0xff) << 8) | (data[1] & 0xff)); // received packet's sequence no.
				System.out.print("["+currSeqNo+"]   ");
			}
		}
		System.out.println();
	}

//	boolean doneSEND = false;
	@Override
	public void sendPacket() throws IOException {
		// System.out.println("sendPacket(): nextseqnum = "+nextseqnum);
		if (imgBytesArrIdx >= imgBytesArrLen) {
			doneSEND = true;
			return;
		}
		if (!isWithinWindow(nextseqnum)) { // do not send if next seqNoInt is not within window
			return;
		}
		synchronized (lock) {
//			System.out.println("sendPacket(): base = "+base+"   |   base+N-1 = "+(base+windowSize-1)+"   |   nextseqnum = "+nextseqnum);
			seqNoInt = incre % 65535;
			incre++;
			
			byte[] buffer = createPacket(); // create new packet 
			// ------------------------------ send the packet --------------------------------
			sendPacket = new DatagramPacket(buffer, buffer.length, IPAddress, portNo);
			clientSocket.send(sendPacket);
			long sendPacketTime = System.nanoTime();
			
			if (isFirstPacket) { // is this first packet?
				startTime = sendPacketTime;
				isFirstPacket = false;
			}
			
			// ------------------------------ update values --------------------------------
			nextseqnum = (nextseqnum+1) % 65535; // increment next available sequence no
			pktsBuffer.set(seqNoInt-base, sendPacket); // add sent packet to packets buffer
			timersBuffer.set(seqNoInt-base, sendPacketTime);
			// printPktsBuffer();
		}
	}
	
//	boolean doneACK = false;
	@Override
	public void ackPacket() throws IOException {
		rcvPacket.setLength(2);
		clientSocket.setSoTimeout(0);
		clientSocket.receive(rcvPacket);
		ackBuffer = rcvPacket.getData();
		rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] & 0xff));
//		System.out.println("received a packet! base = "+base+"   |   base+N-1 = "+(base+windowSize-1)+"   |   nextSeqNoInt = "+nextseqnum+"   |   rcvSeqNoInt = "+rcvSeqNoInt);

		synchronized (lock) {
			if (!isWithinSent(rcvSeqNoInt)) {
//				System.out.println("received packet not within sent?");
				return;
			}

			if (pktsBuffer.get(rcvSeqNoInt-base) != null) { // packet not yet been ack
//				System.out.println("this packet has not been ack. Now ack it");
				pktsBuffer.set(rcvSeqNoInt-base, null);
				timersBuffer.set(rcvSeqNoInt-base, null);
			}
//			printPktsBuffer();
			
			// ---------------------------------- move sliding window --------------------------------
			if (pktsBuffer.get(0) == null) { // move sliding window
				int endingIdx = 0; // endingIdx is the index for the pktsBuffer containing a packet that has not been ack'd
//				System.out.println("base is now null. base = "+base);
				for (int i = 0; i < pktsBuffer.size(); i++) {
					if (pktsBuffer.get(i) != null) {
						endingIdx = i;
//						System.out.println("pktsBuffer.get("+i+") != null. endingIdx = "+i);
						break;
					}
					if (base == nextseqnum) {
						endingIdx = i;
//						System.out.println("base == nextseqnum: base = "+base+"   |   nextseqnum = "+nextseqnum);
						break;
					}
					if (endFlag == (byte)1 && base == lastSeqNo) { // TODO: check whether both conditions are needed????
						doneACK = true;
						closeAll(); // updates end time
						return;
					}
					base = (base+1) % 65535;
//					System.out.println("incremented base = "+base);
				}
//				System.out.println("slide the window! endingIdx = "+endingIdx);
				for (int i = 0; i < endingIdx; i++) { // slide the window
					pktsBuffer.remove(0);
					pktsBuffer.add(null);
					timersBuffer.remove(0);
					timersBuffer.add(null);
				}
//				System.out.println("window slided! base = "+base+"   |   base+N-1 = "+(base+windowSize-1)+"   |   nextSeqNoInt = "+nextseqnum+"   |   rcvSeqNoInt = "+rcvSeqNoInt);
			}
//			printPktsBuffer();
		}
	}

	@Override
	public void resendPackets() throws IOException {
		synchronized (lock) {
			for (int i = 0; i < timersBuffer.size(); i++) {
				if (timersBuffer.get(i) != (Long) null && isTimeout(timersBuffer.get(i))) {
//				if (timersBuffer.get(i) != (Long) null && System.nanoTime()-timersBuffer.get(i) >= retryTimeout) {
					clientSocket.send(pktsBuffer.get(i));
					timersBuffer.set(i, System.nanoTime());
				}
			}
		}
	}
		
	@Override
	public void printOutputs() {
		System.out.println("================== Part2b: output ==================");
//		System.out.println("No of retransmission = "+noOfRetransmission);
		estimatedTimeInNano = endTime - startTime; 
		estimatedTimeInSec = ((double)estimatedTimeInNano)/1000000000.0; // convert from nano-sec to sec
		throughput = fileSizeKB/estimatedTimeInSec;
		System.out.println("Throughput = "+throughput);
		System.out.println("================== Program terminates ==================");
	}
}

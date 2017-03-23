/* Isabella Chan s1330027 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/** Dummynet configuration
mount -t vboxsf dummynetshared /mnt/shared
ipfw add pipe 100 in
ipfw add pipe 200 out
ipfw pipe 100 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
ipfw pipe 200 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
*/

class ResendTask2b extends TimerTask {
	Client2b client2b;
	DatagramPacket sendPacket;
	Timer timer;
	public ResendTask2b(Client2b client2b, DatagramPacket sendPacket, Timer timer) {
		this.client2b = client2b;
		this.sendPacket = sendPacket;
		this.timer = timer;
	}
	@Override
	public void run() {
		try {
			System.out.println("calling resend task 2b!!!");
			client2b.resendCurrPkt(sendPacket, timer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
		return;
	}
}

public class Client2b extends AbstractClient {

	private ArrayList<Timer> timersBuffer = new ArrayList<Timer>(); // to store all timers

	public Client2b(String localhost, int portNo, String filename, int retryTimeout, int windowSize) throws IOException {
		super(localhost, portNo, filename, retryTimeout, windowSize);
		for (int i = 0; i < windowSize; i++) { // initialise timers and packets buffers
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

	public void resendCurrPkt(DatagramPacket sendPacket, Timer timer) throws IOException {
		System.out.println("------- resend a packet! ---------");
		clientSocket.send(sendPacket);
		timer.cancel();
		timer = new Timer();
		timer.schedule(new ResendTask2b(this, sendPacket, timer), retryTimeout);
	}
	
	@Override
	public void sendPacket() throws IOException {
		if (imgBytesArrIdx >= imgBytesArrLen) {
			doneSEND = true;
			return;
		}
		if (!isWithinWindow(nextseqnum)) { // do not send if next seqNoInt is not within window
			return;
		}
		synchronized (lock) {
			seqNoInt = incre % 65535;
			incre++;
			
			byte[] buffer = createPacket(); // create new packet 
			
			// ------------------------------ send the packet --------------------------------
			sendPacket = new DatagramPacket(buffer, buffer.length, IPAddress, portNo);
			clientSocket.send(sendPacket);
			Timer timer = new Timer();
			timer.schedule(new ResendTask2b(this, sendPacket, timer), retryTimeout);
			long sendPacketTime = System.nanoTime();
			
			if (isFirstPacket) { // is this first packet?
				startTime = sendPacketTime;
				isFirstPacket = false;
			}
			
			// ------------------------------ update values --------------------------------
			nextseqnum = (nextseqnum+1) % 65535; // increment next available sequence no
			pktsBuffer.set(seqNoInt-base, sendPacket); // add sent packet to packets buffer
			timersBuffer.set(seqNoInt-base, timer);
			// printPktsBuffer();
		}
	}
	
	@Override
	public void ackPacket() throws IOException {
		
		receivePacket(); // updates rcvSeqNo and ackBuffer
		
		synchronized (lock) {
			if (!isWithinSent(rcvSeqNoInt)) {
				return;
			}

			if (pktsBuffer.get(rcvSeqNoInt-base) != null) { // packet not yet been ack
				pktsBuffer.set(rcvSeqNoInt-base, null);
				timersBuffer.set(rcvSeqNoInt-base, null);
			}
//			printPktsBuffer();
			
			// ---------------------------------- move sliding window --------------------------------
			if (pktsBuffer.get(0) == null) { // move sliding window
				int endingIdx = 0; // endingIdx is the index for the pktsBuffer containing a packet that has not been ack'd
				for (int i = 0; i < pktsBuffer.size(); i++) {
					if (pktsBuffer.get(i) != null) {
						endingIdx = i;
						break;
					}
					if (base == nextseqnum) {
						endingIdx = i;
						break;
					}
					if (endFlag == (byte)1 && base == lastSeqNo) {
						doneACK = true;
						closeAll(); // updates end time
						return;
					}
					base = (base+1) % 65535;
				}
				for (int i = 0; i < endingIdx; i++) { // slide the window
					pktsBuffer.remove(0);
					pktsBuffer.add(null);
					timersBuffer.get(0).cancel();
					timersBuffer.remove(0);
					timersBuffer.add(null);
				}
			}
//			printPktsBuffer();
		}
	}

	@Override
	public void resendPackets() throws IOException {
		
	}
		
	@Override
	public void printOutputs() {
		estimatedTimeInNano = endTime - startTime; 
		estimatedTimeInSec = ((double)estimatedTimeInNano)/1000000000.0; // convert from nano-sec to sec
		throughput = fileSizeKB/estimatedTimeInSec;
		System.out.println("--------------------- Part2b output --------------------");
		System.out.println("Throughput = "+throughput);
		System.out.println("------------------ Program Terminates ------------------");
	}
}

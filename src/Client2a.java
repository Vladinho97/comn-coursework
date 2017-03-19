/* Isabella Chan s1330027 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Timer;
import java.util.TimerTask;

/** Dummynet configuration
mount -t vboxsf dummynetshared /mnt/shared
ipfw add pipe 100 in
ipfw add pipe 200 out
ipfw pipe 100 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
ipfw pipe 200 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
*/

class ResendTask extends TimerTask {
	private Client2a client;
	public ResendTask(Client2a client) {
		this.client = client;
	}
	@Override
	public void run() {
		try {
			client.resendPackets();
		} catch (IOException e) {
//			e.printStackTrace();
		}
		return;
	}
}

public class Client2a extends AbstractClient {
	
	private Timer timer = new Timer();

	public Client2a(String localhost, int portNo, String filename,
			int retryTimeout, int windowSize) throws IOException {
		super(localhost, portNo, filename, retryTimeout, windowSize);
	}

	boolean doneSEND = false; 
	/** Client only sends if there are bytes left in file and there is space in the window */
	public void sendPacket() throws IOException {
		if (imgBytesArrIdx >= imgBytesArrLen) {
			doneSEND = true;
			return;
		}
		if (pktsBuffer.size() >= windowSize) {
			return;
		}
		synchronized (lock) {
			seqNoInt = incre % 65535;
			incre++;
			// ------------------------------ create new packet --------------------------------
			int packetIdx = 3;
			int packetSize;
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

			if (endFlag == (byte)1)
				lastSeqNo = seqNoInt; // store sequence no for final packet for acking purpose
			
			buffer[0] = (byte) (seqNoInt >>> 8); // store sequence no. value as two byte values
			buffer[1] = (byte) seqNoInt;
			buffer[2] = endFlag;
			
			while (packetIdx < packetSize) { // write imgBytesArr byte values into packet
				buffer[packetIdx] = imgBytesArr[imgBytesArrIdx];
				packetIdx++;
				imgBytesArrIdx++;
			}
			// ------------------------------ send the packet --------------------------------
			sendPacket = new DatagramPacket(buffer, buffer.length, IPAddress, portNo);
			clientSocket.send(sendPacket);
			// set time for first packet
			if (isFirstPacket) {
				startTime = System.nanoTime();
				isFirstPacket = false;
			}
			// ------------------------------ update values --------------------------------
			if (base == nextseqnum) { // if no unAck'd packets
				timer.cancel();
				timer = new Timer();
				timer.schedule(new ResendTask(this), retryTimeout);
			}
			nextseqnum = (nextseqnum+1) % 65535; // increment next available sequence no
			pktsBuffer.add(sendPacket); // add sent packet to packets buffer
		}
	}

	boolean doneACK = false;
	/** Tries to receive a packet (with sequence no = base) and ack it. */
	public void ackPacket() throws IOException {
		rcvPacket.setLength(2);
		clientSocket.setSoTimeout(0);
		clientSocket.receive(rcvPacket);
		ackBuffer = rcvPacket.getData();
		rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] & 0xff));
//			System.out.println("base : "+base+"   |   received : "+rcvSeqNoInt+"   |   nextseqnum : "+nextseqnum);
		synchronized (lock) {
			
			if (rcvSeqNoInt < base) 
				return;
			
			// -------------- ack packet if received sequence no. is greater than equal to base no.-----------------
			for (int i = 0; i < (rcvSeqNoInt-base+1); i++) {
				pktsBuffer.remove(0); // removes packets from buffer
			}
			
			base = (rcvSeqNoInt+1) % 65535;
			
			if (rcvSeqNoInt == lastSeqNo) { // ack'ed last packet
				doneACK = true;
				timer.cancel();
				closeAll();
				return;
			}
			
			if (base == nextseqnum) { // no more unAck'ed packet
//			System.out.println("base == nextseqnum, cancel timer");
				timer.cancel();
			} else {
				timer.cancel();
				timer = new Timer();
				timer.schedule(new ResendTask(this), retryTimeout);
//			System.out.println("base != nextseqnum. Schedule new timer");
			}
				
		}
	}

	@Override
	public void resendPackets() throws IOException {
		synchronized (lock) {
//			System.out.println("resendPackets(): base : "+base+"    |   nextseqnum : "+nextseqnum+"   |   seqNoInt : "+seqNoInt+"   |    pktsBuffer.size() : "+pktsBuffer.size());
			for (int i = 0; i < pktsBuffer.size(); i++) {
				clientSocket.send(pktsBuffer.get(i));
				noOfRetransmission++;
			}
			timer.cancel();
			timer = new Timer();
			timer.schedule(new ResendTask(this), retryTimeout);
//			System.out.println("scheduled timer");
		}
	}

	@Override
	public void printOutputs() {
		System.out.println("================== Part2a: output ==================");
		System.out.println("No of retransmission = "+noOfRetransmission);
		estimatedTimeInNano = endTime - startTime; 
		estimatedTimeInSec = ((double)estimatedTimeInNano)/1000000000.0; // convert from nano-sec to sec
		throughput = fileSizeKB/estimatedTimeInSec;
		System.out.println("Throughput = "+throughput);
		System.out.println("================== Program terminates ==================");
	}
}

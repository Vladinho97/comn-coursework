/* Isabella Chan s1330027 */

import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Timer;
import java.util.TimerTask;

//mount -t vboxsf dummynetshared /mnt/shared
//ipfw add pipe 100 in
//ipfw add pipe 200 out
//ipfw pipe 100 config delay 5/25/100ms plr 0.005 bw 10Mbits/s
//ipfw pipe 200 config delay 5/25/100ms plr 0.005 bw 10Mbits/s

class ResendTask extends TimerTask {
	private Client2a client;
	public ResendTask(Client2a client) {
		this.client = client;
	}
	@Override
	public void run() {
		try {
//			client.bw.write("+++++++++++++++++++++ Running timer task ++++++++++++++++++++++");
			client.resendPacket();
//			client.bw.write("+++++++++++++++++++++ Finish timer task +++++++++++++++++++++++");
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
	// tries to receive a packet - the packet with base sequence no
	public void ackPacket() throws IOException {
		rcvPacket.setLength(2);
		clientSocket.setSoTimeout(0);
		clientSocket.receive(rcvPacket);
		ackBuffer = rcvPacket.getData();
		rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] & 0xff));
//			System.out.println("base : "+base+"   |   received : "+rcvSeqNoInt+"   |   nextseqnum : "+nextseqnum);
		synchronized (lock) {
			if (rcvSeqNoInt >= base) {
				for (int i = 0; i < (rcvSeqNoInt-base+1); i++) {
					pktsBuffer.remove(0);
				}
				base = (rcvSeqNoInt+1) % 65535;
				if (endFlag==(byte)1 && pktsBuffer.size()==0) { // acked last packet
					doneACK = true;
					timer.cancel();
					closeAll();
					return;
				}
				if (base == nextseqnum) {
//						System.out.println("base == nextseqnum, cancel timer");
					timer.cancel();
				} else {
					timer.cancel();
					timer = new Timer();
					timer.schedule(new ResendTask(this), retryTimeout);
//						System.out.println("Schedule new timer");
				}
			} else {
				
			}
		}
//		}
	}

	public void resendPacket() throws IOException {
		synchronized (lock) {
//			System.out.println("resendPackets(): base : "+base+"    |   nextseqnum : "+nextseqnum+"   |   seqNoInt : "+seqNoInt+"   |    pktsBuffer.size() : "+pktsBuffer.size());
			for (int i = 0; i < pktsBuffer.size(); i++) {
				DatagramPacket currPkt = pktsBuffer.get(i);
				byte[] dataByte = currPkt.getData();
				byte one = dataByte[0];
				byte two = dataByte[1];
				int currSeqNo = (((one & 0xff)<<8) | (two & 0xff));
				clientSocket.send(currPkt);
				noOfRetransmission++;
			}
			timer.cancel();
			timer = new Timer();
			timer.schedule(new ResendTask(this), retryTimeout);
//			System.out.println("scheduled timer");
		}
	}

}

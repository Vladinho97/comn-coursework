import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


// ============================================================================================================
//
// 												RESEND TASK
//
// ============================================================================================================
class ResendTask extends TimerTask {
	private Client client;
	public ResendTask(Client client) {
		this.client = client;
	}
	@Override
	public void run() {
		try {
			client.bw.write("+++++++++++++++++++++ Running timer task ++++++++++++++++++++++");
			client.resendPackets();
			client.bw.write("+++++++++++++++++++++ Finish timer task +++++++++++++++++++++++");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;
	}
}

//============================================================================================================
//
//												CLIENT CLASS
//
//============================================================================================================
public class Client {

	private String localhost, filename;
	private int portNo, retryTimeout, windowSize;

	// ============= variables related to image file =================
	private byte[] imgBytesArr;
	private int imgBytesArrLen, imgBytesArrIdx = 0;

	// ============= variables related to client socket ==============
	private DatagramSocket clientSocket = new DatagramSocket();
	private InetAddress IPAddress;
	private DatagramPacket sendPacket;

	// ============= variables related to sequence no ================
	private int incre = 0, seqNoInt = 0, base = 0, nextseqnum = 0;
	private ArrayList<DatagramPacket> pktsBuffer = new ArrayList<DatagramPacket>();
	private byte endFlag = (byte) 0; // last packet flag
	Timer timer = new Timer();
	private int attempt = 0; // no. of attempts to send the last packet

	// ============= variables related to receiving packets ==========
	private byte[] ackBuffer = new byte[2]; // ACK value from rcvPacket stored here
	private DatagramPacket rcvPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
	private int rcvSeqNoInt; // received sequence no. in integer

	// ============= for logging purpose =============================
	FileWriter fw;
	BufferedWriter bw;

	public Client(String localhost, int portNo, String filename, int retryTimeout, int windowSize) throws IOException {
		this.localhost = localhost;
		this.portNo = portNo;
		this.filename = filename;
		this.retryTimeout = retryTimeout;
		this.windowSize = windowSize;
		this.IPAddress = InetAddress.getByName(localhost);
		this.fw = new FileWriter("output-sender.txt");
		this.bw = new BufferedWriter(fw);
	}

	// opens image file and read into bytes array
	public void openFile() throws IOException {
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(file);
		this.imgBytesArrLen = (int) file.length();
		this.imgBytesArr = new byte[imgBytesArrLen];
		fis.read(imgBytesArr);
		fis.close();
	}

	Object lock = new Object();

	boolean doneSEND = false;
	public void sendPacket() throws IOException {
		if (imgBytesArrIdx >= imgBytesArrLen) {
			bw.write("sendPacket(): no more packets to be sent\n");
			doneSEND = true;
			return;
		}
		if (pktsBuffer.size() >= windowSize) {
			return;
		}
		synchronized (lock) {
			bw.write("sendPacket(): locked object\n");
			seqNoInt = incre % 65535;
			incre++;
			bw.write("sendPacket(): creating a packet with seqNoInt = "+seqNoInt+"    |    base : "+base+"   |   nextseqnum : "+nextseqnum+"\n");
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
			bw.write("sendPacket(): sent packet to IPAddress : "+IPAddress+"    |    portNo : "+portNo);
			// ------------------------------ update values --------------------------------
			if (base == nextseqnum) { // if no unAck'd packets
				timer.cancel();
				timer = new Timer();
				timer.schedule(new ResendTask(this), retryTimeout);
			}
			nextseqnum = (nextseqnum+1) % 65535; // increment next available sequence no
			pktsBuffer.add(sendPacket); // add sent packet to packets buffer

			bw.write("sendPacket(): sent packet. seqNoInt : "+seqNoInt+"   |   base : "+base+"   |   nextseqnum : "+nextseqnum+"    |   pktsBuffer.Size() : "+pktsBuffer.size()+"\n");
		}
	}

	boolean doneACK = false;
	// tries to receive a packet - the packet with base sequence no
	public void ackPacket() throws IOException {
		rcvPacket.setLength(2);
		// ack last packet 
		if (endFlag==(byte)1 && pktsBuffer.size()==1) {
			timer.cancel();
			bw.write("ackPacket(): Acking last packet!\n");
			clientSocket.setSoTimeout(retryTimeout);
			try {
				clientSocket.receive(rcvPacket);
				ackBuffer = rcvPacket.getData();
				rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] & 0xff));
				bw.write("ackPacket(): received packet with rcvSeqNoInt : "+rcvSeqNoInt+"\n");
				synchronized (lock) {
					if (rcvSeqNoInt == base) {
						pktsBuffer.remove(0);
						bw.write("ackPacket(): locked object\n"
								+ "ackPacket(): base : "+base+"   |   nextseqnum : "+nextseqnum+"\n");
						doneACK = true;
						bw.write("ackPacket(): doneACK!");
						bw.close();
						fw.close();
						clientSocket.close();
					} else {
						bw.write("ackPacket(): rcvSeqNoInt != base. disregard\n");
					}
				}
			} catch (SocketTimeoutException e) {
				if (attempt >= 50) {
					pktsBuffer.remove(0);
					doneACK = true;
					bw.close();
					fw.close();
					clientSocket.close();
					bw.write("ackPacket(): last packet ack attempt >= 50, assume lost! doneACK!\n");
				}
				attempt++;
				clientSocket.send(pktsBuffer.get(0));
				bw.write("ackPacket(): socketTimeOutExcpetion, increment attempt and resend last packet!\n");
			}
		} else { // ack normal packet
			bw.write("ackPacket(): Acking normal packets\n");
			clientSocket.setSoTimeout(0);
			bw.write("ackPacket(): clientSocket.getPort() : "+clientSocket.getPort()+"   |   clientSocket.getInetAddress(): "+clientSocket.getInetAddress()+"\n");
			clientSocket.receive(rcvPacket);
			ackBuffer = rcvPacket.getData();
			rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] & 0xff));
			bw.write("ackPacket(): received packet with rcvSeqNoInt : "+rcvSeqNoInt+"\n");
			synchronized (lock) {
				bw.write("ackPacket(): locked object\n"
						+ "ackPacket(): base : "+base+"   |   nextseqnum : "+nextseqnum+"\n");
				if (rcvSeqNoInt == base) {
					bw.write("ackPacket(): rcvSeqNoInt == base, update variables.\n");
					base = (base+1) % 65535;
					pktsBuffer.remove(0);
					bw.write("ackPacket(): update base : "+base+"   |   pktsBuffer.size() : "+pktsBuffer.size()+"\n");
					if (base == nextseqnum) {
						timer.cancel();
						bw.write("ackPacket(): base == nextseqnum, timer cancelled.\n");
					} else {
						timer.cancel();
						timer = new Timer();
						timer.schedule(new ResendTask(this), retryTimeout);
						bw.write("ackPacket(): base != nextseqnum. new timer and scheduled.\n");
					}
				} else {
					bw.write("ackPacket(): rcvSeqNoInt != base. disregard\n");
				}
			}
		}
	}

	public void resendPackets() throws IOException {
		bw.write("resendPackets(): calling \n");
		synchronized (lock) {
			bw.write("resendPackets(): lock object\n");
			bw.write("resendPackets(): base : "+base+"    |   nextseqnum : "+nextseqnum+"   |   seqNoInt : "+seqNoInt+"   |    pktsBuffer.size() : "+pktsBuffer.size()+"\n");
			for (int i = 0; i < pktsBuffer.size(); i++) {
				DatagramPacket currPkt = pktsBuffer.get(i);
				byte[] dataByte = currPkt.getData();
				byte one = dataByte[0];
				byte two = dataByte[1];
				int currSeqNo = (((one & 0xff)<<8) | (two & 0xff));
				bw.write("resendPackets(): resend packet with seqNoInt : "+currSeqNo+"    |   portNo : "+currPkt.getPort()+"   |   IPAddress : "+currPkt.getAddress()+"\n");
				clientSocket.send(currPkt);
			}
			timer.cancel();
			timer = new Timer();
			timer.schedule(new ResendTask(this), retryTimeout);
			bw.write("resendPackets(): scheduled new timer.\n");
		}
	}
}

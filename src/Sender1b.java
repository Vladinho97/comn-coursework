import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class Sender1b {

	public static void main(String[] args) {
		if (args.length != 4) { // ignoring WindowSize parameter for just now
			System.err.println("Usage: java Sender1a localhost <Port> <Filename> [RetryTimeout] [WindowSize]");
			System.exit(1);
		}
		
		String localhost = args[0];
		int portNo = Integer.parseInt(args[1]);
		String filename = args[2];
		int retryTimeout = Integer.parseInt(args[3]);
		// int windowSize = Integer.parseInt(args[4]);
		
		// for experimentation
		int noOfRetransmission = 0;

		try {
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName(localhost);
			DatagramPacket sendPacket;
			
			int incre = 0;
			int seqNo = 0;
			int packetIdx;
			int packetSize;
			byte endFlag = (byte) 0;

			// from server
			int rcvSeqNo;
			boolean ack;
			DatagramPacket rcvPacket;
			byte[] ackBuffer = new byte[2];
			
			// open image file and convert into a byte array
			File file = new File(filename);
			
			// for measuring throughput - get the size of file in kB
			double fileSizeBytes = file.length();
			double fileSizeKB = (fileSizeBytes/1024);
			
			
			
			int check = 0;
			
			
			
			byte[] imgBytesArr = new byte[(int) file.length()];
			FileInputStream fis = new FileInputStream(file);
			fis.read(imgBytesArr);
			fis.close();
			int len = imgBytesArr.length;
//			System.out.println("imgBytesArr length : "+len);
			int idx = 0; // index for imgByteArr
			// to measure time elapse between first and last packet.
			boolean isFirstPacket = true;
			boolean isLastPacket = false;
			long startTime = System.nanoTime();
			long endTime = System.nanoTime();
			while (idx < len) {
				packetIdx = 3;
				byte[] buffer;
				
				if ((len-idx) >= 1024) {
					packetSize = 1027;
					buffer = new byte[packetSize];
					if ((len-idx) == 1024) {
						endFlag = (byte) 1;
						isLastPacket = true;
					} else { 
						endFlag = (byte) 0;
					}
				} else {
					packetSize = 3+len-idx;
					buffer = new byte[packetSize];
					endFlag = (byte) 1;
					isLastPacket = true;
				}
				
				buffer[0] = (byte) (seqNo >>> 8);
				buffer[1] = (byte) seqNo;
				buffer[2] = endFlag; 
//				System.out.println("imgByteArr idx : "+idx);
				System.out.println("SeqNo : "+seqNo);
				System.out.println("endFlag : "+endFlag);
				while (packetIdx < packetSize) {
					buffer[packetIdx] = imgBytesArr[idx];
					packetIdx++;
					idx++;
				}
				packetIdx = 3;
				
				sendPacket = new DatagramPacket(buffer, buffer.length, IPAddress, portNo);
				clientSocket.send(sendPacket);
				check++;
				System.out.println("send packet #: "+check);
				
				// set startTime
				if (isFirstPacket) {
					startTime = System.nanoTime();
					isFirstPacket = false;
				} 
				System.out.println("send packet. waiting for ack packet.");
				ack = false;
				while (!ack) {
					System.out.println("imgByte Idx = "+idx);
					clientSocket.setSoTimeout(retryTimeout);
					rcvPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
					System.out.println("============= timer started ===========");
					try {
						rcvPacket.setLength(2);
						clientSocket.receive(rcvPacket);
						System.out.println("received ack packet");
						rcvSeqNo = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] & 0xff));
						System.out.println("expected seqNo : "+seqNo);
						System.out.println("ack received with rcvSeqNo : "+rcvSeqNo);
						if (rcvSeqNo == seqNo) {
							ack = true;
							if (seqNo == 0) {
								seqNo = 1;
							} else {
								seqNo = 0;
							}							
							System.out.println("ack received");
							System.out.println("seqNo : "+seqNo);
							// set endTime
							if (isLastPacket) {
								endTime = System.nanoTime();
							}
						}
					} catch (SocketTimeoutException e) {
						clientSocket.send(sendPacket);
						noOfRetransmission++;
						check++;
						System.out.println("send packet #: "+check);

						System.out.println("time out occured. clientSocket resent packet.");
						System.out.println("seqNo : "+seqNo);
					}
				}				
			}
			clientSocket.close();
			System.out.println("No of retransmission : "+noOfRetransmission);
			long estimatedTimeInNano = endTime - startTime; 
			double throughput = fileSizeKB/(-estimatedTimeInNano*(10^(-9)));
			System.out.println("Throughput : "+throughput);
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
		
		
		
	}
}

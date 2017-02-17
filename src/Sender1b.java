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
		
		// Program outputs
		int noOfRetransmission = 0;
		double throughput;
		long estimatedTimeInNano;
		double estimatedTimeInSec;

		try {
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName(localhost);
			DatagramPacket sendPacket;
			
			// from server : sequence no. and ACK
			int rcvSeqNo;
			boolean ack;
			DatagramPacket rcvPacket;
			byte[] ackBuffer = new byte[2];
			
			// open image file and convert into a byte array
			File file = new File(filename);
			double fileSizeBytes = file.length(); // for measuring throughput - get the size of file in kB
			double fileSizeKB = (fileSizeBytes/1024);
			byte[] imgBytesArr = new byte[(int) file.length()]; 
			FileInputStream fis = new FileInputStream(file);
			fis.read(imgBytesArr); // file in byte array
			fis.close();
						
			// to measure time elapse between first and last packet.
			boolean isFirstPacket = true;
			boolean isLastPacket = false;
			long startTime = System.nanoTime();
			long endTime = System.nanoTime();

			int len = imgBytesArr.length; // total no. of bytes
			int idx = 0; // index to increment imgByteArr
			int seqNo = 0;
			int bufferIdx; // index to increment current buffer
			int bufferSize; // max 1027 
			byte endFlag = (byte) 0;
			while (idx < len) { // while there are bytes left
				bufferIdx = 3; 
				byte[] buffer; // current packet
				
				if ((len-idx) >= 1024) {
					bufferSize = 1027;
					buffer = new byte[bufferSize];
					if ((len-idx) == 1024) {
						endFlag = (byte) 1;
						isLastPacket = true;
					} else { 
						endFlag = (byte) 0;
					}
				} else { // last packet
					bufferSize = 3+len-idx;
					buffer = new byte[bufferSize];
					endFlag = (byte) 1;
					isLastPacket = true;
				}
				
				buffer[0] = (byte) (seqNo >>> 8); // sequence no. as bytes in packet
				buffer[1] = (byte) seqNo;
				buffer[2] = endFlag; 

				while (bufferIdx < bufferSize) {
					buffer[bufferIdx] = imgBytesArr[idx]; // put file byte values into current packet
					bufferIdx++;
					idx++;
				}
				bufferIdx = 3; // reset packet index
				
				sendPacket = new DatagramPacket(buffer, buffer.length, IPAddress, portNo);
				clientSocket.send(sendPacket);
				
				if (isFirstPacket) { // set startTime
					startTime = System.nanoTime();
					isFirstPacket = false;
				} 
				
				ack = false; // wait for ACK 
				while (!ack) { // loop until ACK with appropriate sequence no has been received
					clientSocket.setSoTimeout(retryTimeout); // wait for retryTimeout amount of time
					rcvPacket = new DatagramPacket(ackBuffer, ackBuffer.length); 

					try {
						rcvPacket.setLength(2);
						clientSocket.receive(rcvPacket); // receive packet from server
						rcvSeqNo = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] & 0xff)); 
						
						if (rcvSeqNo == seqNo) { // if received sequence no. matches with expected sequence no.
							ack = true;
							if (seqNo == 0)  // update sequence no. for the next packet
								seqNo = 1;
							else 
								seqNo = 0;
												
							if (isLastPacket)	
								endTime = System.nanoTime(); // set endTime if last packet
						}
					} catch (SocketTimeoutException e) {
						clientSocket.send(sendPacket);
						noOfRetransmission++;
					}
				}				
			}
			clientSocket.close();
			
			System.out.println("================== Part1b: output ==================");
			System.out.println("No of retransmission = "+noOfRetransmission);
			estimatedTimeInNano = endTime - startTime; 
			estimatedTimeInSec = (double)estimatedTimeInNano/1000000000.0; // convert from nano-sec to sec
//			System.out.println("Estimated time in sec: "+estimatedTimeInSec);
			throughput = fileSizeKB/estimatedTimeInSec;
			System.out.println("Throughput = "+throughput);
			System.out.println("================== Program terminates ==================");
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

/* Isabella Chan s1330027 */

import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class Sender1b {

	public static void main(String[] args) {
		if (args.length != 4) { // ignoring WindowSize parameter, exit code 1 if missing arguments
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
		long estimatedTimeInNano; // time in nano seconds
		double estimatedTimeInSec; // time in seconds
		// to measure time elapse between first and last packet.
		boolean isFirstPacket = true;
		boolean isLastPacket = false;
		Long startTime = null; // start and end time
		Long endTime = null;

		try {
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName(localhost);
			DatagramPacket sendPacket; // current packet to be sent to server
			DatagramPacket rcvPacket; // received packet from server
			
			byte[] ackBuffer = new byte[2]; // ACK value aka sequence no. in 2 bytes
			int rcvSeqNoInt; // sequence no. received from server in integer
			boolean ack; // flag to indicate that ACK received from server 
			int attempt; // the number of attempts (resending packet) for the current packet.
			
			File file = new File(filename); // open image file
			double fileSizeKB = (file.length()/1024); // for measuring throughput - file size in kilo-bytes
			byte[] imgBytesArr = new byte[(int) file.length()]; // image file in byte array
			FileInputStream fis = new FileInputStream(file);
			fis.read(imgBytesArr);
			fis.close();
						
			int len = imgBytesArr.length; // total no. of bytes
			int idx = 0; // index to increment imgByteArr
			int seqNoInt = 0; // sequence no. begins with 0
			byte endFlag = (byte) 0;

			int packetIdx; // index to increment current packet (buffer array)
			int packetSize; // max 1027 
			while (idx < len) { // while there are bytes left
				packetIdx = 3; // Index for payload starts with 3
				byte[] buffer; // current packet
				
				if ((len-idx) >= 1024) { // maximum size of packet is 1027
					packetSize = 1027;
					buffer = new byte[packetSize];
					if ((len-idx) == 1024) { // is last packet
						endFlag = (byte) 1;
						isLastPacket = true;
					} else { // not last packet
						endFlag = (byte) 0;
					}
				} else { // last packet
					packetSize = 3+len-idx;
					buffer = new byte[packetSize];
					endFlag = (byte) 1;
					isLastPacket = true;
				}
				
				//////////////// LOGGING ///////////////
				if (isLastPacket) 	System.out.println("I am the last packet.");
				//////////////// LOGGING ///////////////
				
				buffer[0] = (byte) (seqNoInt >>> 8); // sequence no. as bytes in packet
				buffer[1] = (byte) seqNoInt;
				buffer[2] = endFlag; 

				while (packetIdx < packetSize) { // 
					buffer[packetIdx] = imgBytesArr[idx]; // put file byte values into current packet
					packetIdx++;
					idx++;
				}
				
				sendPacket = new DatagramPacket(buffer, buffer.length, IPAddress, portNo); // send packet to server
				clientSocket.send(sendPacket);
				
				if (isFirstPacket) { // set startTime
					startTime = System.nanoTime();
					isFirstPacket = false;
				} 
				
				ack = false; // wait for ACK 
				attempt = 1; // only sent packet once so far, this variable is associated with the last package
				while (!ack) { // loop until ACK with appropriate sequence no has been received
					clientSocket.setSoTimeout(retryTimeout); // wait for retryTimeout amount of time for the ACK packet to arrive
					rcvPacket = new DatagramPacket(ackBuffer, ackBuffer.length); 

					try {
						rcvPacket.setLength(2);
						clientSocket.receive(rcvPacket); // receive packet from server
						rcvSeqNoInt = (((ackBuffer[0] & 0xff) << 8) | (ackBuffer[1] & 0xff)); 
						
						if (rcvSeqNoInt == seqNoInt) { // if received sequence no. matches with expected sequence no.
							ack = true;
							
							if (seqNoInt == 0)	seqNoInt = 1;	// update sequence no. for the next packet
							else	seqNoInt = 0;
												
							if (isLastPacket)	endTime = System.nanoTime(); // set endTime if last packet
						}
						
					} catch (SocketTimeoutException e) { // timed out and have not received ACK, resend packet to server
						if (isLastPacket && (attempt >= 50)) { // if attempted more than 50 times, assume receiver terminates and last ACK package is lost
							System.out.println("last packet sent more than 50 times. terminate!");
							endTime = System.nanoTime(); // records end time
							break; // breaks the while loops
						}
						clientSocket.send(sendPacket);
						if (isLastPacket)	attempt++; // only counts the last packet in the case that the last ACK package is lost
						if (isLastPacket)	System.out.println("Resend last packet attempt : "+attempt);
						noOfRetransmission++;
					}
				}				
			}
			clientSocket.close();
			
			// program output
			System.out.println("================== Part1b: output ==================");
			System.out.println("No of retransmission = "+noOfRetransmission);
			estimatedTimeInNano = endTime - startTime; 
			estimatedTimeInSec = (double)estimatedTimeInNano/1000000000.0; // convert from nano-sec to sec
			/////////////// LOGGING ////////////////
			System.out.println("Estimated time in sec: "+estimatedTimeInSec);
			/////////////// LOGGING ////////////////
			throughput = fileSizeKB/estimatedTimeInSec;
			System.out.println("Throughput = "+throughput);
			System.out.println("================== Program terminates ==================");
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

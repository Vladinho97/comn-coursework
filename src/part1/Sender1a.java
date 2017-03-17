package part1;
/* Isabella Chan s1330027 */

import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Sender1a {

	public static void main(String[] args) throws Exception {
		
		if (args.length != 3) { // ignoring RetryTimeout and WindowSize parameter, exit code 1 if missing arguments
			System.err.println("Usage: java Sender1a localhost <Port> <Filename> [RetryTimeout] [WindowSize]");
			System.exit(1);
		}
		
		String localhost = args[0];
		int portNo = Integer.parseInt(args[1]);
		String filename = args[2];
		// int retryTimeout = Integer.parseInt(args[3]);
		// int windowSize = Integer.parseInt(args[4]);
		
		try {
			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName(localhost); // IP address of server
			DatagramPacket sendPacket; // current packet to be sent 
			
			int incre = 0; // to increment sequence no.
			int seqNoInt; // sequence no. in integer
			int packetSize; // current packet size, maximum = 1027
			int packetIdx; // index pointer for the current packet
			
			File file = new File(filename);
			FileInputStream fis = new FileInputStream(file);
			byte[] imgBytesArr = new byte[(int) file.length()]; // current file in byte array
			fis.read(imgBytesArr);
			fis.close();
			
			int len = imgBytesArr.length; // total no. of bytes 
			int idx = 0; // index pointer for imgBytesArr
			while (idx < len) { // while there are bytes left in the image file
				seqNoInt = incre % Integer.MAX_VALUE;
				incre++; // increment sequence no.
				packetIdx = 3; // packet index pointer starts at 3 for image byte values
				byte[] buffer; // buffer for current packet
				
				if ((len-idx) >= 1024) {
					packetSize = 1027; // maximum size of packet is 1027
					buffer = new byte[packetSize];
					if ((len-idx) == 1024)
						buffer[2] = (byte) 1; // last packet has size 1027
					else 
						buffer[2] = (byte) 0; // not last packet
				} else { 
					packetSize = 3+len-idx; // last packet has size less than 1027
					buffer = new byte[packetSize];
					buffer[2] = (byte) 1;
				}
				
				buffer[0] = (byte) (seqNoInt >>> 8); // store sequence no. value as two byte values
				buffer[1] = (byte) seqNoInt;
				
				while (packetIdx < packetSize) { // write imgBytesArr byte values into packet
					buffer[packetIdx] = imgBytesArr[idx];
					packetIdx++;
					idx++;
				}
				
				sendPacket = new DatagramPacket(buffer, buffer.length, IPAddress, portNo);
				clientSocket.send(sendPacket);
				Thread.sleep(10);
			}
			clientSocket.close();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Receiver1a {
	
	public static void main(String[] args) throws Exception {
		
		if (args.length != 2) { // ignoring WindowSize parameter, exit code 1 if missing arguments
			System.err.println("Usage: Receiver1a <Port> <Filename> [WindowSize]");
			System.exit(1);
		}
		
		int portNo = Integer.parseInt(args[0]);
		String filename = args[1];
		// int windowSize = Integer.parseInt(args[2]);
		
		try {
			int seqNoInt; // current sequence no. in integer
			int lastSeqNoInt = -1; // last known sequence no. in integer, constantly updated as the value of seqNoInt
			int checkSeq; // (= seqNoInt - lastSeqNoInt) to check for missing sequence no.
			
			byte[] buffer = new byte[1027]; // received packet buffer: 3 bytes header and 1024 bytes payload
			byte endFlag; // endFlag = 1 for last packet, 0 otherwise
			DatagramSocket serverSocket = new DatagramSocket(portNo);
			DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
			
			OutputStream out = new BufferedOutputStream(new FileOutputStream(filename)); // write image into file
			
			int packetSize; // variable to store current received packet size
			while (true) {
				receivePacket.setLength(1027);
				serverSocket.receive(receivePacket);
				packetSize = receivePacket.getLength();
				// IPAddress = receivePacket.getAddress();
				
				seqNoInt = (((buffer[0] & 0xff) << 8) | (buffer[1] & 0xff)); // obtain sequence no.
				endFlag = buffer[2]; // obtain end flag value
				
				checkSeq = seqNoInt - lastSeqNoInt; // == 1 if no missing packets
				lastSeqNoInt = seqNoInt; // update as current packet's sequence no.
				if (checkSeq != 1) { // missing packets
					byte[] missingBytesArr = new byte[1024]; // each missing packet has 1024 bytes
					for (int i = 0; i < (checkSeq-1); i++) { // for each missing packet (1024 missing bytes), refill byte values
						for (int j = 0; j < 1024; j++) { 
							missingBytesArr[j] = (byte) 10; 
						}
						out.write(missingBytesArr); 
					}
				} // finish refilling missing bytes
				
				byte[] currBuff = new byte[packetSize-3]; // array to hold image file bytes
				int currIdx = 0; // currBuff index pointer
				for (int i = 3; i < packetSize; i++) {
					currBuff[currIdx] = buffer[i];
					currIdx++;
				}
				out.write(currBuff); // write image file byte values from current packet to file
				
				if (endFlag == ((byte) 1)) { // terminates program if it is the last packet
					out.close();
					serverSocket.close();
					break;
				}
			}
		} catch (Exception e) {
			System.err.println("Error: "+e.getMessage());
			e.printStackTrace();
		}
	}
}

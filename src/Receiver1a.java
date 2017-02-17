import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Receiver1a {
	
	public static void main(String[] args) throws Exception {
		
		if (args.length != 2) { // ignoring WindowSize parameter for just now 
			System.err.println("Usage: Receiver1a <Port> <Filename> [WindowSize]");
			System.exit(1);
		}
		
		int portNo = Integer.parseInt(args[0]);
		String filename = args[1];
		// int windowSize = Integer.parseInt(args[2]);
		
		try {
			int lastHeaderInt = -1;
			int headerInt;
			int checkSequence;
			byte endFlag;
			
			byte[] buffer = new byte[1027];
			DatagramSocket serverSocket = new DatagramSocket(portNo);
			DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
			
			int packetSize;
			OutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
			
			while (true) {
				receivePacket.setLength(1027);
				serverSocket.receive(receivePacket);
				packetSize = receivePacket.getLength();
				// IPAddress = receivePacket.getAddress();
				
				headerInt = (((buffer[0] & 0xff) << 8) | (buffer[1] & 0xff));
				endFlag = buffer[2];
				
				checkSequence = headerInt - lastHeaderInt;
				lastHeaderInt = headerInt;
				if (checkSequence != 1) {
					byte[] currentBuffer = new byte[1024];
					for (int i = 0; i < checkSequence; i++) {
						for (int j = 0; j < 1024; j++) {
							currentBuffer[j] = (byte) 10;
						}
					}
					out.write(currentBuffer);
				} else {
					byte[] currentBuffer = new byte[packetSize-3];
					int currIdx = 0;
					for (int i = 3; i < packetSize; i++) {
						currentBuffer[currIdx] = buffer[i];
						currIdx++;
					}
					out.write(currentBuffer);
				}
				
				if (endFlag == ((byte) 1)) {
					out.close();
					serverSocket.close();
					break;
				}
			}
		} catch (Exception e) {
			System.err.println("Error: "+e.getMessage());
		}
	}
}

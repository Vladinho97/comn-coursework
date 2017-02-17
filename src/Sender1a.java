import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Sender1a {

	public static void main(String[] args) throws Exception {
		
		if (args.length != 3) { // ignoring RetryTimeout and WindowSize parameter for just now
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
			InetAddress IPAddress = InetAddress.getByName(localhost);
			DatagramPacket sendPacket;
			
			int incre = 0;
			int headerInt;
			int packetIdx;
			int packetSize;
			byte endFlag = (byte) 0;
			
			File file = new File(filename);
			byte[] imgBytesArr = new byte[(int) file.length()];
			
			FileInputStream fis = new FileInputStream(file);
			fis.read(imgBytesArr);
			fis.close();
			int len = imgBytesArr.length;
			
			int idx = 0;
			while (idx < len) {
				headerInt = incre % Integer.MAX_VALUE;
				incre++;
				packetIdx = 3;
//				int packetSize;
				byte[] buffer;
				
				if ((len-idx) >= 1024) {
					packetSize = 1027;
					buffer = new byte[packetSize];
					if ((len-idx) == 1024) 
						buffer[2] = (byte) 1;
					else 
						buffer[2] = (byte) 0;
				} else {
					packetSize = 3+len-idx;
					buffer = new byte[packetSize];
					buffer[2] = (byte) 1;
				}
				
				buffer[0] = (byte) (headerInt >>> 8);
				buffer[1] = (byte) headerInt;
				
				while (packetIdx < packetSize) {
					buffer[packetIdx] = imgBytesArr[idx];
					packetIdx++;
					idx++;
				}
//				packetIdx = 3;
				
				sendPacket = new DatagramPacket(buffer, buffer.length, IPAddress, portNo);
				clientSocket.send(sendPacket);
				Thread.sleep(10);
			}
			clientSocket.close();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}
}

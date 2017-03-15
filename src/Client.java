import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;


public class Client {
	
	String localhost, filename;
	int portNo, retryTimeout, windowSize;
	InetAddress IPAddress;
	
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

	// ========================= program outputs ==========================
	double fileSizeKB, throughput, estimatedTimeInSec;
	long estimatedTimeInNano;
	int noOfRetransmission = 0;
	boolean isFirstPacket = true;
	Long startTime = null, endTime = null;
	
	public void printOutputs() {
		System.out.println("================== Part2a: output ==================");
		System.out.println("No of retransmission = "+noOfRetransmission);
		estimatedTimeInNano = endTime - startTime; 
		estimatedTimeInSec = ((double)estimatedTimeInNano)/1000000000.0; // convert from nano-sec to sec
		throughput = fileSizeKB/estimatedTimeInSec;
		System.out.println("Throughput = "+throughput);
		System.out.println("================== Program terminates ==================");
	}
	
	// ================ variables related to image file ===================
	byte[] imgBytesArr;
	int imgBytesArrLen, imgBytesArrIdx = 0;
	
	/** Opens file and reads bytes into a byte array */
	public void openFile() throws IOException {
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(file);
		this.imgBytesArrLen = (int) file.length();
		this.imgBytesArr = new byte[imgBytesArrLen];
		this.fileSizeKB = ((double)file.length())/1024; 
		fis.read(imgBytesArr);
		fis.close();
	}
	

}

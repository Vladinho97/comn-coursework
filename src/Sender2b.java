
public class Sender2b {
	
	/** 
	 * Even for part 2b, although you need to be more careful about timeout 
	 * check for each packet, multi-threading is not absolutely necessary. 
	 * The trick is when a (retransmitted) packet is sent, the absolute time 
	 * that the packet was sent should be recorded. Then, before the sender 
	 * calls recvfrom(), it should scan the sent times for all unacked packets, 
	 * find out the timestamp of the oldest unacked packet and calls 
	 * setSoTimeout() by adjusting (computing) the timeout value based on the 
	 * timestamp and the current time.
	 */
	
}

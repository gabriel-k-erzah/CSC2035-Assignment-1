/*
 * Replace the following string of 0s with your student number
 * 000000000
 */
import java.io.*;
import java.net.*;

public class Protocol {

	static final String  NORMAL_MODE="nm"   ;         // normal transfer mode: (for Part 1 and 2)
	static final String	 TIMEOUT_MODE ="wt"  ;        // timeout transfer mode: (for Part 3)
	static final String	 LOST_MODE ="wl"  ;           // lost Ack transfer mode: (for Part 4)
	static final int DEFAULT_TIMEOUT =1000  ;         // default timeout in milliseconds (for Part 3)
	static final int DEFAULT_RETRIES =4  ;            // default number of consecutive retries (for Part 3)
	public static final int MAX_Segment_SIZE = 4096;  //the max segment size that can be used when creating the received packet's buffer

	/*
	 * The following attributes control the execution of the transfer protocol and provide access to the 
	 * resources needed for the transfer 
	 * 
	 */ 

	private InetAddress ipAddress;      // the address of the server to transfer to. This should be a well-formed IP address.
	private int portNumber; 		    // the  port the server is listening on
	private DatagramSocket socket;      // the socket that the client binds to

	private File inputFile;            // the client-side CSV file that has the readings to transfer  
	private String outputFileName ;    // the name of the output file to create on the server to store the readings
	private int maxPatchSize;		   // the patch size - no of readings to be sent in the payload of a single Data segment

	private Segment dataSeg   ;        // the protocol Data segment for sending Data segments (with payload read from the csv file) to the server 
	private Segment ackSeg  ;          // the protocol Ack segment for receiving ACK segments from the server

	private int timeout;              // the timeout in milliseconds to use for the protocol with timeout (for Part 3)
	private int maxRetries;           // the maximum number of consecutive retries (retransmissions) to allow before exiting the client (for Part 3)(This is per segment)
	private int currRetry;            // the current number of consecutive retries (retransmissions) following an Ack loss (for Part 3)(This is per segment)

	private int fileTotalReadings;    // number of all readings in the csv file
	private int sentReadings;         // number of readings successfully sent and acknowledged
	private int totalSegments;        // total segments that the client sent to the server

	// Shared Protocol instance so Client and Server access and operate on the same values for the protocol’s attributes (the above attributes).
	public static Protocol instance = new Protocol();

	/**************************************************************************************************************************************
	 **************************************************************************************************************************************
	 * For this assignment, you have to implement the following methods:
	 *		sendMetadata()
	 *      readandSend()
	 *      receiveAck()
	 *      startTimeoutWithRetransmission()
	 *		receiveWithAckLoss()
	 * Do not change any method signatures, and do not change any other methods or code provided.
	 ***************************************************************************************************************************************
	 **************************************************************************************************************************************/
	/* 
	 * This method sends protocol metadata to the server.
	 * See coursework specification for full details.	
	 */

    /*Big picture,
    *
    *before sending the actual csv reading we need to tell the client what to expect,
    *
    * "I will be sending over this many readings, in batches of X, please store them in this output file."
    *
    * */
	public void sendMetadata() {
		try {
			// first count how many readings are actually in the csv file
			int total = 0;
			try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
				String line;
				while ((line = br.readLine()) != null) {
					// ignore blank lines
					if (!line.trim().isEmpty()) total++;
				}
			}
			this.fileTotalReadings = total;

			// build the meta data payload: total readings, patch size, and the output file name
			//String payload = this.fileTotalReadings + "," + this.maxPatchSize + "," + this.outputFileName;
			String payload = this.fileTotalReadings + "," + this.outputFileName + "," + this.maxPatchSize;



			// create the meta segment with seqNum 0 since this is always the first thing sent
			System.out.println("DEBUG META payload=[" + payload + "]");
			Segment meta = new Segment(0, SegmentType.Meta, payload, payload.getBytes().length);

			// turn the meta segment into raw bytes so it can be sent through UDP
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
				oos.writeObject(meta);
				oos.flush();
			}

			// send the packet to the server using the IP and port from initProtocol
			byte[] bytes = baos.toByteArray();
			DatagramPacket packet = new DatagramPacket(bytes, bytes.length, this.ipAddress, this.portNumber);
			this.socket.send(packet);

			// confirmation for the client terminal so I know it actually sent
			System.out.println("CLIENT: Sending meta data");
			System.out.printf("CLIENT: META [SEQ#0] (Number of readings:%d, file name:%s, patch size:%d)%n",
					this.fileTotalReadings, this.outputFileName, this.maxPatchSize);
			System.out.println("------------------------------------------------------------------");

		} catch (IOException e) {
			// if anything fails during reading or sending, print it and stop the client
			System.out.println("CLIENT: Failed to send meta data: " + e.getMessage());
			System.out.println("CLIENT: Exit ...");
			System.exit(0);
		}
	}

	/* 
	 * This method read and send the next data segment (dataSeg) to the server. 
	 * See coursework specification for full details.
	 */
	public void readAndSend()
    {

//psudo code added
/*procedure readAndSend()

    if sentReadings >= fileTotalReadings then
        exit procedure      // nothing left to send

    open inputFile for reading
    skip first sentReadings non-empty lines
    readingsBatch = empty list

    while readingsBatch.size < maxPatchSize
          and not end_of_file
          read next non-empty line
          add line to readingsBatch

    payload = join readingsBatch with ";"   // join with semicolons

    dataSeg = new Segment()
    dataSeg.type = Data
    dataSeg.seqNum = toggle between 1 and 0 each send
    dataSeg.size = length(payload)
    dataSeg.payLoad = payload
    dataSeg.checksum = calculateChecksum(payload)

    serialize dataSeg to bytes
    send bytes through UDP socket to ipAddress:portNumber

    sentReadings += readingsBatch.size
    totalSegments += 1

    print "CLIENT: Send: DATA [SEQ#", dataSeg.seqNum,
          "] (size:", dataSeg.size, ", crc:", dataSeg.checksum,
          ", content:", payload, ")"

end procedure

 */

        System.exit(0);
	}

	/* 
	 * This method receives the current Ack segment (ackSeg) from the server 
	 * See coursework specification for full details.
	 */
	public boolean receiveAck() { 
		System.exit(0);
		return false;
	}

	/* 
	 * This method starts a timer and does re-transmission of the Data segment 
	 * See coursework specification for full details.
	 */
	public void startTimeoutWithRetransmission()   {  
		System.exit(0);
	}


	/* 
	 * This method is used by the server to receive the Data segment in Lost Ack mode
	 * See coursework specification for full details.
	 */
	public void receiveWithAckLoss(DatagramSocket serverSocket, float loss)  {
		System.exit(0);
	}


	/*************************************************************************************************************************************
	 **************************************************************************************************************************************
	 **************************************************************************************************************************************
	These methods are implemented for you .. Do NOT Change them 
	 **************************************************************************************************************************************
	 **************************************************************************************************************************************
	 **************************************************************************************************************************************/	 
	/* 
	 * This method initialises ALL the 14 attributes needed to allow the Protocol methods to work properly
	 */
	public void initProtocol(String hostName , String portNumber, String fileName, String outputFileName, String batchSize) throws UnknownHostException, SocketException {
		instance.ipAddress = InetAddress.getByName(hostName);
		instance.portNumber = Integer.parseInt(portNumber);
		instance.socket = new DatagramSocket();

		instance.inputFile = checkFile(fileName); //check if the CSV file does exist
		instance.outputFileName =  outputFileName;
		instance.maxPatchSize= Integer.parseInt(batchSize);

		instance.dataSeg = new Segment(); //initialise the data segment for sending readings to the server
		instance.ackSeg = new Segment();  //initialise the ack segment for receiving Acks from the server

		instance.fileTotalReadings = 0; 
		instance.sentReadings=0;
		instance.totalSegments =0;

		instance.timeout = DEFAULT_TIMEOUT;
		instance.maxRetries = DEFAULT_RETRIES;
		instance.currRetry = 0;		 
	}


	/* 
	 * check if the csv file does exist before sending it 
	 */
	private static File checkFile(String fileName)
	{
		File file = new File(fileName);
		if(!file.exists()) {
			System.out.println("CLIENT: File does not exists"); 
			System.out.println("CLIENT: Exit .."); 
			System.exit(0);
		}
		return file;
	}

	/* 
	 * returns true with the given probability to simulate network errors (Ack loss)(for Part 4)
	 */
	private static Boolean isLost(float prob) 
	{ 
		double randomValue = Math.random();  //0.0 to 99.9
		return randomValue <= prob;
	}

	/* 
	 * getter and setter methods	 *
	 */
	public String getOutputFileName() {
		return outputFileName;
	} 

	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	} 

	public int getMaxPatchSize() {
		return maxPatchSize;
	} 

	public void setMaxPatchSize(int maxPatchSize) {
		this.maxPatchSize = maxPatchSize;
	} 

	public int getFileTotalReadings() {
		return fileTotalReadings;
	} 

	public void setFileTotalReadings(int fileTotalReadings) {
		this.fileTotalReadings = fileTotalReadings;
	}

	public void setDataSeg(Segment dataSeg) {
		this.dataSeg = dataSeg;
	}

	public void setAckSeg(Segment ackSeg) {
		this.ackSeg = ackSeg;
	}

	public void setCurrRetry(int currRetry) {
		this.currRetry = currRetry;
	}

}

/*
 * Replace the following string of 0s with your student number
 *240242385*
 */
import java.io.*;
import java.net.*;
import java.util.*;

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

	/****************************************************************************************************************************************************************************************************************************************************************************
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


            // if anything fails during reading or sending, print it and stop the client
		} catch (IOException e) {

			System.out.println("CLIENT: Failed to send meta data: " + e.getMessage());
			System.out.println("CLIENT: Exit ...");
			System.exit(0);
		}
	}

    // part 1 done

	/* 
	 * This method reads and sends the next data segment (dataSeg) to the server.
	 * See coursework specification for full details.
	 */


    /* read up to patch-size readings and send as one Data segment */
    public void readAndSend()
    {
        if (sentReadings >= fileTotalReadings) {
            return;
        }

        List<String> batchReadings = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {

            String line;
            int skipped = 0;

            // skip already sent non-empty lines
            while (skipped < sentReadings && (line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    skipped++;
                }
            }

            // collect up to maxPatchSize non-empty lines
            int count = 0;
            while (count < maxPatchSize && (line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    batchReadings.add(line);
                    count++;
                }
            }

            if (batchReadings.isEmpty()) {
                return;
            }

            String payload = (batchReadings.size() == 1) ? batchReadings.get(0) : String.join(";", batchReadings);

            // toggle seq based on previous instance value (initProtocol sets dataSeg initially)
            int lastSeq = this.dataSeg.getSeqNum();
            int nextSeq = (lastSeq == 1) ? 0 : 1;

            // create data segment (constructor sets payload & size & checksum)
            dataSeg = new Segment(nextSeq, SegmentType.Data, payload, payload.length());

            // serialize and send
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(dataSeg);
                oos.flush();
            }

            byte[] bytes = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, this.ipAddress, this.portNumber);
            this.socket.send(packet);

            // update counters and print
            sentReadings += batchReadings.size();
            totalSegments += 1;

            System.out.println("CLIENT: Send: DATA [SEQ#" + dataSeg.getSeqNum() + "](size:" + dataSeg.getSize()
                    + ", crc: " + dataSeg.getChecksum() + ", content:" + payload + ")");
            System.out.println("***************************************************************************************************");

            //error handling
        } catch (IOException e) {
            System.out.println("CLIENT: Error reading/sending data: " + e.getMessage());
            System.out.println("CLIENT: Exit ...");
            System.exit(0);
        }
    }

    /*
	 * This method receives the current Ack segment (ackSeg) from the server 
	 * See coursework specification for full details.
	 */
    public boolean receiveAck() throws SocketTimeoutException {
        try {
            //Prepare a buffer and packet to receive an incoming UDP packet
            byte[] buf = new byte[Protocol.MAX_Segment_SIZE];
            DatagramPacket incomingPacket = new DatagramPacket(buf, buf.length);


            //Block and wait for a packet from the server
            socket.receive(incomingPacket);

            //Deserialize the incoming packet into a Segment object
            int len = incomingPacket.getLength();
            ByteArrayInputStream bais = new ByteArrayInputStream(incomingPacket.getData(), 0, len);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Segment ack = (Segment) ois.readObject();

            //store the ack in class var
            this.ackSeg = ack;

            //Check that the received segment is an ACK
            if (ack.getType() != SegmentType.Ack) {
                // not an Ack ignore / signal failure
                return false;
            }

            // We need to verify the ACK sequence number matches the last sent Data segment
            int expectedSeq = dataSeg.getSeqNum();    // seq of the data we most recently sent
            if (ack.getSeqNum() == expectedSeq) {
                //Correct ACK print message, update state if final, and return true
                System.out.println("CLIENT: RECEIVE: ACK [SEQ#" + ack.getSeqNum() + "]");

                // If we've already sent all readings and this ACK confirms the final one:
                if (sentReadings >= fileTotalReadings) {
                    System.out.println("***************************************************************************************************");
                    System.out.println("Total segments: " + totalSegments);
                    System.exit(0);   // per spec: exit when final ack received
                }

                // Otherwise this ack is good and we continue
                return true;
            } else {
                //Wrong seq duplicate or unexpected indicate failure
                return false;
            }

        } catch (SocketTimeoutException ste) {
            //let caller (timeout loop) handle retransmission
            throw ste;

        } catch (ClassNotFoundException e) {
            System.out.println("CLIENT: Received unknown object: " + e.getMessage());
            return false;

        } catch (IOException e) {
            System.out.println("CLIENT: Error receiving/deserializing ACK: " + e.getMessage());
            System.out.println("CLIENT: Exit ...");
            System.exit(0);
            return false; // unreachable
        }
    }

    /*
	 * This method starts a timer and does re-transmission of the Data segment 
	 * See coursework specification for full details.
	 */

    public void startTimeoutWithRetransmission() {
        try {
            socket.setSoTimeout(this.timeout);

            while (true) {
                try {
                    boolean got = this.receiveAck(); // declares throws SocketTimeoutException
                    if (got) {
                        this.currRetry = 0;
                        return; // success for this segment
                    }
                    // wrong/duplicate ACK: loop again until correct ACK or timeout
                } catch (SocketTimeoutException ste) {
                    this.currRetry++;
                    if (this.currRetry > this.maxRetries) {
                        System.out.println("CLIENT: Maximum retries exceeded (" + this.maxRetries + "). Exit.");
                        System.out.println("CLIENT: Exit ...");
                        System.exit(0);
                    }

                    System.out.println("CLIENT: TIMEOUT ALERT");
                    System.out.println("CLIENT: Re-sending the same segment again, current retry " + this.currRetry);

                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                            oos.writeObject(this.dataSeg);
                            oos.flush();
                        }
                        byte[] bytes = baos.toByteArray();

                        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, this.ipAddress, this.portNumber);
                        this.socket.send(packet);
                        this.totalSegments += 1;
                        System.out.println("CLIENT: Send: DATA [SEQ#" + dataSeg.getSeqNum() + "]"
                                + "(size:" + dataSeg.getSize() + ", crc:" + dataSeg.getChecksum() + ")");


                    } catch (IOException ioe) {
                        System.out.println("CLIENT: Error re-sending segment: " + ioe.getMessage());
                        System.out.println("CLIENT: Exit ...");
                        System.exit(0);
                    }
                }
            }
        } catch (SocketException se) {
            System.out.println("CLIENT: Socket error while setting timeout: " + se.getMessage());
            System.out.println("CLIENT: Exit ...");
            System.exit(0);
        } finally {
            //always restore blocking mode
            try { socket.setSoTimeout(0); } catch (Exception ignore) {}
        }
    }

	/* 
	 * This method is used by the server to receive the Data segment in Lost Ack mode
	 * See coursework specification for full details.
	 */
    /*
     * Lost-ACK mode (server side):
     * - We still receive everything normally.
     * - We sometimes "lose" the ACK on purpose (using 'loss' probability).
     * - Client should timeout and resend the SAME DATA; we must be idempotent.
     */
    public void receiveWithAckLoss(DatagramSocket serverSocket, float loss)  {
        int expectedSeq = 0;
        boolean outputReady = false;
        int totalExpected = 0;
        int written = 0;

        BufferedWriter out = null;

        try {
            while (true) {
                //Block for a packet from the client
                byte[] buf = new byte[Protocol.MAX_Segment_SIZE];
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                serverSocket.receive(pkt); // blocking

                //Safe deserialize only use the actual received length
                int len = pkt.getLength();
                Segment seg;
                try (ObjectInputStream ois = new ObjectInputStream(
                        new ByteArrayInputStream(pkt.getData(), 0, len))) {
                    seg = (Segment) ois.readObject();
                }

                //Handle META first (client sends this before any data
                if (seg.getType() == SegmentType.Meta) {

                    //format: "totalReadings,outputFileName,patchSize"
                    String metaPayload;
                    try {
                        metaPayload = seg.getPayLoad();
                    } catch (Exception ignore) {
                        metaPayload = "";
                    }

                    String[] parts = metaPayload.split(",", 3);
                    if (parts.length == 3) {
                        totalExpected      = Integer.parseInt(parts[0].trim());
                        this.outputFileName = parts[1].trim();
                        this.maxPatchSize   = Integer.parseInt(parts[2].trim());
                    }

                    //open output file fresh on every run
                    if (out != null) { try { out.close(); } catch (IOException ignore) {} }
                    out = new BufferedWriter(new FileWriter(this.outputFileName, false));
                    outputReady = true;

                    System.out.println("SERVER: META received -> total=" + totalExpected
                            + ", file=" + this.outputFileName + ", patch=" + this.maxPatchSize);

                    //ACK META simulate losing it
                    if (!isLost(loss)) {

                        Segment ack = new Segment(seg.getSeqNum(), SegmentType.Ack, "", 0);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                            oos.writeObject(ack);
                            oos.flush();
                        }
                        byte[] ab = baos.toByteArray();
                        DatagramPacket ap = new DatagramPacket(ab, ab.length, pkt.getAddress(), pkt.getPort());
                        serverSocket.send(ap);
                        System.out.println("SERVER: SEND ACK (META) seq=" + seg.getSeqNum());
                    } else {
                        System.out.println("SERVER: *** Simulated ACK loss (META) seq=" + seg.getSeqNum() + " ***");
                    }
                    continue;
                }

                //data handling idempotent write maybe-ACK
                if (seg.getType() == SegmentType.Data) {
                    if (!outputReady) {

                        // if we get data before it shouldn't happen. ACK anyway to avoid deadlock.
                        System.out.println("SERVER: DATA before META — ACKing anyway to keep client moving");
                        if (!isLost(loss)) {
                            Segment ack = new Segment(seg.getSeqNum(), SegmentType.Ack, "", 0);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                                oos.writeObject(ack);
                                oos.flush();
                            }
                            byte[] ab = baos.toByteArray();
                            DatagramPacket ap = new DatagramPacket(ab, ab.length, pkt.getAddress(), pkt.getPort());
                            serverSocket.send(ap);
                            System.out.println("SERVER: SEND ACK (no META) seq=" + seg.getSeqNum());
                        } else {
                            System.out.println("SERVER: *** Simulated ACK loss (no META) seq=" + seg.getSeqNum() + " ***");
                        }
                        continue;
                    }

                    boolean inOrder = (seg.getSeqNum() == expectedSeq);

                    if (inOrder) {
                        // first time seeing this data
                        String payload;
                        try {
                            payload = seg.getPayLoad();
                        } catch (Exception ignore) {
                            payload = "";
                        }

                        if (payload != null && !payload.isEmpty()) {
                            String[] lines = payload.split(";");
                            for (String line : lines) {
                                if (!line.trim().isEmpty()) {
                                    out.write(line);
                                    out.newLine();
                                    written++;
                                }
                            }
                            out.flush();
                        }

                        // flip expected seq for next unique data
                        expectedSeq = (expectedSeq == 0) ? 1 : 0;

                        System.out.println("SERVER: Wrote DATA seq=" + seg.getSeqNum()
                                + " (batch lines: " + (payload.isEmpty() ? 0 : payload.split(";").length) + ")");
                    } else {
                        // duplicate DATA (client retried because its ACK got “lost”)
                        System.out.println("SERVER: Duplicate DATA seq=" + seg.getSeqNum()
                                + " — not writing again, will just re-ACK");
                    }

                    if (!isLost(loss)) {
                        Segment ack = new Segment(seg.getSeqNum(), SegmentType.Ack, "", 0);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                            oos.writeObject(ack);
                            oos.flush();
                        }
                        byte[] ab = baos.toByteArray();
                        DatagramPacket ap = new DatagramPacket(ab, ab.length, pkt.getAddress(), pkt.getPort());
                        serverSocket.send(ap);
                        System.out.println("SERVER: SEND ACK (DATA) seq=" + seg.getSeqNum());
                    } else {
                        System.out.println("SERVER: *** Simulated ACK loss (DATA) seq=" + seg.getSeqNum() + " ***");
                    }


                    if (totalExpected > 0 && written >= totalExpected) {
                        System.out.println("SERVER: All readings received (" + written + "/" + totalExpected + "). Done!");
                        break;
                    }
                }

                // Any other segment types: ignore or log and continue
            }
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println("SERVER: Error in receiveWithAckLoss: " + ex.getMessage());
        } finally {
            try { if (out != null) out.close(); } catch (IOException ignore) {}
        }
    }


	/*************************************************************************************************************************************
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

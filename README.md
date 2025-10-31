# CSC2035-Assignment-1
A Reliable Sensor Transmission Protocol (RSTP) that works on top of UDP

useful CLI commands

removal of stale code commands
--------------------------------------------------

del *.class
rm *.class


compliation commands
--------------------------------------------------

javac Server.java
javac Client.java

running server & client 
--------------------------------------------------

java Server 8080 nm
java Client 127.0.0.1 8080 data.csv results.txt 2 nm

⸻

Reflection on Protocol Development and Debugging Process
---------------------------------------------------

Working on this assignment really changed how I approach programming, especially when it comes to structuring logic and using proper error handling. At first, I saw this project as just a way to pass messages between a client and server, but as I started implementing each part of the protocol, I realised it’s more like building a chain of trust — every segment has to arrive safely, acknowledged, and in order, otherwise everything falls apart.

⸻

Big Picture Overview

The protocol we built is made up of five key methods that all link together to ensure reliable data transfer over UDP:
	1.	sendMetadata() – This is the first handshake between the client and server. It’s where the client tells the server what to expect, like how many readings there are, what the output file name will be, and how big each batch (patch size) is. It sets the stage for the rest of the communication and makes sure both sides are aligned before sending any real data.
	2.	readAndSend() – This method reads chunks of data from the CSV file and sends them to the server in controlled batches. Each batch becomes one data segment. What I liked about this part is the sequence toggling between 0 and 1 — it’s a really smart but simple way of tracking what data the server has acknowledged. It’s also the first place where I really started to understand how important order and structure are in communication protocols.
	3.	receiveAck() – This is where the client patiently waits for confirmation from the server that its last segment was received. It taught me a lot about validation — checking not just if something came back, but if it’s the right thing. I added logic to verify both the type (ACK) and the sequence number, which was a key detail to avoid false positives.
	4.	startTimeoutWithRetransmission() – This one really showed me the importance of error handling and control flow. Using a try-catch block with a timeout system made the protocol more resilient. I’d never really thought much about using try and catch before this project, but now I see how they make everything cleaner and safer. Instead of crashing when something goes wrong, the client can intelligently retry or shut down gracefully. I also liked that I could use finally to reset the socket state, which ensures the system always returns to a stable condition.
	5.	receiveWithAckLoss() – This was the final piece, and also the most interesting. It simulates real-world network conditions where ACKs might get lost. Implementing this gave me a deeper appreciation for reliability in networking — how systems handle duplication, missing acknowledgements, and reordering without corrupting the final data. Writing this logic with conditionals for when to “lose” ACKs or resend data really made me feel like I was coding a simplified version of TCP’s behaviour.

⸻

Debugging Process

Most of my debugging came down to logic and flow. I started off just trying to get packets sent and received, but quickly realised small mistakes in sequencing or timeout logic could break everything. Printing detailed logs like
"CLIENT: TIMEOUT ALERT" or "SERVER: *** Simulated ACK loss (DATA) seq=0 ***"
made a huge difference because I could actually trace the exact moment something went wrong.

I also learned to test step by step — first just the metadata transfer, then normal data, then adding timeout logic, and finally the ACK loss mode. Doing it progressively helped me catch issues early and understand how each part depended on the previous one.

When I got to part 3, the timeout retransmission loop really tested my patience at first — I had infinite loops at one point! But once I structured the try-catch properly and added a max retry limit, it started behaving perfectly. That was the moment I really saw the benefit of exception handling: it keeps your logic clean while still being flexible enough to handle the unexpected.

⸻

Code Patterns and Good Practices I Learned
	•	Exception handling (try, catch, finally) — This is definitely the biggest takeaway for me. I used to ignore it, but now I see how it’s the backbone of reliable software. Instead of guessing where something might fail, you control it and recover safely.
	•	Sequencing logic (0/1 alternation) — A very simple but effective way to maintain order. It taught me that not every problem needs a complex solution — sometimes minimal logic works best.
	•	Clear separation of methods — Each function had a clear purpose, and I noticed how much easier it was to debug when responsibilities didn’t overlap.
	•	Verbose logging — Writing detailed console outputs gave me live feedback and made debugging more intuitive than just guessing what was happening behind the scenes.
	•	Idempotent design — The ACK loss mode made me realise that re-sending the same data shouldn’t cause duplication or corruption, which is a great principle for designing safe systems.

⸻

Finer Details and Overall Growth

This project helped me develop a deeper mindset for defensive programming — thinking about what can go wrong before it happens. It also showed me the relationship between client and server logic: how one small decision on one side affects the other.

I think the main thing that stands out is how I moved from just writing code that works to writing code that handles failure. The structure of these methods really shows that — every action has a backup plan, and that’s what separates good code from great code.

By the end, I could actually look at my logs and understand every part of the communication, from the first metadata packet to the final ACK, and that’s something I’m genuinely proud of.

⸻

Summary

Overall, this was one of the most complete and educational coding processes I’ve done so far. It made me appreciate how network reliability is achieved and gave me strong habits like writing modular code, handling exceptions, and testing step by step.
If I ever design a system again — whether it’s in C++, Python, or something else — I’ll definitely carry these patterns with me, especially structured error handling and clean retry logic.

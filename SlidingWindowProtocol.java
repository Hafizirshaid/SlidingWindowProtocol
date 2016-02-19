/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */





import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.io.FileOutputStream;
import java.io.IOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.net.DatagramPacket;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JTextField;

import javax.swing.JOptionPane;


class packet implements Serializable
{
	/*  packet class  */

	boolean is_final_packet = false;     /* Flag to tell the receiver this is the final packet  */

	int check_sum = 0;                   /* checksum  */

	int num;                             /* number of packet in the array that conatin the file packets */

	int Packet_Type;                     /* 0-->data ; 1-->ack ; 3--> messege */

	long Data_Length;                    /* Data lenght in bytes */

	byte[] Data;                         /* Sequance number in window */ 

	int seq;                             /* sequance number in sliding window */

	public packet(byte [] data , int type , int num)
	{

		this.Data = data;

		this.Packet_Type = type;

		this.num = num;
		if(type == 0)
		{
			Data_Length = data.length;

			checksum ();
		}
	}
	void checksum ()
	{
		for ( int i = 0 ; i < Data_Length ; i++ )
		{
			check_sum += (int) Data[ i ];
		}
	}
	public void khareb()
	{
		check_sum++;
	}
	public void ma_tkhareb()
	{
		check_sum--;
	}
	public boolean is_valid_check_sum ()
	{
		int cc = 0;
		for ( int i = 0 ; i < Data_Length ; i++)
		{
			cc += (int) Data[ i ];
		}
		if( cc == check_sum )
		{
			return true;
		}
		else
		{
			return false;
		}
	}
}

class file_information implements Serializable 
{
	String file_name;                 /* file name  */

	long lenght_in_bytes;             /* number of bytes in the file */

	int num_of_packets;               /* number of packet that the sender wants to send  */ 

	int window_size;                  /* the size of window*/

	int Max_Buffer_Size ;             /* Maximum size of Duffer used in recivin informaion */

	public file_information(String file_name , long length ,int packets_num , int window_size,int Max_buff)
	{
		this.file_name = file_name;

		this.Max_Buffer_Size = Max_buff;

		this.lenght_in_bytes = length;

		this.num_of_packets = packets_num;

		this.window_size = window_size;

	}
}


class receiver extends Thread
{
	int receive_packet_count = 0;          /*  number of received packets  */

	InetAddress ip;                        /*   ip address */

	int port_num ;                         /* for sender */

	//int port_rec_num = 9000;             /* port num of reciver this program */

	String File_Name;                      /* File name   */

	int WindowSize ;                       /* Size of window*/

	DatagramSocket socket;                 /*  General socket  */

	int Max_Buffer_Size = 50000;            /* Maximum size of Duffer used in recivin informaion */

	int Currupted_packet_count = 0;        /*  number of curruptet packets  */

	int cout_of_packet_from_sender = 0;    /*  number of packets that sender want to send it  */

	JTextField acksc;
	
	JTextField curr;
	public receiver(int port,InetAddress ip_num,DatagramSocket ss,JTextField acks,JTextField curr)
	{
		this.curr = curr;
		this.acksc = acks;
		this.socket = ss;
		this.ip = ip_num;
		this.port_num = port;
		System.out.print(ip+" "+port_num);

	}
	public  void send_packet_final(packet p)
	{
		byte[] buf;
		try 
		{
			buf = serialize(p);
			DatagramPacket send=new DatagramPacket(buf, buf.length,ip,port_num);
			socket.send(send);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}


	}

	public  byte [] serialize(Object obj) throws IOException 
	{
		/*covert the object into array of bytes*/
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(out);
		os.writeObject ( obj ) ;
		return out.toByteArray ();
	}

	/* -------------------------------------------  */
	public  Object deserialize(byte[] data) throws IOException, ClassNotFoundException 
	{
		/*convert the byte in to object  */
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);
		return is.readObject();
	}

	/* -------------------------------------------  */

	public  void run()
	{
		try
		{
			System.out.print("hayne sha3ál ");
			//ip = InetAddress.getByName( "127.0.0.1" );


			//socket = new DatagramSocket( port_rec_num );


			byte [] hsb=new byte[ Max_Buffer_Size ];
			DatagramPacket hsp=new DatagramPacket(hsb, hsb.length);
			socket.receive(hsp);     /*wait until the handshake is ok */

			packet handshake=(packet) deserialize(hsb);
			if(handshake.is_valid_check_sum() && handshake.Packet_Type == 3 && handshake.num == 0)
			{
				/* valid packet conatin the data */

				file_information info = (file_information) deserialize(handshake.Data);
				File_Name = info.file_name;
				WindowSize = info.window_size;
				cout_of_packet_from_sender = info.num_of_packets;



				System.out.print("Handshake OK \n");
				packet pp=new packet(null,4,0);

				send_packet_final(pp);


				//JOptionPane.showMessageDialog(null,WindowSize+"  "+File_Name);
				FileOutputStream f = new FileOutputStream( new File (File_Name) );
				int seq = 0;
				/* handshake here */

				int count_of_packets = 0;
				int num = 0;
				int count_valid_packet = 0;
				WindowSize++;
				while(count_valid_packet < (cout_of_packet_from_sender - 1))
				{

					byte [] buf = new byte [ Max_Buffer_Size ];
					DatagramPacket p = new DatagramPacket (buf, buf.length);
					//socket.setSoTimeout(1000);
					socket.receive(p);

					packet c = (packet) deserialize(buf);

					/*if(c.is_final_packet == true)
			{
				break;
			}*/
					if(c.is_valid_check_sum())
					{

						if( c.seq == seq )
						{
							count_valid_packet++;
							System.out.println(c.seq + "   num  " + c.num + " is received  ");

							packet ack = new packet (null,1,num);
							ack.seq = seq;
							byte []ack_arr = serialize (ack);
							DatagramPacket ack_for = new DatagramPacket (ack_arr, ack_arr.length ,ip , port_num);

							socket.send( ack_for );

							num++;
							seq++;
							seq = seq % WindowSize;
							//arrive.add(c);
							count_of_packets++;
							acksc.setText(""+count_of_packets);
							f.write(c.Data);/* write data to file */
							System.out.println(" send the ack \n");
							System.out.print("---- the count of packets at this moment is " + count_of_packets + "---\n");

						}
						else
						{
							/*  keb el packet */
							//System.out.print("Do nothing 1\n");
							//int $7m = 0;
							//$7m++;
						}
					}
					else
					{
						System.out.print("Courrupted Packet Received " + Currupted_packet_count +" +\n");
						Currupted_packet_count++;
						curr.setText(""+Currupted_packet_count);
						
					}
				}
				f.close();
				System.out.println("\n   "+count_of_packets +"     khareb "+ Currupted_packet_count);	
				//System.exit(10);
			}
			else 
			{
				System.out.print("Error in hand shake");
				//System.exit(1);
			}
		}
		catch(Exception ex){

			JOptionPane.showMessageDialog(null, ex.getMessage());
			ex.printStackTrace();
		}
	}

}


/**
 *
 * @author Hafiz
 */
/*------------------------Start-----------------------------*/

/*----------------------------------------------------------*/
public class SlidingWindowProtocol extends javax.swing.JFrame
{

	/* -------------------------------------------  */

	Random rand = new Random();      /* random number generation  */

	int count_packets = 0;           /* sent packets*/

	int count_acks = 0;              /*received packets*/

	int currupted_packets = 0 ;      /*currupted packets */

	int error = 20;                  /* percent of error in sending packets*/

	static int max_num_of_byte = 10000;     /* Maximum number of byes in each packet */

	static DatagramSocket Socket;           /* General Socket to send and receive data*/

	int port_num;                    /* port number of receiver */

	int WindowSize = 20;             /* Size of window*/

	int base = 0;                    /* Base of Window*/

	InetAddress ip;                  /* IP Number of receiver */

	int next = 0;                    /* next in window to send*/

	timer [] timer_array;            /* each packet in window has timer in this array */

	int sleep_time = 20;             /* sleep time for timer */

	int Max_Buffer_Size =50000;      /* Maximum size of Duffer used in recivin informaion */

	Object mutex = new Object();     /* used in Syncronized  */

	/* -------------------------------------------  */
	class receiver_acks extends Thread
	{
		/*to receive all acks and stop timer if the packet is valid*/

		public receiver_acks()
		{
			start();
		}
		public  void run()
		{
			try 
			{
				method();
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			} 
			catch (ClassNotFoundException e) 
			{
				e.printStackTrace();
			}	
		}

		public synchronized void method() throws IOException, ClassNotFoundException
		{
			while(true)
			{
				byte [] buf = new byte [ Max_Buffer_Size ];
				DatagramPacket p = new DatagramPacket(buf, buf.length);
				Socket.receive(p);
				packet ack = (packet) deserialize(buf);
				if( ack.Packet_Type == 1 && ack.is_valid_check_sum() )
				{
					count_acks++;
					//timer_array[ ack.seq ].stop = true;     //stop the timer
					timer_array[ ack.seq ].stop(); 
					base++;                                   //slide the window 

					System.out.print("\nthe timer is stopped " + ack.seq+"  num ->" +ack.num + " and window is slided  \n");

				}
				else
				{
					//do nothing or other thing
					System.out.print("m3aleg");
				}
			}
		}
	}

	/* -------------------------------------------  */
	class timer extends Thread 
	{ 
		/*
		 * timer class if thread will sleep aspacific time then resend the packet if and only if no other thread stop this 
		 */
		packet p;
		//static Object mutex2 = new Object();
		public timer(packet p)
		{
			this.p = p;
		}
		int count = 0;
		public void run()
		{
			synchronized (new Object()) 
			{
				try 
				{
					if(p.is_final_packet == false)
					{
						Thread.sleep(sleep_time);	        
						send_packet_final(p);          /* resend the packet */
						this.run();
						System.out.println(p.seq + " num  " + p.num + "  is timed out \n");

					}
				} 
				catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
		}
	}
	/* ------------------------------------------- */

	static int count_num_of_packet(int File_Size)
	{
		int x = File_Size;
		int y = x / max_num_of_byte;
		if( x % max_num_of_byte != 0)
		{
			y++;
		}
		return y;
	}	

	/* -------------------------------------------  */

	public  void send_packet_final(packet p)
	{
		byte[] buf;
		try 
		{
			buf = serialize(p);
			DatagramPacket send=new DatagramPacket(buf, buf.length,ip,port_num);
			Socket.send(send);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}

	/* -------------------------------------------  */
	public  byte[] serialize(Object obj) throws IOException 
	{
		/*
		 *covert the object into array of bytes 
		 */
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(out);
		os.writeObject ( obj ) ;
		return out.toByteArray ();
	}
	/* -------------------------------------------  */

	public  Object deserialize(byte[] data) throws IOException, ClassNotFoundException
	{ 
		/*
		 * convert the byte in to object 
		 * */
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = new ObjectInputStream(in);
		return is.readObject();
	}
	/* -------------------------------------------  */
	/**
	 * @param args
	 * @throws Exception
	 */



	public Interface() 
	{
		initComponents();
		sendBtn.setVisible(false);
		browseBtn.setVisible(false);
		stopBtn.setVisible(false);
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated Code">
	private void initComponents()
	{

		recIP = new javax.swing.JTextField();
		senderPortNum = new javax.swing.JTextField();
		jLabel1 = new javax.swing.JLabel();
		jLabel2 = new javax.swing.JLabel();
		stopBtn = new javax.swing.JButton();
		jButton2 = new javax.swing.JButton();
		jScrollPane1 = new javax.swing.JScrollPane();
		jTextArea1 = new javax.swing.JTextArea();
		jLabel3 = new javax.swing.JLabel();
		jLabel4 = new javax.swing.JLabel();
		jScrollPane2 = new javax.swing.JScrollPane();
		jTextArea2 = new javax.swing.JTextArea();
		browseBtn = new javax.swing.JButton();
		sendBtn = new javax.swing.JButton();
		jSeparator1 = new javax.swing.JSeparator();
		jLabel5 = new javax.swing.JLabel();
		jLabel6 = new javax.swing.JLabel();
		jLabel7 = new javax.swing.JLabel();
		jLabel8 = new javax.swing.JLabel();
		jLabel9 = new javax.swing.JLabel();
		errPercent = new javax.swing.JTextField();
		sentPckt = new javax.swing.JTextField();
		recPckt = new javax.swing.JTextField();
		corPckt = new javax.swing.JTextField();
		wSTextF = new javax.swing.JTextField();
		jLabel10 = new javax.swing.JLabel();
		recPortNum = new javax.swing.JTextField();
		jMenuBar1 = new javax.swing.JMenuBar();
		jMenu1 = new javax.swing.JMenu();
		jMenu2 = new javax.swing.JMenu();
		jMenuItem1 = new javax.swing.JMenuItem();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setTitle("File Sender");
		setBackground(new java.awt.Color(255, 204, 51));
		setForeground(new java.awt.Color(51, 153, 0));
		setMaximumSize(new java.awt.Dimension(676, 500));
		setResizable(false);

		jLabel1.setText("IP Address:");

		jLabel2.setText("Port Number:");

		stopBtn.setText("Start Receiver");
		stopBtn.addActionListener(new java.awt.event.ActionListener() 
		{
			public void actionPerformed(java.awt.event.ActionEvent evt) 
			{
				stopBtnActionPerformed(evt);
			}
		});

		jButton2.setText("Connect");
		jButton2.addActionListener(new java.awt.event.ActionListener() 
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				jButton2ActionPerformed(evt);
			}
		});

		jTextArea1.setColumns(20);
		jTextArea1.setRows(5);
		jScrollPane1.setViewportView(jTextArea1);

		jLabel3.setText("Sent Files");

		jLabel4.setText("Received Files");

		jTextArea2.setColumns(20);
		jTextArea2.setRows(5);
		jScrollPane2.setViewportView(jTextArea2);

		browseBtn.setText("Browse and Send");
		browseBtn.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt) 
			{
				browseBtnActionPerformed(evt);
			}
		});

		sendBtn.setText("Send");
		sendBtn.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt) 
			{
				sendBtnActionPerformed(evt);
			}
		});

		jLabel5.setText("Error %:");

		jLabel6.setText("Sent Packets:");

		jLabel7.setText("Received Packets:");

		jLabel8.setText("Corrupted Packets:");

		jLabel9.setText("Sliding window size:");

		sentPckt.setEnabled(false);

		recPckt.setEnabled(false);

		corPckt.setEnabled(false);

		jLabel10.setText("Receiver Port #:");

		jMenu1.setText("File");
		jMenuBar1.add(jMenu1);

		jMenu2.setText("Help");

		jMenuItem1.setText("About");
		jMenuItem1.addActionListener(new java.awt.event.ActionListener() 
		{
			public void actionPerformed(java.awt.event.ActionEvent evt) 
			{
				jMenuItem1ActionPerformed(evt);
			}
		});
		jMenu2.add(jMenuItem1);

		jMenuBar1.add(jMenu2);

		setJMenuBar(jMenuBar1);

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
						.addContainerGap()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(layout.createSequentialGroup()
										.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
												.addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 307, javax.swing.GroupLayout.PREFERRED_SIZE)
												.addComponent(jLabel4)
												.addComponent(browseBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
												.addGap(0, 0, Short.MAX_VALUE))
												.addGroup(layout.createSequentialGroup()
														.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
																.addComponent(sendBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)
																.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
																		.addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 307, javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addComponent(jLabel3)))
																		.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
																				.addGroup(layout.createSequentialGroup()
																						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
																								.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
																										.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
																												.addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																												.addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																												.addComponent(stopBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
																												.addComponent(jLabel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
																												.addGap(18, 18, 18)
																												.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
																														.addComponent(recIP)
																														.addComponent(senderPortNum)
																														.addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE)
																														.addComponent(recPortNum)))
																														.addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 343, javax.swing.GroupLayout.PREFERRED_SIZE)))
																														.addGroup(layout.createSequentialGroup()
																																.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
																																		.addGroup(layout.createSequentialGroup()
																																				.addGap(85, 85, 85)
																																				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
																																						.addComponent(jLabel9)
																																						.addComponent(jLabel5, javax.swing.GroupLayout.Alignment.LEADING)
																																						.addComponent(jLabel7, javax.swing.GroupLayout.Alignment.LEADING)
																																						.addComponent(jLabel6, javax.swing.GroupLayout.Alignment.LEADING)))
																																						.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
																																								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																																								.addComponent(jLabel8)))
																																								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
																																										.addGroup(layout.createSequentialGroup()
																																												.addGap(18, 18, 18)
																																												.addComponent(wSTextF))
																																												.addGroup(layout.createSequentialGroup()
																																														.addGap(17, 17, 17)
																																														.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
																																																.addComponent(corPckt, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
																																																.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
																																																		.addComponent(errPercent, javax.swing.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
																																																		.addComponent(sentPckt)
																																																		.addComponent(recPckt)))))
																																																		.addGap(0, 0, Short.MAX_VALUE)))))
																																																		.addContainerGap())
				);
		layout.setVerticalGroup(
				layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(layout.createSequentialGroup()
										.addContainerGap()
										.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(recIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
												.addComponent(jLabel1)))
												.addGroup(layout.createSequentialGroup()
														.addGap(25, 25, 25)
														.addComponent(jLabel3)))
														.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
														.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
																.addGroup(layout.createSequentialGroup()
																		.addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)
																		.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																		.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
																				.addComponent(browseBtn)
																				.addComponent(sendBtn))
																				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 55, Short.MAX_VALUE)
																				.addComponent(jLabel4)
																				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
																				.addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE))
																				.addGroup(layout.createSequentialGroup()
																						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
																								.addComponent(senderPortNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
																								.addComponent(jLabel2))
																								.addGap(11, 11, 11)
																								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
																										.addComponent(recPortNum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
																										.addComponent(jLabel10))
																										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																										.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
																												.addComponent(stopBtn)
																												.addComponent(jButton2))
																												.addGap(43, 43, 43)
																												.addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
																												.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																												.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
																														.addComponent(jLabel5)
																														.addComponent(errPercent, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
																														.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																														.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
																																.addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
																																.addComponent(sentPckt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
																																.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																																.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
																																		.addComponent(jLabel7)
																																		.addComponent(recPckt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
																																		.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																																		.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
																																				.addComponent(jLabel8)
																																				.addComponent(corPckt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
																																				.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
																																				.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
																																						.addComponent(jLabel9)
																																						.addComponent(wSTextF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
																																						.addGap(0, 0, Short.MAX_VALUE)))
																																						.addContainerGap())
				);

		pack();
	}// </editor-fold>

	private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {                                           
		// TODO add your handling code here:
		JOptionPane.showMessageDialog(rootPane, "Programmed By: Eng. Hafiz Irshaid & Eng. Mustafa Shbere, © 2012. \n"
				+ "Submitted To: Dr.Raed Alqadi. \n"
				+ "Course: Computer Network ǀ.  \n"
				+ "An-Najah National University.");
	}                                          

	private void browseBtnActionPerformed(java.awt.event.ActionEvent evt) 
	{                                          

		try
		{
			WindowSize = Integer.parseInt(wSTextF.getText());
			
			error = Integer.parseInt(errPercent.getText());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
		
		timer_array = new timer [ WindowSize + 1 ] ;
		
		JFileChooser fc = new JFileChooser();
		fc.show();
		fc.showOpenDialog(null);
		File ff = fc.getSelectedFile();

		System.out.print(ff.getPath()+"  "+ff.length());
		FileInputStream f = null;
		try 
		{
			f = new FileInputStream(ff);
		} 
		catch (FileNotFoundException ex) 
		{
			Logger.getLogger(Interface.class.getName()).log(Level.SEVERE, null, ex);
		}
		byte[] data = new byte[ (int) ff.length() ];
		try 
		{
			f.read(data);
		} 
		catch (IOException ex) 
		{
			Logger.getLogger(Interface.class.getName()).log(Level.SEVERE, null, ex);
		}

		/*
		 * this method read all data in the file and put it in this array of byte as its lenght 
		 */

		System.out.print("----------------------------\n");

		/* encapsulate each 10 byte into Class Packet                */

		/* we have array called data that contain bytes in the file  */

		System.out.print("\n");


		packet[] packets = new packet [ (count_num_of_packet ( data.length ) + 1 ) ] ;

		int p = 0;
		int seq = 0;
		int hafiz = WindowSize + 1;
		for ( int i = 0 ; i < (packets.length + 1 ); i++ )
		{

			if(packets.length == i)
			{
				byte []temp = new byte[10];
				packets[ i -1 ] = new packet(temp,0,i);
				packets[ i -1 ].is_final_packet = true;
				break;
			}

			byte [] temp = new byte [ max_num_of_byte ] ;

			for ( int j = 0 ; j < max_num_of_byte ; j++ )
			{
				if((data.length-p) == 0) 
					break;

				temp[ j ] = data[ p ];
				p++;
			}
			packets[ i ] = new packet(temp,0,i);
			packets[ i ].seq = seq;
			seq++;
			seq = seq % hafiz;

			//make data packet


		}
		/*
		 * here implement hand shake 3 way 
		 * */
		boolean handshake_OK = false;
		file_information info = new file_information(ff.getName(), ff.length(), packets.length, WindowSize, Max_Buffer_Size);  
		/* make file information packet to send to receiver*/

		byte [] hsb = null;
		try 
		{
			hsb=serialize(info);
		}
		catch (IOException ex)
		{
			Logger.getLogger(Interface.class.getName()).log(Level.SEVERE, null, ex);
		}
		packet hsp=new packet(hsb, 3, 0);
		send_packet_final(hsp);   /*send handshake packet type 3*/

		timer hand_shake_timer = new timer(hsp);     /* start the timer */
		byte[] hand_buf = new byte[ Max_Buffer_Size ];
		DatagramPacket ack_handshake = new DatagramPacket(hand_buf, hand_buf.length);
		try 
		{
			Socket.receive( ack_handshake );
		} 
		catch (IOException ex) 
		{
			Logger.getLogger(Interface.class.getName()).log(Level.SEVERE, null, ex);
		}

		packet ack_h = null;
		try 
		{
			ack_h = (packet) deserialize(hand_buf);
		}
		catch (IOException ex) 
		{
			Logger.getLogger(Interface.class.getName()).log(Level.SEVERE, null, ex);
		} 
		catch (ClassNotFoundException ex) 
		{
			Logger.getLogger(Interface.class.getName()).log(Level.SEVERE, null, ex);
		}

		if(ack_h.Packet_Type == 4 && ack_h.is_valid_check_sum()) 
		{
			handshake_OK = true;
		}
		else 
		{
			handshake_OK = false;
			System.out.print("handshake error");
		}


		/*
		 * here we implement sliding window ---> ok 
		 * */

		if(handshake_OK == true)
		{
			System.out.print("Handshake OK \n");


			int count_of_khareb_packets = packets.length * (error / 100);   /* Count Error Percent*/

			receiver_acks thr = new receiver_acks();                        /*  start the ack receiver*/
			packets[ packets.length - 1 ].is_final_packet = true;


			//int counter = 0;

			synchronized ( mutex ) //must synchronized
			{
				while (count_packets <( packets.length - 1))
				{
					if (next >= (packets.length))
					{
						break;
					}
					else
					{
						int $x = 0;
						$x++;
					}
					if ((next - base) >= WindowSize) 
					{
						/*
						 * wait untill window is slide do nothing
						 * System.out.print("Do nothing \n");
						 */
						int $x = 0;
						$x++;
					} 
					else 
					{
						// send packet


						System.out.print("\nthe packet " + packets[next].seq
								+ "num -> " + packets[next].num
								+ " is sent and timer started\n");

						timer_array[ packets[next].seq ] = new timer(packets[next]);
						timer_array[ packets [ next ].seq ].start();


						/* here khareb */

						packet Curr_packet_to_send = new packet(packets[next].Data,packets[next].Packet_Type,packets[next].Packet_Type);
						Curr_packet_to_send.seq = packets[next].seq;
						int rand_num = rand.nextInt();

						rand_num *= 100;

						if(rand_num < error)
						{
							Curr_packet_to_send.khareb();
						}

						send_packet_final(Curr_packet_to_send);

						count_packets++;
						sentPckt.setText(""+count_packets);

						next++;
					}
				}
			}
		} 
		else 
		{
			/* handshake not ok */
			System.out.print("Handshake Error");

			int $x = 0;
			$x++;
			//System.out.print("Do nothing \n");
		}
		//System.out.print("Do nothing hafiz kamal irshaid \n");



	}                                         

	private void sendBtnActionPerformed(java.awt.event.ActionEvent evt)
	{                                        



	}                                       

	private void jButton2ActionPerformed(java.awt.event.ActionEvent evt)
	{                                         

		try 
		{
			String ip_p=recIP.getText();

			ip = InetAddress.getByName(ip_p);
		} 
		catch (UnknownHostException ex)
		{
			JOptionPane.showMessageDialog(rootPane, ex.getMessage());
			ex.printStackTrace();
		}

		port_num = Integer.parseInt(recPortNum.getText());
		try 
		{
			Socket = new DatagramSocket(Integer.parseInt(senderPortNum.getText()));
			jButton2.setEnabled(false);
			browseBtn.setVisible(true);
			stopBtn.setVisible(true);
		}
		catch (SocketException ex) 
		{
			ex.printStackTrace();
			JOptionPane.showMessageDialog(rootPane, ex.getMessage());
		}


	}                                        

	private void stopBtnActionPerformed(java.awt.event.ActionEvent evt) 
	{
		// TODO add your handling code here:
		receiver r=new receiver(port_num, ip,Socket,recPckt,corPckt);
		r.start();

	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) throws Exception 
	{
		/* Set the Nimbus look and feel */
		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
		/* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
		 * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
		 */
		try 
		{
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) 
			{
				if ("Nimbus".equals(info.getName())) 
				{
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		}
		catch (ClassNotFoundException ex) 
		{
			java.util.logging.Logger.getLogger(Interface.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
		catch (InstantiationException ex) 
		{
			java.util.logging.Logger.getLogger(Interface.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} 
		catch (IllegalAccessException ex) 
		{
			java.util.logging.Logger.getLogger(Interface.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} 
		catch (javax.swing.UnsupportedLookAndFeelException ex) 
		{
			java.util.logging.Logger.getLogger(Interface.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
		//</editor-fold>


		/* Create and display the form */
		java.awt.EventQueue.invokeLater(new Runnable() 
		{
			public void run()
			{
				new Interface().setVisible(true);
			}
		});
	}
	// Variables declaration - do not modify
	private javax.swing.JButton browseBtn;
	private javax.swing.JTextField corPckt;
	private javax.swing.JTextField errPercent;

	private javax.swing.JButton jButton2;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel10;

	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel4;

	private javax.swing.JLabel jLabel5;

	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabel7;

	private javax.swing.JLabel jLabel8;
	private javax.swing.JLabel jLabel9;

	private javax.swing.JMenu jMenu1;
	private javax.swing.JMenu jMenu2;
	private javax.swing.JMenuBar jMenuBar1;

	private javax.swing.JMenuItem jMenuItem1;
	private javax.swing.JScrollPane jScrollPane1;

	private javax.swing.JScrollPane jScrollPane2;
	private javax.swing.JSeparator jSeparator1;
	private javax.swing.JTextArea jTextArea1;
	private javax.swing.JTextArea jTextArea2;

	private javax.swing.JTextField recIP;
	private javax.swing.JTextField recPckt;
	private javax.swing.JTextField recPortNum;

	private javax.swing.JButton sendBtn;
	private javax.swing.JTextField senderPortNum;
	private javax.swing.JTextField sentPckt;
	private javax.swing.JButton stopBtn;
	private javax.swing.JTextField wSTextF;
	// End of variables declaration
}

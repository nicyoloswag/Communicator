package com.github.assisstion.Communicator.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import com.github.assisstion.Communicator.message.AudioMessageProcessor;
import com.github.assisstion.Communicator.message.MessageCommandProcessor;
import com.github.assisstion.Communicator.message.MessageCommandProcessorImpl;
import com.github.assisstion.Communicator.message.MessageProcessor;
import com.markusfeng.SocketRelay.A.SocketClient;
import com.markusfeng.SocketRelay.A.SocketHandler;
import com.markusfeng.SocketRelay.A.SocketServer;
import com.markusfeng.SocketRelay.C.SocketHelper;

/*
 * Uses SocketRelay API 1.0
 */
public class ChatFrame extends JFrame{

	private static final int PORT_MODIFIER = 1;

	/**
	 *
	 */
	private static final long serialVersionUID = -3551670774266923639L;
	private JPanel contentPane;
	private JTextField cHost;
	private JTextField cPort;
	private JTextField sPort;
	protected List<ChatPanel> panels;
	private JPanel panelX;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args){
		EventQueue.invokeLater(() -> {
			try{
				new ChatFrame().setVisible(true);
			}
			catch(Exception e){
				e.printStackTrace();
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public ChatFrame(){
		setTitle("Communicator");
		panels = new LinkedList<ChatPanel>();

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 228, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		panelX = new JPanel();
		contentPane.add(panelX, BorderLayout.CENTER);
		panelX.setLayout(new BoxLayout(panelX, BoxLayout.Y_AXIS));

		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new TitledBorder(null, "Client", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelX.add(panel_1);
		panel_1.setLayout(new BoxLayout(panel_1, BoxLayout.Y_AXIS));

		JPanel panel_4 = new JPanel();
		panel_1.add(panel_4);

		JLabel lblLocation = new JLabel("Host");
		panel_4.add(lblLocation);

		cHost = new JTextField();
		panel_4.add(cHost);
		cHost.setColumns(10);

		JPanel panel_7 = new JPanel();
		panel_1.add(panel_7);

		JLabel lblPort = new JLabel("Port");
		panel_7.add(lblPort);

		cPort = new JTextField();
		panel_7.add(cPort);
		cPort.setColumns(10);

		JPanel panel_3 = new JPanel();
		panel_1.add(panel_3);

		JButton btnStartClient = new JButton("Start Client");
		btnStartClient.addActionListener(e -> {
			MessageCommandProcessor mcp = new MessageCommandProcessorImpl();
			MessageProcessor processor = new MessageProcessor(mcp);
			mcp.setMessageProcessor(processor);
			try{
				startClient(cHost.getText(), Integer.parseInt(cPort.getText()), processor);
			}
			catch(NumberFormatException | IOException e1){
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		});
		panel_3.add(btnStartClient);

		JPanel panel_2 = new JPanel();
		panel_2.setBorder(new TitledBorder(null, "Server", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelX.add(panel_2);
		panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.Y_AXIS));

		JPanel panel_5 = new JPanel();
		panel_2.add(panel_5);

		JLabel lblPort_1 = new JLabel("Port");
		panel_5.add(lblPort_1);

		sPort = new JTextField();
		panel_5.add(sPort);
		sPort.setColumns(10);

		JPanel panel_6 = new JPanel();
		panel_2.add(panel_6);

		JButton btnStartServer = new JButton("Start Server");
		btnStartServer.addActionListener(e ->{
			MessageCommandProcessor mcp = new MessageCommandProcessorImpl();
			MessageProcessor processor = new MessageProcessor(mcp);
			mcp.setMessageProcessor(processor);
			try{
				startServer(Integer.parseInt(sPort.getText()), processor);
			}
			catch(NumberFormatException | IOException e1){
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		panel_6.add(btnStartServer);
	}

	public void startClient(String host, int port, MessageProcessor process) throws IOException{
		ChatPanel panel = generatePanel(process);
		new Thread(new ClientStarter(panel, host, port, process)).start();
		validate();
	}

	public class ClientStarter implements Runnable{

		protected String host;
		protected int port;
		protected MessageProcessor process;
		protected AudioMessageProcessor audioProcess;
		protected ChatPanel panel;

		public ClientStarter(ChatPanel panel, String host, int port, MessageProcessor process){
			this.panel = panel;
			this.host = host;
			this.port = port;
			this.process = process;
			audioProcess = process.getAudioProcess();
		}

		@Override
		public void run(){
			try(
					SocketClient<SocketHandler<String>> client =
					SocketHelper.getStringClient(host, port, process);
					SocketClient<SocketHandler<byte[]>> audioClient =
							audioProcess != null ? SocketHelper.getByteArrayClient(
									host, port + PORT_MODIFIER, audioProcess) : null; ){
				client.open();
				if(audioClient != null){
					audioClient.open();
				}
				panel.logger.info("Client Opened!");

				Thread chatThread = new Thread(panel);
				chatThread.start();
				synchronized(this){
					try{
						if(!panel.isDone()){
							chatThread.join();
						}
					}
					catch(InterruptedException e){
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				System.out.println("Done!");
			}
			catch(Exception e1){
				panel.logger.info("Exception caught!");
				e1.printStackTrace(new PrintWriter(new LogWriter(panel.logger, CustomLevel.NOMESSAGE)));
				/*
				Container container = panel.getParent().getParent().getParent();
				System.out.println(container.getClass());
				if(container instanceof JFrame){
					container.setVisible(false);
				}*/
			}
		}
	}

	public void startServer(int port, MessageProcessor process) throws IOException{
		ChatPanel panel = generatePanel(process);
		new Thread(new ServerStarter(panel, port, process)).start();
		validate();
	}

	public class ServerStarter implements Runnable{

		protected int port;
		protected MessageProcessor process;
		protected ChatPanel panel;
		protected AudioMessageProcessor audioProcess;

		public ServerStarter(ChatPanel panel, int port, MessageProcessor process){
			this.panel = panel;
			this.port = port;
			this.process = process;
			audioProcess = process.getAudioProcess();
		}

		@Override
		public void run(){
			try(SocketServer<SocketHandler<String>> server =
					SocketHelper.getStringServer(port, process);
					SocketServer<SocketHandler<byte[]>> audioServer =
							audioProcess != null ? SocketHelper.getByteArrayServer(
									port + PORT_MODIFIER, audioProcess) : null; ){
				server.open();
				if(audioServer != null){
					audioServer.open();
				}
				panel.logger.info("Server Opened!");
				Thread chatThread = new Thread(panel);
				chatThread.start();
				synchronized(this){
					try{
						if(!panel.isDone()){
							chatThread.join();
						}
					}
					catch(InterruptedException e){
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				System.out.println("Done!");
			}
			catch(Exception e1){
				panel.logger.info("Exception caught!");
				e1.printStackTrace(new PrintWriter(new LogWriter(panel.logger, CustomLevel.NOMESSAGE)));
				/*
				Container container = panel.getParent().getParent().getParent();
				System.out.println(container.getClass());
				if(container instanceof JFrame){
					container.setVisible(false);
				}*/
			}
		}
	}

	public ChatPanel generatePanel(MessageProcessor process){
		ChatPanel panel = new ChatPanel(process);
		panels.add(panel);
		JFrame frame = new JFrame();
		frame.setBounds(150, 150, 450, 300);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter(){

			@Override
			public void windowClosed(WindowEvent e){
				// TODO Auto-generated method stub
				panel.terminate();
			}

		});
		frame.setTitle("Chat Window");
		frame.setContentPane(panel);
		frame.setVisible(true);
		return panel;
	}

}

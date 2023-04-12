package com.digero.maestro;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.digero.common.util.Version;
import com.digero.maestro.view.ProjectFrame;

//import org.boris.winrun4j.DDE;

public class MaestroMain
{
	public static final String APP_NAME = "Maestro";
	public static final String APP_URL = "https://github.com/digero/maestro/";
	public static Version APP_VERSION = new Version(0, 0, 0);

	private static ProjectFrame mainWindow = null;
	public static Logger logger = Logger.getLogger("com.digero.maestro");

	private static ServerSocket serverSocket;
	
	public static void main(final String[] args) throws Exception
	{
		try
		{
			Properties props = new Properties();
			props.load(MaestroMain.class.getResourceAsStream("version.txt"));
			String versionString = props.getProperty("version.Maestro");
			if (versionString != null)
				APP_VERSION = Version.parseVersion(versionString);
		}
		catch (IOException ex)
		{
		}
		
		if(!openPort()) {
			sendArgsToPort(args);
			return;
		}

		System.setProperty("sun.sound.useNewAudioEngine", "true");

		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
		}
		
		mainWindow = new ProjectFrame();
		SwingUtilities.invokeAndWait(new Runnable()
		{
			@Override public void run()
			{
				mainWindow.setVisible(true);
				openSongFromCommandLine(args);
			}
		});
		try
		{
			//DDE.addActivationListener(mainWindow);
			//DDE.ready();
			ready();
		}
		catch (UnsatisfiedLinkError err)
		{
			// Ignore (we weren't started via WinRun4J)
			//System.err.println("we weren't started via WinRun4J");
			//logger.info("we weren't started via WinRun4J 32 bit");
			//System.exit(0);
		}
	}
	
	public static void setMIDIFileResolved () {
		if (mainWindow == null) return;
		mainWindow.setMIDIFileResolved();
	}

	/** Tells the WinRun4J launcher that we're ready to accept activate() calls. */
	public static native void ready();

	/** A new activation from WinRun4J 32bit (a.k.a. a file was opened) */
	public static void activate(final String[] args)
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override public void run()
			{
				openSongFromCommandLine(args);
			}
		});
	}
	
	/** A new activation from WinRun4J 64bit (a.k.a. a file was opened) */
	public static void activate(String arg0) {
		final String[] args = {arg0.substring(1, arg0.length()-1)};
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override public void run()
			{
				MaestroMain.openSongFromCommandLine(args);
			}
		});
	}
	
	public static void execute(String cmdLine)
	{
		openSongFromCommandLine(new String[] { cmdLine });
	}

	public static void openSongFromCommandLine(String[] args)
	{
		if (mainWindow == null) {
			return;
		}

		int state = mainWindow.getExtendedState();
		if ((state & JFrame.ICONIFIED) != 0)
			mainWindow.setExtendedState(state & ~JFrame.ICONIFIED);

		if (args.length > 0)
		{
			File file = new File(args[0]);
			if (file.exists())
				mainWindow.openFile(file);
		}
	}

	/** @deprecated Use isNativeVolumeSupported() instead. */
	@Deprecated public static native boolean isVolumeSupported();

	public static boolean isNativeVolumeSupported()
	{
		try
		{
			return isVolumeSupported();
		}
		catch (UnsatisfiedLinkError err)
		{
			//logger.info("isNativeVolumeSupported = false");
			return false;
		}
	}

	public static native float getVolume();

	public static native void setVolume(float volume);

	public static void onVolumeChanged()
	{
		if (mainWindow != null)
			mainWindow.onVolumeChanged();
	}
	
	private static boolean openPort() {
		
		try {
			serverSocket = new ServerSocket(8000+APP_VERSION.getBuild());
			if (serverSocket == null) {
				//System.out.println("Port is null");
				return false;
			}
			if (serverSocket.getLocalPort() != 8000+APP_VERSION.getBuild()) {
				//System.out.println("Port is "+serverSocket.getLocalPort());
				return false;
			}			
		} catch (IOException e) {
			//e.printStackTrace();
			return false;
		}
		//System.out.println("Made port");
		(new Thread() {
			public void run() {
				try {
					while (true) {
						Socket socket = serverSocket.accept();
						//System.out.println("Accepted");
				        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			        	//while (socket.isConnected()) {
			        		String data = in.readLine();
						
			        		if (data != null) {
			        			//System.out.println("Received "+data);
			        			String[] datas = {data};
			        			activate(datas);
			        		} else {
			        			//System.out.println("Received nothing");
			        		}
			        	//}
			        	//socket.close();
				    }
			    } catch (IOException e) {
			    	//e.printStackTrace();
			    }
			}
		}).start();
	    return true;
	}
	
	private static void sendArgsToPort(final String[] args) {
		try {
			Socket clientSocket = new Socket("localhost", 8000+APP_VERSION.getBuild());
			DataOutputStream os = new DataOutputStream(clientSocket.getOutputStream());
			//for (String arg : args) {
				os.writeBytes(args[0]);
				//System.out.println("Wrote "+args[0]+" to 8001");
			//}
			clientSocket.close();
		} catch (IOException e) {
			//e.printStackTrace();
		}
	}
}

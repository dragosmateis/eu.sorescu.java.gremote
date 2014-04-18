package eu.sorescu.java.gremote;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.prefs.Preferences;

public class Dispatcher {
	public static void main(String[] args) throws IOException, InterruptedException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (args == null)
			args = new String[0];
		if (args.length == 0)
			System.out.println("Usage syntax: ");
		Method matchingMethod = null;
		for (Method method : Dispatcher.class.getMethods()) {
			MethodMetadata callable = method.getAnnotation(MethodMetadata.class);
			if (callable == null)
				continue;
			if (args.length == 0) {
				System.out.println("\tjava " + Dispatcher.class.getName() + " " + method.getName() + " " + callable.params());
				System.out.println("\t\tDescription: " + callable.description());
				System.out.println();
			}
			if (method.getParameterTypes().length == args.length - 1)
				if (args.length > 0)
					if (method.getName().equals(args[0]))
						matchingMethod = method;
		}
		if (args.length > 0) {
			if (matchingMethod != null) {
				try {
					Object result = matchingMethod.invoke(null, (Object[]) Arrays.copyOfRange(args, 1, args.length));
					System.out.println("RESULT: " + result);
				} catch (Throwable t) {
					t.printStackTrace();
					if (trayIcon != null)
						SystemTray.getSystemTray().remove(trayIcon);
				}
			} else {
				System.out.println("No method found: " + args[0] + " with " + (args.length - 1) + " parameters.");
			}
		}
	}

	@MethodMetadata(params = "<key>", description = "Retrieves the configuration value <key>")
	public static String GET_CONFIG(String key) {
		return Preferences.userNodeForPackage(Dispatcher.class).get(key, null);
	}

	@MethodMetadata(params = "<key> <value>", description = "Sets the configuration value <key> to the value <value>")
	public static void SET_CONFIG(String key, String value) {
		Preferences.userNodeForPackage(Dispatcher.class).put(key, value);
	}

	@MethodMetadata(params = "<port>", description = "Starts the dispatcher; <CLASS_PATH> configuration item must be set prior to execution")
	public static final void START(String port) throws IOException, InterruptedException {
		setIcon(Color.gray, "starting...");
		int counter = 0;
		try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port))) {
			setIcon(Color.green, counter + " requests since started.");
			for (;;) {
				try (Socket socket = serverSocket.accept()) {
					System.out.println("SOCKET CONNECTION RECEIVED: " + socket.getRemoteSocketAddress());
					new Dispatcher(socket).exec();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}

	private final Socket socket;

	public Dispatcher(Socket socket) throws IOException {
		this.socket = socket;
	}

	private void exec() throws IOException, InterruptedException {
		setIcon(Color.orange, "Building process...");
		ProcessBuilder builder = new ProcessBuilder("java", "-classpath", GET_CONFIG("CLASS_PATH"), Agent.class.getName());
		builder.directory(new File(".").getAbsoluteFile());
		setIcon(Color.yellow, "Starting process...");
		Process process = builder.start();
		setIcon(Color.orange, "Process up...");
		StreamPipeline p1 = new StreamPipeline(socket.getInputStream(), process.getOutputStream());
		StreamPipeline p2 = new StreamPipeline(process.getInputStream(), socket.getOutputStream());
		StreamPipeline p3 = new StreamPipeline(process.getErrorStream(), socket.getOutputStream());
		p1.start();
		p2.start();
		p3.start();
		setIcon(Color.red, "Waiting for process to exit...");
		try {
			for (; p1.isAlive() && (p2.isAlive() || p3.isAlive());) {
				// System.out.println("P1:" + p1.isAlive());
				// System.out.println("P2:" + p2.isAlive());
				// System.out.println("P3:" + p3.isAlive());
				Thread.sleep(1000);
			}
		} catch (Throwable t) {
			System.out.println("ERRRRRRRRRR");
			t.printStackTrace();
		}
		System.out.println("OUT");
		process.waitFor();
		try {
			socket.getOutputStream().write("\r\nAgent is closed now.".getBytes());
		} catch (Throwable t) {
		}
		socket.close();
		setIcon(Color.green, "Process ended");
	}

	private static TrayIcon trayIcon = null;

	private static TrayIcon getIcon() {
		if (trayIcon != null)
			return trayIcon;
		trayIcon = new TrayIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
		try {
			SystemTray.getSystemTray().add(trayIcon);
			PopupMenu menu = new PopupMenu();
			menu.add("Groovy Dispatcher");
			MenuItem item = new MenuItem("Quit");
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					System.exit(0);
				}
			});
			menu.add(item);
			trayIcon.setPopupMenu(menu);
		} catch (AWTException e) {
			throw new RuntimeException(e);
		}
		return trayIcon;
	}

	private static void setIcon(Color color, String tooltip) {
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics graphics = image.createGraphics();
		graphics.setColor(new Color(0, 0, 0, 0));
		graphics.fillRect(0, 0, 16, 16);
		graphics.setColor(color);
		graphics.fillOval(1, 1, 14, 14);
		getIcon().setImage(image);
		getIcon().setToolTip(tooltip);
		System.out.println(tooltip);
	}
}
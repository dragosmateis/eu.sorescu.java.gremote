package eu.sorescu.java.gremote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;

public class Client {
	public static void main(String[] args) throws UnknownHostException, IOException {
		Client client = new Client("localhost", 65189);
		for (String file : args) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			baos.write(Files.readAllBytes(new File(file).toPath()));
			baos.write(0);
			client.exec(new ByteArrayInputStream(baos.toByteArray()), System.out);
		}
		client.exec(System.in, System.out);
	}

	private final String host;
	private final int port;

	public Client(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public String exec(String code) throws UnknownHostException, IOException {
		code += "\0";
		return new String(exec(code.getBytes()));
	}

	public byte[] exec(byte[] data) throws UnknownHostException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		exec(new ByteArrayInputStream(data), baos);
		return baos.toByteArray();
	}

	public void exec(InputStream is, OutputStream os) throws UnknownHostException, IOException {
		try (Socket socket = new Socket(host, port)) {
			StreamPipeline p1 = new StreamPipeline(is, socket.getOutputStream());
			StreamPipeline p2 = new StreamPipeline(socket.getInputStream(), os);
			p1.start();
			p2.start();
			for (; !socket.isClosed();) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
					Thread.dumpStack();
				}
			}
		}
	}
}
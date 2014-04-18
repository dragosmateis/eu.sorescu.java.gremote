package eu.sorescu.java.gremote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StreamPipeline extends Thread {
	private final InputStream is;
	private final OutputStream os;

	public StreamPipeline(InputStream is, OutputStream os) {
		this.is = is;
		this.os = os;
		this.setDaemon(true);
	}

	@Override
	public void run() {
		try {
			for (;;) {
				int i = is.read();
				if (i < 0)
					break;
				os.write(i);
				os.flush();
			}
		} catch (IOException e) {
		}
	}
}
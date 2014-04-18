package eu.sorescu.java.gremote;

import groovy.lang.GroovyShell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

public class Agent {
	public static void main(String[] args) {
		try {
			new Agent().run();
		} catch (Throwable t) {
			t.printStackTrace();
			System.out.flush();
			System.err.flush();
			System.exit(1);
		}
		System.err.flush();
		System.out.flush();
	}

	public void run() throws IOException {
		Timer timer = new Timer();
		for (;;) {
			byte[] scriptBytes = readUntilNullOrZero(System.in);
			if (scriptBytes == null)
				return;
			try {
				timer.cancel();
			} catch (Throwable t) {
			}
			timer = new Timer();
			timer.schedule(new TimerTask() {
				@Override
				public void run() {
					System.out.println("WATCHDOG duration timed out.");
					System.exit(1);
				}
			}, 10 * 1000);
			GroovyShell shell = new GroovyShell();
			shell.evaluate(new String(scriptBytes));
			System.out.write(0);
			System.err.write(0);
			System.out.flush();
			System.err.flush();
		}
	}

	public static byte[] readUntilNullOrZero(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (;;) {
			int i = is.read();
			if ((i < 0) || (i == 0) || (i == 3) || (i == 26)) {
				byte[] result = baos.toByteArray();
				if (result.length == 0)
					if (i < 0)
						result = null;
				return result;
			}
			baos.write(i);
		}
	}
}
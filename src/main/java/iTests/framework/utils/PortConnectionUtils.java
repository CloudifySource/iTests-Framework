package iTests.framework.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class PortConnectionUtils {

	static public boolean isPortOpen(final String address, final int port) {
		Socket socket = null;
		try {
			socket = new Socket();
			socket.connect(new InetSocketAddress(address, port));
			return true;
		} catch (final Exception e) {
			return false;
		} finally {
			try {
				if (socket != null) {
					socket.close();
				}

			} catch (final IOException ioe) {
				// ignore
			}
		}
	}
}

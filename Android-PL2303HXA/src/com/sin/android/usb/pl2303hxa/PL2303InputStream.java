package com.sin.android.usb.pl2303hxa;

import java.io.IOException;
import java.io.InputStream;

public class PL2303InputStream extends InputStream {
	private PL2303Driver driver = null;

	public PL2303InputStream(PL2303Driver driver) {
		super();
		this.driver = driver;
	}

	@Override
	public int read() throws IOException {
		int data = driver.read();
		if (driver.isReadSuccess())
			return data;
		else
			throw new IOException("Read Timeout");
	}

	@Override
	public void close() throws IOException {
		driver.cleanRead();
	}
}

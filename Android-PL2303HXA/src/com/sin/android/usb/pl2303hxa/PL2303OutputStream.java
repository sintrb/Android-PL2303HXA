package com.sin.android.usb.pl2303hxa;

import java.io.IOException;
import java.io.OutputStream;

public class PL2303OutputStream extends OutputStream {
	private PL2303Driver driver = null;

	public PL2303OutputStream(PL2303Driver driver) {
		super();
		this.driver = driver;
	}

	@Override
	public void close() throws IOException {
		driver.cleanRead();
	}

	@Override
	public void write(int oneByte) throws IOException {
		driver.write((byte) oneByte);
	}
}

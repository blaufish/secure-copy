package org.securecopy.messages;

import java.util.Arrays;

public class WriteFileMessage extends Message {
	public final byte[] bytes;
	public final int byteCount;

	public WriteFileMessage(byte[] bytes, int byteCount) {
		super();
		this.bytes = Arrays.copyOf(bytes, byteCount);
		this.byteCount = byteCount;
	}

}

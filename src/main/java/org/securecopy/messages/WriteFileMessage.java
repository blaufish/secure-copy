package org.securecopy.messages;

public class WriteFileMessage extends Message {
	public final byte[] bytes;
	public final int byteCount;

	public WriteFileMessage(byte[] bytes, int byteCount) {
		super();
		this.bytes = bytes;
		this.byteCount = byteCount;
	}

}

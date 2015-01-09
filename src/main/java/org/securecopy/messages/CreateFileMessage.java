package org.securecopy.messages;

public class CreateFileMessage extends Message {

	public final String destinationFileName;
	public final long lastModified;

	public CreateFileMessage(String destinationFileName, long lastModified) {
		this.destinationFileName = destinationFileName;
		this.lastModified = lastModified;
	}

}

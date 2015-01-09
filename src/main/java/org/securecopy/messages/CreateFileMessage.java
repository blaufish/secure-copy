package org.securecopy.messages;

public class CreateFileMessage extends Message {

	public final String destinationFileName;

	public CreateFileMessage(String destinationFileName) {
		this.destinationFileName = destinationFileName;
	}

}

package org.securecopy.actors;

import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.securecopy.Util;
import org.securecopy.messages.CloseFileMessage;
import org.securecopy.messages.CreateFileMessage;
import org.securecopy.messages.Message;
import org.securecopy.messages.WriteFileMessage;

public class MessageDigestActor extends ReliableActor {
	private final MessageDigest md;
	private final PrintWriter hashPrintWriter;
	boolean sane = false;
	private String filename;

	public MessageDigestActor(PrintWriter hashPrintWriter) throws NoSuchAlgorithmException {
		this.hashPrintWriter = hashPrintWriter;
		md = MessageDigest.getInstance("SHA-256");
	}

	@Override
	public void onReceive(Message message) {
		if (message instanceof CreateFileMessage) {
			CreateFileMessage msg = (CreateFileMessage) message;
			handleCreate(msg);
		} else if (message instanceof WriteFileMessage) {
			handleWrite((WriteFileMessage) message);
		} else if (message instanceof CloseFileMessage) {
			handleClose();
		}
	}

	private void handleCreate(CreateFileMessage msg) {
		md.reset();
		filename = msg.destinationFileName;

		sane = true;
	}

	private void handleWrite(WriteFileMessage msg) {
		if (!sane)
			return;
		md.update(msg.bytes, 0, msg.byteCount);
	}

	private void handleClose() {
		if (!sane)
			return;
		byte[] digest = md.digest();
		String hex = Util.encodeHexString(digest);
		hashPrintWriter.printf("%s %s\n", hex, filename);
		hashPrintWriter.flush();
	}

}

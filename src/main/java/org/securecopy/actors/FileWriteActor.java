package org.securecopy.actors;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.securecopy.messages.CloseFileMessage;
import org.securecopy.messages.CreateFileMessage;
import org.securecopy.messages.Message;
import org.securecopy.messages.WriteFileMessage;

public class FileWriteActor extends ReliableActor {
	private boolean sane;
	private FileOutputStream fos;

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

	private void handleWrite(WriteFileMessage msg) {
		if (!sane) return ;
		
		try {
			fos.write(msg.bytes, 0, msg.byteCount);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fos = null;
		}
	}

	private void handleCreate(CreateFileMessage msg) {
		try {
			fos = new FileOutputStream(msg.destinationFileName);
			sane = true;
		} catch (FileNotFoundException e) {
			fos = null;
			e.printStackTrace();
			sane = false;
		}
	}

	private void handleClose() {
		if (fos != null) {
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				fos = null;
			}
		}
	}

}

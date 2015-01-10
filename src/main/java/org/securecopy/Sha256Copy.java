package org.securecopy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;

import org.securecopy.actors.FileWriteActor;
import org.securecopy.actors.MessageDigestActor;
import org.securecopy.actors.ReliableActorFramework;
import org.securecopy.messages.CloseFileMessage;
import org.securecopy.messages.CreateFileMessage;
import org.securecopy.messages.WriteFileMessage;

public class Sha256Copy implements AutoCloseable {
	private static final int NTFS_LARGEST_ALLOCATION_SIZE = 65536;
	private static final int BLOCKSIZE = 1000 * NTFS_LARGEST_ALLOCATION_SIZE;
	private final ReliableActorFramework actors;
	private final PrintWriter hashPrintWriter;

	private Sha256Copy(ReliableActorFramework actors,
			PrintWriter hashPrintWriter) {
		this.actors = actors;
		this.hashPrintWriter = hashPrintWriter;
	}

	protected void copyFile(File sourceFile, String destinationFileName)
			throws FileNotFoundException, IOException {
		try (FileInputStream fis = new FileInputStream(sourceFile)) {
			actors.post(new CreateFileMessage(destinationFileName, sourceFile
					.lastModified()));
			byte[] input = new byte[BLOCKSIZE];
			int readBytes;
			while ((readBytes = fis.read(input)) != -1) {
				actors.post(new WriteFileMessage(input, readBytes));
			}
			actors.post(new CloseFileMessage());
		}
	}

	public static Sha256Copy initilize(String destination)
			throws FileNotFoundException, NoSuchAlgorithmException {
		new File(destination).mkdirs();
		String hashfilename = String.format("%s%s%s-%d.txt", destination,
				File.separator, "sha256", System.currentTimeMillis());
		PrintWriter hashPrintWriter = new PrintWriter(hashfilename);
		ReliableActorFramework actors = new ReliableActorFramework(
				new FileWriteActor(), new MessageDigestActor(hashPrintWriter));
		actors.start();
		Sha256Copy object = new Sha256Copy(actors, hashPrintWriter);
		return object;
	}

	@Override
	public void close() throws Exception {
		actors.stop();
		if (hashPrintWriter != null)
			hashPrintWriter.close();
	}

}

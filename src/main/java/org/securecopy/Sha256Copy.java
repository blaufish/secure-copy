package org.securecopy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.securecopy.actors.FileWriteActor;
import org.securecopy.actors.MessageDigestActor;
import org.securecopy.actors.ReliableActorFramework;
import org.securecopy.messages.CloseFileMessage;
import org.securecopy.messages.CreateFileMessage;
import org.securecopy.messages.WriteFileMessage;

public class Sha256Copy implements AutoCloseable {
	private static final int NTFS_LARGEST_ALLOCATION_SIZE = 65536;
	private static final int BLOCKSIZE = 1000 * NTFS_LARGEST_ALLOCATION_SIZE;
	private static ReliableActorFramework actors;
	private boolean multitheading;
	private PrintWriter hashPrintWriter;


	protected void copyFile(File sourceFile, String destinationFileName) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
		if (multitheading) {
			copyFileMultiThreaded(sourceFile, destinationFileName);
		}
		else {
			copyFileSingleThreaded(sourceFile, destinationFileName);
		}		
	}

	private void copyFileMultiThreaded(File sourceFile,
			String destinationFileName) throws FileNotFoundException,
			IOException {
		try (FileInputStream fis = new FileInputStream(sourceFile)) {
			actors.post(new CreateFileMessage(destinationFileName));
			byte[] input = new byte[BLOCKSIZE];
			int readBytes;
			while ((readBytes = fis.read(input)) != -1) {
				actors.post(new WriteFileMessage(input, readBytes));
			}
			actors.post(new CloseFileMessage());
		}
	}

	private void copyFileSingleThreaded(File sourceFile, String destinationFileName)
			throws FileNotFoundException, IOException, NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		try (FileInputStream fis = new FileInputStream(sourceFile)) {

			try (FileOutputStream fos = new FileOutputStream(
					destinationFileName)) {
				byte[] input = new byte[BLOCKSIZE];
				int readBytes;
				while ((readBytes = fis.read(input)) != -1) {
					md.update(input, 0, readBytes);
					fos.write(input, 0, readBytes);
				}
				byte[] digest = md.digest();
				String hex = Hex.encodeHexString(digest);
				hashPrintWriter.printf("%s %s\n", hex, destinationFileName);
			}
		}
	}

	public static Sha256Copy initilize(String destination, boolean multithreading)
			throws FileNotFoundException, NoSuchAlgorithmException {
		new File(destination).mkdirs();
		String hashfilename = String.format("%s%s%s-%d.txt", destination,
				File.separator, "sha256", System.currentTimeMillis());
		Sha256Copy object = new Sha256Copy();
		object.hashPrintWriter = new PrintWriter(hashfilename);
		object.multitheading = multithreading;
		if (multithreading) {
			actors = new ReliableActorFramework(new FileWriteActor(), new MessageDigestActor(object.hashPrintWriter));
			actors.start();
		}
		return object;
	}

	@Override
	public void close() throws Exception {
		actors.stop();
		if (hashPrintWriter != null)
			hashPrintWriter.close();
	}

}

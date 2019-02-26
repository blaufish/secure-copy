package org.securecopy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.securecopy.actors.FileWriteActor;
import org.securecopy.actors.MessageDigestActor;
import org.securecopy.actors.ReliableActorFramework;
import org.securecopy.messages.CloseFileMessage;
import org.securecopy.messages.CreateFileMessage;
import org.securecopy.messages.WriteFileMessage;

public class Sha256Copy implements AutoCloseable {
	private final ReliableActorFramework actors;
	private final PrintWriter hashPrintWriter;
	private final int blocksize;

	private Sha256Copy(ReliableActorFramework actors, PrintWriter hashPrintWriter, int blocksize) {
		this.actors = actors;
		this.hashPrintWriter = hashPrintWriter;
		this.blocksize = blocksize;
	}

	protected void copyFile(File sourceFile, String destinationFileName) throws FileNotFoundException, IOException {
		try (FileInputStream fis = new FileInputStream(sourceFile)) {
			actors.post(new CreateFileMessage(destinationFileName, sourceFile.lastModified()));
			byte[] input = new byte[blocksize];
			int readBytes;
			while ((readBytes = fis.read(input)) != -1) {
				if (readBytes < input.length / 4) {
					// Drop unused memory early
					input = Arrays.copyOf(input, readBytes);
				}
				actors.post(new WriteFileMessage(input, readBytes));
				// Get a new fresh block of memory
				input = new byte[blocksize];
			}
			actors.post(new CloseFileMessage());
		}
	}

	public static Sha256Copy initilize(String destination, int blocksize)
			throws FileNotFoundException, NoSuchAlgorithmException {
		new File(destination).mkdirs();
		String hashfilename = String.format("%s%s%s-%d.txt", destination, File.separator, "sha256",
				System.currentTimeMillis());
		PrintWriter hashPrintWriter = new PrintWriter(hashfilename);
		final long queueDepth = Runtime.getRuntime().freeMemory() / 2 / blocksize;
		System.out.format("Queue depth: up to [%d] of [%d] blocks (%s)\n", queueDepth, blocksize,
				FileUtils.byteCountToDisplaySize(queueDepth * blocksize));
		ReliableActorFramework actors = new ReliableActorFramework(queueDepth, new FileWriteActor(),
				new MessageDigestActor(hashPrintWriter));
		actors.start();
		Sha256Copy object = new Sha256Copy(actors, hashPrintWriter, blocksize);
		return object;
	}

	@Override
	public void close() throws Exception {
		actors.stop();
		if (hashPrintWriter != null)
			hashPrintWriter.close();
	}

}

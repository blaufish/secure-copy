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

public class Sha256Copy implements AutoCloseable {
	private PrintWriter hashPrintWriter;

	protected Sha256Copy(PrintWriter hashPrintWriter) {
		this.hashPrintWriter = hashPrintWriter;
	}

	protected void copyFile(File sourceFile, String destinationFileName)
			throws FileNotFoundException, IOException, NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		try (FileInputStream fis = new FileInputStream(sourceFile)) {

			try (FileOutputStream fos = new FileOutputStream(
					destinationFileName)) {
				byte[] input = new byte[64_000_000];
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

	public static Sha256Copy initilize(String destination)
			throws FileNotFoundException {
		new File(destination).mkdirs();
		String hashfilename = String.format("%s%s%s-%d.txt", destination,
				File.separator, "sha256", System.currentTimeMillis());
		PrintWriter hashPrintWriter = new PrintWriter(hashfilename);
		Sha256Copy object = new Sha256Copy(hashPrintWriter);
		return object;
	}

	@Override
	public void close() throws Exception {
		if (hashPrintWriter != null)
			hashPrintWriter.close();
	}

}

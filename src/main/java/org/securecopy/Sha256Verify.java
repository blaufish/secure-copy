package org.securecopy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

class Sha256Verify {
	static long filesToVerify = 0;
	static long filesVerified = 0;
	static long filesBad = 0;
	static long filesError = 0;

	public void verify(String destination) throws IOException {
		File[] files = new File(destination).listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.matches("sha256-.*\\.txt");
			}
		});
		if (files == null) {
			System.out.printf("\nError, no files found: %s\n", destination);
			return;
		}
		verify(files);
	}

	static void verify(File[] sha256files) throws IOException {
		Map<String, String> fileSha256Map = new TreeMap<>();
		for (File file : sha256files) {
			List<String> lines = Util.readLines(file);
			for (String line : lines) {
				final String checksum = line.substring(0, 64);
				final String filename = line.substring(65);
				fileSha256Map.put(filename, checksum);
			}
		}
		filesToVerify = fileSha256Map.size();
		for (Entry<String, String> entry : fileSha256Map.entrySet()) {
			final String filename = entry.getKey();
			final String hash = entry.getValue();
			try {
				String newhash = sha256sum(filename);
				if (hash.equals(newhash)) {
					filesVerified++;
				} else {
					filesBad++;
					output = "";
					System.out.printf("\nHash verification failed: %s\n", filename);
				}
			} catch (NoSuchAlgorithmException | IOException e) {
				filesError++;
				output = "";
				System.out.printf("\nIO ERROR: %s %s\n", filename, e.getMessage());
			}
			statistics();
		}

	}

	static String output = "";
	static long lastStatistics = 0;

	private static void statistics() {
		if (System.currentTimeMillis() - lastStatistics < 5000)
			return;
		for (int i = 0; i < output.length(); i++)
			System.out.print("\b");
		double progress = (filesVerified + filesBad + filesError) * 100.0 / filesToVerify;
		output = String.format("Verifying: %1.1f%%, verified successfully: %d bad: %d error: %d  ", progress,
				filesVerified, filesBad, filesError);
		System.out.print(output);
	}

	private static String sha256sum(String filename)
			throws NoSuchAlgorithmException, IOException, FileNotFoundException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		try (FileInputStream fis = new FileInputStream(filename)) {
			byte[] input = new byte[65535];
			int readBytes;
			while ((readBytes = fis.read(input)) != -1) {
				md.update(input, 0, readBytes);
			}
		}
		byte[] digest = md.digest();
		String hex = Util.encodeHexString(digest);
		return hex;
	}

}

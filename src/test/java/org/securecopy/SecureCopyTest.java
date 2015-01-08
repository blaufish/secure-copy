package org.securecopy;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SecureCopyTest {

	private static final String TEMP_DIR = ".tmp";
	private static final String SOURCE_DIR = TEMP_DIR + File.separator + "source";
	private static final String DESTINATION_DIR = TEMP_DIR + File.separator + "destination";

	@Test
	public void testMain() throws Exception {
		SecureCopy.main(SOURCE_DIR, DESTINATION_DIR);

		File[] files = locateSha256sum();
		assertEquals(1, files.length);
		
		List<String> checksumLines = FileUtils.readLines(files[0]);
		assertEquals(150, checksumLines.size());
		
		for (String line : checksumLines) {
			final String checksum = line.substring(0, 64);
			final String filename = line.substring(65);
			String hex = sha256sum(filename);
			System.out.printf("%s %s: %s\n", checksum, filename,
					checksum.equals(hex) ? "OK" : "FAILED!!!");
			assertEquals(checksum, hex);
		}	
	}

	private String sha256sum(String filename) throws NoSuchAlgorithmException, IOException,
			FileNotFoundException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		try (FileInputStream fis = new FileInputStream(filename)) {
			byte[] input = new byte[4_000_000];
			int readBytes;
			while ((readBytes = fis.read(input)) != -1) {
				md.update(input, 0, readBytes);
			}
		}
		byte[] digest = md.digest();
		String hex = Hex.encodeHexString(digest);
		return hex;
	}

	private File[] locateSha256sum() {
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.matches("sha256-.*");
			}
		};
		File[] files = new File(DESTINATION_DIR).listFiles(filter);
		return files;
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		File tempdir = new File(TEMP_DIR);
		FileUtils.deleteDirectory(tempdir);
		tempdir.mkdir();
		createSourceDir();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		File tempdir = new File(TEMP_DIR);
		FileUtils.deleteDirectory(tempdir);
	}

	private static void createSourceDir() throws IOException {
		Random rnd = new Random(1337);
		createSourceDir(rnd, SOURCE_DIR, 3);
	}

	private static void createSourceDir(Random rnd, String dir, int depth)
			throws IOException {
		new File(dir).mkdir();
		for (int i = 0; i < 10; i++) {
			int size = (int) (1_000_000 * rnd.nextDouble());
			byte[] data = new byte[size];
			rnd.nextBytes(data);
			String filename = dir + "/a" + rnd.nextLong();
			File file = new File(filename);
			FileUtils.writeByteArrayToFile(file, data);
			System.out.println("created: " + filename);
		}
		if (depth <= 0)
			return;
		depth--;
		for (int i = 0; i < 2; i++) {
			String filename = dir + "/b" + rnd.nextLong();
			createSourceDir(rnd, filename, depth);
		}

	}
}

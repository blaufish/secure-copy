package org.securecopy;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SecureCopyTest {

	private static final String TEMP_DIR = ".tmp";
	private static final String SOURCE_DIR = TEMP_DIR + File.separator + "source";
	private static final String DESTINATION_DIR = TEMP_DIR + File.separator + "destination";

	@Test
	public void testMain() throws Exception {
		SecureCopy.main("--copy", SOURCE_DIR, DESTINATION_DIR);

		File[] files = locateSha256sum();
		assertEquals(1, files.length);

		List<String> checksumLines = Util.readLines(files[0]);
		assertEquals(21, checksumLines.size());

		for (String line : checksumLines) {
			final String checksum = line.substring(0, 64);
			final String filename = line.substring(65);
			String hex = sha256sum(filename);
			System.out.printf("%s %s: %s\n", checksum, filename, checksum.equals(hex) ? "OK" : "FAILED!!!");
			assertEquals(checksum, hex);
			assertTrue(checksum, validHashes.contains(checksum));
		}

		SecureCopy.main("--verify", DESTINATION_DIR);
	}

	Set<String> validHashes = new HashSet<>();
	{
		validHashes.add("79af71ea2e71265c3507d0680ef2588354a8ac9f51e72c8d44858c893def6ffd");
		validHashes.add("e8a81fc8bcde87530524afdcec379c26c8d0c175609aaf66ad35b7f451300b08");
		validHashes.add("ee84ba0ae0092e0531a9c6bcec73d22a867a9d7a29567bfa1fe70006d3c31d68");
		validHashes.add("c11002d4dc9de726314ebdae00abd4db5fb8de92a7811b79f78f30bd16ac42b4");
		validHashes.add("5dc32532fef585c0db0db772f03c724f30f6d17afe57e63342e7a79382c891d0");
		validHashes.add("2a5d97b6aff9765f1ba708d70483ac550817d20677481d62b0b4681321b9bf0f");
		validHashes.add("026dd09e3cba3a989d1486ef9aa7c49abe6896b1c1b687ed6a80a604b7cbb969");
		validHashes.add("e9dc87865a977cbf734a79bd4a45b2e20c784dafe34be6add5a1eb37c01ba61e");
		validHashes.add("c9afe68de848dcd6656a7748f312d1c89ee7abec8a832f6ee809b9b380e81676");
		validHashes.add("403f098a7a64cbfeeaf8ffab658036f7230c2938f0bc3b098821c03690f79837");
		validHashes.add("45ade1deb50e2c370d03b95fea2204b9a965c236dd0864580ac88661d236bbf5");
		validHashes.add("b549825ce94df8b14002f30f165624f850174492457f954ba0904e1bf9de335f");
		validHashes.add("19ed4839cab9adfcaa5a0aea620f5ea173b17f6cb6034af00a5463c9a061c216");
		validHashes.add("b7c9f457aa88560830d4cd7da632fd34513beda6383c2958e5ca223baaf9630a");
		validHashes.add("3c0f01c3ecd35e937ed6c68173ba338c991931a3e826bc3d47244ef564ebc3a1");
		validHashes.add("a5d3773e0f16d590b876587bbcc81d50e2a55bdc277306469e84fa934b79f429");
		validHashes.add("cd9142b13efed0efac35d786429f38ba490a10becc48022f9135a562da2ae7a8");
		validHashes.add("4c7a007211531b601ac203594e37ada5f9f1e23c013eaa65ecc7c0f741c3e95a");
		validHashes.add("27ba28ef65704afeb14cb03da597cf407d81a72babd6c0fce849ac096519ea25");
		validHashes.add("d011e7383c8cd523521e61e0ad6ecc3de508935ce36bcb4de210aa1c75554c81");
		validHashes.add("092a4b38b7a0f14247fcb2c39248ad85f2ce73a88db4fdc7d7457dd37e9e3ae5");

	}

	private String sha256sum(String filename) throws NoSuchAlgorithmException, IOException, FileNotFoundException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		try (FileInputStream fis = new FileInputStream(filename)) {
			byte[] input = new byte[4_000_000];
			int readBytes;
			while ((readBytes = fis.read(input)) != -1) {
				md.update(input, 0, readBytes);
			}
		}
		byte[] digest = md.digest();
		String hex = Util.encodeHexString(digest);
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
		Util.deleteDirectory(tempdir);
		tempdir.mkdir();
		createSourceDir();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		File tempdir = new File(TEMP_DIR);
		Util.deleteDirectory(tempdir);
	}

	private static void createSourceDir() throws IOException {
		Random rnd = new Random(1337);
		createSourceDir(rnd, SOURCE_DIR, 2);
	}

	private static void createSourceDir(Random rnd, String dir, int depth) throws IOException {
		new File(dir).mkdir();
		for (int i = 0; i < 3; i++) {
			int size = (int) (1_000_000 * rnd.nextDouble());
			byte[] data = new byte[size];
			rnd.nextBytes(data);
			String filename = dir + "/a" + rnd.nextLong();
			File file = new File(filename);
			Util.writeByteArrayToFile(file, data);
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

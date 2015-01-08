package org.securecopy;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Random;

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
		fail("Not yet implemented (todo: explicitly call sha256 sum to verify)");
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

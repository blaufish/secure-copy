package org.securecopy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;

public class SecureCopy {

	public static void main(String... args) throws Exception {
		if (args.length != 2) {
			System.out.println("SecureCopy <source> <destination>");
			return;
		}

		final String source = args[0];
		String destination = args[1];

		ListFileUtility lister = new ListFileUtility();
		Collection<File> files = lister.listFiles(source);
		System.out.format("Entries: %d Dirs: %d Files: %d Size: %s\n",
				files.size(), lister.dirCount, lister.fileCount,
				FileUtils.byteCountToDisplaySize(lister.sizeCount));

		new File(destination).mkdirs();
		String hashfilename = String.format("%s%s%s-%d.txt", destination, File.separator, "sha256", System.currentTimeMillis());
		try (PrintWriter hashPrintWriter = new PrintWriter(hashfilename)) {
			CopyUtility cu = new CopyUtility();
			cu.copyFiles(source, destination, hashPrintWriter);			
		}
		

	}
	
	private static void copyFile(File sourceFile, String destinationFileName, PrintWriter hashprinter)
			throws FileNotFoundException, IOException, NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		try (FileInputStream fis = new FileInputStream(sourceFile)) {

			try (FileOutputStream fos = new FileOutputStream(
					destinationFileName)) {
				byte[] input = new byte[4_000_000];
				int readBytes;
				while ((readBytes = fis.read(input)) != -1) {
					md.update(input, 0, readBytes);
					fos.write(input, 0, readBytes);
				}
				byte[] digest = md.digest();
				String hex = Hex.encodeHexString(digest);
				hashprinter.printf("%s %s\n", hex, destinationFileName);
			}
		}
	}

	static class CopyUtility extends DirectoryWalker<File> {
		String destination;
		PrintWriter hashPrintWriter;
		List<File> currentDirectory = new ArrayList<File>();
		String currentDestinationPath = null;
		int fileCount = 0;
		int dirCount = 0;
		long sizeCount = 0;

		public Collection<File> copyFiles(String source, String destination, PrintWriter hashPrintWriter)
				throws Exception {
			this.destination = destination;
			this.hashPrintWriter = hashPrintWriter;
			
			File sourceDirectory = new File(source);

			Collection<File> files = new ArrayList<File>();
			walk(sourceDirectory, files);
			return files;
		}

		@Override
		protected boolean handleDirectory(File directory, int depth,
				Collection<File> results) {
			if (depth == 0) {
				currentDestinationPath = destination;
			} else {
				dirCount++;
				currentDirectory.add(directory);
				currentDestinationPath = calculateDestinationPath();
			}
//			System.out.format("Create dir: %s\n", currentDestinationPath);
			new File(currentDestinationPath).mkdirs();
			return true;
		}

		@Override
		protected void handleDirectoryEnd(File directory, int depth,
				Collection<File> results) {
			currentDirectory.remove(directory);
			currentDestinationPath = calculateDestinationPath();
		}

		@Override
		protected void handleFile(File file, int depth, Collection<File> results) {
			fileCount++;
			sizeCount += file.length();
			try {
				String destinationFileName = currentDestinationPath
						+ File.separator + file.getName();
				copyFile(file, destinationFileName, hashPrintWriter);
			} catch (IOException | NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
			
		}

		private String calculateDestinationPath() {
			String dir = destination;
			for (File d : currentDirectory) {
				dir += File.separator + d.getName();
			}
			return dir;
		}

	}

}

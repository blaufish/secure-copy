package org.securecopy;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;;

public class SecureCopy {

	public static void main(String... args) throws Exception {
		if (args.length == 3 && args[0].equals("--copy")) {
			final String source = args[1];
			String destination = args[2];
			copy(source, destination);
		} else if (args.length == 2 && args[0].equals("--verify")) {
			final String destination = args[1];
			Sha256Verify verify = new Sha256Verify();
			verify.verify(destination);
		} else {
			System.out.println("SecureCopy --copy <source> <destination>");
			System.out.println("SecureCopy --verify <destination>");
		}
		return;

	}

	private static void copy(final String source, String destination)
			throws IOException, FileNotFoundException, Exception, NoSuchAlgorithmException {
		System.out.format("Index %s...\n", source);
		ListFileUtility lister = new ListFileUtility();
		Collection<File> files = lister.listFiles(source);
		System.out.format("Entries: %d Dirs: %d Files: %d Size: %s\n", files.size(), lister.dirCount, lister.fileCount,
				Util.byteCountToDisplaySize(lister.sizeCount));

		int blocksize = 262144;

		try (Sha256Copy copier = Sha256Copy.initilize(destination, blocksize)) {
			long t0, t1;
			t0 = System.nanoTime();
			CopyUtility cu = new CopyUtility(destination, lister.sizeCount, copier);
			Path sourceDirectory = Paths.get(source);
			Files.walkFileTree(sourceDirectory, cu);
			t1 = System.nanoTime();
			System.out.println(" done! " + Util.nanoExecTimeToText(t0, t1));
		}
	}

	private static class CopyUtility extends SimpleFileVisitor<Path> {
		final String destination;
		final long started;
		final Sha256Copy sha256copy;
		List<File> currentDirectory = new ArrayList<File>();
		String currentDestinationPath = null;
		long bytesToCopy;
		long bytesCopied = 0;
		long depth = 0;

		public CopyUtility(String destination, long bytesToCopy, Sha256Copy sha256copy) throws FileNotFoundException {
			super();
			this.destination = destination;
			this.bytesToCopy = bytesToCopy;
			this.sha256copy = sha256copy;
			this.started = System.currentTimeMillis();
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			statistics();
			if (depth == 0) {
				currentDestinationPath = destination;
			} else {
				currentDirectory.add(dir.toFile());
				currentDestinationPath = calculateDestinationPath();
			}
			depth++;
			new File(currentDestinationPath).mkdirs();
			return CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
			if (e != null)
				throw e;
			statistics();
			currentDirectory.remove(dir.toFile());
			currentDestinationPath = calculateDestinationPath();
			depth--;
			return CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
			String destinationFileName = currentDestinationPath + File.separator + file.getFileName();

			File dstfile = new File(destinationFileName);

			if (dstfile.exists() && dstfile.length() == attr.size()) {
				bytesToCopy -= attr.size();
				statistics();
				return CONTINUE;
			}

			try {
				sha256copy.copyFile(file.toFile(), destinationFileName);
				bytesCopied += attr.size();
			} catch (IOException e) {
				System.out.printf("\nFile not found: %s\n", e.getMessage());
				bytesToCopy -= attr.size();
				return TERMINATE;
			}
			statistics();
			return CONTINUE;
		}

		String statisticsLine = "";
		long lastStatistics = 0;

		private void statistics() {
			long now = System.currentTimeMillis();
			if (lastStatistics == 0) {
				lastStatistics = now;
				return;
			}
			if ((now - lastStatistics) < 5_000)
				return;
			final long secondsElapsed = (now - started) / 1000;
			if (secondsElapsed <= 0)
				return;
			lastStatistics = now;
			for (int i = 0; i < statisticsLine.length(); i++)
				System.out.print("\b");
			final double percentDone = (bytesCopied * 100.0) / bytesToCopy;
			final long secondsLeft = (long) ((secondsElapsed / percentDone) * (100.0 - percentDone));
			String timeLeft = Util.formatTime(secondsLeft);
			statisticsLine = String.format("%s of %s, %1.1f%% (%s/s)... Estimated time left: %s      ",
					Util.byteCountToDisplaySize(bytesCopied), Util.byteCountToDisplaySize(bytesToCopy), percentDone,
					Util.byteCountToDisplaySize(bytesCopied / secondsElapsed), timeLeft);
			System.out.print(statisticsLine);
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

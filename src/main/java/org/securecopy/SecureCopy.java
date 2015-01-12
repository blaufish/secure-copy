package org.securecopy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;

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
		}
		else {
			System.out.println("SecureCopy --copy <source> <destination>");
			System.out.println("SecureCopy --verify <destination>");
		}
		return;

	}

	private static void copy(final String source, String destination)
			throws IOException, FileNotFoundException, Exception,
			NoSuchAlgorithmException {
		System.out.format("Index %s...\n", source);
		ListFileUtility lister = new ListFileUtility();
		Collection<File> files = lister.listFiles(source);
		System.out.format("Entries: %d Dirs: %d Files: %d Size: %s\n",
				files.size(), lister.dirCount, lister.fileCount,
				FileUtils.byteCountToDisplaySize(lister.sizeCount));

		int blocksize = benchmark(lister.largestFile);

		try (Sha256Copy copier = Sha256Copy.initilize(destination, blocksize)) {
			CopyUtility cu = new CopyUtility(destination, lister.sizeCount,
					copier);
			cu.copyFiles(source, destination);
		}
	}

	private static int benchmark(File largestFile) throws IOException {
		System.out.format("Benchmarking on %s:\n",
				largestFile.getAbsolutePath());
		TreeMap<Long, Integer> timeToBlockSize = new TreeMap<Long, Integer>();
		final int blockbasesize = 1024;
		try (FileInputStream fis = new FileInputStream(largestFile)) {
			// warm up so startup issues doesn't affect benchmarking
			benchmarkRead1GB(fis, blockbasesize);
		}
		for (int i = 1; i < 10000; i *= 4) {
			try (FileInputStream fis = new FileInputStream(largestFile)) {
				int blocksize = blockbasesize * i;
				final long time = benchmarkRead1GB(fis, blocksize);
				System.out.printf("   %8d %.1fs\n", blocksize, time / 1000.0);
				timeToBlockSize.put(time, blocksize);
			}
		}
		Integer optimalBlocksize = timeToBlockSize.firstEntry().getValue();
		System.out.format("Selected blocksize: %d\n", optimalBlocksize);
		return optimalBlocksize;
	}

	private static long benchmarkRead1GB(FileInputStream fis, int blocksize)
			throws IOException {
		long started;
		long stopped;
		byte[] input = new byte[blocksize];
		int readBytes;
		long totalReadBytes = 0;
		started = System.currentTimeMillis();
		while (((readBytes = fis.read(input)) != -1)
				&& totalReadBytes < 1_000_000_000) {
			totalReadBytes += readBytes;
		}
		stopped = System.currentTimeMillis();
		final long time = stopped - started;
		return time;
	}

	private static class CopyUtility extends DirectoryWalker<File> {
		final String destination;
		final long started;
		final Sha256Copy sha256copy;
		List<File> currentDirectory = new ArrayList<File>();
		String currentDestinationPath = null;
		long bytesToCopy;
		long bytesCopied = 0;

		public CopyUtility(String destination, long bytesToCopy,
				Sha256Copy sha256copy) throws FileNotFoundException {
			super();
			this.destination = destination;
			this.bytesToCopy = bytesToCopy;
			this.sha256copy = sha256copy;
			this.started = System.currentTimeMillis();
		}

		public Collection<File> copyFiles(String source, String destination)
				throws Exception {
			File sourceDirectory = new File(source);

			Collection<File> files = new ArrayList<File>();
			walk(sourceDirectory, files);
			System.out.println(" done!");
			return files;
		}

		@Override
		protected boolean handleDirectory(File directory, int depth,
				Collection<File> results) {
			if (depth == 0) {
				currentDestinationPath = destination;
			} else {
				currentDirectory.add(directory);
				currentDestinationPath = calculateDestinationPath();
			}
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
			String destinationFileName = currentDestinationPath
					+ File.separator + file.getName();

			File dstfile = new File(destinationFileName);

			if (dstfile.exists() && dstfile.length() == file.length()) {
				bytesToCopy -= file.length();
				return;
			}

			try {
				sha256copy.copyFile(file, destinationFileName);
				bytesCopied += file.length();
			} catch (IOException e) {
				System.out.printf("\nFile not found: %s\n", e.getMessage());
				bytesToCopy -= file.length();
			}
			statistics();
		}

		String statisticsLine = "";
		long lastStatistics = 0;

		private void statistics() {
			long now = System.currentTimeMillis();
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
			String timeLeft = formatTime(secondsLeft);
			statisticsLine = String
					.format("%s of %s, %1.1f%% (%s/s)... Estimated time left: %s      ",
							FileUtils.byteCountToDisplaySize(bytesCopied),
							FileUtils.byteCountToDisplaySize(bytesToCopy),
							percentDone,
							FileUtils.byteCountToDisplaySize(bytesCopied
									/ secondsElapsed), timeLeft);
			System.out.print(statisticsLine);
		}

		private String formatTime(long seconds) {
			if (seconds > 3600) {
				return String.format("%1.1f hours", seconds / 3600.0);
			} else if (seconds <= 0) {
				return "N/A";
			} else {
				return String.format("%dm %ds", seconds / 60, seconds % 60);
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

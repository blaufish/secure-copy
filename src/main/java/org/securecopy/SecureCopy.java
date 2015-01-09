package org.securecopy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

		try (Sha256Copy copier = Sha256Copy.initilize(destination)) {
			CopyUtility cu = new CopyUtility(destination, lister.sizeCount, copier);
			cu.copyFiles(source, destination);
		}

	}

	private static class CopyUtility extends DirectoryWalker<File> {
		final String destination;
		final long bytesToCopy;
		final long started;
		final Sha256Copy sha256copy;
		List<File> currentDirectory = new ArrayList<File>();
		String currentDestinationPath = null;
		long sizeCount = 0;

		public CopyUtility(String destination, long bytesToCopy, Sha256Copy sha256copy) throws FileNotFoundException {
			super();
			this.destination = destination;
			this.bytesToCopy = bytesToCopy;
			this.sha256copy = sha256copy;
			this.started = System.currentTimeMillis();
		}

		public Collection<File> copyFiles(String source, String destination) throws Exception {
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
			// System.out.format("Create dir: %s\n", currentDestinationPath);
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
			sizeCount += file.length();
			try {
				String destinationFileName = currentDestinationPath
						+ File.separator + file.getName();
				sha256copy.copyFile(file, destinationFileName);
			} catch (IOException | NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
			statistics();
		}

		String statisticsLine = "";
		long lastStatistics = 0;

		private void statistics() {
			long now = System.currentTimeMillis();
			if ((now - lastStatistics) < 10_000)
				return;
			final long secondsElapsed = (now - started) / 1000;
			if (secondsElapsed <= 0)
				return;
			lastStatistics = now;
			for (int i = 0; i < statisticsLine.length(); i++)
				System.out.print("\b");
			final long percentDone = (sizeCount * 100) / bytesToCopy;
			statisticsLine = String
					.format("%s of %s, %s%% (%s/s)...  ",
							FileUtils.byteCountToDisplaySize(sizeCount),
							FileUtils.byteCountToDisplaySize(bytesToCopy),
							percentDone,
							FileUtils.byteCountToDisplaySize(sizeCount / secondsElapsed));
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

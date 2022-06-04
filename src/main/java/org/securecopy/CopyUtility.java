package org.securecopy;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.securecopy.actors.FileWriteActor;
import org.securecopy.actors.MessageDigestActor;
import org.securecopy.actors.ReliableActorFramework;
import org.securecopy.messages.CloseFileMessage;
import org.securecopy.messages.CreateFileMessage;
import org.securecopy.messages.WriteFileMessage;

class CopyUtility extends SimpleFileVisitor<Path> {
	final String destination;
	static long started = 0;
	final Sha256Copy sha256copy;
	List<File> currentDirectory = new ArrayList<File>();
	String currentDestinationPath = null;
	static long bytesToCopy;
	static long bytesCopied = 0;
	long depth = 0;

	static void copy(final String source, String destination)
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
			bytesToCopy = lister.sizeCount;
			CopyUtility cu = new CopyUtility(destination,  copier);
			Path sourceDirectory = Paths.get(source);
			Files.walkFileTree(sourceDirectory, cu);
			t1 = System.nanoTime();
			System.out.println(" done! " + Util.nanoExecTimeToText(t0, t1));
		}
	}

	CopyUtility(String destination, Sha256Copy sha256copy) throws FileNotFoundException {
		super();
		this.destination = destination;
		this.sha256copy = sha256copy;
		started = System.currentTimeMillis();
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
		} catch (IOException e) {
			System.out.printf("\nIO Error: %s\n", e.getMessage());
			bytesToCopy -= attr.size();
			return TERMINATE;
		}
		statistics();
		return CONTINUE;
	}

	static String statisticsLine = "";
	static long lastStatistics = 0;

	private static void statistics() {
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

	static class Sha256Copy implements AutoCloseable {
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
					actors.post(new WriteFileMessage(input, readBytes));
					// Get a new fresh block of memory
					input = new byte[blocksize];
					bytesCopied += readBytes;
					statistics();
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
					Util.byteCountToDisplaySize(queueDepth * blocksize));
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

}

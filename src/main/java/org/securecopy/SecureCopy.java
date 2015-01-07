package org.securecopy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.DirectoryWalker;
import org.apache.commons.io.FileUtils;

public class SecureCopy {

	public static void main(String... args) throws IOException {
		if (args.length != 2) {
			System.out.println("SecureCopy <source> <destination>");
			return;
		}

		ListFileUtility lister = new ListFileUtility();
		Collection<File> files = lister.listFiles(args[0]);
		System.out.format("Entries: %d Dirs: %d Files: %d Size: %s\n",
				files.size(), lister.dirCount, lister.fileCount,
				FileUtils.byteCountToDisplaySize(lister.sizeCount));

		for (File f : files)
			System.out.println(f.getAbsolutePath());
		
		CopyUtility cu = new CopyUtility();
		cu.copyFiles(args[0], args[1]);

	}

	static class CopyUtility extends DirectoryWalker<File> {
		String destination;
		List<File> currentDirectory = new ArrayList<File>();
		int fileCount = 0;
		int dirCount = 0;
		long sizeCount = 0;

		public Collection<File> copyFiles(String source, String destination)
				throws IOException {
			File sourceDirectory = new File(source);
			this.destination = destination;

			Collection<File> files = new ArrayList<File>();
			walk(sourceDirectory, files);
			return files;
		}

		@Override
		protected boolean handleDirectory(File directory, int depth,
				Collection<File> results) {
			dirCount++;
			if (depth > 0) {
				currentDirectory.add(directory);
				String dir = calculateDestinationPath();
				System.out.format("Create dir: %s\n", dir);
				new File(dir).mkdir();
			}
			return true;
		}

		@Override
		protected void handleDirectoryEnd(File directory, int depth,
				Collection<File> results) {
			currentDirectory.remove(directory);
		}

		@Override
		protected void handleFile(File file, int depth, Collection<File> results) {
			fileCount++;
			sizeCount += file.length();
		}

		private String calculateDestinationPath() {
			String dir = destination;
			for(File d : currentDirectory) {
				dir += "/" + d.getName();
			}
			return dir;
		}

	}

}

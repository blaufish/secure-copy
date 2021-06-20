package org.securecopy;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;

import static java.nio.file.FileVisitResult.*;

class ListFileUtility {
	int fileCount = 0;
	int dirCount = 0;
	long sizeCount = 0;
	long largestFileSize = 0;
	File largestFile;
	public Collection<File> listFiles(String directory) throws IOException {
		FileWalkerVisitor visitor = new FileWalkerVisitor();
		Files.walkFileTree(Paths.get(directory), visitor);
		return visitor.files;
	}

	private class FileWalkerVisitor extends SimpleFileVisitor<Path> {
		Collection<File> files = new ArrayList<File>();

		@Override
	    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
			fileCount++;
			final long size = attr.size();
			sizeCount += size;
			if (largestFileSize < size) {
				largestFileSize = size;
				largestFile = file.toFile();
			}
			return CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			dirCount++;
			files.add(dir.toFile());
			return CONTINUE;
		}
	}
}

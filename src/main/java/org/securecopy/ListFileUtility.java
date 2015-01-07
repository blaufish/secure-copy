package org.securecopy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.DirectoryWalker;

class ListFileUtility extends DirectoryWalker<File> {
		int fileCount = 0;
		int dirCount = 0;
		long sizeCount = 0;
		
		public Collection<File> listFiles(String directory) throws IOException {
			File dir = new File(directory);
			Collection<File> files = new ArrayList<File>();
			walk(dir, files);
			return files;			
		}
		
		@Override
		protected boolean handleDirectory(File directory, int depth,
				Collection<File> results) {
			dirCount++;
			results.add(directory);
			return true;
		}

		@Override
		protected void handleFile(File file, int depth, Collection<File> results) {
			fileCount++;
			sizeCount += file.length();
		}

}

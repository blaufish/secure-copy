package org.securecopy;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.TreeMap;

public class BenchmarkRead {
	public static void main(String... args) throws Exception {
		benchmark(args[1]);

	}

	private static void benchmark(String fn) throws IOException {
		System.out.format("Benchmarking on %s:\n", fn);
		TreeMap<Long, Integer> timeToBlockSize = new TreeMap<Long, Integer>();
		final int blockbasesize = 1024;
		try (FileInputStream fis = new FileInputStream(fn)) {
			// warm up so startup issues doesn't affect benchmarking
			benchmarkRead1GB(fis, blockbasesize);
		}
		for (int i = 1; i < 10000; i *= 4) {
			try (FileInputStream fis = new FileInputStream(fn)) {
				int blocksize = blockbasesize * i;
				final long time = benchmarkRead1GB(fis, blocksize);
				System.out.printf("   %8d %.1fs\n", blocksize, time / 1000.0);
				timeToBlockSize.put(time, blocksize);
			}
		}
		Integer optimalBlocksize = timeToBlockSize.firstEntry().getValue();
		System.out.format("Selected blocksize: %d\n", optimalBlocksize);
	}

	private static long benchmarkRead1GB(FileInputStream fis, int blocksize) throws IOException {
		long started;
		long stopped;
		byte[] input = new byte[blocksize];
		int readBytes;
		long totalReadBytes = 0;
		started = System.currentTimeMillis();
		while (((readBytes = fis.read(input)) != -1) && totalReadBytes < 1_000_000_000) {
			totalReadBytes += readBytes;
		}
		stopped = System.currentTimeMillis();
		final long time = stopped - started;
		return time;
	}
}

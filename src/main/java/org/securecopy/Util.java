package org.securecopy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Util {
	final static long KILO = 1024;
	final static long MEGA = KILO * KILO;
	final static long GIGA = KILO * KILO * KILO;
	final static long TERA = KILO * KILO * KILO * KILO;
	final static char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	static String byteCountToDisplaySize(long bytes) {
		if (bytes > TERA) {
			return (bytes / TERA) + "TiB";
		}
		if (bytes > GIGA) {
			return (bytes / GIGA) + "GiB";
		}
		if (bytes > MEGA) {
			return (bytes / MEGA) + "MiB";
		}
		if (bytes > KILO) {
			return (bytes / KILO) + "KiB";
		}
		return bytes + "B";
	}

	static String nanoExecTimeToText(long t0, long t1) {
		final long NANOS_PER_MINUTE_LONG = 60_000_000_000l;
		final double NANOS_PER_SECOND_DOUBLE = 1_000_000_000f;
		long time = t1 - t0;
		double seconds = (time % NANOS_PER_MINUTE_LONG) / NANOS_PER_SECOND_DOUBLE;
		time /= NANOS_PER_MINUTE_LONG;
		long mins = time % 60;
		time /= 60;
		long hours = time;
		return String.format("%d hours, %d mins, %.1f seconds", hours, mins, seconds);
	}

	static String formatTime(long seconds) {
		if (seconds > 3600) {
			return String.format("%1.1f hours", seconds / 3600.0);
		} else if (seconds <= 0) {
			return "N/A";
		} else {
			return String.format("%dm %ds", seconds / 60, seconds % 60);
		}
	}

	static List<String> readLines(File file) throws IOException {
		ArrayList<String> lines = new ArrayList<String>();
		try (FileReader fr = new FileReader(file)) {
			try (BufferedReader br = new BufferedReader(fr)) {
				String line;
				while ((line = br.readLine()) != null) {
					lines.add(line);
				}
			}
		}
		return lines;
	}

	public static String encodeHexString(byte[] digest) {
		StringBuilder sb = new StringBuilder();
		for (byte d : digest) {
			int a = d >> 4 & 0xf;
			int b = d & 0xf;
			sb.append(HEX[a]).append(HEX[b]);
		}
		return sb.toString();
	}

	public static void deleteDirectory(File tempdir) throws IOException {
		if (!tempdir.exists()) {
			return;
		}
		Path rootPath = Paths.get(tempdir.getAbsolutePath());
		final List<Path> pathsToDelete = Files.walk(rootPath).sorted(Comparator.reverseOrder())
				.collect(Collectors.toList());
		for (Path path : pathsToDelete) {
			Files.deleteIfExists(path);
		}
	}

	public static void writeByteArrayToFile(File file, byte[] data) throws IOException {
		try (OutputStream fw = new FileOutputStream(file)) {
			fw.write(data);
		}

	}
}

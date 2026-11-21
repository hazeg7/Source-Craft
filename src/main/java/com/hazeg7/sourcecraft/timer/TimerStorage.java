package com.hazeg7.sourcecraft.timer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * JSON persistence for course zones, personal bests, and recent run history.
 */
public final class TimerStorage {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static final class StoredCourseData {
		public List<TimerZone> zones = new ArrayList<>();
		public List<TimerRecord> records = new ArrayList<>();
		public long personalBestMs = -1L;
		public long personalBestProMs = -1L;
	}

	private TimerStorage() {
	}

	public static StoredCourseData load(Path file) {
		if (file == null || !Files.exists(file)) {
			return new StoredCourseData();
		}
		try {
			String raw = Files.readString(file, StandardCharsets.UTF_8);
			StoredCourseData data = GSON.fromJson(raw, StoredCourseData.class);
			if (data == null) {
				return new StoredCourseData();
			}
			if (data.zones == null) {
				data.zones = new ArrayList<>();
			}
			if (data.records == null) {
				data.records = new ArrayList<>();
			}
			return data;
		} catch (Exception ignored) {
			return new StoredCourseData();
		}
	}

	public static void save(Path file, StoredCourseData data) {
		if (file == null || data == null) {
			return;
		}
		try {
			Path parent = file.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			// Keep top 50 fastest records bounded on disk
			List<TimerRecord> sorted = new ArrayList<>(data.records);
			sorted.sort(Comparator.comparingLong(TimerRecord::elapsedMs));
			if (sorted.size() > 50) {
				sorted = new ArrayList<>(sorted.subList(0, 50));
			}
			data.records = sorted;
			Files.writeString(file, GSON.toJson(data), StandardCharsets.UTF_8);
		} catch (IOException ignored) {
		}
	}
}

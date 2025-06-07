package carpetbotrestriction;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

// Definitely didn't use chatgpt for this crap
// I hate file I/O
public class CBRConfig {
    private final Map<String, String> config = new HashMap<>();
    private final File file;

    public CBRConfig(String filePath) {
        this.file = new File(filePath);
    }

    public void clear() {
        config.clear();
    }

    public void remove(String key) {
        config.remove(key);
    }

    public void set(String key, int value) {
        config.put(key, String.valueOf(value));
    }

    public void set(String key, boolean value) {
        config.put(key, String.valueOf(value));
    }

    public int get(String key, int defaultValue) {
        try {
            return Integer.parseInt(config.getOrDefault(key, Integer.toString(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean get(String key, boolean defaultValue) {
        return Boolean.parseBoolean(config.getOrDefault(key, Boolean.toString(defaultValue)));
    }

    public void save() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            for (Map.Entry<String, String> entry : config.entrySet()) {
                writer.println(entry.getKey() + "=" + entry.getValue());
            }
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    public void load() throws IOException {
        if (!file.exists()) return;
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    config.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
    }
}
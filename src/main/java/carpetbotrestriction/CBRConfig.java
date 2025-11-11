package carpetbotrestriction;

import java.util.HashMap;
import java.util.Map;

public class CBRConfig {
    private Map<String, Object> config = new HashMap<>();

    public CBRConfig() {
        // Standard: 1 Bot pro User (statt 3)
        config.put("max_bots_per_player", 1);
    }

    public void set(String key, Object value) {
        config.put(key, value);
    }

    public String get(String key) {
        Object value = config.get(key);
        return value != null ? value.toString() : null;
    }

    public int getInt(String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return defaultValue;
    }

    public Map<String, String> getAll() {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : config.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toString());
        }
        return result;
    }
}
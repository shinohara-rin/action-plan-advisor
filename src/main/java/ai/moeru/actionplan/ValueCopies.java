package ai.moeru.actionplan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class ValueCopies {
	private ValueCopies() {
	}

	static Map<String, String> stringMap(Map<String, String> input) {
		if (input == null || input.isEmpty()) {
			return Map.of();
		}
		TreeMap<String, String> copy = new TreeMap<>();
		input.forEach((key, value) -> {
			if (key == null || key.isBlank() || value == null || value.isBlank()) {
				throw new IllegalArgumentException("fact keys and values must be nonblank");
			}
			copy.put(key, value);
		});
		return Collections.unmodifiableMap(copy);
	}

	static Map<String, Long> longMap(Map<String, Long> input) {
		if (input == null || input.isEmpty()) {
			return Map.of();
		}
		TreeMap<String, Long> copy = new TreeMap<>();
		input.forEach((key, value) -> {
			if (key == null || key.isBlank() || value == null) {
				throw new IllegalArgumentException("metric keys and values are required");
			}
			copy.put(key, value);
		});
		return Collections.unmodifiableMap(copy);
	}

	static Map<String, Object> argumentMap(Map<String, Object> input) {
		if (input == null || input.isEmpty()) {
			return Map.of();
		}
		LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
		input.forEach((key, value) -> {
			if (key == null || key.isBlank()) {
				throw new IllegalArgumentException("argument key is required");
			}
			copy.put(key, argumentValue(value));
		});
		return Collections.unmodifiableMap(copy);
	}

	private static Object argumentValue(Object value) {
		if (value == null || value instanceof String || value instanceof Boolean
			|| value instanceof Byte || value instanceof Short || value instanceof Integer
			|| value instanceof Long || value instanceof Float || value instanceof Double) {
			return value;
		}
		if (value instanceof Map<?, ?> map) {
			LinkedHashMap<String, Object> typed = new LinkedHashMap<>();
			map.forEach((key, nested) -> typed.put(String.valueOf(key), nested));
			return argumentMap(typed);
		}
		if (value instanceof List<?> list) {
			ArrayList<Object> copy = new ArrayList<>(list.size());
			list.forEach(item -> copy.add(argumentValue(item)));
			return List.copyOf(copy);
		}
		throw new IllegalArgumentException("unsupported argument value type " + value.getClass().getName());
	}
}


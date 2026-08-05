package ai.moeru.actionplan;

import java.util.Map;
import java.util.Objects;

public record FactIdentity(FactType type, Map<String, String> keys) {
	public FactIdentity {
		type = Objects.requireNonNull(type, "type");
		keys = ValueCopies.stringMap(keys);
	}

	public boolean matches(FactType expectedType, Map<String, String> requiredKeys) {
		if (!type.equals(expectedType)) {
			return false;
		}
		return ValueCopies.stringMap(requiredKeys).entrySet().stream()
			.allMatch(entry -> entry.getValue().equals(keys.get(entry.getKey())));
	}
}


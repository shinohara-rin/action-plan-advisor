package ai.moeru.actionplan;

import java.util.Map;
import java.util.Objects;

public record GoalKey(FactType type, Map<String, String> keys, Map<String, Long> minimums) {
	public GoalKey {
		type = Objects.requireNonNull(type, "type");
		keys = ValueCopies.stringMap(keys);
		minimums = ValueCopies.longMap(minimums);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(type.id());
		keys.forEach((key, value) -> builder.append('|').append(key).append('=').append(value));
		minimums.forEach((key, value) -> builder.append('|').append(key).append(">=").append(value));
		return builder.toString();
	}
}


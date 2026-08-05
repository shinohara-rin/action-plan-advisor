package ai.moeru.actionplan;

import java.util.Map;
import java.util.Objects;

public record Goal(FactType type, Map<String, String> keys, Map<String, Long> minimums) {
	public Goal {
		type = Objects.requireNonNull(type, "type");
		keys = ValueCopies.stringMap(keys);
		minimums = ValueCopies.longMap(minimums);
	}

	public FactCondition condition() {
		return new FactCondition(type, keys, minimums);
	}

	public GoalKey key() {
		return new GoalKey(type, keys, minimums);
	}
}


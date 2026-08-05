package ai.moeru.actionplan;

import java.util.Map;
import java.util.Objects;

public record FactCondition(
	FactType type,
	Map<String, String> requiredKeys,
	Map<String, Long> minimumMetrics
) {
	public FactCondition {
		type = Objects.requireNonNull(type, "type");
		requiredKeys = ValueCopies.stringMap(requiredKeys);
		minimumMetrics = ValueCopies.longMap(minimumMetrics);
	}

	public boolean satisfiedBy(Fact fact) {
		if (fact == null || !fact.identity().matches(type, requiredKeys)) {
			return false;
		}
		return minimumMetrics.entrySet().stream()
			.allMatch(entry -> fact.metrics().getOrDefault(entry.getKey(), Long.MIN_VALUE) >= entry.getValue());
	}
}


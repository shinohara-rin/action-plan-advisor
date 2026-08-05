package ai.moeru.actionplan;

import java.util.Map;
import java.util.Objects;

public record Fact(FactIdentity identity, Map<String, Long> metrics) {
	public Fact {
		identity = Objects.requireNonNull(identity, "identity");
		metrics = ValueCopies.longMap(metrics);
	}
}


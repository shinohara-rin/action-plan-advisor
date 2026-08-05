package ai.moeru.actionplan;

import java.util.List;

public record StateSnapshot(List<Fact> facts) {
	public StateSnapshot {
		facts = facts == null ? List.of() : List.copyOf(facts);
	}

	public boolean satisfies(FactCondition condition) {
		return facts.stream().anyMatch(condition::satisfiedBy);
	}
}


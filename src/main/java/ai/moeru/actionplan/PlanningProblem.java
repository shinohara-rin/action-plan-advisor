package ai.moeru.actionplan;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record PlanningProblem(
	StateSnapshot state,
	Goal goal,
	List<MethodProvider> providers,
	PlanningOptions options,
	Set<MethodKey> blockedMethods
) {
	public PlanningProblem {
		state = Objects.requireNonNull(state, "state");
		goal = Objects.requireNonNull(goal, "goal");
		providers = providers == null ? List.of() : List.copyOf(providers);
		options = options == null ? PlanningOptions.defaults() : options;
		blockedMethods = blockedMethods == null ? Set.of() : Set.copyOf(blockedMethods);
	}
}


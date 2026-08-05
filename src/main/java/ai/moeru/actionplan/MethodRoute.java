package ai.moeru.actionplan;

import java.util.List;
import java.util.Objects;

public record MethodRoute(MethodKey methodKey, List<PlanCommand> commands, long cost) {
	public MethodRoute {
		methodKey = Objects.requireNonNull(methodKey, "methodKey");
		commands = commands == null ? List.of() : List.copyOf(commands);
		cost = Costs.nonnegative(cost);
	}
}


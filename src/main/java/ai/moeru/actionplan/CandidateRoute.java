package ai.moeru.actionplan;

import java.util.List;
import java.util.Objects;

public record CandidateRoute(
	MethodKey methodKey,
	List<PlanCommand> commands,
	long cost,
	int providerRank
) {
	public CandidateRoute {
		methodKey = Objects.requireNonNull(methodKey, "methodKey");
		commands = commands == null ? List.of() : List.copyOf(commands);
		cost = Costs.nonnegative(cost);
	}

	public static CandidateRoute from(MethodRoute route, int providerRank) {
		return new CandidateRoute(route.methodKey(), route.commands(), route.cost(), providerRank);
	}

	public static CandidateRoute satisfied() {
		return new CandidateRoute(new MethodKey(new ProviderId("core"), "already_satisfied"), List.of(), 0, Integer.MIN_VALUE);
	}
}


package ai.moeru.actionplan;

import java.util.List;
import java.util.Optional;

public record PlanAdvice(
	List<CandidateRoute> candidates,
	Optional<CandidateRoute> recommendation,
	PlanningFailure failure,
	PlanningStats stats,
	List<PlanningTraceEvent> trace
) {
	public PlanAdvice {
		candidates = candidates == null ? List.of() : List.copyOf(candidates);
		recommendation = recommendation == null ? Optional.empty() : recommendation;
		failure = failure == null ? PlanningFailure.none() : failure;
		trace = trace == null ? List.of() : List.copyOf(trace);
	}

	public boolean advised() {
		return recommendation.isPresent();
	}
}


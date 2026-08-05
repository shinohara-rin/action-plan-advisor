package ai.moeru.actionplan;

import java.util.Map;
import java.util.Optional;

public interface ResolutionContext {
	StateSnapshot state();

	Optional<CandidateRoute> resolve(Goal prerequisite);

	boolean blocked(MethodKey methodKey);

	boolean exhausted();

	void trace(String eventType, MethodKey methodKey, String commandId, Map<String, Object> payload);
}


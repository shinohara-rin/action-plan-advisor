package ai.moeru.actionplan;

import java.util.Optional;

public interface MethodProvider {
	ProviderId id();

	int rank();

	Optional<MethodRoute> resolve(Goal goal, ResolutionContext context);
}


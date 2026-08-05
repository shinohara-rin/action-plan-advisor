package ai.moeru.actionplan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class BackwardChainingPlanAdvisor implements PlanAdvisor {
	private static final Comparator<CandidateRoute> ROUTE_ORDER = Comparator
		.comparingLong(CandidateRoute::cost)
		.thenComparingInt(CandidateRoute::providerRank)
		.thenComparing(route -> route.methodKey().providerId())
		.thenComparing(route -> route.methodKey().methodId());

	@Override
	public PlanAdvice advise(PlanningProblem problem) {
		Objects.requireNonNull(problem, "problem");
		return new Session(problem).advise();
	}

	private static final class Session {
		private final PlanningProblem problem;
		private final List<MethodProvider> providers;
		private final Map<GoalKey, CandidateRoute> successfulRoutes = new HashMap<>();
		private final List<PlanningTraceEvent> trace = new ArrayList<>();
		private List<CandidateRoute> topCandidates = List.of();
		private int expandedGoals;
		private int routeCacheHits;
		private boolean budgetExceeded;

		private Session(PlanningProblem problem) {
			this.problem = problem;
			this.providers = problem.providers().stream()
				.sorted(Comparator.comparingInt(MethodProvider::rank).thenComparing(MethodProvider::id))
				.toList();
		}

		private PlanAdvice advise() {
			Optional<CandidateRoute> route = resolveGoal(problem.goal(), 0, new LinkedHashSet<>(), true);
			PlanningStats stats = new PlanningStats(expandedGoals, routeCacheHits, problem.options().explorationBudget());
			trace.add(event("resolution_stats", null, "", Map.of(
				"expandedGoals", expandedGoals,
				"routeCacheHits", routeCacheHits,
				"explorationBudget", problem.options().explorationBudget()
			)));
			if (budgetExceeded) {
				return new PlanAdvice(topCandidates, Optional.empty(), new PlanningFailure(
					"resolution_budget_exceeded",
					"route advice exceeded its deterministic exploration budget"
				), stats, trace);
			}
			if (route.isEmpty()) {
				return new PlanAdvice(topCandidates, Optional.empty(), new PlanningFailure(
					"no_route",
					"no route can satisfy " + problem.goal().key()
				), stats, trace);
			}
			return new PlanAdvice(topCandidates, route, PlanningFailure.none(), stats, trace);
		}

		private Optional<CandidateRoute> resolveGoal(
			Goal goal,
			int depth,
			LinkedHashSet<GoalKey> resolving,
			boolean topLevel
		) {
			GoalKey key = goal.key();
			trace.add(event("goal_started", null, "", Map.of("goal", key.toString(), "depth", depth)));
			if (++expandedGoals > problem.options().explorationBudget()) {
				budgetExceeded = true;
				trace.add(event("goal_failed", null, "", Map.of("goal", key.toString(), "failureCode", "resolution_budget_exceeded")));
				return Optional.empty();
			}
			if (depth > problem.options().maxDepth()) {
				trace.add(event("goal_failed", null, "", Map.of("goal", key.toString(), "failureCode", "max_depth_exceeded")));
				return Optional.empty();
			}
			if (resolving.contains(key)) {
				trace.add(event("goal_failed", null, "", Map.of("goal", key.toString(), "failureCode", "cycle_detected")));
				return Optional.empty();
			}
			CandidateRoute cached = successfulRoutes.get(key);
			if (cached != null) {
				routeCacheHits++;
				trace.add(event("route_cache_hit", cached.methodKey(), "", Map.of("goal", key.toString(), "cost", cached.cost())));
				return Optional.of(cached);
			}
			if (problem.state().satisfies(goal.condition())) {
				CandidateRoute satisfied = CandidateRoute.satisfied();
				successfulRoutes.put(key, satisfied);
				trace.add(event("goal_satisfied", satisfied.methodKey(), "", Map.of("goal", key.toString())));
				return Optional.of(satisfied);
			}

			resolving.add(key);
			try {
				ArrayList<CandidateRoute> candidates = new ArrayList<>();
				for (MethodProvider provider : providers) {
					ResolutionContext context = new ScopedContext(depth, resolving);
					provider.resolve(goal, context).ifPresent(methodRoute -> {
						if (!problem.blockedMethods().contains(methodRoute.methodKey())) {
							candidates.add(CandidateRoute.from(methodRoute, provider.rank()));
						}
					});
					if (budgetExceeded) {
						return Optional.empty();
					}
				}
				candidates.sort(ROUTE_ORDER);
				List<CandidateRoute> immutableCandidates = List.copyOf(candidates);
				if (topLevel) {
					topCandidates = immutableCandidates;
				}
				if (candidates.isEmpty()) {
					trace.add(event("goal_failed", null, "", Map.of("goal", key.toString(), "failureCode", "no_route")));
					return Optional.empty();
				}
				CandidateRoute selected = candidates.getFirst();
				successfulRoutes.put(key, selected);
				trace.add(event("route_selected", selected.methodKey(), "", Map.of("goal", key.toString(), "cost", selected.cost())));
				return Optional.of(selected);
			}
			finally {
				resolving.remove(key);
			}
		}

		private PlanningTraceEvent event(String eventType, MethodKey methodKey, String commandId, Map<String, Object> payload) {
			return new PlanningTraceEvent(
				eventType,
				methodKey == null ? "" : methodKey.providerId().value(),
				methodKey == null ? "" : methodKey.methodId(),
				commandId,
				payload
			);
		}

		private final class ScopedContext implements ResolutionContext {
			private final int depth;
			private final LinkedHashSet<GoalKey> resolving;

			private ScopedContext(int depth, LinkedHashSet<GoalKey> resolving) {
				this.depth = depth;
				this.resolving = resolving;
			}

			@Override
			public StateSnapshot state() {
				return problem.state();
			}

			@Override
			public Optional<CandidateRoute> resolve(Goal prerequisite) {
				return resolveGoal(prerequisite, depth + 1, resolving, false);
			}

			@Override
			public boolean blocked(MethodKey methodKey) {
				return problem.blockedMethods().contains(methodKey);
			}

			@Override
			public boolean exhausted() {
				return budgetExceeded;
			}

			@Override
			public void trace(String eventType, MethodKey methodKey, String commandId, Map<String, Object> payload) {
				trace.add(event(eventType, methodKey, commandId == null ? "" : commandId, payload));
			}
		}
	}
}


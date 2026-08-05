package ai.moeru.actionplan;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackwardChainingPlanAdvisorTest {
	private static final FactType ITEM = new FactType("inventory.item");
	private static final PlanAdvisor ADVISOR = new BackwardChainingPlanAdvisor();

	@Test
	void returnsEmptyRouteWhenGoalIsSatisfied() {
		Goal goal = item("bread", 1);
		PlanAdvice advice = advise(List.of(fact("bread", 1)), goal, List.of(), PlanningOptions.defaults(), Set.of());

		assertTrue(advice.advised());
		assertTrue(advice.recommendation().orElseThrow().commands().isEmpty());
	}

	@Test
	void recursivelyExpandsPrerequisites() {
		MethodProvider provider = provider("craft", 0, (goal, context) -> {
			if ("bread".equals(goal.keys().get("item"))) {
				CandidateRoute wheat = context.resolve(item("wheat", 3)).orElse(null);
				if (wheat == null) return Optional.empty();
				return Optional.of(route("craft", "bread", wheat.cost() + 2, concat(wheat.commands(), command("craft", "bread", "craft"))));
			}
			if ("wheat".equals(goal.keys().get("item"))) {
				return Optional.of(route("craft", "wheat", 3, List.of(command("craft", "wheat", "collect"))));
			}
			return Optional.empty();
		});

		PlanAdvice advice = advise(List.of(), item("bread", 1), List.of(provider), PlanningOptions.defaults(), Set.of());

		assertEquals(List.of("collect", "craft"), advice.recommendation().orElseThrow().commands().stream().map(PlanCommand::commandType).toList());
		assertEquals(5, advice.recommendation().orElseThrow().cost());
	}

	@Test
	void ranksCandidatesByCostThenProviderRankAndIds() {
		MethodProvider expensive = fixedProvider("z", 0, 10);
		MethodProvider later = fixedProvider("b", 2, 5);
		MethodProvider selected = fixedProvider("a", 2, 5);

		PlanAdvice advice = advise(List.of(), item("bread", 1), List.of(expensive, later, selected), PlanningOptions.defaults(), Set.of());

		assertEquals("a", advice.recommendation().orElseThrow().methodKey().providerId().value());
		assertEquals(3, advice.candidates().size());
	}

	@Test
	void excludesBlockedMethod() {
		MethodProvider provider = fixedProvider("craft", 0, 1);
		MethodKey blocked = new MethodKey(new ProviderId("craft"), "method");

		PlanAdvice advice = advise(List.of(), item("bread", 1), List.of(provider), PlanningOptions.defaults(), Set.of(blocked));

		assertFalse(advice.advised());
		assertEquals("no_route", advice.failure().code());
	}

	@Test
	void detectsRecursiveCycles() {
		MethodProvider provider = provider("cycle", 0, (goal, context) -> context.resolve(goal)
			.map(route -> route("cycle", "method", route.cost(), route.commands())));

		PlanAdvice advice = advise(List.of(), item("bread", 1), List.of(provider), PlanningOptions.defaults(), Set.of());

		assertFalse(advice.advised());
		assertTrue(advice.trace().stream().anyMatch(event -> "cycle_detected".equals(event.payload().get("failureCode"))));
	}

	@Test
	void stopsAtMaximumDepth() {
		MethodProvider provider = provider("deeper", 0, (goal, context) -> {
			long count = goal.minimums().get("count");
			return context.resolve(item("bread", count + 1)).map(route -> route("deeper", Long.toString(count), route.cost(), route.commands()));
		});

		PlanAdvice advice = advise(List.of(), item("bread", 1), List.of(provider), new PlanningOptions(1, 100), Set.of());

		assertFalse(advice.advised());
		assertTrue(advice.trace().stream().anyMatch(event -> "max_depth_exceeded".equals(event.payload().get("failureCode"))));
	}

	@Test
	void stopsAtExplorationBudget() {
		MethodProvider provider = provider("deeper", 0, (goal, context) -> context.resolve(item("bread", goal.minimums().get("count") + 1))
			.map(route -> route("deeper", "method", route.cost(), route.commands())));

		PlanAdvice advice = advise(List.of(), item("bread", 1), List.of(provider), new PlanningOptions(10, 1), Set.of());

		assertEquals("resolution_budget_exceeded", advice.failure().code());
	}

	@Test
	void cachesSuccessfulSubroutes() {
		MethodProvider provider = provider("craft", 0, (goal, context) -> {
			if ("bread".equals(goal.keys().get("item"))) {
				context.resolve(item("wheat", 1));
				CandidateRoute second = context.resolve(item("wheat", 1)).orElseThrow();
				return Optional.of(route("craft", "bread", second.cost(), second.commands()));
			}
			return Optional.of(route("craft", "wheat", 1, List.of(command("craft", "wheat", "collect"))));
		});

		PlanAdvice advice = advise(List.of(), item("bread", 1), List.of(provider), PlanningOptions.defaults(), Set.of());

		assertEquals(1, advice.stats().routeCacheHits());
	}

	@Test
	void saturatesCost() {
		assertEquals(Long.MAX_VALUE, Costs.add(Long.MAX_VALUE, 1));
		assertEquals(4, Costs.add(-3, 4));
	}

	@Test
	void deeplyCopiesCommandArguments() {
		ArrayList<String> nested = new ArrayList<>(List.of("a"));
		LinkedHashMap<String, Object> args = new LinkedHashMap<>();
		args.put("nested", nested);
		PlanCommand command = new PlanCommand("step", "test", args, new MethodKey(new ProviderId("p"), "m"));
		nested.add("b");
		args.put("later", true);

		assertEquals(List.of("a"), command.arguments().get("nested"));
		assertFalse(command.arguments().containsKey("later"));
		assertThrows(UnsupportedOperationException.class, () -> ((List<Object>) command.arguments().get("nested")).add("c"));
	}

	private static PlanAdvice advise(List<Fact> facts, Goal goal, List<MethodProvider> providers, PlanningOptions options, Set<MethodKey> blocked) {
		return ADVISOR.advise(new PlanningProblem(new StateSnapshot(facts), goal, providers, options, blocked));
	}

	private static Goal item(String item, long count) {
		return new Goal(ITEM, Map.of("item", item), Map.of("count", count));
	}

	private static Fact fact(String item, long count) {
		return new Fact(new FactIdentity(ITEM, Map.of("item", item)), Map.of("count", count));
	}

	private static MethodProvider fixedProvider(String id, int rank, long cost) {
		return provider(id, rank, (goal, context) -> Optional.of(route(id, "method", cost, List.of(command(id, "method", "act")))));
	}

	private static MethodProvider provider(String id, int rank, Resolver resolver) {
		return new MethodProvider() {
			@Override public ProviderId id() { return new ProviderId(id); }
			@Override public int rank() { return rank; }
			@Override public Optional<MethodRoute> resolve(Goal goal, ResolutionContext context) { return resolver.resolve(goal, context); }
		};
	}

	private static MethodRoute route(String provider, String method, long cost, List<PlanCommand> commands) {
		return new MethodRoute(new MethodKey(new ProviderId(provider), method), commands, cost);
	}

	private static PlanCommand command(String provider, String method, String type) {
		return new PlanCommand(type, type, Map.of(), new MethodKey(new ProviderId(provider), method));
	}

	private static List<PlanCommand> concat(List<PlanCommand> first, PlanCommand last) {
		ArrayList<PlanCommand> commands = new ArrayList<>(first);
		commands.add(last);
		return commands;
	}

	@FunctionalInterface
	private interface Resolver {
		Optional<MethodRoute> resolve(Goal goal, ResolutionContext context);
	}
}

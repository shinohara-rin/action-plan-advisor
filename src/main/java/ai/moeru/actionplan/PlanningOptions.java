package ai.moeru.actionplan;

public record PlanningOptions(int maxDepth, int explorationBudget) {
	public static final int DEFAULT_MAX_DEPTH = 8;
	public static final int DEFAULT_EXPLORATION_BUDGET = 20_000;

	public PlanningOptions {
		maxDepth = Math.max(1, maxDepth);
		explorationBudget = Math.max(1, explorationBudget);
	}

	public static PlanningOptions defaults() {
		return new PlanningOptions(DEFAULT_MAX_DEPTH, DEFAULT_EXPLORATION_BUDGET);
	}
}


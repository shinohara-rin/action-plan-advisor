package ai.moeru.actionplan;

public final class Costs {
	private Costs() {
	}

	public static long add(long left, long right) {
		long normalizedLeft = nonnegative(left);
		long normalizedRight = nonnegative(right);
		if (Long.MAX_VALUE - normalizedLeft < normalizedRight) {
			return Long.MAX_VALUE;
		}
		return normalizedLeft + normalizedRight;
	}

	static long nonnegative(long value) {
		return Math.max(0, value);
	}
}


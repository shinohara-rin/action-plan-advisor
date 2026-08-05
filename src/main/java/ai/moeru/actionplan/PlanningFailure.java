package ai.moeru.actionplan;

public record PlanningFailure(String code, String message) {
	public PlanningFailure {
		code = code == null ? "" : code;
		message = message == null ? "" : message;
	}

	public static PlanningFailure none() {
		return new PlanningFailure("", "");
	}

	public boolean present() {
		return !code.isBlank();
	}
}


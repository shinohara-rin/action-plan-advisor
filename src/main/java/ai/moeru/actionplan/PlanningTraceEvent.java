package ai.moeru.actionplan;

import java.util.Map;

public record PlanningTraceEvent(
	String eventType,
	String providerId,
	String methodId,
	String commandId,
	Map<String, Object> payload
) {
	public PlanningTraceEvent {
		if (eventType == null || eventType.isBlank()) {
			throw new IllegalArgumentException("event type is required");
		}
		providerId = providerId == null ? "" : providerId;
		methodId = methodId == null ? "" : methodId;
		commandId = commandId == null ? "" : commandId;
		payload = ValueCopies.argumentMap(payload);
	}
}


package ai.moeru.actionplan;

import java.util.Map;
import java.util.Objects;

public record PlanCommand(
	String commandId,
	String commandType,
	Map<String, Object> arguments,
	MethodKey methodKey
) {
	public PlanCommand {
		if (commandId == null || commandId.isBlank()) {
			throw new IllegalArgumentException("command id is required");
		}
		if (commandType == null || commandType.isBlank()) {
			throw new IllegalArgumentException("command type is required");
		}
		commandId = commandId.trim();
		commandType = commandType.trim();
		arguments = ValueCopies.argumentMap(arguments);
		methodKey = Objects.requireNonNull(methodKey, "methodKey");
	}
}


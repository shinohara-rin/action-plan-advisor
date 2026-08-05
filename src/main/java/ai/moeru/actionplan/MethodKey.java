package ai.moeru.actionplan;

import java.util.Objects;

public record MethodKey(ProviderId providerId, String methodId) implements Comparable<MethodKey> {
	public MethodKey {
		providerId = Objects.requireNonNull(providerId, "providerId");
		if (methodId == null || methodId.isBlank()) {
			throw new IllegalArgumentException("method id is required");
		}
		methodId = methodId.trim();
	}

	@Override
	public int compareTo(MethodKey other) {
		int providerComparison = providerId.compareTo(other.providerId);
		return providerComparison != 0 ? providerComparison : methodId.compareTo(other.methodId);
	}

	@Override
	public String toString() {
		return providerId.value() + ':' + methodId;
	}
}


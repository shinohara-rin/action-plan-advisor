package ai.moeru.actionplan;

public record ProviderId(String value) implements Comparable<ProviderId> {
	public ProviderId {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("provider id is required");
		}
		value = value.trim();
	}

	@Override
	public int compareTo(ProviderId other) {
		return value.compareTo(other.value);
	}
}


package ai.moeru.actionplan;

public record FactType(String id) implements Comparable<FactType> {
	public FactType {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("fact type id is required");
		}
		id = id.trim();
	}

	@Override
	public int compareTo(FactType other) {
		return id.compareTo(other.id);
	}
}


package me.archdukeliamus.dygenerate;

import java.util.Objects;

/**
 * A description of a surrogate method, containing the name and method type descriptor.
 *
 */
public final class Surrogate {
	private final String surrogateClassFQCN;
	private final String surrogateMethodName;
	private final String surrogateDescriptor;

	/**
	 * Creates a description of a surrogate method.
	 * @param surrogateMethodName The identifier name of the surrogate method
	 * @param surrogateDescriptor The JVM internal method type descriptor of the surrogate method
	 */
	public Surrogate(String surrogateClassFQCN, String surrogateMethodName, String surrogateDescriptor) {
		this.surrogateClassFQCN = Objects.requireNonNull(surrogateClassFQCN, "surrogate class FQCN");
		this.surrogateMethodName = Objects.requireNonNull(surrogateMethodName, "surrogate method name");
		this.surrogateDescriptor = Objects.requireNonNull(surrogateDescriptor, "surrogate method descriptor");
	}
	
	/**
	 * Gets the FQCN of the surrogate method.
	 * @return the owning class FQCN of this surrogate method
	 */
	public String getSurrogateClassFQCN() {
		return surrogateClassFQCN;
	}
	
	/**
	 * Gets the surrogate method name.
	 * @return the method name
	 */
	public String getSurrogateMethodName() {
		return surrogateMethodName;
	}
	
	/**
	 * Gets the surrogate method descriptor.
	 * @return the method descriptor
	 */
	public String getSurrogateDescriptor() {
		return surrogateDescriptor;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Surrogate{");
		sb.append(surrogateClassFQCN);
		sb.append(".");
		sb.append(surrogateMethodName);
		sb.append(":");
		sb.append(surrogateDescriptor);
		sb.append("}");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(surrogateClassFQCN, surrogateDescriptor, surrogateMethodName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Surrogate other = (Surrogate) obj;
		return Objects.equals(surrogateClassFQCN, other.surrogateClassFQCN)
				&& Objects.equals(surrogateDescriptor, other.surrogateDescriptor)
				&& Objects.equals(surrogateMethodName, other.surrogateMethodName);
	}
}

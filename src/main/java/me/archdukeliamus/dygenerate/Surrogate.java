package me.archdukeliamus.dygenerate;

import java.util.Objects;

public final class Surrogate {
	private final String surrogateMethodName;
	private final String surrogateDescriptor;

	public Surrogate(String surrogateMethodName, String surrogateDescriptor) {
		this.surrogateMethodName = surrogateMethodName;
		this.surrogateDescriptor = surrogateDescriptor;
	}
	
	public String getSurrogateMethodName() {
		return surrogateMethodName;
	}
	
	public String getSurrogateDescriptor() {
		return surrogateDescriptor;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Surrogate{");
		sb.append(surrogateMethodName);
		sb.append(":");
		sb.append(surrogateDescriptor);
		sb.append("}");
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(surrogateDescriptor, surrogateMethodName);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Surrogate other = (Surrogate) obj;
		return Objects.equals(surrogateDescriptor, other.surrogateDescriptor) && Objects.equals(surrogateMethodName, other.surrogateMethodName);
	}
}

package me.archdukeliamus.dygenerate;

import java.util.Arrays;
import java.util.Objects;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

/**
 * A description of bootstrapping data needed for invokedynamic or dynamic constants.
 *
 */
public final class BootstrapData {
	private final BootstrapType type;
	private final Handle bootstrapHandle;
	private final Object[] bootstrapArgs;

	BootstrapData(BootstrapType type, Handle bootstrapHandle, Object[] bootstrapArgs) {
		this.type = type;
		this.bootstrapHandle = bootstrapHandle;
		this.bootstrapArgs = bootstrapArgs;
	}
	
	BootstrapType getType() {
		return type;
	}
	
	Handle getBootstrapHandle() {
		return bootstrapHandle;
	}
	
	Object[] getBootstrapArgs() {
		return bootstrapArgs;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("BootstrapData{");
		sb.append(type);
		sb.append(" ");
		sb.append(bootstrapHandle);
		sb.append(",");
		sb.append(Arrays.toString(bootstrapArgs));
		sb.append("}");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.deepHashCode(bootstrapArgs);
		result = prime * result + Objects.hash(bootstrapHandle, type);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		BootstrapData other = (BootstrapData) obj;
		return Arrays.deepEquals(bootstrapArgs, other.bootstrapArgs) && Objects.equals(bootstrapHandle, other.bootstrapHandle) && type == other.type;
	}
	
	
}

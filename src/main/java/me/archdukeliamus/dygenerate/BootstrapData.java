package me.archdukeliamus.dygenerate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.objectweb.asm.Handle;

/**
 * A description of bootstrapping data needed for invokedynamic or dynamic constants.
 *
 */
public final class BootstrapData {
	private final BootstrapType type;
	private final Handle bootstrapHandle;
	private final Object[] bootstrapArgs;

	/**
	 * Internal constructor, direct access to ASM types
	 * @param type Type of bootstrap data
	 * @param bootstrapHandle ASM method handle description of the bootstrap method handle
	 * @param bootstrapArgs Static arguments array to be passed to ASM
	 */
	BootstrapData(BootstrapType type, Handle bootstrapHandle, Object[] bootstrapArgs) {
		this.type = type;
		this.bootstrapHandle = bootstrapHandle;
		this.bootstrapArgs = bootstrapArgs;
	}
	
	/**
	 * Creates bootstrap data from an annotation string for a given type of bootstrap.
	 * @param type The type of bootstrap data to parse
	 * @param value The string value describing the bootstrap method
	 * @return a BootstrapData instance describing the bootstrap data
	 * @throws InvalidBootstrapDataException if the string describing the bootstrap data is not valid
	 */
	public static BootstrapData fromString(BootstrapType type, String value) {
		Objects.requireNonNull(type, "type");
		Objects.requireNonNull(value, "value");
		try {
			Tokeniser tokeniser = new Tokeniser(value);
			tokeniser.tokenise(); // throws
			List<Token> tokens = tokeniser.getOutput();
			Parser parser = new Parser(tokens);
			BootstrapData bootstrapData = parser.parseBootstrapData(type); // throws
			return bootstrapData;
		} catch (TokeniserError ex) {
			throw new InvalidBootstrapDataException(ex.getMessage());
		} catch (ParserError ex) {
			throw new InvalidBootstrapDataException(ex.getMessage());
		}
	}
	
	/**
	 * Gets the type of this bootstrap data.
	 * @return the type of this bootstrap data
	 */
	public BootstrapType getType() {
		return type;
	}
	
	/**
	 * Internal getter for ASM handle
	 */
	Handle getBootstrapHandle() {
		return bootstrapHandle;
	}
	
	/**
	 * Internal getter for ASM static arguments
	 */
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

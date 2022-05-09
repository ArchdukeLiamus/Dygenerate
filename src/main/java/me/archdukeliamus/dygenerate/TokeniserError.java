package me.archdukeliamus.dygenerate;

/**
 * Thrown internally if there is a problem parsing the bootstrap data string in the tokeniser.
 *
 */
class TokeniserError extends RuntimeException {
	private final int globalCharIndex; // the index into the string

	TokeniserError(String message, int charIndex) {
		super(message);
		this.globalCharIndex = charIndex;
	}
	
	int getCharIndex() {
		return globalCharIndex;
	}
}

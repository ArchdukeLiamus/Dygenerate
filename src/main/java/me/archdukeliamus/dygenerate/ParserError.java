package me.archdukeliamus.dygenerate;

/**
 * Thrown internally if there is a problem parsing the bootstrap data string in the parser.
 *
 */
class ParserError extends RuntimeException {
	private final Token offendingToken; // the token that caused this parser error.

	ParserError(String message, Token offendingToken) {
		super(message);
		this.offendingToken = offendingToken;
	}
	
	/**
	 * Returns the offending token that caused the error, or null if caused by unexpected end of input
	 * @return
	 */
	Token getOffendingToken() {
		return offendingToken;
	}

}

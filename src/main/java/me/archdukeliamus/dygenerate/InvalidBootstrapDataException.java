package me.archdukeliamus.dygenerate;

/**
 * Thrown if a string describing bootstrap data is invalid.
 *
 */
public class InvalidBootstrapDataException extends RuntimeException {
	private static final long serialVersionUID = 5023577994417043114L;

	public InvalidBootstrapDataException() {}

	public InvalidBootstrapDataException(String message) {
		super(message);
	}

	public InvalidBootstrapDataException(Throwable cause) {
		super(cause);
	}

	public InvalidBootstrapDataException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidBootstrapDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}

package me.archdukeliamus.dygenerate;

/**
 * Thrown if a class transformation operation fails.
 *
 */
public class ClassTransformException extends RuntimeException {
	private static final long serialVersionUID = -5530766157984143275L;

	public ClassTransformException() {}

	public ClassTransformException(String message) {
		super(message);
	}

	public ClassTransformException(Throwable cause) {
		super(cause);
	}

	public ClassTransformException(String message, Throwable cause) {
		super(message, cause);
	}

	public ClassTransformException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}

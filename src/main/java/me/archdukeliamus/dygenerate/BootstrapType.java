package me.archdukeliamus.dygenerate;

/**
 * A type of bootstrap data, used to determine what type of surrogate an instance of bootstrap data is describing.
 *
 */
public enum BootstrapType {
	/**
	 * An invokedynamic surrogate method.
	 */
	INVOKEDYNAMIC,
	/**
	 * A constant dynamic surrogate method.
	 */
	CONSTANTDYNAMIC
}

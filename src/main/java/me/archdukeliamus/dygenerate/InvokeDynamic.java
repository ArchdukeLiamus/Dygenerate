package me.archdukeliamus.dygenerate;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks methods that are to be used as invokedynamic surrogates. The value string provides information about the
 * bootstrap method.
 * <br><br>
 * An example invokedynamic surrogate looks like:
 * <br>
 * <pre>
 * {@literal @}InvokeDynamic("""
 * 		invokestatic com/example/BootstrapClass.bootstrapMethod:
 * 		(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;
 * 		"""
 * )
 * private static void invokedynamicSurrogate() {
 * 		throw new Error("replaced with opcode after post-processing");
 * }
 * </pre>
 */
@Retention(CLASS)
@Target(METHOD)
public @interface InvokeDynamic {
	/**
	 * The string providing information about the bootstrap method and any arguments.
	 * @return The value string
	 */
	String value();
}

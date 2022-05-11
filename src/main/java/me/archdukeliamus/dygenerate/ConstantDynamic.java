package me.archdukeliamus.dygenerate;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks methods that are to be used as surrogates for dynamic constants (condy). The value string provides information about the
 * bootstrap method.
 * <br><br>
 * An example dynamic constant surrogate looks like:
 * <br>
 * <pre>
 * {@literal @}ConstantDynamic("""
 * 		invokestatic com/example/BootstrapClass.bootstrapMethod:
 * 		(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;I)Ljava/lang/String;
 * 		{
 * 			123
 * 		}
 * 		"""
 * )
 * private static String condy() {
 * 		throw new Error("replaced with opcode after post-processing");
 * }
 * </pre>
 *
 */
@Retention(CLASS)
@Target(METHOD)
public @interface ConstantDynamic {
	/**
	 * The string providing information about the bootstrap method and any arguments.
	 * @return The value string
	 */
	String value();
}

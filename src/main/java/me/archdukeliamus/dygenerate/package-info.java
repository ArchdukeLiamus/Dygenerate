/**
 * The <code>me.archdukeliamus.dygenerate</code> package describes the Dygenerate utility for inserting <code>invokedynamic</code> and
 * dynamic constant <code>ldc</code> opcodes into Java (or any other JVM language) source code. Insertion is accomplished through three parts:
 * <ul>
 * 	<li>The declaration and use of <i>surrogate methods</i>
 * 	<li>The annotation of those surrogate method declarations with <i>bootstrap data</i>
 * 	<li>The <i>post-processing</i> of the class bytecode, replacing the surrogates with their corresponding JVM instructions and metadata
 * </ul>
 * 
 * Dygenerate and its documentation are defined in terms of the Java programming language and the JVM, however any JVM language compiler may be used
 * as long as it can generate bytecode with the required semantics similar to <code>javac</code>.
 * 
 * <h2>Surrogate Methods</h2>
 * 
 * Surrogate methods are the way that source code interacts with the Dygenerate processor. These methods are compiled normally by <code>javac</code> or
 * any other JVM compiler and take the "call shape" of the JVM opcodes that they stand in for. In other words, a surrogate method must have the same
 * effect on the JVM operand stack as the opcode that will replace it after post-processing. Upon post-processing by Dygenerate, the surrogate methods
 * will be removed from the class binary and method invocation instructions that invoke a surrogate will be replaced with their associated JVM opcode.
 * Therefore, in order for the compiler to generate the right bytecode such that the surrogate methods can be properly replaced by Dygenerate into their
 * JVM opcodes, surrogate methods must follow certain restrictions on how they may be declared and used.
 * <br><br>
 * All surrogate methods must be declared in the class in which they are to be used, as classes are processed individually and the needed metadata will
 * be removed after processing. For the same reason, no class may reference a surrogate method in another class, as the data required to identify the
 * callee as a surrogate will not be available. Indirect dispatches are also prohibited as the actual callee cannot be determined at compile-time and
 * may or may not invoke a surrogate. Surrogate methods must not be inherited from, override an inherited method, or be called through an interface.
 * <br><br>
 * Surrogate methods must not be referenced by method references, however, they may be used inside lambda expressions in the defining class. Attempts
 * to reflect or acquire method handles to surrogates will fail at runtime as the methods will not exist.
 * <br><br>
 * Dygenerate cannot check for calls to surrogate methods in other classes or other violations; the best way to prevent accidental calls is to
 * mark surrogate methods as <code>private</code>. <b>In short: do not reference a surrogate method in any way other than a direct call from within
 * the declaring class.</b> In terms of JVM semantics, surrogate methods must only be used as the directly-named target of <code>invokestatic</code>,
 * <code>invokespecial</code>, or <code>invokevirtual</code> instructions in order to be properly replaced.
 * <br><br>
 * The body of a surrogate method may be anything: it does not matter as it will be removed after post-processing anyway. Surrogate methods may even be
 * <code>abstract</code> if the context permits; the only criterion is that the compiler is able to emit calls (or equivalent JVM method call instructions)
 * to the surrogate method. The recommended body of a surrogate method is to throw an unchecked exception; alternatively, a fallback implementation could
 * be provided.
 * 
 * <h3>invokedynamic Surrogate Methods</h3>
 * 
 * Surrogate methods for <code>invokedynamic</code> instructions may take on any method signature they see fit. The name and method descriptor of the
 * surrogate method are used for the name-and-type of the <code>invokedynamic</code> call site, with possible adaptation of arguments to match the call
 * site. Surrogate methods of this type are recommended to be <code>static</code>, though instance methods are permitted provided they do not violate
 * the above rules.
 * <br><br>
 * In the case of an instance surrogate method, the first argument of the produced method descriptor will have the type of the declaring
 * class and will receive the value that would have been <code>this</code>. The remaining arguments are passed as normal, mirroring a
 * <code>invokespecial</code> or <code>invokevirtual</code> call.
 * <br><br>
 * For example, if the declaring class is com.example.MyClass, <code>private int surrogate(int x, int y)</code> produces descriptor
 * <code>surrogate:(Lcom/example/MyClass;II)I</code>.
 * <br><br>
 * Static surrogate methods copy the method descriptor of the surrogate method they replace directly and better reflect the behavior of
 * <code>invokedynamic</code>. They mirror an <code>invokestatic</code> call.
 * <br><br>
 * For example, <code>private static int surrogate(int x, int y)</code> produces descriptor
 * <code>surrogate:(II)I</code>.
 */
package me.archdukeliamus.dygenerate;
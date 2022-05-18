/**
 * The <code>me.archdukeliamus.dygenerate</code> package describes the Dygenerate utility for inserting <code>invokedynamic</code> (indy) and
 * dynamic constant (condy) <code>ldc</code> opcodes into Java (or any other JVM language) source code. Insertion is accomplished through three parts:
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
 * <br><br>
 * Generics are permitted in surrogate methods; the standard rules of type erasure apply.
 * <br><br>
 * Surrogate methods may declare checked exceptions, though they have no effect on the output. In any case, such declarations are ignored by the JVM
 * and Dygenerate.
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
 * For example, if the declaring class is <code>com.example.MyClass</code>, method <code>private int surrogate(int x, int y)</code> produces descriptor
 * <code>surrogate:(Lcom/example/MyClass;II)I</code>.
 * <br><br>
 * Static surrogate methods copy the method descriptor of the surrogate method they replace directly and better reflect the behavior of
 * <code>invokedynamic</code>. They mirror an <code>invokestatic</code> call.
 * <br><br>
 * For example, method <code>private static int surrogate(int x, int y)</code> produces descriptor <code>surrogate:(II)I</code>.
 * 
 * <h3>Dynamic Constant Surrogate Methods</h3>
 * 
 * Surrogate methods for dynamic constants are far more restricted in terms of access modifiers and method signatures. Condy surrogates must be
 * declared <code>static</code> and must not take any arguments. The name of the surrogate method is used as the name of the loaded constant.
 * The return type of the surrogate method informs the type of the constant to be loaded. This reflects the "call shape" of an <code>ldc</code> instruction,
 * which does not "take arguments" and pushes a single "return value" to the operand stack.
 * 
 * <h2>Bootstrap Data Annotations</h2>
 * 
 * Surrogate methods are marked with one of either the {@link me.archdukeliamus.dygenerate.InvokeDynamic} or
 * {@link me.archdukeliamus.dygenerate.ConstantDynamic} annotations declaring the surrogate method's presence, type, and information about the bootstrap
 * method used to bootstrap the <code>invokedynamic</code> call site or dynamic constant. Information about the bootstrap method and any static arguments
 * are specified through the value string of the annotation according to a defined syntax. Bootstrap methods referenced by these annotations may take any
 * form allowed by the Java Virtual Machine Specification and may be in any class.
 * <br><br>
 * The basic syntax is as follows:
 * <br><br>
 * <i>invoketype</i> [<code>interface</code>] [<code>class</code>] <i>bsclass</i><code>.</code><i>bsname</i><code>:</code><i>bsmtype</i> [<code>{</code> <i>args...</i> <code>}</code>]
 * <br>
 * <ul>
 *  <li>where <i>invoketype</i> is one of <code>getfield</code>, <code>putfield</code>, <code>getstatic</code>, <code>putstatic</code>, 
 * 		<code>invokevirtual</code>, <code>invokestatic</code>, <code>invokespecial</code>, <code>newinvokespecial</code>, or <code>invokeinterface</code>
 * 		according to the JVMS
 * 	<li>where <i>bsclass</i> is the internal fully-qualified class name (FQCN) of the class containing the bootstrap method (for example
 * 		<code>com/example/MyClass</code>)
 * 	<li>where <i>bsname</i> is the identifier name of the bootstrap method to be invoked
 * 	<li>where <i>bsmtype</i> is the internal method type descriptor of the bootstrap method to be invoked (for example
 * 		<code>(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;</code>)
 * 	<li>where <i>args...</i> is an optional comma-seperated list of static arguments to be passed to the bootstrap method
 * </ul>
 * Items in brackets [ ] are optional.
 * <br>
 * If <i>bsclass</i> refers to an interface type, <code>interface</code> must be present. <code>class</code> is also allowed for consistency. While
 * redundant and verbose, <code>interface class</code> is also permissible. In the case of a method with no static arguments, the braces may be elided.
 * The arguments are passed as-is and no type conversions are performed.
 * 
 * <h3>Argument Types</h3>
 * 
 * Static arguments may be any of the nine permitted types: integer, long, float, double, string, class, method type, method handle, or dynamic constant.
 * 
 * <h4>Integers and Longs</h4>
 * 
 * Integer and long literals are defined similarly to the JLS, except underscores are not permitted and octal is not allowed. Use <code>0x</code> or
 * <code>0X</code> to start hexadecimal literals. Affix <code>L</code> or <code>l</code> for long literals.
 * 
 * <h4>Floats and Doubles</h4>
 * 
 * Float and double literals use a more restricted syntax compared to the JLS that reduces the amount of edge cases to be handled by the tokeniser.
 * There are no leading zeroes, except for when the integral part of the literal is zero (<code>0.123456</code>). The fractional part of the literal
 * is always required, even for integral values (for example <code>1.0</code>). The exponent is optional and works as in the JLS. Hexadecimal
 * floating-point literals are not supported. Affix <code>f</code> or <code>F</code> for float literals or optionally <code>d</code> or <code>D</code>
 * for double literals.
 * 
 * <h4>Strings</h4>
 * 
 * Strings are defined in double quotes and accept the usual escape sequences. Unicode escapes are not handled by Dygenerate but these are replaced by
 * <code>javac</code> in source before Dygenerate receives them. Text blocks are not supported.
 * 
 * <h4>Class Literals</h4>
 * 
 * Class literals are defined as <code>class</code> <i>classfqcn</i>, where <i>classfqcn</i> is the internal class FQCN of the class to be
 * referenced (for example <code>com/example/MyClass</code>). Arrays may be referred to by their own FQCNs, including arrays of primitives.
 * Primitive types are not permitted as in the JVMS.
 * 
 * <h4>Method Type Literals</h4>
 * 
 * Method type literals are defined as <code>methodtype</code> <i>mtype</i>, where <i>mtype</i> is a method descriptor.
 * 
 * <h4>Method Handle Literals</h4>
 * 
 * Method handle literals are defined as:
 * <br><br>
 * <code>handle</code> <i>invoketype</i> [<code>interface</code>] [<code>class</code>] <i>hclass</i><code>.</code><i>hname</i><code>:</code><i>htype</i>
 * <br>
 * <ul>
 *  <li>where <i>invoketype</i> is one of <code>getfield</code>, <code>putfield</code>, <code>getstatic</code>, <code>putstatic</code>, 
 * 		<code>invokevirtual</code>, <code>invokestatic</code>, <code>invokespecial</code>, <code>newinvokespecial</code>, or <code>invokeinterface</code>
 * 		describing the method's invocation behavior according to the JVMS
 * 	<li>where <i>hclass</i> is the internal FQCN of the class containing the method or field
 * 	<li>where <i>hname</i> is the identifier name of the method or field
 * 	<li>where <i>htype</i> is the internal method type descriptor of the method or field descriptor of the field
 * </ul>
 * Items in brackets [ ] are optional.
 * <br>
 * If <i>hclass</i> refers to an interface type, <code>interface</code> must be present. <code>class</code> is optional.
 * 
 * <h4>Dynamic Constant Literals</h4>
 * 
 * Dynamic constant literals are defined as:
 * <br><br>
 * <code>condy</code> <i>name</i><code>:</code><i>ctype</i><code>:</code><i>invoketype</i> [<code>interface</code>] [<code>class</code>]
 * <i>bsclass</i><code>.</code><i>bsname</i><code>:</code><i>bsmtype</i> [<code>{</code> <i>args...</i> <code>}</code>]
 * <br>
 * <ul>
 * 	<li>where <i>name</i> is an identifier name describing this constant
 * 	<li>where <i>ctype</i> is a field descriptor representing the type of the constant
 *  <li>where <i>invoketype</i> is one of <code>getfield</code>, <code>putfield</code>, <code>getstatic</code>, <code>putstatic</code>, 
 * 		<code>invokevirtual</code>, <code>invokestatic</code>, <code>invokespecial</code>, <code>newinvokespecial</code>, or <code>invokeinterface</code>
 * 		describing the bootstrap method's invocation behavior according to the JVMS
 * 	<li>where <i>bsclass</i> is the internal FQCN of the class containing the bootstrap method
 * 	<li>where <i>bsname</i> is the identifier name of the bootstrap method to be invoked
 * 	<li>where <i>bsmtype</i> is the internal method type descriptor of the bootstrap method to be invoked
 * 	<li>where <i>args...</i> is an optional comma-seperated list of static arguments to be passed to the bootstrap method
 * </ul>
 * Items in brackets [ ] are optional.
 * <br>
 * If <i>bsclass</i> refers to an interface type, <code>interface</code> must be present. <code>class</code> is optional. Braces may be elided if
 * the bootstrap method has no static arguments to be passed. According to the JVMS, the return type of <i>bsmtype</i> must be assignable to <i>ctype</i>.
 * The static arguments may be any argument type, including arguments that are themselves dynamic constants. These may be nested to an implementation
 * and environment dependent depth.
 * 
 * <h3>Syntax Errors</h3>
 * 
 * In the case of a syntax error, transformation of the class will fail and a {@link me.archdukeliamus.dygenerate.ClassTransformException ClassTransformException}
 * will be thrown describing the problem and its location in the string. On the command-line, this information will be printed instead. However,
 * invalid uses of method or class type descriptors are <i>not</i> checked, nor are argument mismatches.
 * 
 * <h2>Post-Processing</h2>
 * 
 * The standard way to invoke Dygenerate is through the command line. Files and folders containing class files are listed as arguments. All files
 * specified, as well as any *.class files found in specified folders will be transformed and overwritten in-place with the patched version.
 * Any surrogate methods are removed and any invocations of them are replaced with bytecodes. Any errors while transforming a class will be
 * printed, though this will not stop processing.
 * <br><br>
 * Alternatively, Dygenerate may be invoked programmatically, passing a byte array and receiving a byte array of the processed output, with
 * {@link me.archdukeliamus.dygenerate.ClassTransformException ClassTransformException} being thrown on potential errors.
 * 
 * <h2>An Example</h2>
 * 
 * This example shows nearly all of the syntax for bootstrap data.
 * <pre>
 * {@literal @}InvokeDynamic(
 * 	"""	
 * 	invokestatic com/example/BootstrapClass.bootstrapMethod:
 * 	(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;ILFDLjava/lang/String;Ljava/lang/Class;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/Object;)Ljava/lang/invoke/CallSite;
 *	{
 *	  12345,
 *	  0xFFFFFFFF00000000L,
 *	  -3.14159f,
 *	  0.0,
 *	  "Hello World!",
 *	  class java/lang/String,
 *	  methodtype ()V,
 *	  handle invokeinterface interface java/lang/Runnable.run:()V,
 *	  condy myDynObject:Ljava/lang/Object;:invokestatic com/example/BootstrapClass.constant:(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;
 *	}
 *	""")
 * private static void invokedynamicSurrogate() {
 *   throw new Error("replaced with opcode after post-processing");
 * }
 * </pre>
 * 
 * <h2>Dependencies</h2>
 * 
 * If Dygenerate is used solely as a bytecode postprocessor on class files ahead-of-time, Dygenerate does not need to be present at runtime.
 * However, any usage of code in the <code>me.archdukeliamus.dygenerate.rtutils</code> <i>will</i> require Dygenerate to be present at runtime.
 */
package me.archdukeliamus.dygenerate;
# Dygenerate

Dygenerate is a tool for easily defining and generating `invokedynamic` and
dynamic constant `ldc` opcodes in the source code of any compatible JVM
language. Just define and annotate the methods, compile your code, then run
Dygenerate on the compiled output to have the bytecodes inserted. Alternatively,
use the API to define transformations programmatically.

## Requirements

- Java 7 or higher (`invokedynamic`)
- Java 11 or higher (`ldc` condy)
- A compiled JVM language that directly maps language methods to JVM methods
- A compiled JVM language that supports JVM static methods (required for `ldc` condy, preferred for `invokedynamic`)
- Support for Java-compatible annotations (optional, but preferred)
- Access to the compiled output of whatever language is in use

## How it works

In your JVM language (for example, Java) define and annotate a method describing
the return type and arguments of the call site, then annotate it with the
InvokeDynamic annotation and specify the bootstrap method and static arguments.
This is called a *surrogate method*. The body of the method does not matter.

```java
@InvokeDynamic("invokestatic com/example/Main.bootstrapMethod:(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;")
private static void helloWorld() {}
```

Now define your bootstrap method, or use a bootstrap method that already exists.

```java
public static CallSite bootstrapMethod(Lookup lookup, String name, MethodType type) {
	try {
		MethodHandle println = lookup.findVirtual(PrintStream.class, "println", MethodType.methodType(void.class, String.class));
		return new ConstantCallSite(println.bindTo(System.out).bindTo("Hello from invokedynamic"));
	} catch (Exception ex) {
		throw new BootstrapMethodError(ex);
	}
}
```

Now just call your surrogate method like you would any other method.

```java
public static void main(String[] args) {
	helloWorld();
}
```

Before Dygenerate:

```
public static Method main:"([Ljava/lang/String;)V"
	stack 0 locals 1
{
	invokestatic    Method helloWorld:"()V";
	return;
}
```

After Dygenerate (`java -jar Dygenerate.jar <path to bin class folder>`):

```
public static Method main:"([Ljava/lang/String;)V"
	stack 0 locals 1
{
	invokedynamic   InvokeDynamic REF_invokeStatic:Method Main.bootstrapMethod:"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;":helloWorld:"()V";
	return;
}
```
(Hello from invokedynamic!)

See the [Javadoc](https://archdukeliamus.github.io/Dygenerate/javadoc/) for more details.

Dygenerate is an experimental project and is still a work in progress. While it is stable for production use, there may still be unknown bugs. Use with care.
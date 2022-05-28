# Dygenerate

Dygenerate is a tool for easily defining and generating `invokedynamic` and
dynamic constant `ldc` opcodes in the source code of any compatible JVM
language. Just define and annotate the methods, compile your code, then run
Dygenerate on the compiled output to have the bytecodes inserted. Alternatively,
use the API to define transformations programmatically.

## But why?

There are times where some level of dynamism is required to implement a feature
which otherwise cannot be performed within the bounds of Java (or any other JVM language)
or would otherwise be cumbersome to do so. The two typical ways to break out of the
limitations of the language are through the Core Reflection API (`java.lang.reflect`)
or direct use of `java.lang.invoke` (JLI).

However, reflection is cumbersome, Java-centric, performs access checks on every
call (unless they are suppressed), and has to box and unbox primitive types.
Additionally, there is no flexibility to manipulate arguments going into the call,
and `Method` objects have to be cached in fields that other code could introspect.

Direct JLI gets us closer (no access checks per-call, no boxing/unboxing,
ability to manipulate and curry arguments) and can potentially run as fast as a
direct call to the target method. However, doing so requires method handles and
call sites to be registered as `static final` fields to actually attain that performance.
This still leaves the possibility of leaking those fields via reflection. It also has
the problem that target resolution must be performed at class load time: lazy loading
is not possible, nor can binding be deferred to a time when more information is
available. For example, the method to be targeted might not yet exist. Furthermore,
all the fields to create and exceptions to catch impose a lot of boilerplate code
that has to be duplicated for each and every unique call site used. (That's bad.)

`invokedynamic` has none of the above problems: call sites can be linked lazily,
and it is <i>never</i> possible to obtain a call site through reflection unless
the instance is deliberately leaked. Additionally, the need for extra fields for
each and every call site and handle goes away, and there's no longer a need to
catch exceptions around handle invocations. In short: the opportunities for
dynamism increase.

And a final note: there's no equivalent to a lazy-loaded dynamic constant either.

Dygenerate exists to enable that potential dynamism, as well as make it easier
to experiment with the possibilities of `invokedynamic` outside the context of
"compiler magic" (which is why this project was started in the first place).

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
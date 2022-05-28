package me.archdukeliamus.dygenerate.rtutils;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;

/**
 * Contains various utility bootstrap methods.
 *
 */
public final class Bootstraps {

	private Bootstraps() {}
	
	/**
	 * A call site that implements duck-typing calls for a given method and signature. The receiver object that would be <code>this</code> is
	 * passed as first argument to the call site target. Up to 8 class types are cached for fast invocation.
	 * @param lk caller lookup
	 * @param name caller name
	 * @param type caller type
	 * @return a DuckTypeCallSite
	 */
	public static CallSite duckTypeCall(Lookup lk, String name, MethodType type) {
		return new DuckTypeCallSite(lk, name, type);
	}
	
	/**
	 * A call site that implements duck-typing calls for a given method and signature. The receiver object that would be <code>this</code> is
	 * passed as first argument to the call site target. The number of class types cached for fast invocation is configurable.
	 * @param lk caller lookup
	 * @param name caller name
	 * @param type caller type
	 * @param polymorphism amount of calling types to cache handles for
	 * @return a DuckTypeCallSite
	 */
	public static CallSite duckTypeCall(Lookup lk, String name, MethodType type, int polymorphism) {
		return new DuckTypeCallSite(lk, name, type, polymorphism);
	}
	
	/**
	 * A constant call site that when its target is invoked, will call <code>lookupHandle</code> with the caller's lookup, name, method type, and the
	 * arguments for that invocation to lookup the target method handle to be called for that invocation as if by <code>invoke</code>.
	 * <code>lookupHandle</code> must accept a subset of arguments compatible with the caller type (as in <code>MethodHandles.foldArguments</code>)
	 * and return a method handle with the same type as the caller type.
	 * <br>
	 * No target caching of any kind is performed. If the call site will be invoked repeatedly, consider writing a custom call site instead.
	 * @param lk caller lookup
	 * @param callerName caller name
	 * @param callerType caller type
	 * @param lookupHandle the method handle that will be called to lookup the target method handle to invoke.
	 * @return a ConstantCallSite
	 */
	public static CallSite lookupAndInvoke(Lookup lk, String callerName, MethodType callerType, MethodHandle lookupHandle) {
		MethodType lookupType = lookupHandle.type();
		if (lookupType.returnType() != MethodHandle.class) throw new IllegalArgumentException("lookup handle does not return a method handle");
		return new ConstantCallSite(MethodHandles.foldArguments(MethodHandles.invoker(callerType), lookupHandle.bindTo(lk).bindTo(callerName).bindTo(callerType)));
	}
	
	/**
	 * A constant call site that when its target is invoked, will call <code>lookupHandle</code> with the caller's lookup, name, method type, and the
	 * arguments for that invocation to lookup the target method handle to be called for that invocation as if by <code>invokeExact</code>.
	 * <code>lookupHandle</code> must accept a subset of arguments compatible with the caller type (as in <code>MethodHandles.foldArguments</code>)
	 * and return a method handle with the same type as the caller type.
	 * <br>
	 * No target caching of any kind is performed. If the call site will be invoked repeatedly, consider writing a custom call site instead.
	 * @param lk caller lookup
	 * @param callerName caller name
	 * @param callerType caller type
	 * @param lookupHandle the method handle that will be called to lookup the target method handle to invoke.
	 * @return a ConstantCallSite
	 */
	public static CallSite lookupAndInvokeExact(Lookup lk, String callerName, MethodType callerType, MethodHandle lookupHandle) {
		MethodType lookupType = lookupHandle.type();
		if (lookupType.returnType() != MethodHandle.class) throw new IllegalArgumentException("lookup handle does not return a method handle");
		return new ConstantCallSite(MethodHandles.foldArguments(MethodHandles.exactInvoker(callerType), lookupHandle.bindTo(lk).bindTo(callerName).bindTo(callerType)));
	}
}

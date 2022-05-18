package me.archdukeliamus.dygenerate.rtutils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class DuckTypeCallSite extends LinkingCallSite {
	private final Lookup callerLookup;
	private final String methodName;
	private final AtomicReference<List<MethodHandle>> cacheList;
	
	private final MethodHandle MH_LINK = findOwnMH("link", MethodType.methodType(MethodHandle.class, Object.class));
	private final MethodHandle MH_TESTCLASS = findOwnMH("testClass", MethodType.methodType(boolean.class, Class.class, Object.class));
	
	public DuckTypeCallSite(Lookup lookup, String name, MethodType type) {
		super(type);
		methodName = name;
		callerLookup = lookup;
		cacheList = new AtomicReference<List<MethodHandle>>(new ArrayList<>());
		// no other class will override so this is fine: normally this would be invoked in the bootstrap method once ctor returns
		installLinker(MH_LINK);
	}
	
	@SuppressWarnings("unused")
	private final MethodHandle link(Object receiver) {
		Class<?> recvClass = receiver.getClass();
		MethodHandle recvHandle = findOrThrow(receiver);
		// install cached
		List<MethodHandle> newHandleCache = new ArrayList<>(cacheList.get()); // MT read
		newHandleCache.add(MH_TESTCLASS.bindTo(recvClass));
		newHandleCache.add(recvHandle);
		cacheList.set(newHandleCache);
		installMultiFastPath(newHandleCache);
		return recvHandle.asType(type());
	}
	
	private final MethodHandle findOrThrow(Object receiver) {
		try {
			// 'this' argument needs dropping, as implicit in instance calls, but explicit at callsite
			return callerLookup.findVirtual(receiver.getClass(), methodName, type().dropParameterTypes(0, 1));
		} catch (NoSuchMethodException | IllegalAccessException ex) {
			// no method to invoke!
			throw new NoSuchMethodError("No such method " + methodName + ":" + type().dropParameterTypes(0, 1).descriptorString());
		}
	}
	
	@SuppressWarnings("unused")
	private final boolean testClass(Class<?> type, Object receiver) {
		return type.isInstance(receiver);
	}
	
	private final MethodHandle findOwnMH(String name, MethodType type) {
		try {
			return MethodHandles.lookup().findSpecial(getClass(), name, type, getClass()).bindTo(this);
		} catch (NoSuchMethodException | IllegalAccessException ex) {
			throw new Error(ex); // Should not happen
		}
	}
}

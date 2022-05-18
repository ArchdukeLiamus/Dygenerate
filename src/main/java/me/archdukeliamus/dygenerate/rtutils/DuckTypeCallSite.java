package me.archdukeliamus.dygenerate.rtutils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.ArrayList;
import java.util.List;

/**
 * A polymorphic inline caching call site that calls a provided method on any object if it exists, otherwise throws
 * NoSuchMethodError for that invocation. The receiver object that would be <code>this</code> is passed as first argument.
 * Remaining arguments are passed for the method to be invoked. The exact type must be present on the target class;
 * argument conversions are not performed even if it would result in the call resolving (method overloads are decided at compile-time).
 * By default, 8 class targets are cached.
 * 
 * Use of this call site is safe by multiple threads. In the case of concurrent dispatches to the linker, the last writer wins.
 * Spurious lookups may result in some cases.
 */
public final class DuckTypeCallSite extends MutableCallSite {
	private final MethodHandle MH_TESTCLASS = findOwnMH("testClass", MethodType.methodType(boolean.class, Class.class, Object.class));
	private final MethodHandle MH_LINK = findOwnMH("link", MethodType.methodType(MethodHandle.class, Object.class));
	private final MethodHandle MH_LOOKUP_AND_CALL = MethodHandles.foldArguments(MethodHandles.exactInvoker(type()), MH_LINK);
	private final Lookup lk; // lookup of the bootstrapper
	private final String name; // name of the method to be duck-invoked
	// cache lists for guard-target MH pairs. The list is ALWAYS handled as if it were immutable- thread safety depends on it
	private volatile List<MethodHandle> cacheList;
	private final int maxPolymorphicCache; // store at most n many class-handle pairs before dropping them; zero means do lookup every time (bad!)
	
	/**
	 * Construct a call site with a default polymorphic cache of 8 classes
	 * @param lk Lookup from the caller
	 * @param name Name of method to invoke
	 * @param type Type of the method to invoke (including Object arg)
	 */
	public DuckTypeCallSite(Lookup lk, String name, MethodType type) {
		super(type);
		this.lk = lk;
		this.name = name;
		this.cacheList = new ArrayList<>();
		this.maxPolymorphicCache = 8;
		setTarget(MH_LOOKUP_AND_CALL);
	}
	
	/**
	 * Construct a call site with a custom cache size
	 * @param lk Lookup from the caller
	 * @param name Name of method to invoke
	 * @param type Type of the method to invoke (including Object arg)
	 * @param maxPolymorphicCache number of class targets to cache. Must be zero or more.
	 */
	public DuckTypeCallSite(Lookup lk, String name, MethodType type, int maxPolymorphicCache) {
		super(type);
		this.lk = lk;
		this.name = name;
		this.cacheList = new ArrayList<>();
		if (maxPolymorphicCache < 0) throw new IllegalArgumentException("Max polymorphic caching must be a positive integer");
		this.maxPolymorphicCache = maxPolymorphicCache;
		setTarget(MH_LOOKUP_AND_CALL);
	}
	
	private final MethodHandle findOrThrow(Object receiver) {
		try {
			// 'this' argument needs dropping, as implicit in instance calls, but explicit at callsite
			return lk.findVirtual(receiver.getClass(), name, type().dropParameterTypes(0, 1));
		} catch (NoSuchMethodException | IllegalAccessException ex) {
			// no method to invoke!
			throw new NoSuchMethodError("No such method " + name + ":" + type().dropParameterTypes(0, 1).descriptorString());
		}
	}
	
	@SuppressWarnings("unused")
	private final boolean testClass(Class<?> type, Object receiver) {
		return type.isInstance(receiver);
	}
	
	@SuppressWarnings("unused")
	private final MethodHandle link(Object recv) {
		Class<?> cls = recv.getClass();
		MethodHandle mh = findOrThrow(recv);
		
		// Try to race the lookup. If we lose, it's fine, the lookup will just try again next time.
		
		List<MethodHandle> newCacheList = new ArrayList<>(cacheList); // volatile read
		newCacheList.add(MH_TESTCLASS.bindTo(cls));
		newCacheList.add(mh);
		if (newCacheList.size() > maxPolymorphicCache * 2) { // too many handles, discard oldest used
			newCacheList.remove(0);
			newCacheList.remove(0);
		}
		// Fallback call
		// Create a handle that calls this method (#link) with the first argument of the arg list, then combines it's result
		// with the original arg list. Target a method handle that executes a method handle with the provided args, taking a method handle as an argument.
		// This takes the handle we just found and executes it, essentially, by looking up a handle and splicing it into the arg list
		// to be pasted to a method handle-invoking method handle (metahandle? either way it's an exact invoker handle)
		// With the handle we just found, make a guard handle to try to fast track it next time
		// If class is instance use handle we found, otherwise lookup again
		
		// A thread race here is okay- at worst an extra spurious lookup occurs. That list
		// only matters in the updated MH construction so that's not a problem. Still need to lock to avoid a desync with the list though.
		// TODO: does this work the way I expect it to?
		synchronized (this) {
			setTarget(genGuardHandle(newCacheList));
			// <a lookup by other thread while here would trigger desync>
			cacheList = newCacheList; // volatile write
		}
		// return handle found, still have to give something to invoke
		return mh.asType(type());
	}
	
	private final MethodHandle findOwnMH(String name, MethodType type) {
		try {
			return MethodHandles.lookup().findSpecial(getClass(), name, type, getClass()).bindTo(this);
		} catch (NoSuchMethodException | IllegalAccessException ex) {
			throw new Error(ex); // Should not happen
		}
	}
	
	// Cascading list of guards (if class = checked, call this, if not any, lookup)
	private final MethodHandle genGuardHandle(List<MethodHandle> cacheList) {
		MethodHandle handle = MH_LOOKUP_AND_CALL;
		for (int i = 0; i < cacheList.size(); i += 2) {
			handle = MethodHandles.guardWithTest(cacheList.get(i), cacheList.get(i+1).asType(type()), handle);
		}
		return handle;
	}
}

package me.archdukeliamus.dygenerate.rtutils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.List;
import java.util.function.Supplier;

/**
 * A call site that on invocation will call a <i>linker method handle</i> to determine the method to be invoked. The linker method
 * handle is called with none, some, or all of the original arguments and must return a MethodHandle determining the target.
 * The linker should also call {@link #installFastPath(MethodHandle, MethodHandle)} to cache the target such that future
 * invocations can skip the linking step. Implementors must provide their own thread safety guarantees.
 *
 */
public class LinkingCallSite extends MutableCallSite {
	private MethodHandle MH_LINK_AND_CALL = null; // has to be initialised later
	
	/**
	 * Create a LinkingCallSite for the given method type using the provided linker.
	 * The types of the linker arguments must be compatible with those of the call site type, though later arguments may be elided.
	 * After this constructor runs, the caller should call {@link #installLinker(MethodHandle)} to install the linker method handle before
	 * any other methods on this class are called.
	 * @param type The type of this call site
	 */
	public LinkingCallSite(MethodType type) {
		super(type);
	}
	
	/**
	 * Install the provided method handle as the linker for this call site. This handle will be called whenever a method needs to be resolved
	 * for a set of given arguments. This method must be called before any other method in this class is used and may only be called once.
	 * 
	 * @param linkerHandle the methodhandle used for linking
	 */
	public void installLinker(MethodHandle linkerHandle) {
		if (MH_LINK_AND_CALL != null) throw new IllegalStateException("already installed");
		MH_LINK_AND_CALL = makeLinkAndCall(linkerHandle);
		setTarget(MH_LINK_AND_CALL);
		
	}
	
	private MethodHandle makeLinkAndCall(MethodHandle linkerHandle) {
		return MethodHandles.foldArguments(MethodHandles.exactInvoker(type()), linkerHandle);
	}
	
	/**
	 * Reset this call site to the unlinked state. The next invocation will cause a link.
	 */
	public void reset() {
		setTarget(MH_LINK_AND_CALL);
	}
	
	/**
	 * Install a guard method handle with the provided guard test and fast-path target to use if the test passes. The arguments must abide by the
	 * same restrictions as MethodHandles.guardWithTest. If the guard test fails, the linker method handle will be called. This method should be
	 * called from within the linker method's implementation.
	 * @param guard The MethodHandle to use as a guard test
	 * @param target The MethodHandle to invoke if the test succeeds
	 */
	public void installFastPath(MethodHandle guard, MethodHandle target) {
		setTarget(MethodHandles.guardWithTest(guard, target, MH_LINK_AND_CALL));
	}
	
	/**
	 * Install a guarded method handles with chained pairs of guards and fast-path targets, checked in descending order. The last pair in the list is
	 * the first check to be invoked. The arguments must abide by the same restrictions as MethodHandles.guardWithTest. If all tests fail, the linker
	 * method handle will be called. This method should be called from within the linker method's implementation.
	 * @param guardTargetPairs a list of guard-target pairs
	 */
	public void installMultiFastPath(List<MethodHandle> guardTargetPairs) {
		if (guardTargetPairs.size() % 2 != 0) throw new IllegalArgumentException("List does not contain complete pairs");
		MethodHandle handle = MH_LINK_AND_CALL;
		for (int i = 0; i < guardTargetPairs.size(); i += 2) {
			handle = MethodHandles.guardWithTest(guardTargetPairs.get(i), guardTargetPairs.get(i+1), handle);
		}
		setTarget(handle);
	}
}

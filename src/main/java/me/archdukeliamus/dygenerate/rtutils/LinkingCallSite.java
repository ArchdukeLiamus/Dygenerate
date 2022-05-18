package me.archdukeliamus.dygenerate.rtutils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.List;

/**
 * A call site that on invocation will call a <i>linker method handle</i> to determine the method to be invoked. The linker method
 * handle is called with none, some, or all of the original arguments and must return a MethodHandle determining the target.
 * The linker should also call {@link #installFastPath(MethodHandle, MethodHandle)} to cache the target such that future
 * invocations can skip the linking step. Implementors must provide their own thread safety guarantees.
 *
 */
public class LinkingCallSite extends MutableCallSite {
	private final MethodHandle MH_LINK_AND_CALL;
	
	/**
	 * Create a LinkingCallSite for the given method type using the provided linker.
	 * The types of the linker arguments must be compatible with those of the call site type, though later arguments may be elided.
	 * The linker MH must return a MethodHandle when invoked.
	 * @param type The type of this call site
	 * @param linker The method handle to use as a linker
	 */
	public LinkingCallSite(MethodType type, MethodHandle linker) {
		super(type);
		if (!MethodHandle.class.isInstance(linker.type().returnType())) throw new IllegalArgumentException("Linker does not return a MethodHandle");
		MH_LINK_AND_CALL = MethodHandles.foldArguments(MethodHandles.exactInvoker(type), linker);
		setTarget(MH_LINK_AND_CALL);
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
		if (guardTargetPairs.isEmpty()) throw new IllegalArgumentException("List is empty");
		if (guardTargetPairs.size() % 2 != 0) throw new IllegalArgumentException("List does not contain complete pairs");
		MethodHandle handle = MH_LINK_AND_CALL;
		for (int i = 0; i < guardTargetPairs.size(); i += 2) {
			handle = MethodHandles.guardWithTest(guardTargetPairs.get(i), guardTargetPairs.get(i+1), handle);
		}
	}
}

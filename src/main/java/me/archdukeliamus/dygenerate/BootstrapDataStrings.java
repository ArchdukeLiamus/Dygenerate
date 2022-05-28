package me.archdukeliamus.dygenerate;

import me.archdukeliamus.dygenerate.rtutils.Bootstraps;

/**
 * A class containing useful constant strings for use in bootstrap data construction.
 * <br><br>
 * Constant strings are organised as follows:
 * <br>
 * <table class="plain">
 * 	<caption>String Organisation</caption>
 * 	<tr><td>D</td><td>Descriptors, both for use as field descriptors or as arguments or return types in method descriptors.</td></tr>
 * 	<tr><td>P</td><td>Package FQPNs.</td></tr>
 *  <tr><td>C</td><td>Class FQCNs. These do not include the <code>class</code> keyword.</td></tr>
 *  <tr><td>A</td><td>Argument sequences, such as the first three arguments of most <code>invokedynamic</code> bootstrap methods.</td></tr>
 *  <tr><td>M</td><td>Method descriptors. These do not include the <code>methodtype</code> keyword.</td></tr>
 *  <tr><td>B</td><td>Full bootstrap methods, including invocation type. Static arguments may be placed afterward or they may be used as handles.</td></tr>
 *  <tr><td>H</td><td>Method handles to various methods. These do not include the <code>handle</code> keyword.</td></tr>
 * </table>
 */
public final class BootstrapDataStrings {

	private BootstrapDataStrings() {}
	
	// Descriptors
	/**
	 * The descriptor for the String class.
	 */
	public static final String D_STRING = "Ljava/lang/String;";
	/**
	 * The descriptor for the Object class.
	 */
	public static final String D_OBJECT = "Ljava/lang/Object;";
	/**
	 * The descriptor for the Class class.
	 */
	public static final String D_CLASS = "Ljava/lang/Class;";
	/**
	 * The descriptor for the MethodHandle class.
	 */
	public static final String D_MHANDLE = "Ljava/lang/invoke/MethodHandle;";
	/**
	 * The descriptor for the MethodHandles.Lookup class.
	 */
	public static final String D_LOOKUP = "Ljava/lang/invoke/MethodHandles$Lookup;";
	/**
	 * The descriptor for the MethodType class.
	 */
	public static final String D_MTYPE = "Ljava/lang/invoke/MethodType;";
	/**
	 * The descriptor for the CallSite class.
	 */
	public static final String D_CALLSITE = "Ljava/lang/invoke/CallSite;";
	
	// Packages
	
	/**
	 * The package java/lang.
	 */
	public static final String P_LANG = "java/lang";
	/**
	 * The package java/lang/invoke.
	 */
	public static final String P_INVOKE = "java/lang/invoke";
	/**
	 * The package java/lang/runtime.
	 */
	public static final String P_RUNTIME = "java/lang/runtime";
	/**
	 * The package for Dygenerate, me/archdukeliamus/dygenerate.
	 */
	public static final String P_DYGENERATE = "me/archdukeliamus/dygenerate";
	/**
	 * The package for Dygenerate runtime utilities, me/archdukeliamus/dygenerate/rtutils.
	 */
	public static final String P_RTUTILS = P_DYGENERATE + "/rtutils";
	
	// Classes
	
	/**
	 * The class FQCN for MethodHandles.
	 */
	public static final String C_MHANDLES = P_INVOKE + "/MethodHandles";
	/**
	 * The class FQCN for ConstantBootstraps.
	 */
	public static final String C_CBOOTSTRAPS = P_INVOKE + "/ConstantBootstraps";
	/**
	 * The class FQCN for LambdaMetafactory.
	 */
	public static final String C_LAMBDAMF = P_INVOKE + "/LambdaMetafactory";
	/**
	 * The class FQCN for ObjectMethods.
	 */
	public static final String C_OBJMETHODS = P_RUNTIME + "/ObjectMethods";
	/**
	 * The class FQCN for Bootstraps.
	 */
	public static final String C_BOOTSTRAPS = P_RTUTILS + "/Bootstraps";
	
	// Argument sequences
	
	/**
	 * The arguments MethodHandles.Lookup, String, MethodType in sequence. These are the first three arguments to most <code>invokedynamic</code>
	 * bootstrap methods.
	 */
	public static final String A_INDY = D_LOOKUP + D_STRING + D_MTYPE;
	/**
	 * The arguments MethodHandles.Lookup, String, Class in sequence. These are the first three arguments to most dynamic constant
	 * bootstrap methods.
	 */
	public static final String A_CONDY = D_LOOKUP + D_STRING + D_CLASS;
	/**
	 * The arguments MethodHandles.Lookup, String, java/lang/invoke/TypeDescriptor in sequence.
	 */
	public static final String A_DESC = D_LOOKUP + D_STRING + "L" + P_INVOKE + "/TypeDescriptor;";
	
	// Method descriptors
	
	/**
	 * The method descriptor for an <code>invokedynamic</code> bootstrap method returning a CallSite and taking no additional static arguments.
	 */
	public static final String M_INDY_NOARG = "(" + A_INDY + ")" + D_CALLSITE;
	/**
	 * The method descriptor for an <code>invokedynamic</code> bootstrap method returning a CallSite and taking an Object array as
	 * additional static arguments.
	 */
	public static final String M_INDY_OBJARRAY = "(" + A_INDY + "[" + D_OBJECT + ")" + D_CALLSITE;
	/**
	 * The method descriptor for an <code>invokedynamic</code> bootstrap method returning a CallSite and taking a method handle as the sole
	 * additional static argument.
	 */
	public static final String M_INDY_MHANDLE = "(" + A_INDY + D_MHANDLE + ")" + D_CALLSITE;
	/**
	 * The method descriptor for an <code>invokedynamic</code> bootstrap method returning a CallSite and taking a method type as the sole
	 * additional static argument.
	 */
	public static final String M_INDY_MTYPE = "(" + A_INDY + D_MTYPE + ")" + D_CALLSITE;
	
	// Bootstrap methods
	
	/**
	 * The bootstrap method CallSite LambdaMetafactory.metafactory(MethodHandles.Lookup, String, MethodType, MethodType, MethodHandle, MethodType).
	 */
	public static final String B_LAMBDAMF = "invokestatic " + C_LAMBDAMF + ".metafactory:(" + A_INDY + D_MTYPE + D_MHANDLE + D_MTYPE + ")" + D_CALLSITE;
	/**
	 * The bootstrap method CallSite LambdaMetafactory.altMetafactory(MethodHandles.Lookup, String, MethodType, Object...).
	 */
	public static final String B_LAMBDAMF_ALT = "invokestatic " + C_LAMBDAMF + ".altMetafactory:(" + A_INDY + "[" + D_OBJECT + ")" + D_CALLSITE;
	/**
	 * The bootstrap method Object ObjectMethods.bootstrap(MethodHandles.Lookup, String, TypeDescriptor, Class, String, MethodHandle...).
	 */
	public static final String B_OBJMTH_BOOTSTRAP = "invokestatic " + C_OBJMETHODS + ".bootstrap:(" + A_DESC + D_CLASS + D_STRING + "[" + D_MHANDLE + ")" + D_OBJECT;
	/**
	 * The bootstrap method for {@link Bootstraps#duckTypeCall(java.lang.invoke.MethodHandles.Lookup, String, java.lang.invoke.MethodType)}.
	 */
	public static final String B_DUCKTYPE_CALL = "invokestatic " + C_BOOTSTRAPS + ".duckTypeCall:(" + A_INDY + ")" + D_CALLSITE;
	/**
	 * The bootstrap method for {@link Bootstraps#duckTypeCall(java.lang.invoke.MethodHandles.Lookup, String, java.lang.invoke.MethodType, int)}.
	 */
	public static final String B_DUCKTYPE_CALL_POLYMORPHIC = "invokestatic " + C_BOOTSTRAPS + ".duckTypeCall:(" + A_INDY + "I)" + D_CALLSITE;
	/**
	 * The bootstrap method for {@link Bootstraps#lookupAndInvoke(java.lang.invoke.MethodHandles.Lookup, String, java.lang.invoke.MethodType, java.lang.invoke.MethodHandle)}.
	 */
	public static final String B_LOOKUP_AND_INVOKE = "invokestatic " + C_BOOTSTRAPS + ".lookupAndInvoke:" + M_INDY_MHANDLE;
	/**
	 * The bootstrap method for {@link Bootstraps#lookupAndInvokeExact(java.lang.invoke.MethodHandles.Lookup, String, java.lang.invoke.MethodType, java.lang.invoke.MethodHandle)}.
	 */
	public static final String B_LOOKUP_AND_INVOKEEXACT = "invokestatic " + C_BOOTSTRAPS + ".lookupAndInvokeExact:" + M_INDY_MHANDLE;
	
	// Method handles
}

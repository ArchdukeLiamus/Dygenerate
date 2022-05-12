package me.archdukeliamus.dygenerate;

/**
 * A class containing various surrogate methods as well as useful constant strings for use in bootstrap data
 * construction. Some of the surrogate methods in this class are special and have unique behavior when called.
 * <br><br>
 * Constant strings are organised as follows:
 * <br>
 * <table class="plain">
 * 	<caption>String Organisation</caption>
 * 	<tr><td>D</td><td>Descriptors, both for use as field descriptors or as arguments or return types in method descriptors.</td></tr>
 * 	<tr><td>P</td><td>Package FQCNs.</td></tr>
 *  <tr><td>C</td><td>Class FQCNs.</td></tr>
 *  <tr><td>A</td><td>Argument sequences, such as the first three arguments of most <code>invokedynamic</code> bootstrap methods.</td></tr>
 *  <tr><td>M</td><td>Method descriptors</td></tr>
 *  <tr><td>B</td><td>Full bootstrap methods, including invocation type. Static arguments may be placed afterward.</td></tr>
 * </table>
 */
public final class Surrogates {

	private Surrogates() {}
	
	// Descriptors
	
	public static final String D_STRING = "Ljava/lang/String;";
	public static final String D_OBJECT = "Ljava/lang/Object;";
	public static final String D_CLASS = "Ljava/lang/Class;";
	public static final String D_MHANDLE = "Ljava/lang/invoke/MethodHandle;";
	public static final String D_LOOKUP = "Ljava/lang/invoke/MethodHandles$Lookup;";
	public static final String D_MTYPE = "Ljava/lang/invoke/MethodType;";
	public static final String D_CALLSITE = "Ljava/lang/invoke/CallSite;";
	
	// Packages
	
	public static final String P_LANG = "java/lang";
	public static final String P_INVOKE = "java/lang/invoke";
	public static final String P_RUNTIME = "java/lang/runtime";
	public static final String P_DYGENERATE = "me/archdukeliamus/dygenerate";
	
	// Classes
	
	public static final String C_MHANDLES = P_INVOKE + "/MethodHandles";
	public static final String C_CBOOTSTRAPS = P_INVOKE + "/ConstantBootstraps";
	public static final String C_LAMBDAMF = P_INVOKE + "/LambdaMetafactory";
	public static final String C_OBJMETHODS = P_RUNTIME + "/ObjectMethods";
	
	// Argument sequences
	
	public static final String A_INDY = D_LOOKUP + D_STRING + D_MTYPE;
	public static final String A_CONDY = D_LOOKUP + D_STRING + D_CLASS;
	public static final String A_DESC = D_LOOKUP + D_STRING + "L" + P_INVOKE + "/TypeDescriptor;";
	
	// Method descriptors
	
	public static final String M_INDY_NOARG = "(" + A_INDY + ")" + D_CALLSITE;
	
	// Bootstrap methods
	
	public static final String B_LAMBDAMF = "invokestatic " + C_LAMBDAMF + ".metafactory:(" + A_INDY + D_MTYPE + D_MHANDLE + D_MTYPE + ")" + D_CALLSITE;
	public static final String B_LAMBDAMF_ALT = "invokestatic " + C_LAMBDAMF + ".altMetafactory:(" + A_INDY + "[" + D_OBJECT + ")" + D_CALLSITE;
	public static final String B_OBJMTH_BOOTSTRAP = "invokestatic " + C_OBJMETHODS + ".bootstrap:(" + A_DESC + D_CLASS + D_STRING + "[" + D_MHANDLE + ")" + D_OBJECT;
}

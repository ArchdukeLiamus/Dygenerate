package me.archdukeliamus.dygenerate;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * Find all methods marked with indy or condy annotations and extract their annotation data
 * @author archd
 *
 */
final class SurrogateMethodClassVisitor extends ClassVisitor {
	private Map<Surrogate,BootstrapData> surrogates;
	
	SurrogateMethodClassVisitor(int api) {
		super(api);
		surrogates = new HashMap<>();
	}
	
	/**
	 * Get the surrogate map
	 * @return
	 */
	Map<Surrogate,BootstrapData> getSurrogates() {
		return surrogates;
	}
	
	/**
	 * Visit a method and hope it's annotated.
	 */
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new SurrogateMethodMethodVisitor(api, this, access, name, descriptor);
	}
}

/**
 * Method visitor.
 * @author archd
 *
 */
final class SurrogateMethodMethodVisitor extends MethodVisitor {
	private final SurrogateMethodClassVisitor parent;
	// info about the current method
	private final int access;
	private final String name;
	private final String descriptor;
	
	SurrogateMethodMethodVisitor(int api, SurrogateMethodClassVisitor parent, int access, String name, String descriptor) {
		super(api);
		this.parent = parent;
		this.access = access;
		this.name = name;
		this.descriptor = descriptor;
	}
	
	/**
	 * Visit an annotation block, but only if it is indy or condy.
	 */
	@Override
	public AnnotationVisitor visitAnnotation(String annoDescriptor, boolean visible) {
		// check for indy
		if (annoDescriptor.equals("Lme/archdukeliamus/dygenerate/InvokeDynamic;")) {
			// create anno visitor and pass method data down
			return new SurrogateMethodIndyAnnotationVisitor(api, parent, access, name, descriptor);
		} else {
			// signal don't care
			return null;
		}
	}
}

/**
 * Visitor for an annotation.
 * @author archd
 *
 */
final class SurrogateMethodIndyAnnotationVisitor extends AnnotationVisitor {
	private final SurrogateMethodClassVisitor parent;
	// info about the current method... again
	private final int methodAccess;
	private final String methodName;
	private final String methodDescriptor;
	
	SurrogateMethodIndyAnnotationVisitor(int api, SurrogateMethodClassVisitor parent, int access, String name, String descriptor) {
		super(api);
		this.parent = parent;
		this.methodAccess = access;
		this.methodName = name;
		this.methodDescriptor = descriptor;
	}
	
	/**
	 * Get the value
	 */
	@Override
	public void visit(String name, Object value) {
		if (name.equals("value") && value instanceof String) {
			// it's the value, so let's finally upcall to add surrogate info
			// needs check for nonstatic call
			String payload = (String) value;
			parent.getSurrogates().put(new Surrogate(methodName,methodDescriptor), new BootstrapData(BootstrapType.INVOKEDYNAMIC, payload));
			System.out.println("Got surrogate " + methodName + ":" + methodDescriptor + " acc: " + methodAccess + " bs: " + payload);
		}
	}
}

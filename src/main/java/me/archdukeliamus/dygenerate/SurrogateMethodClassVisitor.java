package me.archdukeliamus.dygenerate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Find all methods marked with indy or condy annotations and extract their annotation data
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
			return new SurrogateMethodAnnotationVisitor(api, parent, access, name, descriptor, BootstrapType.INVOKEDYNAMIC);
		} else if (annoDescriptor.equals("Lme/archdukeliamus/dygenerate/ConstantDynamic;")) {
			// run checks to see if condy could actually apply here
			// method must have no args + static
			if ((access & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC) {
				throw new ClassTransformException("method " + name + ":" + descriptor + " must be static for type ConstantDynamic");
			}
			if (Type.getMethodType(descriptor).getArgumentTypes().length > 0) {
				throw new ClassTransformException("method " + name + ":" + descriptor + " must not have arguments for type ConstantDynamic");
			}
			// create anno visitor
			return new SurrogateMethodAnnotationVisitor(api, parent, access, name, descriptor, BootstrapType.CONSTANTDYNAMIC);
		} else {
			// signal don't care
			return null;
		}
	}
}

/**
 * Visitor for an annotation.
 *
 */
final class SurrogateMethodAnnotationVisitor extends AnnotationVisitor {
	private final SurrogateMethodClassVisitor parent;
	// info about the current method... again
	private final int methodAccess;
	private final String methodName;
	private final String methodDescriptor;
	// which bootstrap type to create
	private final BootstrapType type;
	
	SurrogateMethodAnnotationVisitor(int api, SurrogateMethodClassVisitor parent, int access, String name, String descriptor, BootstrapType annoType) {
		super(api);
		this.parent = parent;
		this.methodAccess = access;
		this.methodName = name;
		this.methodDescriptor = descriptor;
		this.type = annoType;
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
			// Parse the data
			BootstrapData bootstrapData;
			{
				Tokeniser tokeniser = new Tokeniser(payload);
				tokeniser.tokenise(); // throws
				List<Token> tokens = tokeniser.getOutput();
				Parser parser = new Parser(tokens);
				bootstrapData = parser.parseBootstrapData(type); // throws
			}
			parent.getSurrogates().put(new Surrogate(methodName,methodDescriptor), bootstrapData);
		}
	}
}

package me.archdukeliamus.dygenerate;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.ConstantDynamic;

final class DynamicTransformClassVisitor extends ClassVisitor {
	private final Map<Surrogate,BootstrapData> replacements;
	private String classFQCN; // needed to replace only surrogate methods
	
	DynamicTransformClassVisitor(int api, Map<Surrogate,BootstrapData> replacements) {
		super(api);
		this.replacements = replacements;
	}

	DynamicTransformClassVisitor(int api, ClassVisitor classVisitor, Map<Surrogate,BootstrapData> replacements) {
		super(api, classVisitor);
		this.replacements = replacements;
	}
	
	/**
	 * Get the detected class FQCN.
	 * @return
	 */
	String getClassFQCN() {
		return classFQCN;
	}
	
	/**
	 * Get the surrogate map
	 * @return
	 */
	Map<Surrogate, BootstrapData> getReplacements() {
		return replacements;
	}

	/**
	 * Visit class, get the FQCN used.
	 */
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.classFQCN = name;
		if (cv != null) cv.visit(version, access, name, signature, superName, interfaces);
	}
	
	/**
	 * Visit method, either remove or transform.
	 */
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		// If the method is a surrogate it should be removed.
		// Otherwise the method should have it's code transformed to replace all calls to surrogates with their proper bytecodes.
		BootstrapData bootstrapData = replacements.get(new Surrogate(name, descriptor));
		if (bootstrapData == null) {
			// Method is not a surrogate. Return a method visitor.
			// Determine the delegate method visitor, if it exists
			MethodVisitor delegate = null;
			if (cv != null) {
				delegate = cv.visitMethod(access, name, descriptor, signature, exceptions);
			}
			return new DynamicTransformMethodVisitor(api, delegate, this);
		}
		// otherwise method is a surrogate and should promptly stop existing
		return null;
	}
}

final class DynamicTransformMethodVisitor extends MethodVisitor {
	private final DynamicTransformClassVisitor parent;

	DynamicTransformMethodVisitor(int api, DynamicTransformClassVisitor parent) {
		super(api);
		this.parent = parent;
	}
	
	DynamicTransformMethodVisitor(int api, MethodVisitor mv, DynamicTransformClassVisitor parent) {
		super(api, mv);
		this.parent = parent;
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
		// Method is in this class, it might be a bootstrap
		if (owner.equals(parent.getClassFQCN())) {
			// try to get bootstrap data
			BootstrapData bootstrapData = parent.getReplacements().get(new Surrogate(name,descriptor));
			if (bootstrapData != null) {
				// we got data, substitute appropriately
				if (bootstrapData.getType() == BootstrapType.INVOKEDYNAMIC) {
					// Replace with invokedynamic opcode
					// Does this method invoke with `this`?
					// Opcodes invokespecial, invokevirtual
					// invokestatic does not, invokeinterface should not possibly appear
					// If it does invoke with this, the invoking type descriptor must be modified to include this as argument 0
					boolean invokesWithThis = opcode == Opcodes.INVOKESPECIAL || opcode == Opcodes.INVOKEVIRTUAL;
					if (!invokesWithThis) {
						// Static. No adapting is needed.
						if (mv != null) mv.visitInvokeDynamicInsn(name, descriptor, bootstrapData.getBootstrapHandle(), bootstrapData.getBootstrapArgs());
					} else {
						// This. The invoking descriptor must be fixed.
						// Add one argument at position 0 for the class type for this, shifting all others to the right.
						Type invokingDescriptor = Type.getMethodType(descriptor);
						Type[] invokingArgs = invokingDescriptor.getArgumentTypes();
						Type[] fixedArgs = new Type[invokingArgs.length + 1];
						Type thisClass = Type.getType(parent.getClassFQCN());
						fixedArgs[0] = thisClass;
						System.arraycopy(invokingArgs, 0, fixedArgs, 1, invokingArgs.length);
						String fixedDescriptor = Type.getMethodDescriptor(invokingDescriptor.getReturnType(), fixedArgs);
						if (mv != null) mv.visitInvokeDynamicInsn(name, fixedDescriptor, bootstrapData.getBootstrapHandle(), bootstrapData.getBootstrapArgs());
					}
					
				} else if (bootstrapData.getType() == BootstrapType.CONSTANTDYNAMIC) {
					// Replace with the appropriate ldc
					// Fix the method descriptor return type into a field
					String fixedDescriptor = Type.getMethodType(descriptor).getReturnType().getDescriptor();
					if (mv != null) mv.visitLdcInsn(new ConstantDynamic(name, fixedDescriptor, bootstrapData.getBootstrapHandle(), bootstrapData.getBootstrapArgs()));
				}
			} else {
				// in our class but not a surrogate, don't change
				if (mv != null) mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
			}
		} else {
			// not in our class, don't change
			if (mv != null) mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
	}
}

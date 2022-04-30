package me.archdukeliamus.dygenerate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class Main {
	public static void main(String[] args) throws IOException {
		System.out.println("Dygenerate v0.0.1 by Archduke Liamus");
		if (args.length == 0) {
			usage();
			String placeholder = "/me/archdukeliamus/dygenerate/Main";
			byte[] bytecode = Main.class.getResourceAsStream(placeholder + ".class").readAllBytes();
			byte[] output = transformBytecodes(bytecode);
			Files.write(Paths.get("E:/KittenLang/indy.class"), output);
		} else {
			testSurrogate();
//			String placeholder = "./me/archdukeliamus/dygenerate/Main";
//			byte[] bytecode = Main.class.getResourceAsStream(placeholder + ".class").readAllBytes();
//			transformBytecodes(bytecode);
		}
	}
	
	private void a() {
		surrogate2();
	}
	
	private static void usage() {
		System.out.println("Usage: <classfile/bindir> ...");
	}
	
	/**
	 * Transform the provided bytecode, replacing invokedynamic surrogate methods with invokedynamic instructions, removing the surrogates and their annotations.
	 * @param classBytecode the bytecode to transform
	 * @return the transformed bytecode
	 */
	public static byte[] transformBytecodes(byte[] classBytecode) {
		// PARSE PHASE - find all methods marked indy and get their bootstrap data
		SurrogateMethodClassVisitor smcv = new SurrogateMethodClassVisitor(Opcodes.ASM9);
		{
			ClassReader reader = new ClassReader(classBytecode);
			reader.accept(smcv, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		}
		// PATCH PHASE - find all calls to indy surrogates and patch them with indy itself
		ClassWriter cw = new ClassWriter(0);
		{
			DynamicTransformClassVisitor dtcv = new DynamicTransformClassVisitor(Opcodes.ASM9, cw, smcv.getSurrogates());
			ClassReader reader = new ClassReader(classBytecode);
			reader.accept(dtcv,0);
		}
		return cw.toByteArray();
	}
	
	@InvokeDynamic("bootstrap data here")
	private static void testSurrogate() {
		throw new RuntimeException("should not exist");
	}
	
	@InvokeDynamic("meme")
	private String surrogate2() {
		throw new RuntimeException("should not exist");
	}
}

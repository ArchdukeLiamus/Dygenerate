package me.archdukeliamus.dygenerate;

import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class Main {
	public static void main(String[] args) throws IOException {
		System.out.println("Dygenerate v0.0.1");
		if (args.length == 0) {
			usage();
		} else {
			commandLineTransform(args);
		}
	}
	
	private static void commandLineTransform(String... args) {
		for (String arg : args) {
			Path path;
			try {
				path = Paths.get(arg);
			} catch (InvalidPathException ex) {
				System.out.println(arg + ": invalid path");
				continue;
			}
			
			if (Files.isDirectory(path)) {
				transformDirectory(path);
			} else {
				transformFile(path);
			}
		}
	}
	
	private static void transformFile(Path path) {
		if (!Files.isReadable(path)) {
			System.out.println(path + ": not readable");
			return;
		}
		byte[] bytecode;
		try {
			bytecode = Files.readAllBytes(path);
		} catch (IOException ex) {
			System.out.println(path + ": I/O problem on read");
			return;
		}
		try {
			bytecode = transformBytecodes(bytecode);
		} catch (ClassTransformException ex) {
			System.out.println(path + ": problem transforming bytecode: " + ex.getMessage());
			ex.printStackTrace();
			return;
		}
		try {
			Files.write(path, bytecode);
		} catch (IOException ex) {
			System.out.println(path + ": I/O problem on write");
			return;
		}
		System.out.println("transformed " + path);
	}
	
	private static void transformDirectory(Path path) {
		DirectoryStream<Path> dstream;
		try {
			 dstream = Files.newDirectoryStream(path);
		} catch (IOException ex) {
			System.out.println(path + ": cannot open directory");
			return;
		}
		for (Path subpath : dstream) {
			if (Files.isDirectory(subpath)) {
				transformDirectory(subpath);
			} else if (subpath.getFileName().toString().endsWith(".class")) {
				transformFile(subpath);
			}
		}
		try {
			dstream.close();
		} catch (IOException ex) {
			System.out.println(path + ": cannot close directory?");
			return;
		}
		System.out.println("transformed directory " + path);
	}
	
	private static void usage() {
		System.out.println("Usage: <classfile/bindir> ...");
	}
	
	/**
	 * Transform the provided bytecode, replacing invokedynamic surrogate methods with invokedynamic instructions, removing the surrogates and their annotations.
	 * @param classBytecode the bytecode to transform
	 * @throws ClassTransformException if there is a problem transforming the class
	 * @return the transformed bytecode
	 */
	public static byte[] transformBytecodes(byte[] classBytecode) {
		try {
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
		} catch (ClassTransformException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new ClassTransformException(ex.getMessage(),ex);
		}
	}
}

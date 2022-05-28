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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * Main entry point into Dygenerate and its API. The easiest API method to use is {@link #transformBytecodes(byte[])}. If more advanced
 * transformation behaviour is needed, other methods in this class may be used. These methods allow generation of surrogate data and patching
 * to be performed seperately.
 *
 */
public class Dygenerate {
	
	/**
	 * No instantiation for you.
	 */
	private Dygenerate() {}
	
	/**
	 * Command-line invocation.
	 * @param args Command-line aeguments.
	 */
	public static void main(String[] args) {
		System.out.println("Dygenerate v0.1.0");
		if (args.length == 0) {
			usage();
		} else {
			commandLineTransform(args);
		}
	}
	
	private static void commandLineTransform(String... args) {
		boolean errors = false;
		for (String arg : args) {
			Path path;
			try {
				path = Paths.get(arg);
			} catch (InvalidPathException ex) {
				System.out.println(arg + ": invalid path");
				continue;
			}
			
			if (Files.isDirectory(path)) {
				errors = errors | transformDirectory(path);
			} else {
				errors = errors | transformFile(path);
			}
		}
		if (errors) {
			System.out.println("Processing finished with errors");
			System.exit(1);
		} else {
			System.exit(0);
		}
	}
	
	private static boolean transformFile(Path path) {
		if (!Files.isReadable(path)) {
			System.out.println(path + ": not readable");
			return true;
		}
		byte[] bytecode;
		try {
			bytecode = Files.readAllBytes(path);
		} catch (IOException ex) {
			System.out.println(path + ": I/O problem on read");
			return true;
		}
		try {
			bytecode = transformBytecodes(bytecode);
		} catch (ClassTransformException ex) {
			System.out.println(path + ": problem transforming bytecode: " + ex.getMessage());
			ex.printStackTrace();
			return true;
		}
		try {
			Files.write(path, bytecode);
		} catch (IOException ex) {
			System.out.println(path + ": I/O problem on write");
			return true;
		}
		System.out.println("transformed " + path);
		return false;
	}
	
	private static boolean transformDirectory(Path path) {
		boolean errors = false;
		DirectoryStream<Path> dstream;
		try {
			 dstream = Files.newDirectoryStream(path);
		} catch (IOException ex) {
			System.out.println(path + ": cannot open directory");
			return true;
		}
		for (Path subpath : dstream) {
			if (Files.isDirectory(subpath)) {
				errors = errors | transformDirectory(subpath);
			} else if (subpath.getFileName().toString().endsWith(".class")) {
				errors = errors | transformFile(subpath);
			}
		}
		try {
			dstream.close();
		} catch (IOException ex) {
			System.out.println(path + ": cannot close directory?");
			return true;
		}
		System.out.println("transformed directory " + path);
		return errors;
	}
	
	private static void usage() {
		System.out.println("Usage: <classfiles/dirs> ...");
	}
	
	/**
	 * Transform the provided bytecode, replacing invokedynamic surrogate methods with invokedynamic instructions, removing the
	 * surrogates and their annotations.
	 * @param classBytecode the bytecode to transform
	 * @throws ClassTransformException if there is a problem transforming the class
	 * @return the transformed bytecode
	 */
	public static byte[] transformBytecodes(byte[] classBytecode) {
		Objects.requireNonNull(classBytecode, "bytecode");
		try {
			Map<Surrogate,BootstrapData> surrogateMap = new HashMap<>();
			// PARSE PHASE - find all methods marked indy and get their bootstrap data
			SurrogateMethodClassVisitor smcv = new SurrogateMethodClassVisitor(Opcodes.ASM9, surrogateMap);
			{
				ClassReader reader = new ClassReader(classBytecode);
				reader.accept(smcv, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
			}
			// PATCH PHASE - find all calls to indy surrogates and patch them with indy itself
			ClassWriter cw = new ClassWriter(0);
			{
				DynamicTransformClassVisitor dtcv = new DynamicTransformClassVisitor(Opcodes.ASM9, cw, surrogateMap);
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
	
	/**
	 * Search for surrogate methods and their annotations in the provided class, placing the data into the provided map.
	 * If an exception is thrown, the surrogate map may be incomplete and might not contain all surrogates found in the class.
	 * If the input is not controlled, creating a new map and copying the data on completion may be preferable.
	 * @param classBytecode the bytecode to process
	 * @param surrogateMap the map that will hold found surrogate data.
	 * @throws InvalidBootstrapDataException if bootstrap data in any surrogate is found to be invalid
	 * @throws ClassTransformException if a condy surrogate method has an invalid signature
	 */
	public static void findSurrogateMethods(byte[] classBytecode, Map<Surrogate,BootstrapData> surrogateMap) {
		Objects.requireNonNull(classBytecode, "bytecode");
		Objects.requireNonNull(surrogateMap, "surrogate map");
		try {
			SurrogateMethodClassVisitor smcv = new SurrogateMethodClassVisitor(Opcodes.ASM9, surrogateMap);
			ClassReader reader = new ClassReader(classBytecode);
			reader.accept(smcv, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		} catch (ClassTransformException ex) {
			throw ex;
		} catch (InvalidBootstrapDataException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new ClassTransformException(ex.getMessage(),ex);
		}
	}
	
	/**
	 * Patch references to surrogate methods in the provided class using the provided surrogate map, removing any surrogate
	 * methods present and substituting in invokedynamic and ldc instructions in the place of surrogate method invocations.
	 * The surrogate map is not modified.
	 * @param classBytecode the class to patch
	 * @param surrogateMap the surrogate map containing data about surrogate methods to replace
	 * @return the transformed bytecode
	 * @throws ClassTransformException if there is a problem transforming the class
	 */
	public static byte[] patchSurrogateReferences(byte[] classBytecode, Map<Surrogate,BootstrapData> surrogateMap) {
		Objects.requireNonNull(classBytecode, "bytecode");
		Objects.requireNonNull(surrogateMap, "surrogate map");
		try {
			ClassWriter cw = new ClassWriter(0);
			DynamicTransformClassVisitor dtcv = new DynamicTransformClassVisitor(Opcodes.ASM9, cw, surrogateMap);
			ClassReader reader = new ClassReader(classBytecode);
			reader.accept(dtcv,0);
			return cw.toByteArray();
		} catch (ClassTransformException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new ClassTransformException(ex.getMessage(),ex);
		}
	}
}

package me.archdukeliamus.dygenerate;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Parser for bootstrap data descriptions.
 *
 */
class Parser {
	private final List<Token> tokenList;
	private int index;
	
	Parser(List<Token> tokenList) {
		this.tokenList = tokenList;
		index = 0;
	}
	
	// assist stuff
	
	Token tokenHere() {
		return tokenList.get(index);
	}
	
	boolean remaining() {
		return index < tokenList.size();
	}
	
	void advance() {
		if (!remaining()) {
			emitError("Unexpected end of input");
			return;
		}
		index++;
	}
	
	boolean advanceIf(boolean advance) {
		if (advance) {
			advance();
		}
		return advance;
	}
	
	boolean tokenHereIs(TokenType type) {
		if (!remaining()) return false;
		return tokenHere().getType() == type;
	}
	
	boolean tokenHereIsAnyOf(TokenType... types) {
		if (!remaining()) return false;
		for (int i = 0; i < types.length; i++) {
			if (tokenHereIs(types[i])) return true;
		}
		return false;
	}
	
	// utils
	
	static String parseStringLiteral(String str) {
		StringBuilder finalValue = new StringBuilder();
		boolean readEscape = false; //A backslash has been read, the next character(s) should be specially handled
		for (int i = 1; i < str.length()-1; i++) {
			char charAtI = str.charAt(i);
			//Escaped mode
			if (readEscape) {
				switch (charAtI) {
					case 'b': finalValue.append('\b'); break;
					case 's': finalValue.append(' '); break;
					case 't': finalValue.append('\t'); break;
					case 'n': finalValue.append('\n'); break;
					case 'f': finalValue.append('\f'); break;
					case 'r': finalValue.append('\r'); break;
					case '"': finalValue.append('"'); break;
					case '\'': finalValue.append('\''); break;
					case '\\': finalValue.append('\\'); break;
					default: finalValue.append('\\').append(charAtI); break;
				}
				readEscape = false;
				continue;
			}
			//Set escape handler if \ is read
			if (charAtI == '\\') {
				readEscape = true;
				continue;
			}
			//Append character otherwise.
			finalValue.append(charAtI);
		}
		return finalValue.toString();
	}
	
	private static Map<TokenType,Integer> handleTypes = initHandleTypes();
	private static Map<TokenType,Integer> initHandleTypes() {
		Map<TokenType,Integer> handleTypes = new EnumMap<>(TokenType.class);
		handleTypes.put(TokenType.KW_GETFIELD, Opcodes.H_GETFIELD);
		handleTypes.put(TokenType.KW_GETSTATIC, Opcodes.H_GETSTATIC);
		handleTypes.put(TokenType.KW_PUTFIELD, Opcodes.H_PUTFIELD);
		handleTypes.put(TokenType.KW_PUTSTATIC, Opcodes.H_PUTSTATIC);
		
		handleTypes.put(TokenType.KW_INVOKEVIRTUAL, Opcodes.H_INVOKEVIRTUAL);
		handleTypes.put(TokenType.KW_INVOKESTATIC, Opcodes.H_INVOKESTATIC);
		handleTypes.put(TokenType.KW_INVOKESPECIAL, Opcodes.H_INVOKESPECIAL);
		handleTypes.put(TokenType.KW_NEWINVOKESPECIAL, Opcodes.H_NEWINVOKESPECIAL);
		handleTypes.put(TokenType.KW_INVOKEINTERFACE, Opcodes.H_INVOKEINTERFACE);
		return handleTypes;
	}
	
	// error handling
	
	void emitError(String message) {
		Token here;
		String exMsg;
		if (!remaining()) {
			// if input is empty there is no token
			here = null;
			exMsg = message + " at end of input";
		} else {
			here = tokenHere();
			exMsg = message + " (char " + here.getIndex() + ", \"" + here.getPayload() + "\")";
		}
		throw new ParserError(exMsg,here);
	}
	
	// actual parsing
	
	BootstrapData parseBootstrapData(BootstrapType type) {
		// parse the handle
		Handle handle = parseHandle();
		// parse args if present
		Object[] args = parseArgs();
		// return bootstrap data
		return new BootstrapData(type, handle, args);
	}
	
	Handle parseHandle() {
		// handle data
		boolean isInterface = false;
		int handleType;
		String ownerFQCN;
		String name;
		String descriptor;
		// invoke opcode
		if (!tokenHereIsAnyOf(TokenType.KW_GETFIELD, TokenType.KW_PUTFIELD, TokenType.KW_GETSTATIC,
				TokenType.KW_PUTSTATIC, TokenType.KW_INVOKEVIRTUAL, TokenType.KW_INVOKESTATIC,
				TokenType.KW_INVOKESPECIAL, TokenType.KW_INVOKEINTERFACE, TokenType.KW_NEWINVOKESPECIAL)) {
			emitError("Expected bootstrap invocation opcode type here");
		}
		handleType = handleTypes.get(tokenHere().getType());
		advance();
		// class or interface keyword (if present)
		boolean interfaceKeyword = advanceIf(tokenHereIs(TokenType.KW_INTERFACE));
		advanceIf(tokenHereIs(TokenType.KW_CLASS)); // also accepts "interface class"
		// set if the owner FQCN is an interface
		isInterface = interfaceKeyword || handleType == Opcodes.H_INVOKEINTERFACE;
		// owner FQCN
		if (!tokenHereIs(TokenType.IDENTIFIER)) emitError("Expected identifier (class FQCN) here");
		ownerFQCN = tokenHere().getPayload();
		advance();
		// dot
		if (!advanceIf(tokenHereIs(TokenType.DOT))) emitError("Expected dot (.) here");
		// method name
		if (!tokenHereIs(TokenType.IDENTIFIER)) emitError("Expected identifier (name) here");
		name = tokenHere().getPayload();
		advance();
		// colon
		if (!advanceIf(tokenHereIs(TokenType.COLON))) emitError("Expected colon (:) here");
		// method descriptor
		if (!tokenHereIs(TokenType.IDENTIFIER)) emitError("Expected identifier (method descriptor) here");
		descriptor = tokenHere().getPayload();
		advance();
		// return handle
		return new Handle(handleType, ownerFQCN, name, descriptor, isInterface);
	}
	
	Object[] parseArgs() {
		List<Object> bsArgs = new ArrayList<>();
		// descriptor args (optional)
		if (tokenHereIs(TokenType.LEFT_BRACKET)) {
			advance();
			while (!tokenHereIs(TokenType.RIGHT_BRACKET)) {
				// Get arg here
				Token here = tokenHere();
				String payload = here.getPayload();
				// what type is it? extract the arg and add it
				switch (here.getType()) {
					case INT_LITERAL:
						bsArgs.add(parseInt(payload));
						advance();
						break;
					case LONG_LITERAL:
						bsArgs.add(parseLong(payload));
						advance();
						break;
					case FLOAT_LITERAL:
						bsArgs.add(parseFloat(payload));
						advance();
						break;
					case DOUBLE_LITERAL:
						bsArgs.add(parseDouble(payload));
						advance();
						break;
					case STRING_LITERAL:
						bsArgs.add(parseStringLiteral(payload));
						advance();
						break;
					case KW_CLASS:
						advance(); // advance past class
						if (!tokenHereIs(TokenType.IDENTIFIER)) emitError("Expected identifier (class FQCN) here");
						Type ctype = parseFQCN(tokenHere().getPayload());
						bsArgs.add(ctype);
						advance();
						break;
					case KW_METHODTYPE:
						advance(); // advance past class
						if (!tokenHereIs(TokenType.IDENTIFIER)) emitError("Expected identifier (method descriptor) here");
						Type mtype = parseMethodType(tokenHere().getPayload());
						bsArgs.add(mtype);
						advance();
						break;
					case KW_HANDLE:
						advance();
						Handle argHandle = parseHandle();
						bsArgs.add(argHandle);
						break;
					case KW_CONDY:
						advance();
						ConstantDynamic condy = parseCondy();
						bsArgs.add(condy);
						break;
					default:
						emitError("Arguments expected here");
						break;
				}
				// advance past a comma, or check that it is an ending bracket, otherwise it is an error
				if (tokenHereIsAnyOf(TokenType.COMMA,TokenType.RIGHT_BRACKET)) {
					advanceIf(tokenHereIs(TokenType.COMMA));
				} else {
					emitError("comma or } expected here");
				}
			}
			advance(); // advance past }
		}
		return bsArgs.toArray();
	}
	
	ConstantDynamic parseCondy() {
		String name;
		String descriptor;
		// method name
		if (!tokenHereIs(TokenType.IDENTIFIER)) emitError("Expected identifier (name) here");
		name = tokenHere().getPayload();
		advance();
		// colon
		if (!advanceIf(tokenHereIs(TokenType.COLON))) emitError("Expected colon (:) here");
		// method descriptor
		if (!tokenHereIs(TokenType.IDENTIFIER)) emitError("Expected identifier (field descriptor) here");
		descriptor = tokenHere().getPayload();
		advance();
		// colon
		if (!advanceIf(tokenHereIs(TokenType.COLON))) emitError("Expected colon (:) here");
		// parse the handle
		Handle handle = parseHandle();
		// parse args if present
		Object[] args = parseArgs();
		// return bootstrap data
		return new ConstantDynamic(name, descriptor, handle, args);
	}
	
	private Integer parseInt(String str) {
		try {
			// Parse the negative sign if present
			String negative = "";
			if (str.startsWith("-")) {
				str = str.substring(1);
				negative = "-";
			}
			// Parse hex
			if (str.startsWith("0x") || str.startsWith("0X")) {
				// hex
				return Integer.parseInt(negative + str.substring(2), 16);
			} else {
				// decimal
				return Integer.parseInt(negative + str, 10);
			}
		} catch (NumberFormatException ex) {
			emitError("Illegal int value: " + str);
			// unreachable
			return null;
		}
	}
	
	private Long parseLong(String str) {
		try {
			// Strip the L
			str = str.substring(0, str.length() - 1);
			// Parse the negative sign if present
			String negative = "";
			if (str.startsWith("-")) {
				str = str.substring(1);
				negative = "-";
			}
			// Parse hex
			if (str.startsWith("0x") || str.startsWith("0X")) {
				// hex
				return Long.parseLong(negative + str.substring(2), 16);
			} else {
				// decimal
				return Long.parseLong(negative + str, 10);
			}
		} catch (NumberFormatException ex) {
			emitError("Illegal long value: " + str);
			// unreachable
			return null;
		}
	}
	
	private Float parseFloat(String str) {
		try {
			return Float.parseFloat(str);
		} catch (NumberFormatException ex) {
			emitError("Illegal float value: " + str);
			// unreachable
			return null;
		}
	}
	
	private Double parseDouble(String str) {
		try {
			return Double.parseDouble(str);
		} catch (NumberFormatException ex) {
			emitError("Illegal double value: " + str);
			// unreachable
			return null;
		}
	}
	
	private Type parseFQCN(String str) {
		try {
			return Type.getObjectType(str);
		} catch (IllegalArgumentException ex) {
			emitError("Illegal class descriptor type: " + str);
			// unreachable
			return null;
		}
	}
	
	private Type parseMethodType(String str) {
		try {
			return Type.getMethodType(str);
		} catch (IllegalArgumentException ex) {
			emitError("Illegal class descriptor type: " + str);
			// unreachable
			return null;
		}
	}
	
	private Type parseFieldType(String str) {
		try {
			return Type.getObjectType(str);
		} catch (IllegalArgumentException ex) {
			emitError("Illegal field descriptor type: " + str);
			// unreachable
			return null;
		}
	}
}

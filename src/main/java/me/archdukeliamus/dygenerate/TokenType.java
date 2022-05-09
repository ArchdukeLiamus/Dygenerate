package me.archdukeliamus.dygenerate;

import java.util.HashMap;
import java.util.Map;


enum TokenType {
	// Whitespace
	WHITESPACE, // all whitespace
	// Identifiers
	IDENTIFIER, // Unlike most identifiers, this includes [ < > / ( ) ; to accommodate JVM type descriptors
	// These also aren't syntactically checked and assumed to be correct until later
	// Literal types
	STRING_LITERAL,
	INT_LITERAL,
	LONG_LITERAL,
	FLOAT_LITERAL,
	DOUBLE_LITERAL,
	// Keywords
	// Types
	KW_CLASS, // class, starts class literals
	KW_METHODTYPE, // methodtype, starts method descriptor literals
	KW_HANDLE, // handle, starts method handle literals
	KW_CONDY, // condy, starts condy literals
	KW_THIS, // used for specifying type of the "this" argument in instance indy surrogates
	KW_INTERFACE, // used for signalling that a method handle owner FQCN refers to an interface
	// Invocation
	KW_GETFIELD,
	KW_GETSTATIC,
	KW_PUTFIELD,
	KW_PUTSTATIC,
	KW_INVOKEVIRTUAL,
	KW_INVOKESTATIC,
	KW_INVOKESPECIAL,
	KW_NEWINVOKESPECIAL,
	KW_INVOKEINTERFACE,
	// Seperators
	DOT, // . (access)
	COMMA, // , (method arg seperator)
	COLON, // : (seperates accessor and method descriptor)
	LEFT_BRACKET, // { (starts args)
	RIGHT_BRACKET, // } (ends args)
	;
	
	private static final Map<String,TokenType> keywordLookup = initKeywords();
	
	private static Map<String,TokenType> initKeywords() {
		HashMap<String,TokenType> map = new HashMap<>();
		// Types
		map.put("class", TokenType.KW_CLASS);
		map.put("methodtype", TokenType.KW_METHODTYPE);
		map.put("handle", TokenType.KW_HANDLE);
		map.put("condy", TokenType.KW_CONDY);
		map.put("this", TokenType.KW_THIS);
		map.put("interface", TokenType.KW_INTERFACE);
		// Handle invoker types
		map.put("getfield", TokenType.KW_GETFIELD);
		map.put("getstatic", TokenType.KW_GETSTATIC);
		map.put("putfield", TokenType.KW_PUTFIELD);
		map.put("putstatic", TokenType.KW_PUTSTATIC);
		map.put("invokevirtual", TokenType.KW_INVOKEVIRTUAL);
		map.put("invokestatic", TokenType.KW_INVOKESTATIC);
		map.put("invokespecial", TokenType.KW_INVOKESPECIAL);
		map.put("newinvokespecial", TokenType.KW_NEWINVOKESPECIAL);
		map.put("invokeinterface", TokenType.KW_INVOKEINTERFACE);
		
		return map;
	}
	
	static TokenType lookupKeywordTokenType(String ident) {
		TokenType type = keywordLookup.get(ident);
		if (type != null) {
			return type;
		}
		return TokenType.IDENTIFIER;
	}
	
	static boolean isWhitespace(TokenType t) {
		return t == WHITESPACE;
	}
}

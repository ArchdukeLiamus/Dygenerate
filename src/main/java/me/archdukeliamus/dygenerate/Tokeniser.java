package me.archdukeliamus.dygenerate;

import java.util.ArrayList;
import java.util.List;

class Tokeniser {
	private final List<Token> out; // the output
	private final String inputString; // the input source code from file
	private final StringBuilder stringBuffer; // temp buffer used to emit payloads
	private int globalCharIndex; // the index into the string
	
	/**
	 * Construct a Tokeniser with the given input.
	 * @param input input to tokenise
	 */
	Tokeniser(String input) {
		if (input == null) {
			throw new NullPointerException("input");
		}
		out = new ArrayList<>();
		inputString = input;
		stringBuffer = new StringBuilder();
		globalCharIndex = 0;
	}
	
	// Generic bits
	
	/**
	 * Character at the tokeniser head
	 * @return
	 */
	private char charHere() {
		return inputString.charAt(globalCharIndex);
	}
	
	/**
	 * Advance i characters. Line sequencing will break if an advance call spans a newline.
	 * Check acceptLineEnding to advance past lines.
	 * @param i
	 */
	private void advance(int i) {
		globalCharIndex += i;
	}
	
	/**
	 * True if characters remain
	 * @return
	 */
	private boolean remaining() {
		return globalCharIndex < inputString.length();
	}
	
	// tryX functions - return true if valid, false if not, but do not advance past input
	
	
	private boolean tryChar(char ch) {
		if (globalCharIndex >= inputString.length()) {
			return false;
		}
		return charHere() == ch;
	}
	
	private void bufferAndAdvance() {
		stringBuffer.append(charHere());
		advance(1);
	}
	
	private boolean bufferAndAdvanceIf(char ch) {
		if (tryChar(ch)) {
			bufferAndAdvance();
			return true;
		}
		return false;
	}
	
	/**
	 * Get contents of string buffer
	 * @return
	 */
	private String getBuffer() {
		return stringBuffer.toString();
	}
	
	/**
	 * Clear buffer
	 */
	private void clearBuffer() {
		stringBuffer.setLength(0);
	}
	
	// Token pushing
	
	/**
	 * Return the tokenised output for parser to use
	 * @return
	 */
	List<Token> getOutput() {
		return out;
	}
	
	// Errors
	
	/**
	 * Emit a tokeniser error
	 * @param message
	 */
	private void emitError(String message) {
		throw new TokeniserError(message + " (char " + globalCharIndex + ")",globalCharIndex);
	}
	
	/**
	 * Push a token with payload
	 * @param type
	 * @param payload
	 */
	void pushToken(TokenType type, String payload) {
		out.add(new Token(type, payload, globalCharIndex));
	}
	
	// Actual tokenising
	
	/**
	 * Tokenise the thing
	 */
	void tokenise() {
		while (remaining()) {
			int ch = charHere();
			// normal handling
			switch (ch) {
				
				// String
				case '"':
					tokeniseString();
					break;
					
				// Seperators
				case '.':
					pushToken(TokenType.DOT, ".");
					advance(1);
					break;
				case ',':
					pushToken(TokenType.COMMA, ",");
					advance(1);
					break;
				case ':':
					pushToken(TokenType.COLON, ":");
					advance(1);
					break;
				case '{':
					pushToken(TokenType.LEFT_BRACKET, "{");
					advance(1);
					break;
				case '}':
					pushToken(TokenType.RIGHT_BRACKET, "}");
					advance(1);
					break;
					
				case '-':
				case '0': case '1': case '2': case '3': case '4':
				case '5': case '6': case '7': case '8': case '9':
					// decimal numbers + floats
					tokeniseNumber();
					break;
					
				// Identifiers (ASCII)
				case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
				case 'g': case 'h': case 'i': case 'j': case 'k': case 'l':
				case 'm': case 'n': case 'o': case 'p': case 'q': case 'r':
				case 's': case 't': case 'u': case 'v': case 'w': case 'x':
				case 'y': case 'z':
				case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
				case 'G': case 'H': case 'I': case 'J': case 'K': case 'L':
				case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R':
				case 'S': case 'T': case 'U': case 'V': case 'W': case 'X':
				case 'Y': case 'Z':
				case '$': case '_': case '<': case '>': case '/': case '[': case ';':
				case '(': case ')':
					tokeniseIdentifier();
					break;
				// Other cases
				default:
					if (Character.isWhitespace(charHere())) {
						clearBuffer();
						while (remaining() && Character.isWhitespace(charHere())) {
							advance(1);
						}
						break;
					} else if (Character.isJavaIdentifierStart(charHere())) {
						tokeniseIdentifier();
						break;
					}
					emitError("Unexpected char " + ((char) ch));
					return;
			}
		}
	}
	
	private void tokeniseString() {
		// String literal starts
		// Buffer "
		clearBuffer();
		bufferAndAdvance();
		boolean isPreviousCharEscape = false;
		consumedString: {
			// Keep consuming characters until " is hit in an unescaped context
			while (remaining()) {
				int ch = charHere();
				switch (ch) {
					case '"':
						bufferAndAdvance();
						if (isPreviousCharEscape) {
							break; // \"
						} else {
							break consumedString; // "
						}
					case '\r':
					case '\n':
						emitError("raw newline not permitted in string literal");
						return;
					default:
						bufferAndAdvance();
						break;
				}
				// set if the character just read was a \ to affect behavior in the next iteration
				isPreviousCharEscape = ch == '\\';
			}
			// if here, hit end of input but " not found
			emitError("Unexpected end of input");
			return;
		}
		// push output and clear
		pushToken(TokenType.STRING_LITERAL,getBuffer());
		clearBuffer();
	}
	
	private void tokeniseIdentifier() {
		clearBuffer();
		bufferAndAdvance();
		consumeIdentifier: while (remaining()) {
			int ch = charHere();
			switch (ch) {
				case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
				case 'g': case 'h': case 'i': case 'j': case 'k': case 'l':
				case 'm': case 'n': case 'o': case 'p': case 'q': case 'r':
				case 's': case 't': case 'u': case 'v': case 'w': case 'x':
				case 'y': case 'z':
				case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
				case 'G': case 'H': case 'I': case 'J': case 'K': case 'L':
				case 'M': case 'N': case 'O': case 'P': case 'Q': case 'R':
				case 'S': case 'T': case 'U': case 'V': case 'W': case 'X':
				case 'Y': case 'Z':
				case '$': case '_': case '<': case '>': case '/': case '[': case ';':
				case '(': case ')':
				case '0': case '1': case '2': case '3': case '4':
				case '5': case '6': case '7': case '8': case '9':
					bufferAndAdvance();
					break;
				default:
					if (Character.isJavaIdentifierPart(ch)) {
						bufferAndAdvance();
						break;
					}
					break consumeIdentifier;
			}
		}
		String buffer = getBuffer();
		TokenType type = TokenType.lookupKeywordTokenType(buffer);
		pushToken(type,getBuffer());
		clearBuffer();
	}
	
	private void tokeniseNumber() {
		clearBuffer();
		// decimal float / int input.
		// Starting -
		bufferAndAdvanceIf('-');
		// check the next char to see if it is a number
		if (remaining()) {
			int ch = charHere();
			switch (ch) {
				case '0':
					// Zero needs special handling "0x", "0."
					continueTokeniseZeroNumber();
					return;
				case '1': case '2': case '3': case '4':
				case '5': case '6': case '7': case '8': case '9':
					bufferAndAdvance();
					break;
				default:
					emitError("Number expected here");
					return;
			}
		}
		// Float input will switch out to continueTokeniseFloat the moment we think we will have a float instead
		// Buffer more numbers
		loop: while (remaining()) {
			int ch = charHere();
			switch (ch) {
				case '.':
					// We are seeing stuff that makes this number look like a float.
					continueTokeniseFloat();
					return; // continueTokeniseFloat will handle the pushing
				case '0': case '1': case '2': case '3': case '4':
				case '5': case '6': case '7': case '8': case '9':
					bufferAndAdvance();
					break;
				default:
					break loop;
			}
		}
		// check for long signifier
		TokenType type = TokenType.INT_LITERAL;
		if (tryChar('l') || tryChar('L')) {
			bufferAndAdvance();
			type = TokenType.LONG_LITERAL;
		}
		pushToken(type, getBuffer());
		clearBuffer();
	}
	
	private void continueTokeniseFloat() {
		// The current character is kicking off a float literal at . and there are potentially contents in the string buffer.
		bufferAndAdvance(); // buffer the .
		int buffered;
		buffered = bufferNumbersUntilNonNumberChar(); // start buffering numbers or error if we get none
		if (buffered == 0) {
			emitError("Numbers expected for floating-point decimal");
			return;
		}
		// Try for exponent
		if (tryChar('e') || tryChar('E')) {
			bufferAndAdvance();
			// + or - after the E
			if (tryChar('+') || tryChar('-')) {
				bufferAndAdvance();
			}
			
			buffered = bufferNumbersUntilNonNumberChar();
			if (buffered == 0) {
				// should have been at least one number here
				emitError("Numbers expected after floating-point literal exponent");
				return;
			}
		}
		
		// Floating-point type suffix
		TokenType type = TokenType.DOUBLE_LITERAL;
		boolean isFloatSig = tryChar('f') || tryChar('F');
		if (isFloatSig || tryChar('d') || tryChar('D')) {
			if (isFloatSig) {
				type = TokenType.FLOAT_LITERAL;
			}
			bufferAndAdvance();
		}
		
		pushToken(type, getBuffer());
		clearBuffer();
	}
	
	private int bufferNumbersUntilNonNumberChar() {
		int buffered = 0;
		loop: while (remaining()) {
			int ch = charHere();
			switch (ch) {
				case '0': case '1': case '2': case '3': case '4':
				case '5': case '6': case '7': case '8': case '9':
					bufferAndAdvance();
					buffered++;
					break;
				default:
					break loop;
			}
		}
		return buffered;
	}
	
	private void continueTokeniseZeroNumber() {
		// at this point charHere() == '0'
		// and primed to read a hex value
		// a - may have been buffered
		bufferAndAdvanceIf('0');
		if (tryChar('x') || tryChar('X')) {
			// 0x or 0X
			bufferAndAdvance();
		} else if (tryChar('.')) {
			// 0.
			// it's actually a float
			continueTokeniseFloat();
			return;
		} else {
			// it's just the literal zero
			// parse optional long designator
			TokenType type = TokenType.INT_LITERAL;
			if (tryChar('l') || tryChar('L')) {
				bufferAndAdvance();
				type = TokenType.LONG_LITERAL;
			}
			pushToken(type, getBuffer());
			clearBuffer();
			return;
		}
		// hex literal
		int buffered = 0;
		loop: while (remaining()) {
			int ch = charHere();
			switch (ch) {
				case '0': case '1': case '2': case '3': case '4':
				case '5': case '6': case '7': case '8': case '9':
				case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
				case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
					bufferAndAdvance();
					buffered++;
					break;
				default:
					break loop;
			}
		}
		if (buffered == 0) {
			// raise an error: "0x" or "0X" is not valid
			emitError("Hex digits expected after hex literal start");
			return;
		}
		TokenType type = TokenType.INT_LITERAL;
		if (tryChar('l') || tryChar('L')) {
			bufferAndAdvance();
			type = TokenType.LONG_LITERAL;
		}
		pushToken(type, getBuffer());
		clearBuffer();
	}
}
package me.archdukeliamus.dygenerate;

final class Token {
	private final TokenType type;
	private final String payload;
	private final int index;
	
	Token(TokenType type, String payload, int index) {
		this.type = type;
		this.payload = payload;
		this.index = index;
	}
	
	TokenType getType() {
		return type;
	}
	
	String getPayload() {
		return payload;
	}
	
	int getIndex() {
		return index;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(type.toString());
		if (payload != null) {
			sb.append(",");
			sb.append(payload);
		}
		sb.append("]");
		return sb.toString();
	}
}

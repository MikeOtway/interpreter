package token

data class Token(val type: TokenType, val literal: String) {
    constructor(type: TokenType, character: Char) : this(type, character.toString())
}

enum class TokenType(val value: String) {
    ILLEGAL("ILLEGAL"),
    EOF("EOF"),

    // Identifiers + literals
    IDENT("IDENT"),
    INT("INT"),

    // Operators
    ASSIGN("="),
    PLUS("+"),
    MINUS("-"),
    BANG("!"),
    ASTERISK("*"),
    SLASH("/"),

    LT("<"),
    GT(">"),

    EQ("=="),
    NOT_EQ("!="),

    // Delimiters
    COMMA(","),
    SEMICOLON(";"),

    LPAREN("("),
    RPAREN(")"),
    LBRACE("{"),
    RBRACE("}"),

    // Keywords
    FUNCTION("FUNCTION"),
    LET("LET"),
    TRUE("TRUE"),
    FALSE("FALSE"),
    IF("IF"),
    ELSE("ELSE"),
    RETURN("RETURN");

    companion object {
        var KEYWORDS = mapOf(
            "fn" to FUNCTION,
            "let" to LET,
            "true" to TRUE,
            "false" to FALSE,
            "if" to IF,
            "else" to ELSE,
            "return" to RETURN,
        )
        fun lookupIdentifier(identifier: String): TokenType =
            KEYWORDS[identifier] ?: IDENT
    }
}




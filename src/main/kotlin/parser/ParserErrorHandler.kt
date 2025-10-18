package parser

import token.TokenType

class ParserErrorHandler {
    private val errors = mutableListOf<String>()

    fun recordError(message: String) {
        errors.add(message)
    }

    fun recordPeekError(expected: TokenType, actual: TokenType?) {
        errors.add("expected next token to be $expected, got $actual instead")
    }

    fun getErrors(): List<String> = errors
}
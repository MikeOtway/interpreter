package lexer

import token.Token
import token.TokenType
import token.TokenType.Companion.lookupIdentifier

private const val ZERO = Char.MIN_VALUE

data class Lexer(private val input: String) {
    private var position: Int = 0
    private var readPosition: Int = 0
    private var character: Char = ZERO

    init {
        readChar()
    }

    fun nextToken(): Token {
        skipWhitespace()

        val token = when(character) {
            '=' -> handleEquals()
            '!' -> handleBang()
            '+' -> Token(TokenType.PLUS, character)
            '-' -> Token(TokenType.MINUS, character)
            '/' -> Token(TokenType.SLASH, character)
            '*' -> Token(TokenType.ASTERISK, character)
            '<' -> Token(TokenType.LT, character)
            '>' -> Token(TokenType.GT, character)
            ';' -> Token(TokenType.SEMICOLON, character)
            '(' -> Token(TokenType.LPAREN, character)
            ')' -> Token(TokenType.RPAREN, character)
            ',' -> Token(TokenType.COMMA, character)
            '{' -> Token(TokenType.LBRACE, character)
            '}' -> Token(TokenType.RBRACE, character)
            ZERO -> Token(TokenType.EOF, "")
            else -> when {
                isLetter(character) -> return handleIdentifier()
                isDigit(character) -> return handleNumber()
                else -> Token(TokenType.ILLEGAL, character)
            }
        }

        readChar()
        return token
    }

    private fun handleEquals() = if (peakChar() == '=') {
        readChar()
        Token(TokenType.EQ, "==")
    } else {
        Token(TokenType.ASSIGN, "=")
    }

    private fun handleBang() = if (peakChar() == '=') {
        readChar()
        Token(TokenType.NOT_EQ, "!=")
    } else {
        Token(TokenType.BANG, "!")
    }

    private fun handleIdentifier(): Token {
        val literal = readWhile(::isLetter)
        return Token(lookupIdentifier(literal), literal)
    }

    private fun handleNumber(): Token {
        val literal = readWhile(::isDigit)
        return Token(TokenType.INT, literal)
    }

    private fun isDigit(ch: Char) = ch in '0'..'9'
    private fun isLetter(ch: Char) = ch in 'a'..'z' || ch in 'A'..'Z' || ch == '_'

    private fun readWhile(predicate: (Char) -> Boolean): String {
        val startPosition = position
        while (predicate(character)) readChar()
        return input.substring(startPosition, position)
    }

    private fun skipWhitespace() {
        while (character in setOf(' ', '\t', '\n', '\r')) readChar()
    }

    private fun peakChar() = if (readPosition >= input.length) ZERO else input[readPosition]

    private fun readChar() {
        character = if (readPosition >= input.length) ZERO else input[readPosition]
        position = readPosition
        readPosition += 1
    }
}


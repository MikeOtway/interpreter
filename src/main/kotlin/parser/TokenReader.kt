package parser

import lexer.Lexer
import token.Token
import token.TokenType

class TokenReader(private val lexer: Lexer) {
    private var currentToken: Token? = null
    private var peekToken: Token? = null

    init {
        advanceTokens(2)
    }

    fun advanceToken() {
        currentToken = peekToken
        peekToken = lexer.nextToken()
    }

    fun advanceTokens(times: Int) {
        repeat(times) { advanceToken() }
    }

    fun current(): Token = currentToken ?: throw IllegalStateException("No current token")
    fun peek(): Token? = peekToken
    fun isCurrentToken(type: TokenType): Boolean = currentToken?.type == type
    fun isPeekToken(type: TokenType): Boolean = peekToken?.type == type
    fun isAtEnd(): Boolean = currentToken?.type == TokenType.EOF
}
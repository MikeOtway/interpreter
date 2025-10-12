package parser

import ast.Identifier
import ast.LetStatement
import ast.Program
import ast.ReturnStatement
import ast.Statement
import lexer.Lexer
import token.Token
import token.TokenType

class Parser(private val lexer: Lexer) {
    private val errors = mutableListOf<String>()
    private var currentToken: Token? = null
    private var peekToken: Token? = null


    init {
        advanceTokens(2) // Initialise both current and peek tokens
    }

    fun parseProgram(): Program {
        val program = Program()

        while (currentToken?.type != TokenType.EOF) {
            parseStatement()?.let { program.statements.add(it) }
            advanceToken()
        }
        return program
    }

    fun getErrors(): List<String> = errors

    private fun parseStatement(): Statement? {
        return when (currentToken?.type) {
            TokenType.LET -> parseLetStatement()
            TokenType.RETURN -> parseReturnStatement()
            else -> null
        }
    }

    private fun parseLetStatement(): LetStatement? {
        var statement = LetStatement(token = requireCurrentToken())

        if (!expectNextToken(TokenType.IDENT)) return null

        statement = statement.copy(name = Identifier(
            token = requireCurrentToken(),
            value = requireCurrentToken().literal
        ))

        if (!expectNextToken(TokenType.ASSIGN)) return null

        advanceTokenUntil(TokenType.SEMICOLON)

        return statement
    }

    private fun parseReturnStatement(): ReturnStatement {
        val statement = ReturnStatement(token = requireCurrentToken())
        advanceToken()
        advanceTokenUntil(TokenType.SEMICOLON)
        return statement
    }

    private fun advanceToken() {
        currentToken = peekToken
        peekToken = lexer.nextToken()
    }

    private fun advanceTokens(count: Int) {
        repeat(count) { advanceToken() }
    }

    private fun advanceTokenUntil(tokenType: TokenType) {
        while (!isCurrentToken(tokenType)) {
            advanceToken()
        }
    }

    private fun isCurrentToken(tokenType: TokenType): Boolean =
        currentToken?.type == tokenType

    private fun isPeekToken(tokenType: TokenType): Boolean =
        peekToken?.type == tokenType

    private fun expectNextToken(tokenType: TokenType): Boolean {
        if (isPeekToken(tokenType)) {
            advanceToken()
            return true
        }
        recordPeekError(tokenType)
        return false
    }

    private fun requireCurrentToken(): Token =
        currentToken ?: throw IllegalStateException("Current token is null")

    fun recordPeekError(tokenType: TokenType) {
        val message = "expected next token to be $tokenType, got ${peekToken?.type} instead"
        errors.add(message)
    }
}
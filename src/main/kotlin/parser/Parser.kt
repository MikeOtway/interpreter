package parser

import ast.Expression
import ast.ExpressionStatement
import ast.Identifier
import ast.IntegerLiteral
import ast.LetStatement
import ast.Program
import ast.ReturnStatement
import ast.Statement
import lexer.Lexer
import token.Token
import token.TokenType

typealias PrefixParseFn = () -> Expression?
typealias InlineParseFn = (Expression) -> Expression

class Parser(private val lexer: Lexer) {
    private val errors = mutableListOf<String>()
    private var currentToken: Token? = null
    private var peekToken: Token? = null
    private var prefixParseFunctions = mutableMapOf<TokenType, PrefixParseFn>()
    private var infixParseFunctions = mutableMapOf<TokenType, InlineParseFn>()

    init {
        registerPrefix(TokenType.IDENT, ::parseIdentifier)
        registerPrefix(TokenType.INT,  ::parseIntegerLiteral)

        advanceTokens(2) // Initialise both current and peek tokens
    }

    fun registerPrefix(tokenType: TokenType, parseFn: PrefixParseFn) {
        prefixParseFunctions[tokenType] = parseFn
    }

    fun registerInlineParseFunction(tokenType: TokenType, parseFn: InlineParseFn) {
        infixParseFunctions[tokenType] = parseFn
    }

    private fun parseIdentifier(): Expression =
        Identifier(token = requireCurrentToken(), value = requireCurrentToken().literal)

    private fun parseIntegerLiteral(): Expression? {
        val value = requireCurrentToken().literal.toLongOrNull()
        if (value == null) {
            errors.add("could not parse ${currentToken?.literal} as integer")
            return null
        }

        return IntegerLiteral(token = requireCurrentToken(), value = value)
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
            else -> parseExpressionStatement()
        }
    }

    private fun parseLetStatement(): LetStatement? {
        var statement = LetStatement(token = requireCurrentToken())

        if (!expectNextToken(TokenType.IDENT)) return null

        statement = statement.copy(
            name = Identifier(
                token = requireCurrentToken(),
                value = requireCurrentToken().literal
            )
        )

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

    private fun parseExpressionStatement(): ExpressionStatement {
        val statement = ExpressionStatement(
            token = requireCurrentToken(),
            expression = parseExpression(Precedence.LOWEST)
        )
        if (isPeekToken(TokenType.SEMICOLON)) {
            advanceToken()
        }
        return statement
    }

    private fun parseExpression(precedence: Precedence): Expression? {
        val prefix = prefixParseFunctions[currentToken?.type] ?: return null
        return prefix()
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

    enum class Precedence(val value: Int) {
        LOWEST(0),
        EQUALS(1),        // ==
        LESSGREATER(2),   // > or <
        SUM(3),           // +
        PRODUCT(4),       // *
        PREFIX(5),        // -X or !X
        CALL(6)           // myFunction(X)
    }
}
package parser

import ast.Expression
import ast.ExpressionStatement
import ast.Identifier
import ast.InfixExpression
import ast.IntegerLiteral
import ast.LetStatement
import ast.PrefixExpression
import ast.Program
import ast.ReturnStatement
import ast.Statement
import lexer.Lexer
import parser.Parser.Precedence.EQUALS
import parser.Parser.Precedence.LESSGREATER
import parser.Parser.Precedence.LOWEST
import parser.Parser.Precedence.PRODUCT
import parser.Parser.Precedence.SUM
import token.Token
import token.TokenType

typealias PrefixParseFn = () -> Expression?
typealias InlineParseFn = (Expression?) -> Expression

class Parser(private val lexer: Lexer) {
    private val errors = mutableListOf<String>()
    private var currentToken: Token? = null
    private var peekToken: Token? = null
    private var prefixParseFunctions = mutableMapOf<TokenType, PrefixParseFn>()
    private var infixParseFunctions = mutableMapOf<TokenType, InlineParseFn>()

    init {
        registerPrefix(TokenType.IDENT, ::parseIdentifier)
        registerPrefix(TokenType.INT, ::parseIntegerLiteral)
        registerPrefix(TokenType.BANG, ::parsePrefixExpression)
        registerPrefix(TokenType.MINUS, ::parsePrefixExpression)
        registerInfix(TokenType.PLUS, ::parseInfixExpression)
        registerInfix(TokenType.MINUS, ::parseInfixExpression)
        registerInfix(TokenType.SLASH, ::parseInfixExpression)
        registerInfix(TokenType.ASTERISK, ::parseInfixExpression)
        registerInfix(TokenType.EQ, ::parseInfixExpression)
        registerInfix(TokenType.NOT_EQ, ::parseInfixExpression)
        registerInfix(TokenType.LT, ::parseInfixExpression)
        registerInfix(TokenType.GT, ::parseInfixExpression)

        advanceTokens(2) // Initialise both current and peek tokens
    }

    fun registerPrefix(tokenType: TokenType, parseFn: PrefixParseFn) {
        prefixParseFunctions[tokenType] = parseFn
    }

    fun registerInfix(tokenType: TokenType, parseFn: InlineParseFn) {
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
            expression = parseExpression(LOWEST)
        )
        if (isPeekToken(TokenType.SEMICOLON)) {
            advanceToken()
        }
        return statement
    }

    private fun parsePrefixExpression(): Expression {
        val prefixToken = requireCurrentToken()

        advanceToken()

        return PrefixExpression(
            token = prefixToken,
            operator = prefixToken.literal,
            right = parseExpression(Precedence.PREFIX)
        )
    }

    private fun parseInfixExpression(left: Expression?): Expression {
        val infixToken = requireCurrentToken()

        val precedence = currentPrecedence()
        advanceToken()

        return InfixExpression(
            token = infixToken,
            operator = infixToken.literal,
            left = left,
            right = parseExpression(precedence)
        )
    }

    private fun parseExpression(precedence: Precedence): Expression? {
        val prefix = prefixParseFunctions[currentToken?.type]
        if (prefix == null) {
            errors.add("no prefix parse function for ${requireCurrentToken().type} found")
            return null
        }
        var leftExp =  prefix()

        while (!isPeekToken(TokenType.SEMICOLON) && precedence < peekPrecedence()) {
            val infix = infixParseFunctions[peekToken?.type] ?: return leftExp
            advanceToken()
            leftExp = infix(leftExp)
        }

        return leftExp
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

    private fun peekPrecedence(): Precedence = precedences[peekToken?.type] ?: LOWEST
    private fun currentPrecedence(): Precedence = precedences[currentToken?.type] ?: LOWEST

    val precedences = mapOf(
        TokenType.EQ to EQUALS,
        TokenType.NOT_EQ to EQUALS,
        TokenType.LT to LESSGREATER,
        TokenType.GT to LESSGREATER,
        TokenType.PLUS to SUM,
        TokenType.MINUS to SUM,
        TokenType.SLASH to PRODUCT,
        TokenType.ASTERISK to PRODUCT
    )

    enum class Precedence(val value: Int) {
        LOWEST(0),
        EQUALS(1),
        LESSGREATER(2),
        SUM(3),
        PRODUCT(4),
        PREFIX(5),
        CALL(6)
    }
}
package parser

import ast.BlockStatement
import ast.BooleanExpression
import ast.CallExpression
import ast.Expression
import ast.ExpressionStatement
import ast.FunctionLiteral
import ast.Identifier
import ast.IfExpression
import ast.InfixExpression
import ast.IntegerLiteral
import ast.LetStatement
import ast.PrefixExpression
import ast.Program
import ast.ReturnStatement
import ast.Statement
import lexer.Lexer
import parser.Parser.Precedence.CALL
import parser.Parser.Precedence.EQUALS
import parser.Parser.Precedence.LESSGREATER
import parser.Parser.Precedence.LOWEST
import parser.Parser.Precedence.PRODUCT
import parser.Parser.Precedence.SUM
import token.TokenType

class Parser(lexer: Lexer) {
    private val tokenReader = TokenReader(lexer)
    private val registry = ParserRegistry()
    private val errorHandler = ParserErrorHandler()

    init {
        setupParsers()
    }

    private fun setupParsers() {
        registry.apply {
            registerPrefix(TokenType.IDENT, ::parseIdentifier)
            registerPrefix(TokenType.INT, ::parseIntegerLiteral)
            registerPrefix(TokenType.BANG, ::parsePrefixExpression)
            registerPrefix(TokenType.MINUS, ::parsePrefixExpression)
            registerPrefix(TokenType.TRUE, ::parseBoolean)
            registerPrefix(TokenType.FALSE, ::parseBoolean)
            registerPrefix(TokenType.LPAREN, ::parseGroupedExpression)
            registerPrefix(TokenType.IF, ::parseIfExpression)
            registerPrefix(TokenType.FUNCTION, ::parseFunctionLiteral)
            registerInfix(TokenType.LPAREN, ::parseCallExpression)
            registerInfix(TokenType.PLUS, ::parseInfixExpression)
            registerInfix(TokenType.MINUS, ::parseInfixExpression)
            registerInfix(TokenType.SLASH, ::parseInfixExpression)
            registerInfix(TokenType.ASTERISK, ::parseInfixExpression)
            registerInfix(TokenType.EQ, ::parseInfixExpression)
            registerInfix(TokenType.NOT_EQ, ::parseInfixExpression)
            registerInfix(TokenType.LT, ::parseInfixExpression)
            registerInfix(TokenType.GT, ::parseInfixExpression)
        }
    }

    fun parseProgram(): Program {
        val program = Program()

        while (!tokenReader.isAtEnd()) {
            parseStatement()?.let { program.statements.add(it) }
            tokenReader.advanceToken()
        }
        return program
    }

    private fun parseIdentifier(): Expression =
        Identifier(token = tokenReader.current(), value = tokenReader.current().literal)

    private fun parseBoolean(): Expression =
        BooleanExpression(token = tokenReader.current(), value = tokenReader.isCurrentToken(TokenType.TRUE))

    private fun parseIntegerLiteral(): Expression? {
        val currentToken = tokenReader.current()
        val value = currentToken.literal.toLongOrNull()

        if (value == null) {
            errorHandler.recordError("could not parse ${currentToken.literal} as integer")
            return null
        }

        return IntegerLiteral(token = currentToken, value = value)
    }

    private fun parseStatement(): Statement? {
        return when (tokenReader.current().type) {
            TokenType.LET -> parseLetStatement()
            TokenType.RETURN -> parseReturnStatement()
            else -> parseExpressionStatement()
        }
    }

    private fun parseLetStatement(): LetStatement? {
        var statement = LetStatement(token = tokenReader.current())

        if (!expectNextToken(TokenType.IDENT)) return null

        statement = statement.copy(
            name = Identifier(
                token = tokenReader.current(),
                value = tokenReader.current().literal
            )
        )

        if (!expectNextToken(TokenType.ASSIGN)) return null

        tokenReader.advanceToken()

        statement = statement.copy(value = parseExpression(LOWEST))

        if (tokenReader.isPeekToken(TokenType.SEMICOLON)) tokenReader.advanceToken()

        return statement
    }

    private fun parseReturnStatement(): ReturnStatement {
        val currentToken = tokenReader.current()
        tokenReader.advanceToken()

        val statement = ReturnStatement(token = currentToken, returnValue = parseExpression(LOWEST))

        if (tokenReader.isPeekToken(TokenType.SEMICOLON)) tokenReader.advanceToken()

        return statement
    }

    private fun parseExpressionStatement(): ExpressionStatement {
        val statement = ExpressionStatement(
            token = tokenReader.current(),
            expression = parseExpression(LOWEST)
        )
        if (tokenReader.isPeekToken(TokenType.SEMICOLON)) {
            tokenReader.advanceToken()
        }
        return statement
    }

    private fun parsePrefixExpression(): Expression {
        val prefixToken = tokenReader.current()

        tokenReader.advanceToken()

        return PrefixExpression(
            token = prefixToken,
            operator = prefixToken.literal,
            right = parseExpression(Precedence.PREFIX)
        )
    }

    private fun parseInfixExpression(left: Expression?): Expression {
        val infixToken = tokenReader.current()

        val precedence = currentPrecedence()
        tokenReader.advanceToken()

        return InfixExpression(
            token = infixToken,
            operator = infixToken.literal,
            left = left,
            right = parseExpression(precedence)
        )
    }

    private fun parseGroupedExpression(): Expression? {
        tokenReader.advanceToken()

        val expression = parseExpression(LOWEST)
        if (!expectNextToken(TokenType.RPAREN)) return null

        return expression
    }

    private fun parseIfExpression(): Expression? {
        val current = tokenReader.current()

        if (!expectNextToken(TokenType.LPAREN)) return null

        tokenReader.advanceToken()
        val condition = parseExpression(LOWEST)

        if (!expectNextToken(TokenType.RPAREN)) return null
        if (!expectNextToken(TokenType.LBRACE)) return null

        val consequence = parseBlockStatement()

        val alternative = if (tokenReader.isPeekToken(TokenType.ELSE)) {
            tokenReader.advanceToken()
            if (!expectNextToken(TokenType.LBRACE)) return null
            parseBlockStatement()
        } else null

        return IfExpression(
            token = current,
            condition = condition,
            consequence = consequence,
            alternative = alternative
        )
    }

    private fun parseFunctionLiteral(): Expression? {
        val current = tokenReader.current()

        if (!expectNextToken(TokenType.LPAREN)) return null

        val parameters = parseFunctionParameters()

        if (!expectNextToken(TokenType.LBRACE)) return null

        return FunctionLiteral(
            token = current,
            parameters = parameters,
            body = parseBlockStatement()
        )
    }

    private fun parseCallExpression(function: Expression?): Expression {
        return CallExpression(
            token = tokenReader.current(),
            function = function,
            arguments = parseCallArguments()
        )
    }

    private fun parseCallArguments(): List<Expression?>? {
        val arguments = mutableListOf<Expression?>()

        if (tokenReader.isPeekToken(TokenType.RPAREN)) {
            tokenReader.advanceToken()
            return arguments
        }

        tokenReader.advanceToken()
        arguments.add(parseExpression(LOWEST))

        while (tokenReader.isPeekToken(TokenType.COMMA)) {
            tokenReader.advanceTokens(2)
            arguments.add(parseExpression(LOWEST))
        }

        if (!expectNextToken(TokenType.RPAREN)) return null
        return arguments
    }

    private fun parseFunctionParameters(): List<Identifier>? {
        val parameters = mutableListOf<Identifier>()
        if (tokenReader.isPeekToken(TokenType.RPAREN)) {
            tokenReader.advanceToken()
            return parameters
        }

        tokenReader.advanceToken()

        val parameter = Identifier(token = tokenReader.current(), value = tokenReader.current().literal)
        parameters.add(parameter)

        while (tokenReader.isPeekToken(TokenType.COMMA)) {
            tokenReader.advanceTokens(2)
            val parameter = Identifier(token = tokenReader.current(), value = tokenReader.current().literal)
            parameters.add(parameter)
        }

        if (!expectNextToken(TokenType.RPAREN)) return null

        return parameters
    }

    private fun parseBlockStatement(): BlockStatement {
        val current = tokenReader.current()
        val statements = mutableListOf<Statement>()

        tokenReader.advanceToken()

        while (!tokenReader.isCurrentToken(TokenType.RBRACE) && !tokenReader.isCurrentToken(TokenType.EOF)) {
            val statement = parseStatement()
            if (statement != null) statements.add(statement)
            tokenReader.advanceToken()
        }
        return BlockStatement(token = current, statements = statements)
    }

    private fun parseExpression(precedence: Precedence): Expression? {
        val prefix = registry.getPrefixParser(tokenReader.current().type)
        if (prefix == null) {
            errorHandler.recordError("no prefix parse function for ${tokenReader.current().type} found")
            return null
        }
        var leftExp = prefix()

        while (!tokenReader.isPeekToken(TokenType.SEMICOLON) && precedence < peekPrecedence()) {
            val infix = registry.getInfixParser(tokenReader.peek()?.type) ?: return leftExp
            tokenReader.advanceToken()
            leftExp = infix(leftExp)
        }

        return leftExp
    }

    private fun expectNextToken(tokenType: TokenType): Boolean {
        if (tokenReader.isPeekToken(tokenType)) {
            tokenReader.advanceToken()
            return true
        }
        errorHandler.recordPeekError(expected = tokenType, actual = tokenReader.peek()?.type)
        return false
    }

    fun getErrors(): List<String> = errorHandler.getErrors()

    private fun peekPrecedence(): Precedence = precedences[tokenReader.peek()?.type] ?: LOWEST
    private fun currentPrecedence(): Precedence = precedences[tokenReader.current().type] ?: LOWEST

    val precedences = mapOf(
        TokenType.EQ to EQUALS,
        TokenType.NOT_EQ to EQUALS,
        TokenType.LT to LESSGREATER,
        TokenType.GT to LESSGREATER,
        TokenType.PLUS to SUM,
        TokenType.MINUS to SUM,
        TokenType.SLASH to PRODUCT,
        TokenType.ASTERISK to PRODUCT,
        TokenType.LPAREN to CALL
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

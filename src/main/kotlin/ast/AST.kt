package ast

import token.Token

/**
 * Base interface for all AST nodes
 */
interface Node {
    fun tokenLiteral(): String
}

interface Statement : Node
interface Expression : Node

/**
 * Represents an AST program containing multiple statements
 */
data class Program(val statements: MutableList<Statement> = mutableListOf()) : Node {
    override fun tokenLiteral(): String =
        if (statements.isNotEmpty()) statements.first().tokenLiteral() else ""

    override fun toString(): String =
        statements.joinToString("") { it.toString() }
}

data class LetStatement(
    val token: Token,
    val name: Identifier? = null,
    val value: Expression? = null
) : Statement {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String =
        "${tokenLiteral()} $name = ${value ?: ""};"
}

data class Identifier(
    val token: Token,
    val value: String
) : Expression {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = value
}

data class ReturnStatement(
    val token: Token,
    val returnValue: Expression? = null
) : Statement {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = "${tokenLiteral()} ${returnValue?.toString() ?: ""};"
}

data class ExpressionStatement(
    val token: Token,
    val expression: Expression? = null
) : Statement {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = expression?.toString() ?: ""
}

data class IntegerLiteral(
    val token: Token,
    val value: Long
) : Expression {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = token.literal
}

data class PrefixExpression(
    val token: Token,
    val operator: String,
    val right: Expression?
) : Expression {
    override fun tokenLiteral() = token.literal
    override fun toString(): String = "($operator$right)"
}

data class InfixExpression(
    val token: Token,
    val left: Expression?,
    val operator: String,
    val right: Expression?
) : Expression {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = "($left $operator $right)"
}

data class BooleanExpression(
    val token: Token,
    val value: Boolean
) : Expression {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = token.literal
}

data class IfExpression(
    val token: Token,
    val condition: Expression?,
    val consequence: BlockStatement,
    val alternative: BlockStatement? = null
) : Expression {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String =
        "if $condition $consequence" + (alternative?.let { " else $it" } ?: "")
}

data class BlockStatement(
    val token: Token,
    val statements: List<Statement>
) : Statement {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = statements.joinToString(separator = "")
}

data class FunctionLiteral(
    val token: Token,
    val parameters: List<Identifier>?,
    val body: BlockStatement
): Expression {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = "${tokenLiteral()}( ${parameters?.joinToString()}) $body"
}

data class CallExpression(
    val token: Token,
    val function: Expression?,
    val arguments: List<Expression?>?
): Expression {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = "$function(${arguments?.joinToString()})"
}
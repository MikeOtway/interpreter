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
): Statement {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = expression?.toString() ?: ""
}

data class IntegerLiteral(
    val token: Token,
    val value: Long
): Expression {
    override fun tokenLiteral(): String = token.literal
    override fun toString(): String = token.literal
}
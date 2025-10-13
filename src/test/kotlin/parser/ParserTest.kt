package parser

import ast.BooleanExpression
import ast.Expression
import ast.ExpressionStatement
import ast.Identifier
import ast.IfExpression
import ast.InfixExpression
import ast.IntegerLiteral
import ast.LetStatement
import ast.PrefixExpression
import ast.ReturnStatement
import ast.Statement
import lexer.Lexer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.test.assertNotNull


class ParserTest {

    companion object {
        private const val LET_TOKEN = "let"
        private const val RETURN_TOKEN = "return"
    }

    @Test
    fun `should parse let statements`() {
        val input = """
            let x = 5;
            let y = 10;
            let foobar = 838383;
        """.trimIndent()

        val program = parseInput(input, expectedStatements = 3)
        val expectedIdentifiers = listOf("x", "y", "foobar")

        expectedIdentifiers.forEachIndexed { index, expectedIdentifier ->
            val statement = program.statements[index]
            assertThat(statement.tokenLiteral()).isEqualTo(LET_TOKEN)

            assertExpressionType<LetStatement>(statement) {
                assertThat(it.name?.value).isEqualTo(expectedIdentifier)
                assertThat(it.name?.tokenLiteral()).isEqualTo(expectedIdentifier)
            }
        }
    }

    @Test
    fun `should parse return statements`() {
        val input = """
            return 5;
            return 10;
            return 993322;
        """.trimIndent()

        val program = parseInput(input, expectedStatements = 3)

        program.statements.forEach { statement ->
            assertThat(statement.tokenLiteral()).isEqualTo(RETURN_TOKEN)
            assertExpressionType<ReturnStatement>(statement) {}
        }
    }

    @Test
    fun `should parse identifier expression`() {
        val input = "foobar;"
        val program = parseInput(input)
        val expression = getExpressionFromStatement(program.statements.first())

        assertIdentifier(expression, "foobar")
    }

    @Test
    fun `should parse literal expressions`() {
        val tests = listOf(
            LiteralTestCase("5;", 5L),
            LiteralTestCase("true;", true),
            LiteralTestCase("false;", false)
        )

        tests.forEach { testCase ->
            val program = parseInput(testCase.input)
            val expression = getExpressionFromStatement(program.statements.first())
            assertLiteral(expression, testCase.expected)
        }
    }

    @Test
    fun `should parse prefix expressions`() {
        val prefixTests = listOf(
            PrefixTestCase("!5;", "!", 5L),
            PrefixTestCase("-15;", "-", 15L),
            PrefixTestCase("!true;", "!", true),
            PrefixTestCase("!false;", "!", false)
        )
        prefixTests.forEach { scenario ->
            val program = parseInput(scenario.input)
            val expression = getExpressionFromStatement(program.statements.first())

            assertExpressionType<PrefixExpression>(expression) {
                assertThat(it.operator).isEqualTo(scenario.operator)
                assertLiteral(it.right, scenario.value)
            }
        }
    }

    @Test
    fun `should parse infix expressions`() {
        val infixTests = listOf(
            InfixTestCase(input = "5 + 5;", leftValue = 5L, operator = "+", rightValue = 5L),
            InfixTestCase(input = "5 - 5;", leftValue = 5L, operator = "-", rightValue = 5L),
            InfixTestCase(input = "5 * 5;", leftValue = 5L, operator = "*", rightValue = 5L),
            InfixTestCase(input = "5 / 5;", leftValue = 5L, operator = "/", rightValue = 5L),
            InfixTestCase(input = "5 > 5;", leftValue = 5L, operator = ">", rightValue = 5L),
            InfixTestCase(input = "5 < 5;", leftValue = 5L, operator = "<", rightValue = 5L),
            InfixTestCase(input = "5 == 5;", leftValue = 5L, operator = "==", rightValue = 5L),
            InfixTestCase(input = "5 != 5;", leftValue = 5L, operator = "!=", rightValue = 5L),
            InfixTestCase(input = "true == true", leftValue = true, operator = "==", rightValue = true),
            InfixTestCase(input = "true != false;", leftValue = true, operator = "!=", rightValue = false),
            InfixTestCase(input = "false == false", leftValue = false, operator = "==", rightValue = false)
        )

        infixTests.forEach { scenario ->
            val program = parseInput(scenario.input)
            val expression = getExpressionFromStatement(program.statements.first())

            assertInfixExpression(expression, scenario.leftValue, scenario.operator, scenario.rightValue)
        }
    }

    @Test
    fun `should parse operator with precedence`() {
        val operatorTests = mapOf(
            "-1 * 2 + 3" to "(((-1) * 2) + 3)",
            "-a * b" to "((-a) * b)",
            "!-a" to "(!(-a))",
            "true" to "true",
            "false" to "false",
            "3 > 5 == false" to "((3 > 5) == false)",
            "3 < 5 == true" to "((3 < 5) == true)",
            "a + b + c" to "((a + b) + c)",
            "a + b - c" to "((a + b) - c)",
            "a * b * c" to "((a * b) * c)",
            "a * b / c" to "((a * b) / c)",
            "a + b / c" to "(a + (b / c))",
            "a + b * c + d / e - f" to "(((a + (b * c)) + (d / e)) - f)",
            "3 + 4; -5 * 5" to "(3 + 4)((-5) * 5)",
            "5 > 4 == 3 < 4" to "((5 > 4) == (3 < 4))",
            "5 < 4 != 3 > 4" to "((5 < 4) != (3 > 4))",
            "3 + 4 * 5 == 3 * 1 + 4 * 5" to "((3 + (4 * 5)) == ((3 * 1) + (4 * 5)))",
            "1 + (2 + 3) + 4" to "((1 + (2 + 3)) + 4)",
            "(5 + 5) * 2" to "((5 + 5) * 2)",
            "2 / (5 + 5)" to "(2 / (5 + 5))",
            "-(5 + 5)" to "(-(5 + 5))",
            "!(true == true)" to "(!(true == true))",
        )

        operatorTests.forEach { (input, expected) ->
            val program = createParser(input).parseProgram()
            assertThat(program.toString()).isEqualTo(expected)
        }
    }

    @Test
    fun `should parse if expression`() {
        val testCases = mapOf(
            "if (x < y) { x }" to null,
            "if (x < y) { x } else { y }" to "y"
        )

        testCases.forEach { (input, expectedAlternative) ->
            val program = parseInput(input)
            val expression = getExpressionFromStatement(program.statements.first())

            assertExpressionType<IfExpression>(expression) { ifExpr ->
                // Check condition
                assertInfixExpression(ifExpr.condition, "x", "<", "y")

                // Check consequence block
                assertThat(ifExpr.consequence.statements).hasSize(1)
                val consequenceExpr = getExpressionFromStatement(ifExpr.consequence.statements.first())
                assertIdentifier(consequenceExpr, "x")

                // Check alternative block if present
                expectedAlternative?.let { alt ->
                    val alternativeExpr = getExpressionFromStatement(ifExpr.alternative?.statements?.first()!!)
                    assertIdentifier(alternativeExpr, alt)
                }
            }
        }
    }

    private fun assertIdentifier(expression: Expression?, value: String) {
        assertExpressionType<Identifier>(expression) {
            assertThat(it.value).isEqualTo(value)
            assertThat(it.tokenLiteral()).isEqualTo(value)
        }
    }

    private fun assertLiteral(expression: Expression?, value: Any) {
        when (value) {
            is String -> assertIdentifier(expression, value)
            is Long -> assertIntegerLiteral(expression, value)
            is Boolean -> assertBooleanExpression(expression, value)
            else -> fail("Unsupported literal type: ${value::class}")
        }
    }

    private fun assertIntegerLiteral(expression: Expression?, value: Long) {
        assertExpressionType<IntegerLiteral>(expression) {
            assertThat(it.value).isEqualTo(value)
            assertThat(it.tokenLiteral()).isEqualTo("$value")
        }
    }

    private fun assertBooleanExpression(expression: Expression?, value: Boolean) {
        assertExpressionType<BooleanExpression>(expression) {
            assertThat(it.value).isEqualTo(value)
            assertThat(it.tokenLiteral()).isEqualTo("$value")
        }
    }

    private fun assertInfixExpression(expression: Expression?, leftValue: Any, operator: String, rightValue: Any) {
        assertExpressionType<InfixExpression>(expression) {
            assertLiteral(it.left, leftValue)
            assertThat(it.operator).isEqualTo(operator)
            assertLiteral(it.right, rightValue)
        }
    }

    data class LiteralTestCase(val input: String, val expected: Any)
    data class PrefixTestCase(val input: String, val operator: String, val value: Any)
    data class InfixTestCase(val input: String, val leftValue: Any, val operator: String, val rightValue: Any)

    private fun parseInput(input: String, expectedStatements: Int = 1) = createParser(input)
        .parseProgram()
        .also { program ->
            assertNotNull(program)
            assertThat(program.statements).hasSize(expectedStatements)
        }

    private fun getExpressionFromStatement(statement: Statement) =
        (statement as ExpressionStatement).expression

    private inline fun <reified T> assertExpressionType(
        expression: Any?,
        assertions: (T) -> Unit
    ) {
        assertThat(expression).isInstanceOf(T::class.java)
        assertions(expression as T)
    }

    private fun createParser(input: String) = Parser(Lexer(input))
        .also { checkParserErrors(it) }

    private fun checkParserErrors(parser: Parser) {
        val errors = parser.getErrors()
        if (errors.isEmpty()) return

        fail(buildString {
            appendLine("Parser encountered ${errors.size} errors:")
            errors.forEachIndexed { index, error ->
                appendLine("${index + 1}. $error")
            }
        })
    }
}

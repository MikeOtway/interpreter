package parser

import ast.Expression
import ast.ExpressionStatement
import ast.Identifier
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

        assertExpressionType<Identifier>(expression) {
            assertThat(it.value).isEqualTo("foobar")
            assertThat(it.tokenLiteral()).isEqualTo("foobar")
        }
    }

    @Test
    fun `should parse integer literal expression`() {
        val input = "5;"
        val program = parseInput(input)

        val expression = getExpressionFromStatement(program.statements.first())

        assertIntegerLiteral(expression, 5)
    }

    @Test
    fun `should parse prefix expressions`() {
        val prefixTests = listOf(
            PrefixScenario(input = "!5;", operator = "!", integerValue = 5),
            PrefixScenario(input = "-15;", operator = "-", integerValue = 15)
        )
        prefixTests.forEach { scenario ->
            val program = parseInput(scenario.input)

            val expression = getExpressionFromStatement(program.statements.first())
            assertExpressionType<PrefixExpression>(expression) {
                assertThat(it.operator).isEqualTo(scenario.operator)
                assertIntegerLiteral(it.right, scenario.integerValue)
            }
        }
    }

    @Test
    fun `should parse infix expressions`() {
        val infixTests = listOf(
            InfixScenario(input = "5 + 5;", leftValue = 5, operator = "+", rightValue = 5),
            InfixScenario(input = "5 - 5;", leftValue = 5, operator = "-", rightValue = 5),
            InfixScenario(input = "5 * 5;", leftValue = 5, operator = "*", rightValue = 5),
            InfixScenario(input = "5 / 5;", leftValue = 5, operator = "/", rightValue = 5),
            InfixScenario(input = "5 > 5;", leftValue = 5, operator = ">", rightValue = 5),
            InfixScenario(input = "5 < 5;", leftValue = 5, operator = "<", rightValue = 5),
            InfixScenario(input = "5 == 5;", leftValue = 5, operator = "==", rightValue = 5),
            InfixScenario(input = "5 != 5;", leftValue = 5, operator = "!=", rightValue = 5),
        )

        infixTests.forEach { scenario ->
            val program = parseInput(scenario.input)

            val expression = getExpressionFromStatement(program.statements.first())
            assertExpressionType<InfixExpression>(expression) {
                assertIntegerLiteral(it.left, scenario.leftValue)
                assertThat(it.operator).isEqualTo(scenario.operator)
                assertIntegerLiteral(it.right, scenario.rightValue)
            }
        }
    }

    @Test
    fun `should parse operator with precedence`() {
        val operatorTests = listOf(
            Scenario(input = "-1 * 2 + 3", expected = "(((-1) * 2) + 3)"),
            Scenario(input = "-a * b", expected = "((-a) * b)"),
            Scenario(input = "!-a", expected = "(!(-a))"),
            Scenario(input = "a + b + c", expected = "((a + b) + c)"),
            Scenario(input = "a + b - c", expected = "((a + b) - c)"),
            Scenario(input = "a * b * c", expected = "((a * b) * c)"),
            Scenario(input = "a * b / c", expected = "((a * b) / c)"),
            Scenario(input = "a + b / c", expected = "(a + (b / c))"),
            Scenario(input = "a + b * c + d / e - f", expected = "(((a + (b * c)) + (d / e)) - f)"),
            Scenario(input = "3 + 4; -5 * 5", expected = "(3 + 4)((-5) * 5)"),
            Scenario(input = "5 > 4 == 3 < 4", expected = "((5 > 4) == (3 < 4))"),
            Scenario(input = "5 < 4 != 3 > 4", expected = "((5 < 4) != (3 > 4))"),
            Scenario(input = "3 + 4 * 5 == 3 * 1 + 4 * 5", expected = "((3 + (4 * 5)) == ((3 * 1) + (4 * 5)))")
        )

        operatorTests.forEach { scenario ->
            val program = createParser(scenario.input)
                .parseProgram()

            assertThat(program.toString()).isEqualTo(scenario.expected)
        }
    }

    private fun assertIntegerLiteral(expression: Expression?, value: Long) {
        assertExpressionType<IntegerLiteral>(expression) {
            assertThat(it.value).isEqualTo(value)
            assertThat(it.tokenLiteral()).isEqualTo("$value")
        }
    }

    data class PrefixScenario(val input: String, val operator: String, val integerValue: Long)
    data class InfixScenario(val input: String, val leftValue: Long, val operator: String, val rightValue: Long)
    data class Scenario(val input: String, val expected: String)

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

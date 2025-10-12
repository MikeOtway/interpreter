package parser

import ast.ExpressionStatement
import ast.Identifier
import ast.IntegerLiteral
import ast.LetStatement
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
            testLetStatement(program.statements[index], expectedIdentifier)
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

        program.statements.forEach {
            assertThat(it).isInstanceOf(ReturnStatement::class.java)
            assertThat(it.tokenLiteral()).isEqualTo(RETURN_TOKEN)
        }
    }

    @Test
    fun `should parse identifier expression`() {
        val input = "foobar;"

        val program = parseInput(input, expectedStatements = 1)

        val expression = getExpressionFromStatement(program.statements.first())
        assertExpressionType<Identifier>(expression) {
            assertThat(it.value).isEqualTo("foobar")
            assertThat(it.tokenLiteral()).isEqualTo("foobar")
        }
    }

    @Test
    fun `should parse integer literal expression`() {
        val input = "5;"
        val program = parseInput(input, expectedStatements = 1)

        val expression = getExpressionFromStatement(program.statements.first())
        assertExpressionType<IntegerLiteral>(expression) {
            assertThat(it.value).isEqualTo(5)
            assertThat(it.tokenLiteral()).isEqualTo("5")
        }
    }

    private fun parseInput(input: String, expectedStatements: Int) = createParser(input)
        .parseProgram()
        .also { program ->
            assertNotNull(program)
            assertThat(program.statements).hasSize(expectedStatements)
        }

    private fun getExpressionFromStatement(statement: Statement) =
        (statement as ExpressionStatement).expression

    private inline fun <reified  T> assertExpressionType(
        expression: Any?,
        assertions: (T) -> Unit
    ){
        assertThat(expression).isInstanceOf(T::class.java)
        assertions(expression as T)
    }

    private fun createParser(input: String) = Parser(Lexer(input))
        .also { checkParserErrors(it) }

    private fun testLetStatement(statement: Statement, expectedIdentifier: String) {
        assertThat(statement.tokenLiteral()).isEqualTo(LET_TOKEN)
        assertThat(statement).isInstanceOf(LetStatement::class.java)

        val letStatement = statement as LetStatement
        assertThat(letStatement.name?.value).isEqualTo(expectedIdentifier)
        assertThat(letStatement.name?.tokenLiteral()).isEqualTo(expectedIdentifier)
    }

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

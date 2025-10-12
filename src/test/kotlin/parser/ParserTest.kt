package parser

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

    private fun createParser(input: String): Parser {
        val lexer = Lexer(input)
        return Parser(lexer)
    }

    @Test
    fun `should parse let statements`() {
        val input = """
            let x = 5;
            let y = 10;
            let foobar = 838383;
        """.trimIndent()

        val parser = createParser(input)
        val program = parser.parseProgram()
        checkParserErrors(parser)

        assertNotNull(program)
        assertThat(program.statements).hasSize(3)

        val expectedIdentifiers = listOf("x", "y", "foobar")

        expectedIdentifiers.forEachIndexed { index, expectedIdentifier ->
            val statement = program.statements[index]
            testLetStatement(statement, expectedIdentifier)
        }
    }

    @Test
    fun `should parse return statements`() {
        val input = """
            return 5;
            return 10;
            return 993322;
        """.trimIndent()

        val parser = createParser(input)
        val program = parser.parseProgram()
        checkParserErrors(parser)

        assertNotNull(program)
        assertThat(program.statements).hasSize(3)

        program.statements.forEach {
            assertThat(it).isInstanceOf(ReturnStatement::class.java)
            assertThat(it.tokenLiteral()).isEqualTo(RETURN_TOKEN)
        }
    }

//    @Test
//    fun `should report parser errors`() {
//        val input = "let = 5;"
//        val parser = createParser(input)
//
//        assertThrows<IllegalStateException> {
//            parser.parseProgram()
//            checkParserErrors(parser)
//        }
//    }

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

        val errorMessage = buildString {
            appendLine("Parser encountered ${errors.size} errors:")
            errors.forEachIndexed { index, error ->
                appendLine("${index + 1}. $error")
            }
        }
        fail(errorMessage)
    }
}
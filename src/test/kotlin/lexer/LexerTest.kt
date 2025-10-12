package lexer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import token.TokenType

@DisplayName("Lexer Tests")
class LexerTest {

    private companion object {
        const val BASIC_INPUT = """
            let five = 5;
            let ten = 10;
        """

        const val FUNCTION_INPUT = """
            let add = fn(x, y) {
              x + y;
            };
        """

        const val OPERATORS_INPUT = """
            !-*5;
            5 < 10 > 5;
            10 == 10;
            10 != 9;
        """

        const val CONTROL_FLOW_INPUT = """
            if (5 < 10) {
              return true;
            } else {
              return false;
            }
        """
    }

    @Nested
    @DisplayName("Basic Tokens")
    inner class BasicTokens {
        @Test
        fun `should tokenize variable declarations`() {
            val expectedTokens = listOf(
                TokenTest(TokenType.LET, "let"),
                TokenTest(TokenType.IDENT, "five"),
                TokenTest(TokenType.ASSIGN, "="),
                TokenTest(TokenType.INT, "5"),
                TokenTest(TokenType.SEMICOLON, ";"),
                TokenTest(TokenType.LET, "let"),
                TokenTest(TokenType.IDENT, "ten"),
                TokenTest(TokenType.ASSIGN, "="),
                TokenTest(TokenType.INT, "10"),
                TokenTest(TokenType.SEMICOLON, ";"),
                TokenTest(TokenType.EOF, "")
            )

            assertTokenization(BASIC_INPUT.trimIndent(), expectedTokens)
        }
    }

    @Nested
    @DisplayName("Function Tokens")
    inner class FunctionTokens {
        @Test
        fun `should tokenize function declaration`() {
            val expectedTokens = listOf(
                TokenTest(TokenType.LET, "let"),
                TokenTest(TokenType.IDENT, "add"),
                TokenTest(TokenType.ASSIGN, "="),
                TokenTest(TokenType.FUNCTION, "fn"),
                TokenTest(TokenType.LPAREN, "("),
                TokenTest(TokenType.IDENT, "x"),
                TokenTest(TokenType.COMMA, ","),
                TokenTest(TokenType.IDENT, "y"),
                TokenTest(TokenType.RPAREN, ")"),
                TokenTest(TokenType.LBRACE, "{"),
                TokenTest(TokenType.IDENT, "x"),
                TokenTest(TokenType.PLUS, "+"),
                TokenTest(TokenType.IDENT, "y"),
                TokenTest(TokenType.SEMICOLON, ";"),
                TokenTest(TokenType.RBRACE, "}"),
                TokenTest(TokenType.SEMICOLON, ";"),
                TokenTest(TokenType.EOF, "")
            )

            assertTokenization(FUNCTION_INPUT.trimIndent(), expectedTokens)
        }
    }

    @Nested
    @DisplayName("Operator Tokens")
    inner class OperatorTokens {
        @Test
        fun `should tokenize operator declaration`() {
            val expectedTokens = listOf(
                TokenTest(TokenType.BANG, "!"),
                TokenTest(TokenType.MINUS, "-"),
                TokenTest(TokenType.ASTERISK, "*"),
                TokenTest(TokenType.INT, "5"),
                TokenTest(TokenType.SEMICOLON, ";"),
                TokenTest(TokenType.INT, "5"),
                TokenTest(TokenType.LT, "<"),
                TokenTest(TokenType.INT, "10"),
                TokenTest(TokenType.GT, ">"),
                TokenTest(TokenType.INT, "5"),
                TokenTest(TokenType.SEMICOLON, ";"),
                TokenTest(TokenType.INT, "10"),
                TokenTest(TokenType.EQ, "=="),
                TokenTest(TokenType.INT, "10"),
                TokenTest(TokenType.SEMICOLON, ";"),
                TokenTest(TokenType.INT, "10"),
                TokenTest(TokenType.NOT_EQ, "!="),
                TokenTest(TokenType.INT, "9"),
                TokenTest(TokenType.SEMICOLON, ";"),
                TokenTest(TokenType.EOF, "")
            )

            assertTokenization(OPERATORS_INPUT.trimIndent(), expectedTokens)
        }
    }

    @Nested
    @DisplayName("Control Flow Tokens")
    inner class ControlFlowTokens {
        @Test
        fun `should tokenize control flow declaration`() {
            val expectedTokens = listOf(
                TokenTest(TokenType.IF, "if"),
                TokenTest(TokenType.LPAREN, "("),
                TokenTest(TokenType.INT, "5"),
                TokenTest(TokenType.LT, "<"),
                TokenTest(TokenType.INT, "10"),
                TokenTest(TokenType.RPAREN, ")"),
                TokenTest(TokenType.LBRACE, "{"),
                TokenTest(TokenType.RETURN, "return"),
                TokenTest(TokenType.TRUE, "true"),
                TokenTest(TokenType.SEMICOLON, ";"),
                TokenTest(TokenType.RBRACE, "}"),
                TokenTest(TokenType.ELSE, "else"),
                TokenTest(TokenType.LBRACE, "{"),
                TokenTest(TokenType.RETURN, "return"),
                TokenTest(TokenType.FALSE, "false"),
                TokenTest(TokenType.SEMICOLON, ";"),
                TokenTest(TokenType.RBRACE, "}"),
                TokenTest(TokenType.EOF, "")
            )

            assertTokenization(CONTROL_FLOW_INPUT.trimIndent(), expectedTokens)
        }
    }

    private fun assertTokenization(input: String, expectedTokens: List<TokenTest>) {
        val lexer = Lexer(input)

        expectedTokens.forEach { expected ->
            val token = lexer.nextToken()
            assertThat(token.type)
                .withFailMessage("Expected token type ${expected.expectedType} but got ${token.type}")
                .isEqualTo(expected.expectedType)
            assertThat(token.literal)
                .withFailMessage("Expected literal '${expected.expectedLiteral}' but got '${token.literal}'")
                .isEqualTo(expected.expectedLiteral)
        }
    }
}

data class TokenTest(
    val expectedType: TokenType,
    val expectedLiteral: String
)
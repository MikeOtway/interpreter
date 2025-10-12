package ast

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import token.Token
import token.TokenType

class ASTTest {

    @Test
    fun `should test write to string`() {
        val program = Program(
            statements = mutableListOf(
                LetStatement(
                    token = Token(type = TokenType.LET, literal = "let"),
                    name = Identifier(
                        token = Token(type = TokenType.IDENT, literal = "myVar"),
                        value = "myVar"
                    ),
                    value = Identifier(
                        token = Token(type = TokenType.IDENT, literal = "anotherVar"),
                        value = "anotherVar"
                    )
                )
            )
        )
        assertThat(program.toString()).isEqualTo("let myVar = anotherVar;")
    }
}
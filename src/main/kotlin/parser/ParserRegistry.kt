package parser

import ast.Expression
import token.TokenType

typealias PrefixParseFn = () -> Expression?
typealias InfixParseFn = (Expression?) -> Expression

class ParserRegistry {
    private val prefixParseFunctions = mutableMapOf<TokenType, PrefixParseFn>()
    private val infixParseFunctions = mutableMapOf<TokenType, InfixParseFn>()

    fun registerPrefix(tokenType: TokenType, parseFn: PrefixParseFn) {
        prefixParseFunctions[tokenType] = parseFn
    }

    fun registerInfix(tokenType: TokenType, parseFn: InfixParseFn) {
        infixParseFunctions[tokenType] = parseFn
    }

    fun getPrefixParser(tokenType: TokenType): PrefixParseFn? = prefixParseFunctions[tokenType]
    fun getInfixParser(tokenType: TokenType?): InfixParseFn? = infixParseFunctions[tokenType]
}
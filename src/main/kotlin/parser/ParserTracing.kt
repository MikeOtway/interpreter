package parser

class ParserTracing {
    private var traceLevel: Int = 0
    private val traceIdentPlaceholder: String = "\t"

    private fun identLevel(): String =
        traceIdentPlaceholder.repeat((traceLevel - 1).coerceAtLeast(0))

    private fun tracePrint(fs: String) {
        println("${identLevel()}$fs")
    }

    private fun incIdent() { traceLevel += 1 }
    private fun decIdent() { traceLevel -= 1 }

    fun trace(message: String): String {
        incIdent()
        tracePrint("BEGIN $message")
        return message
    }

    fun untrace(message: String) {
        tracePrint("END $message")
        decIdent()
    }
}

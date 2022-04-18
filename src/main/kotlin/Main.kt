import KotZen.parse
import KotZen.eof
import Parser.*
import Evaluator.*
import Result.*
import KotZen.skipRight


fun main(args: Array<String>) {
    val parser = LangParser()
    val globalContext: MutableContext = mutableMapOf()
    while(true) {
        print(">> ")
        val (remaining, parserResult) = parser
            .statementP
            .skipRight(eof)
            .parse(readLine()!!)
        when(parserResult) {
            null -> println("Syntax error: unable to parse \"" + remaining.unparsed() + "\"")
            else ->
            {
                val computationResult = evaluateStatement(parserResult as Statement, globalContext)
                when(computationResult) {
                    is Success -> println(computationResult.value)
                    is Failure -> println("Evaluator error: " + computationResult.reason)
                }
            }
        }
    }
}
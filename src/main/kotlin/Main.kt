import KotZen.parse
import KotZen.eof
import Parser.*
import Evaluator.*
import KotZen.skipRight


fun main(args: Array<String>) {
    val parser = LangParser()
    val globalContext: MutableContext = mutableMapOf()
    while(true) {
        print(">> ")
        val (remaining, result) = parser
            .statementP
            .skipRight(eof)
            .parse(readLine()!!)
        when(result) {
            null -> println("Syntax error: unable to parse \"" + remaining.unparsed() + "\"")
            else ->
            {
                println(evaluateStatement(result as Statement, globalContext))
            }
        }
    }
}
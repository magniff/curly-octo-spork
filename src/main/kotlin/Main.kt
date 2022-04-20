import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState


import KotZen.parse
import Parser.*
import Evaluator.*
import Result.*
import kotlinx.coroutines.*


val RED = Color(200, 0, 0, 20)
val GREEN = Color(0, 200, 0, 20)


suspend fun processInput(input: String, parser: LangParser) : Result<String, String> {
    val globalContext: MutableContext = mutableMapOf()
    val (remaining, parserResult) = parser
        .wholeProgramP
        .parse(input)
    return when (parserResult) {
        null -> "Syntax error: unable to parse ${remaining.unparsed()}".fail()
        else -> {
            when (val computationResult = evaluateStmtList(parserResult as List<Statement>, globalContext)) {
                is Success -> computationResult.value.success()
                is Failure -> computationResult.reason.fail()
            }
        }
    }
}


class ComputationState {
    /**
     * So, here we really need a single-threaded dispatcher like Dispatchers.Main
     * to make it run smoothly, yet for some reason I keep hitting into this pesky issue:
     * https://github.com/Kotlin/kotlinx.coroutines/issues/932
     * no matter of what version of the coroutine I am using
     * Dispatchers.Default/IO are not suitable here, but I figured that's better than nothing
     */
    private var coroutineScope = CoroutineScope(Dispatchers.Default)

    var outputBuffer : String by mutableStateOf("")
    var inputBuffer : String by mutableStateOf("")
    var isError : Boolean by mutableStateOf(false)

    fun evaluate(input: String, parser: LangParser) : Unit {
        this.inputBuffer = input

        /**
         * Cancel whatever we are crunching right now
         */
        coroutineScope.cancel()
        coroutineScope = CoroutineScope(Dispatchers.Default)

        coroutineScope.launch {
            when(val result = processInput(input, parser)) {
                is Success -> {
                    this@ComputationState.outputBuffer = result.value
                    this@ComputationState.isError = false
                }
                is Failure -> {
                    this@ComputationState.outputBuffer = result.reason
                    this@ComputationState.isError = true
                }
            }
        }
    }
}


@Composable
fun Application() {
    // Main widget
    val computationState = remember { ComputationState() }
    MaterialTheme {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Grab user's input
            val parser = LangParser()
            TextField(
                computationState.inputBuffer,
                {
                        currentInput ->
                    computationState.evaluate(currentInput, parser)
                },
                isError = computationState.isError,
                placeholder = {Text("Type your code here...")}
            )
            // Present the output
            Box(
                modifier = Modifier.background(if(computationState.isError) RED else GREEN),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(text = computationState.outputBuffer)
            }
        }
    }
}


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Compose for Desktop",
        state = rememberWindowState(width = 300.dp, height = 300.dp)
    ) {
        Application()
    }
}

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState


import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.coroutines.Dispatchers


import KotZen.parse
import Parser.*
import Evaluator.*
import Result.*
import androidx.compose.ui.window.awaitApplication


val RED = Color(200, 0, 0, 20)
val GREEN = Color(0, 200, 0, 20)


@Composable
fun TextBox(text: String, color: Color) {
    Box(
        modifier = Modifier.background(color),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text = text)
    }
}


fun processInput(input: String, parser: LangParser) : Result<String, String> {
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


suspend fun main() = awaitApplication {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Compose for Desktop",
        state = rememberWindowState(width = 300.dp, height = 300.dp)
    ) {
        val parser = LangParser()
        // Text, that being passed from the user
        val inputBuffer = remember { mutableStateOf("") }
        // Text, that being return by the runner
        val outputBuffer = remember { mutableStateOf("No input...") }
        // Flag, indicating the presence of an error
        val isError = remember { mutableStateOf(false) }

        // Main widget
        MaterialTheme {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Draw the output
                TextBox(outputBuffer.value, color = if (isError.value) RED else GREEN)
                // Grab the user's input here
                TextField(
                    inputBuffer.value,
                    {
                        currentInput ->
                            inputBuffer.value = currentInput
                            when(val result = processInput(inputBuffer.value, parser)) {
                                is Success -> {
                                    isError.value = false
                                    outputBuffer.value = result.value
                                }
                                is Failure -> {
                                    isError.value = true
                                    outputBuffer.value = result.reason
                                }
                            }
                    },
                    isError = isError.value,
                    placeholder = {Text("Type your code here...")}
                )
            }
        }
    }
}

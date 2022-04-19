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


import KotZen.parse
import Parser.*
import Evaluator.*
import Result.*


val RED = Color(200, 0, 0, 20)
val GREEN = Color(0, 200, 0, 20)


@Composable
fun TextBox(text: String, color: Color) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .width(400.dp)
            .background(color)
            .padding(start = 10.dp),
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


fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Compose for Desktop",
        state = rememberWindowState(width = 300.dp, height = 300.dp)
    ) {
        val inputBuffer = remember { mutableStateOf("") }
        val outputBuffer = remember { mutableStateOf("") }
        val isError = remember { mutableStateOf(false) }

        val parser = LangParser()
        MaterialTheme {
            Column(Modifier.fillMaxSize(), Arrangement.spacedBy(5.dp)) {
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
                TextBox(outputBuffer.value, color = if (isError.value) RED else GREEN)
            }
        }
    }
}

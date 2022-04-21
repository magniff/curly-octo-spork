package Evaluator

import kotlin.math.pow

import Parser.Expression
import Parser.Statement
import Parser.ExpressionType
import Result.*
import kotlinx.coroutines.yield

typealias Context = Map<Expression.NameNode, Expression>
typealias MutableContext = MutableMap<Expression.NameNode, Expression>


/**
 * Evaluates the expression
 * @param expr Expression to evaluate
 * @param context Current variable binding mapping
 * @param expectType What type to expect at the end
 * @return Fully evaluated expression or an error message
 */
suspend fun evaluateExpression(
    expr: Expression, context: Context, expectType: ExpressionType = ExpressionType.Whatever
) : Result<Expression, String>
{
    val evaluationResult = when (expr) {
        // Number is a normal form
        is Expression.NumberNode -> expr.success()
        // This should be ensured by the type checker, or... hmm
        is Expression.NameNode -> {
            context[expr]?.success() ?: ("Error: Unknown variable ${expr.value}").fail()
        }
        is Expression.Sequence ->
            expr
                .operands
                .mapM {evaluateExpression(it, context, ExpressionType.Number)}
                .map { Expression.Sequence(it) }
        is Expression.SequenceRange ->
            evaluateExpression(expr.from, context, ExpressionType.Number)
                .bind {
                    fromValue ->
                        evaluateExpression(expr.to, context, ExpressionType.Number)
                            .bind {
                                toValue ->
                                    // As we now have fromValue and toValue expressions, it is time to sanity check the
                                    // range itself
                                    if ((fromValue as Expression.NumberNode).value > (toValue as Expression.NumberNode).value)
                                        "Range is malformed, starting value is greater than the ending one".fail()
                                    else
                                        Expression.SequenceRange(fromValue, toValue).success()
                                }
                            }
        is Expression.AddNode ->
            expr
                .operands
                .mapM {evaluateExpression(it, context, ExpressionType.Number)}
                .map {expressions -> expressions.map {(it as Expression.NumberNode).value}}
                .map {floats -> floats.fold (0.0) {sum, elem -> sum + elem} }
                .map {Expression.NumberNode(it.toFloat())}
        is Expression.MulNode ->
            expr
                .operands
                .mapM {evaluateExpression(it, context, ExpressionType.Number)}
                .map {expressions -> expressions.map {(it as Expression.NumberNode).value}}
                .map {floats -> floats.fold (1.0) {sum, elem -> sum * elem} }
                .map {Expression.NumberNode(it.toFloat())}
        is Expression.SubNode ->
            evaluateExpression(expr.operands.first(), context, ExpressionType.Number)
                .bind {
                    firstOne ->
                        expr.operands
                            .slice(1 until expr.operands.size)
                            .mapM {evaluateExpression(it, context, ExpressionType.Number)}
                            .map {expressions -> expressions.map {(it as Expression.NumberNode).value}}
                            .map {floats -> floats.fold (0.0) {sum, elem -> sum + elem} }
                            .map {Expression.NumberNode(it.toFloat()) }
                            .bind {
                                rest ->
                                    Expression
                                        .NumberNode((firstOne as Expression.NumberNode).value - rest.value)
                                        .success()
                            }
                }
        is Expression.DivNode ->
            evaluateExpression(expr.operands.first(), context, ExpressionType.Number)
                .bind {
                    firstOne ->
                        expr.operands
                            .slice(1 until expr.operands.size)
                            .mapM {evaluateExpression(it, context, ExpressionType.Number)}
                            .map {expressions -> expressions.map {(it as Expression.NumberNode).value}}
                            .map {it.fold (1.0) {sum, elem -> sum * elem} }
                            .map {Expression.NumberNode(it.toFloat()) }
                            .bind {
                                rest ->
                                    if (rest.value == 0.0.toFloat()) {
                                        "Trying to divide ${(firstOne as Expression.NumberNode).value} by zero, it's a no-no"
                                            .fail()
                                    } else {
                                        Expression
                                            .NumberNode((firstOne as Expression.NumberNode).value / rest.value )
                                            .success()
                                    }
                            }
                }
        is Expression.PowNode -> {
            evaluateExpression(expr.operands.first(), context, ExpressionType.Number)
                .bind {
                    primer ->
                        expr.operands
                            .slice(1 until expr.operands.size)
                            .mapM {evaluateExpression(it, context, ExpressionType.Number)}
                            .map {expressions -> expressions.map {(it as Expression.NumberNode).value}}
                            .map {
                                floats ->
                                    floats
                                        .fold((primer as Expression.NumberNode).value)
                                        { soFar, elem -> soFar.pow(elem) }
                            }
                            .map {Expression.NumberNode(it)}
                }
        }
        is Expression.Map -> {
            val extendedContext = context.toMutableMap()
            evaluateExpression(
                expr.sequence,
                context,
                ExpressionType.Or(
                    listOf(ExpressionType.Sequence, ExpressionType.SequenceRange)
                )
            )
                .bind {
                    sequence ->
                        if (sequence.type == ExpressionType.Sequence) {
                            // Strategy for literal sequences
                            (sequence as Expression.Sequence).operands
                                .mapM { operand ->
                                    // Presence of that side effect kind of sucks ass
                                    extendedContext[expr.mapper.abs1] = operand
                                    evaluateExpression(expr.mapper.body, extendedContext, ExpressionType.Number)
                                }
                                .map { Expression.Sequence(it) }
                        } else {
                            // Strategy for range sequences
                            evaluateExpression(
                                Expression.Map(
                                    // Construct a sequence literal from the Range
                                    Expression.Sequence(
                                        operands =
                                            (
                                                    ((sequence as Expression.SequenceRange).from as Expression.NumberNode)
                                                        .value
                                                        .toInt() ..
                                                 (sequence.to as Expression.NumberNode)
                                                        .value
                                                         .toInt()
                                            )
                                            .map {
                                                    intValue ->
                                                        yield()
                                                        Expression.NumberNode(intValue.toFloat())
                                            }
                                            .toList()
                                    ),
                                    expr.mapper
                                ),
                                context,
                                ExpressionType.Sequence
                            )
                        }
                }
        }
        // FoldM just blew my mind, so here's an iterative version
        is Expression.Reduce -> {
            val extendedContext = context.toMutableMap()
            val sequence = evaluateExpression(
                expr.sequence,
                context,
                ExpressionType.Or(
                    listOf(
                        ExpressionType.Sequence,
                        ExpressionType.SequenceRange
                    )
                )
            )
            if (!sequence.isSuccess()) {
                return sequence
            }
            // Ok, now we are somewhat sure about the future of this computation instance
            // Lets first evaluate the primer value
            var resultSoFar = evaluateExpression(expr.primer, context, ExpressionType.Number)
            // This is the first strategy for the literal sequences
            if ((sequence as Success<Expression>).value.type == ExpressionType.Sequence) {
                // reduce an eagerly evaluated sequence node
                for (member in (sequence as Success<Expression.Sequence>).value.operands) {
                    // Going back to the caller on each iteration
                    yield()
                    if (!resultSoFar.isSuccess()) {
                        return resultSoFar
                    }
                    extendedContext[expr.reducer.abs1] = (resultSoFar as Success<Expression>).value
                    extendedContext[expr.reducer.abs2] =  member
                    resultSoFar = evaluateExpression(expr.reducer.body, extendedContext, ExpressionType.Number)
                }
            } else {
                val intRange = (
                    ((sequence as Success<Expression.SequenceRange>).value.from as Expression.NumberNode).value.toInt() ..
                    (sequence.value.to as Expression.NumberNode).value.toInt()
                )
                for (intValue in intRange) {
                    // Going back to the caller on each iteration
                    yield()
                    if (!resultSoFar.isSuccess()) {
                        return resultSoFar
                    }
                    extendedContext[expr.reducer.abs1] = (resultSoFar as Success<Expression>).value
                    extendedContext[expr.reducer.abs2] =  Expression.NumberNode(intValue.toFloat())
                    resultSoFar = evaluateExpression(expr.reducer.body, extendedContext, ExpressionType.Number)
                }
            }
            resultSoFar
        }
    }
    yield()
    return evaluationResult.bind {
        expression ->
            // While the evaluator is still happy, let us check the type of the result
            if (!checkType(expression, expectType)) {
                when(val expressionRepr = expressionToString(expression, context)) {
                    is Success ->
                        {
                            (
                                "Expression ${expressionRepr.value} should be a " +
                                "${expectType}, yet it is a ${expression.type}"
                            )
                            .fail()
                        }
                    // This branch should be unreachable
                    is Failure -> "Type error occurred, cannot elaborate on that...".fail()
                }
            } else {
                expression.success()
            }
    }
}


fun checkType(expression: Expression, etype: ExpressionType) : Boolean {
    return when(etype) {
        // Trying hard to match any option
        is ExpressionType.Whatever -> true
        is ExpressionType.Or -> {
            for (option in etype.options) {
                if (checkType(expression, option)) {
                    return true
                }
            }
            return false
        }
        else -> {
            expression.type == etype
        }
    }
}


suspend fun expressionToString(expr: Expression, context: Context) : Result<String, String> {
    return when(expr) {
        is Expression.NumberNode -> expr.value.toString().success()
        is Expression.NameNode ->
        {
            val lookupResult = context[expr]
            if (lookupResult == null) {
                Failure("Error: unknown variable " + expr.value)
            } else {
                expressionToString(lookupResult, context)
            }
        }
        is Expression.Sequence -> {
            expr
                .operands
                .mapM { expressionToString(it, context) }
                .map {
                    it.joinToString( prefix="{", postfix="}", separator=", " )
                }
        }
        is Expression.SequenceRange -> {
            expressionToString(expr.from, context)
                .bind {
                    fromRepresentation ->
                        expressionToString(expr.to, context)
                            .bind {
                                toRepresentation ->
                                    "{$fromRepresentation .. $toRepresentation}".success()
                            }
                }
        }
        // Anything in "non-normal" form should be normalized first
        // Here we could end up getting into an infinite loop, so WARNING and god bless you
        else -> evaluateExpression(expr, context).bind { expressionToString(it, context) }
    }
}


suspend fun evaluateStatement(stmt: Statement, context: MutableContext) : Result<String, String> {
    return when(stmt) {
        is Statement.VarDeclarationStmt ->
            {
                // Normalize the expression inside, mutate the computation context
                evaluateExpression(stmt.expression, context)
                    .bind {
                        result ->
                            context[stmt.name] = result
                            "".success()
                    }
            }
        is Statement.PrintStmt -> stmt.string.value.success()
        is Statement.OutStmt -> expressionToString(stmt.expression, context)
    }
}


suspend fun evaluateStmtList(stmts: List<Statement>, context: MutableContext) : Result<String, String> {
    return (
        stmts
        .mapM { evaluateStatement(it, context) }
        .map {
            it.joinToString(separator="")
        }
    )
}

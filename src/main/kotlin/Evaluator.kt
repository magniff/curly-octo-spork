package Evaluator

import kotlin.math.pow

import Parser.Expression
import Parser.Statement
import Result.Result
import Result.Success
import Result.Failure

typealias Context = Map<Expression.NameNode, Expression>
typealias MutableContext = MutableMap<Expression.NameNode, Expression>


// Computes the value of expr.
// context is read-only here
fun evaluateExpression(expr: Expression, context: Context) : Expression {
    return when (expr) {
        // Number is a normal form
        is Expression.NumberNode -> expr
        // This should be ensured by the typechecker, or... hmm
        is Expression.NameNode -> context.get(expr)!!
        is Expression.Sequence ->
            Expression.Sequence(
                expr.operands.map {evaluateExpression(it, context)}
            )
        is Expression.SequenceRange ->
            Expression.Sequence(
                (
                    (evaluateExpression(expr.from, context) as Expression.NumberNode).value.toInt() ..
                    (evaluateExpression(expr.to, context) as Expression.NumberNode).value.toInt()
                )
                .map {
                    Expression.NumberNode(it.toFloat())
                }
            )
        is Expression.AddNode ->
            Expression.NumberNode(
                expr.operands
                    .map {evaluateExpression(it, context)}
                    .map {(it as Expression.NumberNode).value}
                    .fold (0.0) {sum, elem -> sum + elem}
                    .toFloat()
            )
        is Expression.MulNode ->
            Expression.NumberNode(
                expr.operands
                    .map {evaluateExpression(it, context)}
                    .map {(it as Expression.NumberNode).value}
                    .fold (1.0) {mul, elem -> mul * elem}
                    .toFloat()
            )
        is Expression.SubNode ->
            Expression.NumberNode(
                (evaluateExpression(expr.operands.first(), context) as Expression.NumberNode).value -
                expr.operands
                    .slice(1..expr.operands.size-1)
                    .map {evaluateExpression(it, context)}
                    .map {(it as Expression.NumberNode).value}
                    .fold (0.0) {sum, elem -> sum + elem}
                    .toFloat()
            )
        is Expression.DivNode ->
            Expression.NumberNode(
                (evaluateExpression(expr.operands.first(), context) as Expression.NumberNode).value /
                expr.operands
                    .slice(1..expr.operands.size-1)
                    .map {evaluateExpression(it, context)}
                    .map {(it as Expression.NumberNode).value}
                    .fold (1.0) {mul, elem -> mul * elem}
                    .toFloat()
            )
        is Expression.PowNode -> {
            val primer = (evaluateExpression(expr.operands.first(), context) as Expression.NumberNode).value
            Expression.NumberNode(
                expr.operands
                    .slice(1..expr.operands.size-1)
                    .map {evaluateExpression(it, context)}
                    .map {(it as Expression.NumberNode).value}
                    .fold (primer) {sofar, elem -> sofar.pow(elem)}
                    .toFloat()
            )
        }
        is Expression.Map -> {
            var extendedContext = context.toMutableMap()
            Expression.Sequence(
                (evaluateExpression(expr.sequence, context) as Expression.Sequence)
                .operands
                .map {
                    operand ->
                        extendedContext.put(expr.mapper.abs1, operand)
                        evaluateExpression(expr.mapper.body, extendedContext)
                }
            )
        }
        is Expression.Reduce -> {
            var extendedContext = context.toMutableMap()
            var resultSoFar = evaluateExpression(expr.primer, context)
            for (member in (evaluateExpression(expr.sequence, context) as Expression.Sequence).operands) {
                extendedContext.put(expr.reducer.abs1, resultSoFar)
                extendedContext.put(expr.reducer.abs2, member)
                resultSoFar = evaluateExpression(expr.reducer.body, extendedContext)
            }
            resultSoFar
        }
    }
}


fun expressionToString(expr: Expression, context: Context) : String {
    return when(expr) {
        is Expression.NumberNode -> expr.value.toString()
        is Expression.NameNode -> expressionToString(context.get(expr)!!, context)
        is Expression.Sequence -> {
            expr
                .operands
                .map { expressionToString(it, context) }
                .joinToString( prefix="{", postfix="}", separator=", " )
        }
        // Anything in "non-normal" form should be normalized first
        // Here we could end up getting into an infinite loop, so WARNING and god bless you
        else -> expressionToString(evaluateExpression(expr, context), context)
    }
}


fun evaluateStatement(stmt: Statement, context: MutableContext) : String {
    return when(stmt) {
        is Statement.VarDeclarationStmt ->
            {
                // Normalize the expression inside, mutate the computation context
                context.put(
                    stmt.name,
                    evaluateExpression(stmt.expression, context)
                )
                // There's nothing to add to the output
                ""
            }
        is Statement.PrintStmt -> stmt.string.value
        is Statement.OutStmt -> expressionToString(stmt.expression, context)
    }
}


fun evaluateStmtList(stmts: List<Statement>, context: MutableContext) : String {
    return (
        stmts
        .map { evaluateStatement(it, context) }
        .joinToString(separator="")
    )
}

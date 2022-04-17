package Parser


import KotZen.*


/**
 * Any ParseTree/AST node, used in parser/typechecker/evaluator.
 */
open class ASTNode

data class StringNode(var value: String) : ASTNode()

sealed class Statement : ASTNode() {
    data class VarDeclarationStmt(var name: Expression.NameNode, var expression: Expression) : Statement()
    data class PrintStmt(var string: StringNode) : Statement()
    data class OutStmt(var expression: Expression) : Statement()
}

/**
 * Lambda function with a single argument
 * e.g: a -> a + 1
 * @param abs1 The name of the abstraction variable
 * @param body The body of the function
 */
data class Lambda1(var abs1: Expression.NameNode, var body: Expression) : ASTNode()

/**
 * Lambda function with a pair of arguments.
 * e.g: a b -> a + b
 * @param abs1 The name of the first variable
 * @param abs2 The name of the second variable
 * @param body The body of the function
 */
data class Lambda2(
    var abs1: Expression.NameNode,
    var abs2: Expression.NameNode,
    var body: Expression
) : ASTNode()

sealed class Expression : ASTNode() {
    data class NumberNode(var value: Float) : Expression()
    data class NameNode(var value: String) : Expression()
    data class AddNode(var operands: List<Expression>) : Expression()
    data class SubNode(var operands: List<Expression>) : Expression()
    data class MulNode(var operands: List<Expression>) : Expression()
    data class DivNode(var operands: List<Expression>) : Expression()
    data class PowNode(var operands: List<Expression>) : Expression()
    data class SequenceRange(var from: Expression, var to: Expression) : Expression()
    data class Sequence(var operands: List<Expression>) : Expression()

    data class Map(
        var sequence: Expression,
        var mapper: Lambda1
    ) : Expression()

    data class Reduce(
        var sequence: Expression,
        var primer: Expression,
        var reducer: Lambda2
    ) : Expression()
}


/**
 * Parsetree to AST converter, sort of =)
 * @param node Given the node value, that is supposed to be the raw parser's output
 * produces its simplified, AST like version.
 */
fun flattenParsetree(node: ASTNode) : ASTNode {
    return when (node) {
        is Lambda1 -> Lambda1(node.abs1, flattenParsetree(node.body) as Expression)
        is Lambda2 -> Lambda2(node.abs1, node.abs2, flattenParsetree(node.body) as Expression)
        is StringNode -> node
        is Statement.VarDeclarationStmt ->
            Statement.VarDeclarationStmt(
                node.name, flattenParsetree(node.expression) as Expression
            )
        is Statement.PrintStmt ->
            Statement.PrintStmt(flattenParsetree(node.string) as StringNode)
        is Statement.OutStmt ->
            Statement.OutStmt(flattenParsetree(node.expression) as Expression)
        is Expression.NumberNode -> node
        is Expression.SequenceRange ->
            Expression.SequenceRange(
                flattenParsetree(node.from) as Expression, flattenParsetree(node.to) as Expression
            )
        is Expression.Sequence ->
            Expression.Sequence(
                node.operands.map {flattenParsetree(it) as Expression}
            )
        is Expression.Map ->
            Expression.Map(
                flattenParsetree(node.sequence) as Expression,
                flattenParsetree(node.mapper) as Lambda1
            )
        is Expression.Reduce->
            Expression.Reduce(
                flattenParsetree(node.sequence) as Expression,
                flattenParsetree(node.primer) as Expression,
                flattenParsetree(node.reducer) as Lambda2,
            )
        is Expression.NameNode -> node
        // Super hideous self repetition, dunno what to do here
        // Can Kotlin do Union types or be somewhat useful with guarding?
        is Expression.AddNode -> {
            if (node.operands.size == 1) {
                flattenParsetree(node.operands[0])
            } else {
                Expression.AddNode(node.operands.map { flattenParsetree(it) as Expression })
            }
        }
        is Expression.SubNode -> {
            if (node.operands.size == 1) {
                flattenParsetree(node.operands[0])
            } else {
                Expression.SubNode(node.operands.map { flattenParsetree(it) as Expression })
            }
        }
        is Expression.MulNode -> {
            if (node.operands.size == 1) {
                flattenParsetree(node.operands[0])
            } else {
                Expression.MulNode(node.operands.map { flattenParsetree(it) as Expression })
            }
        }
        is Expression.DivNode -> {
            if (node.operands.size == 1) {
                flattenParsetree(node.operands[0])
            } else {
                Expression.DivNode(node.operands.map { flattenParsetree(it) as Expression })
            }
        }
        is Expression.PowNode -> {
            if (node.operands.size == 1) {
                flattenParsetree(node.operands[0])
            } else {
                Expression.PowNode(node.operands.map { flattenParsetree(it) as Expression})
            }
        }
        else -> node
    }
}


class LangParser {
    val numberP = decimal.map { Expression.NumberNode(it.toFloat()) }.token()
    // Parses varnames.
    val nameP = alpha.atLeastOne().token().text().map { Expression.NameNode(it) }
    val stringP =
        alpha
            .or(digit)
            .or(whitespace)
            .many()
            .between(char('"'), char('"'))
            .text()
            .token()
            .map { StringNode(it) }
    // Parses a sequence range expression:
    // {rangeFrom .. rangeTo}
    val sequenceRangeP =
        defer(::addOpP)
            .skipRight(symbol("..").token())
            .bind {
                rangeFrom ->
                    defer(::addOpP)
                        .bind {
                            rangeTo -> pure(Expression.SequenceRange(rangeFrom, rangeTo))
                        }
            }
            .between(
                char('{').token(), char('}').token()
            )
    // Parses a sequence literal:
    // {expression0, expression1, ..., expressionn}
    // ex: {1, 2, reduce({1..500}, 0, a b -> a + b)}
    val sequenceP =
        defer(::addOpP)
            .delimitedBy(symbol(",").token())
            .bind {
                operands -> pure(Expression.Sequence(operands))
            }
            .between(
                char('{').token(), char('}').token()
            )
    // Parses map expression:
    // map(expression, mapper)
    // where mapper is a lambda1 thingy
    val mapP =
        symbol("map").token()
            .skipLeft(
                defer(::addOpP) // sequence
                    .skipRight(symbol(",").token())
                    .bind {
                        sequence ->
                            lambda1P // mapper
                                .bind {
                                    mapper -> pure(Expression.Map(sequence, mapper))
                                }
                    }
                    .between(
                        char('(').token(), char(')').token()
                    )
            )
    // Parses reduce expression:
    // reduce(expression, primer, reducer)
    // where reducer is a lambda2 thingy
    val reduceP =
        symbol("reduce").token()
            .skipLeft(
                defer(::addOpP) // sequence
                    .skipRight(symbol(",").token())
                    .bind {
                        sequence ->
                            defer(::addOpP) // primer
                                .skipRight(symbol(",").token())
                                .bind {
                                    primer ->
                                        lambda2P // reducer
                                            .bind {
                                                reducer ->
                                                    pure(Expression.Reduce(sequence, primer, reducer))
                                            }
                                }
                    }
                    .between(
                        char('(').token(), char(')').token()
                    )
            )
    private val powOperandP =
        defer(::addOpP).between(char('(').token(), char(')').token())
            .or(sequenceRangeP)
            .or(sequenceP)
            .or(numberP)
            .or(reduceP)
            .or(mapP)
            .or(nameP)
            .token()
    // powOperand ^ powOperand ^ ... ^ powOperand
    private val divOperandP =
        powOperandP
            .delimitedBy(symbol("^").token())
            .map { Expression.PowNode(it) }
    // Parses division expression.
    // divOperand / divOperand / divOperand
    private val mulOperandP =
        divOperandP
            .delimitedBy(symbol("/").token())
            .map { Expression.DivNode(it) }
    // Parses multiplication expression.
    // mulOperand * mulOperand * ... * mulOperand
    private val subOperandP =
        mulOperandP
            .delimitedBy(symbol("*").token())
            .map { Expression.MulNode(it) }
    // Parses subtraction expression.
    // subOperand - subOperand - ... - subOperand
    private val addOperandP =
        subOperandP
            .delimitedBy(symbol("-").token())
            .map { Expression.SubNode(it) }
    // Parses addition expression.
    // For the matter of simplicity it also represents the top-level nonterminal for any expression.
    // addOperandP + addOperandP + ... + addOperandP
    private fun addOpP() : Parser<Expression> =
         addOperandP
            .delimitedBy(symbol("+").token())
            .map { Expression.AddNode(it) }
    // Parses lambda with a single arguments
    // abs1name -> body
    val lambda1P =
        nameP // abs1name
            .skipRight(symbol("->").token())
            .bind {
                abs1name ->
                    expressionP // body
                        .bind {
                            body -> pure(Lambda1(abs1name, body)).token()
                        } 
            }
    // Parses  lambda with a pair of arguments:
    // abs1name abs2name -> body
    val lambda2P =
        nameP // abs1name
            .bind {
                abs1name ->
                    nameP // abs2name
                        .skipRight(symbol("->").token())
                        .bind {
                            abs2name ->
                                expressionP // body
                                    .bind {
                                         body ->
                                            pure(Lambda2(abs1name, abs2name, body)).token()
                                    }
                        }
            }
    // Parses any expression, here it so happend to be AddOpP
    val expressionP: Parser<Expression> = addOpP()
    // Parses variable binding statement:
    // var varname = expression
    val assignStatementP =
        symbol("var").token()
            .skipLeft(
                nameP // varname
                    .skipRight(symbol("=").token())
                    .bind {
                        varname ->
                            expressionP // expression
                                .bind {
                                    expression ->
                                        pure(Statement.VarDeclarationStmt(varname, expression))
                                }
                    }
            )
    // Parses expression-printing statement:
    // out expression
    val outStatementP =
        symbol("out").token()
            .skipLeft(
                expressionP // expression
                    .bind {
                        expression ->
                            pure(Statement.OutStmt(expression))
                    }
            )
    // Parses string-printing statement:
    // print "somestring"
    val printStatementP =
        symbol("print").token()
            .skipLeft(
                stringP // The string itself
                    .bind {
                        string ->
                            pure(Statement.PrintStmt(string))
                    }
            )
    // Parses any statement
    val statementP = 
        assignStatementP
            .or(outStatementP)
            .or(printStatementP)
            .map {
                flattenParsetree(it)
            }
    // Use this to parse your code
    val wholeProgramP =
        statementP
            .delimitedBy(char('\n').many())
            .skipRight(eof)
}


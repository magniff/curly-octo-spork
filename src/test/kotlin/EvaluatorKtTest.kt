import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

import KotZen.parse

import Evaluator.*
import Result.*
import Parser.*


import kotlinx.coroutines.runBlocking

internal class EvaluatorKtTest {
    @Test
    fun test_evalName() {
        /**
         * Trivial test for the name resolver
         * var == 10
         */
        assertEquals(
            Success(Expression.NumberNode(10.0.toFloat())),
            runBlocking { evaluateExpression(
                Expression.NameNode("var"),
                mapOf(Expression.NameNode("var") to Expression.NumberNode(10.0.toFloat()))
            ) }
        )
    }
    @Test
    fun test_evalNumber() {
        /**
         * Number literals evaluates to themselves
         * 10 == 10
         */
        assertEquals(
            Success(Expression.NumberNode(10.0.toFloat())),
            runBlocking { evaluateExpression(Expression.NumberNode(10.0.toFloat()), emptyMap()) }
        )
    }
    @Test
    fun test_evalSequenceRange() {
        /**
         * {0 .. 2} == {0 .. 2}
         */
        assertEquals(
            Success(
                Expression.SequenceRange(
                    Expression.NumberNode(0.0.toFloat()),
                    Expression.NumberNode(2.0.toFloat())
                ),
            ),
            runBlocking { evaluateExpression(
                Expression.SequenceRange(
                    Expression.NumberNode(0.0.toFloat()),
                    Expression.NumberNode(2.0.toFloat())
                ),
                emptyMap()
            )}
        )
        /**
         * {0 .. 1 + 3} == {0, 1, 2, 3, 4}
         */
        assertEquals(
            Success(
                Expression.SequenceRange(
                    Expression.NumberNode(0.0.toFloat()),
                    Expression.NumberNode(4.0.toFloat()),
                )
            ),
            runBlocking {
                evaluateExpression(
                    Expression.SequenceRange(
                        Expression.NumberNode(0.0.toFloat()),
                        Expression.AddNode(
                            listOf(
                                Expression.NumberNode(1.0.toFloat()),
                                Expression.NumberNode(3.0.toFloat())
                            )
                        )
                    ),
                    emptyMap()
                )
            }
        )
    }
    @Test
    fun test_evalAdd() {
        /**
         * 5 + 6 + 7 + 8 == 26
         */
        assertEquals(
            Success(Expression.NumberNode(26.0.toFloat())),
            runBlocking {
                evaluateExpression(
                    Expression.AddNode(
                        listOf(
                            Expression.NumberNode(5.0.toFloat()),
                            Expression.NumberNode(6.0.toFloat()),
                            Expression.NumberNode(7.0.toFloat()),
                            Expression.NumberNode(8.0.toFloat())
                        )
                    ),
                    // No context needed for the test
                    emptyMap()
                )
            }
        )
        /**
         * The same things, but via the name resolution
         * a + b + c + d == 26
         */
        assertEquals(
            Success(Expression.NumberNode(26.0.toFloat())),
            runBlocking {
                evaluateExpression(
                    Expression.AddNode(
                        listOf(
                            Expression.NameNode("a"),
                            Expression.NameNode("b"),
                            Expression.NameNode("c"),
                            Expression.NameNode("d"),
                        )
                    ),
                    mapOf(
                        Expression.NameNode("a") to Expression.NumberNode(5.0.toFloat()),
                        Expression.NameNode("b") to Expression.NumberNode(6.0.toFloat()),
                        Expression.NameNode("c") to Expression.NumberNode(7.0.toFloat()),
                        Expression.NameNode("d") to Expression.NumberNode(8.0.toFloat()),
                    )
                )
            }
        )
    }
    @Test
    fun test_evalMul() {
        /**
         * 5 * 6 * 7 == 210
         */
        assertEquals(
            Success(Expression.NumberNode(210.0.toFloat())),
            runBlocking {
                evaluateExpression(
                    Expression.MulNode(
                        listOf(
                            Expression.NumberNode(5.0.toFloat()),
                            Expression.NumberNode(6.0.toFloat()),
                            Expression.NumberNode(7.0.toFloat()),
                        )
                    ),
                    emptyMap()
                )
            }
        )
    }
    @Test
    fun test_evalPow() {
        /**
         * 2 ^ 3 ^ 4 == (2 ^ 3) ^ 4 == 4096
         * though associativity for ^ is often chosen the other way around
         */
        assertEquals(
            Success(Expression.NumberNode(4096.0.toFloat())),
            runBlocking {
                evaluateExpression(
                    Expression.PowNode(
                        listOf(
                            Expression.NumberNode(2.0.toFloat()),
                            Expression.NumberNode(3.0.toFloat()),
                            Expression.NumberNode(4.0.toFloat()),
                        )
                    ),
                    emptyMap()
                )
            }
        )
    }
    @Test
    fun test_evalSub() {
        /**
         * 5 - 6 - 7 == -8
         */
        assertEquals(
            Success(Expression.NumberNode(-8.0.toFloat())),
            runBlocking {
                evaluateExpression(
                    Expression.SubNode(
                        listOf(
                            Expression.NumberNode(5.0.toFloat()),
                            Expression.NumberNode(6.0.toFloat()),
                            Expression.NumberNode(7.0.toFloat())
                        )
                    ),
                    emptyMap()
                )
            }
        )
    }
    @Test
    fun test_evalDiv() {
        /**
         * 4 / 8 / 2 == 0.25
         */
        assertEquals(
            Success(Expression.NumberNode(0.25.toFloat())),
            runBlocking {
                evaluateExpression(
                    Expression.DivNode(
                        listOf(
                            Expression.NumberNode(4.0.toFloat()),
                            Expression.NumberNode(8.0.toFloat()),
                            Expression.NumberNode(2.0.toFloat())
                        )
                    ),
                    emptyMap()
                )
            }
        )
    }
    @Test
    fun test_evalMap() {
        /**
         * map({1, 1 + 1, 3}, value -> value + 1) == {2, 3, 4}
         */
        assertEquals(
            Success(
                Expression.Sequence(
                    listOf(
                        Expression.NumberNode(2.0.toFloat()),
                        Expression.NumberNode(3.0.toFloat()),
                        Expression.NumberNode(4.0.toFloat()),
                    )
                )
            ),
            runBlocking {
                evaluateExpression(
                    Expression.Map(
                        Expression.Sequence(
                            listOf(
                                Expression.NumberNode(1.0.toFloat()),
                                Expression.AddNode(
                                    listOf(
                                        Expression.NumberNode(1.0.toFloat()),
                                        Expression.NumberNode(1.0.toFloat()),
                                    )
                                ),
                                Expression.NumberNode(3.0.toFloat())
                            )
                        ),
                        Lambda1(
                            Expression.NameNode("value"),
                            Expression.AddNode(
                                listOf(
                                    Expression.NameNode("value"),
                                    Expression.NumberNode(1.0.toFloat())
                                )
                            )
                        )
                    ),
                    emptyMap()
                )
            }
        )
        /**
         * map({0 .. 4}, value -> value + 1) == {1, 2, 3, 4, 5}
         */
        assertEquals(
            Success(
                Expression.Sequence(
                    listOf(
                        Expression.NumberNode(1.0.toFloat()),
                        Expression.NumberNode(2.0.toFloat()),
                        Expression.NumberNode(3.0.toFloat()),
                        Expression.NumberNode(4.0.toFloat()),
                        Expression.NumberNode(5.0.toFloat()),
                    )
                )
            ),
            runBlocking {
                evaluateExpression(
                    Expression.Map(
                        Expression.SequenceRange(
                            Expression.NumberNode(0.0.toFloat()),
                            Expression.NumberNode(4.0.toFloat()),
                        ),
                        Lambda1(
                            Expression.NameNode("value"),
                            Expression.AddNode(
                                listOf(
                                    Expression.NameNode("value"),
                                    Expression.NumberNode(1.0.toFloat())
                                )
                            )
                        )
                    ),
                    emptyMap()
                )
            }
        )
    }
    @Test
    fun test_evalReduce() {
        /**
         * reduce({}, 5, x y -> x + y) == 5
         */
        assertEquals(
            Success(Expression.NumberNode(5.0.toFloat())),
            runBlocking {
                evaluateExpression(
                    Expression.Reduce(
                        Expression.Sequence(
                            listOf()
                        ),
                        Expression.NumberNode(5.0.toFloat()),
                        Lambda2(
                            Expression.NameNode("x"),
                            Expression.NameNode("y"),
                            Expression.AddNode(
                                listOf(
                                    Expression.NameNode("x"),
                                    Expression.NameNode("y"),
                                )
                            )
                        )
                    ),
                    emptyMap()
                )
            }
        )
        /**
         * reduce({1}, 0, x y -> x + y) == 1
         */
        assertEquals(
            Success(Expression.NumberNode(1.0.toFloat())),
            runBlocking {
                evaluateExpression(
                    Expression.Reduce(
                        Expression.Sequence(
                            listOf(
                                Expression.NumberNode(1.0.toFloat()),
                            )
                        ),
                        Expression.NumberNode(0.0.toFloat()),
                        Lambda2(
                            Expression.NameNode("x"),
                            Expression.NameNode("y"),
                            Expression.AddNode(
                                listOf(
                                    Expression.NameNode("x"),
                                    Expression.NameNode("y"),
                                )
                            )
                        )
                    ),
                    emptyMap()
                )
            }
        )
        /**
         * reduce({1, 2, 3}, 0, x y -> x + y) == 6
         */
        assertEquals(
            Success(Expression.NumberNode(6.0.toFloat())),
            runBlocking {
                evaluateExpression(
                    Expression.Reduce(
                        Expression.Sequence(
                            listOf(
                                Expression.NumberNode(1.0.toFloat()),
                                Expression.NumberNode(2.0.toFloat()),
                                Expression.NumberNode(3.0.toFloat())
                            )
                        ),
                        Expression.NumberNode(0.0.toFloat()),
                        Lambda2(
                            Expression.NameNode("x"),
                            Expression.NameNode("y"),
                            Expression.AddNode(
                                listOf(
                                    Expression.NameNode("x"),
                                    Expression.NameNode("y"),
                                )
                            )
                        )
                    ),
                    emptyMap()
                )
            }
        )
    }
    @Test
    fun test_printNumber() {
        assertEquals(
            Success("-5.0"),
            runBlocking {
                expressionToString(
                    Expression.NumberNode(-5.0.toFloat()),
                    emptyMap()
                )
            }
        )
        assertEquals(
            Success("0.0"),
            runBlocking {
                expressionToString(
                    Expression.NumberNode(0.0.toFloat()),
                    emptyMap()
                )
            }
        )
        assertEquals(
            Success("10.0"),
            runBlocking {
                expressionToString(
                    Expression.NumberNode(10.0.toFloat()),
                    emptyMap()
                )
            }
        )
    }
    @Test
    fun test_printSequence() {
        assertEquals(
            Success("{}"),
            runBlocking {
                expressionToString(
                    Expression.Sequence(
                        listOf(
                        )
                    ),
                    emptyMap()
                )
            }
        )
        assertEquals(
            Success("{0.0, 1.0, 2.0}"),
            runBlocking {
                expressionToString(
                    Expression.Sequence(
                        listOf(
                            Expression.NumberNode(0.0.toFloat()),
                            Expression.NumberNode(1.0.toFloat()),
                            Expression.NumberNode(2.0.toFloat()),
                        )
                    ),
                    emptyMap()
                )
            }
        )
        assertEquals(
            Success("{0.0 .. 2.0}"),
            runBlocking {
                expressionToString(
                    Expression.SequenceRange(
                        Expression.NumberNode(0.0.toFloat()),
                        Expression.NumberNode(2.0.toFloat()),
                    ),
                    emptyMap()
                )
            }
        )
    }
    @Test
    fun test_printName() {
        assertEquals(
            Success("10.0"),
            runBlocking {
                expressionToString(
                    Expression.NameNode("foo"),
                    mapOf(Expression.NameNode("foo") to Expression.NumberNode(10.0.toFloat()))
                )
            }
        )
        assertEquals(
            Success("10.0"),
            runBlocking {
                expressionToString(
                    Expression.NameNode("foo"),
                    mapOf(
                        Expression.NameNode("foo") to
                                Expression.AddNode(
                                    listOf(
                                        Expression.NumberNode(5.0.toFloat()),
                                        Expression.NumberNode(4.0.toFloat()),
                                        Expression.NumberNode(1.0.toFloat())
                                    )
                                )
                    )
                )
            }
        )
    }
    @Test
    fun test_evalWholeProgram() {
       val parser = LangParser()
       val code =
       """
            var n = 500
            var sequence = map({0 .. n}, i -> (-1)^i / (2.0 * i + 1))
            var pi = 4 * reduce(sequence, 0, x y -> x + y)
            print "pi is "
            out pi
       """
        assertEquals(
            Success("pi is 3.143589"),
            runBlocking {
                evaluateStmtList(
                    parser.wholeProgramP.parse(code).second!! as List<Statement>,
                    mutableMapOf()
                )
            }
        )
    }
}

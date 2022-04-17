import KotZen.*
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import Parser.*

internal class ParserKtTest {
    @Test
    fun test_numberP() {
        assertEquals(Expression.NumberNode(10.0.toFloat()), LangParser().numberP.parse("10").second)
        assertEquals(Expression.NumberNode(2.0.toFloat()), LangParser().numberP.parse("2").second)
        assertEquals(Expression.NumberNode(-2.78.toFloat()), LangParser().numberP.parse(" -2.78   ").second)
//        assertEquals(Expression.NumberNode(10.01.toFloat()), Parser().parse("10.01"))
    }
    @Test
    fun test_stringP() {
        assertEquals(StringNode("hello"), LangParser().stringP.parse("\"hello\"").second)
        assertEquals(StringNode("hello world"), LangParser().stringP.parse("\"hello world\"").second)
        // assertEquals(StringNode(""), LangParser().stringP.parse(""))
    }
    @Test
    fun test_nameP() {
        assertEquals(Expression.NameNode("hello"), LangParser().nameP.parse("hello").second)
        assertEquals(Expression.NameNode("hello"), LangParser().nameP.parse(" hello ").second)
    }
    @Test
    fun test_sequenceP() {
        assertEquals(
            Expression.Sequence(
                listOf(
                    Expression.NumberNode(0.0.toFloat()),
                    Expression.NumberNode(5.0.toFloat()),
                    Expression.AddNode(
                        listOf(
                            Expression.NumberNode(1.0.toFloat()),
                            Expression.NumberNode(7.0.toFloat()),
                            Expression.NameNode("hello"),
                        )
                    )
                )
            ),
            LangParser()
                .sequenceP
                .map { flattenParsetree(it) }
                .parse(" {0, 5, 1 + 7 + hello} ")
                .second
        )
    }
    @Test
    fun test_sequenceRangeP() {
        assertEquals(
            Expression.SequenceRange(
                Expression.NumberNode(0.0.toFloat()),
                Expression.NumberNode(5.0.toFloat())
            ),
            LangParser()
                .sequenceRangeP
                .map { flattenParsetree(it) }
                .parse(" { 0 .. 5   } ")
                .second
        )
        assertEquals(
            Expression.SequenceRange(
                Expression.NumberNode(0.0.toFloat()),
                Expression.AddNode(
                    listOf(
                        Expression.NumberNode(1.0.toFloat()),
                        Expression.MulNode(
                            listOf(
                                Expression.NumberNode(2.0.toFloat()),
                                Expression.NumberNode(3.0.toFloat())
                            )
                        )
                    )
                )
            ),
            LangParser()
                .sequenceRangeP
                .map { flattenParsetree(it) }
                .parse(" { 0 .. 1 + 2 * 3   } ")
                .second
        )
    }
    @Test
    fun test_expressionP() {
        assertEquals(
            Expression.NumberNode(10.0.toFloat()),
            LangParser()
                .expressionP
                .map { flattenParsetree(it) }
                .parse("10")
                .second
        )
        assertEquals(
            Expression.NumberNode(10.0.toFloat()),
            LangParser()
                .expressionP
                .map { flattenParsetree(it) }
                .parse("( 10 )")
                .second
        )
        assertEquals(
            Expression.AddNode(
                listOf(
                    Expression.NumberNode(10.0.toFloat()),
                    Expression.NumberNode(20.0.toFloat()))
            ),
            LangParser()
                .expressionP
                .map { flattenParsetree(it) }
                .parse("10 + 20")
                .second
        )
        assertEquals(
            Expression.AddNode(
                listOf(
                    Expression.NumberNode(10.0.toFloat()),
                    Expression.NumberNode(20.0.toFloat()))
            ),
            LangParser()
                .expressionP
                .map { flattenParsetree(it) }
                .parse("(10 + (   20))")
                .second
        )
        assertEquals(
            Expression.MulNode(
                listOf(
                    Expression.NumberNode(10.0.toFloat()),
                    Expression.NumberNode(20.0.toFloat()))
            ),
            LangParser()
                .expressionP
                .map { flattenParsetree(it) }
                .parse("10 * 20")
                .second
        )
        assertEquals(
            Expression.PowNode(
                listOf(
                    Expression.NumberNode(10.0.toFloat()),
                    Expression.NumberNode(20.0.toFloat()))
            ),
            LangParser()
                .expressionP
                .map { flattenParsetree(it) }
                .parse("10 ^ 20")
                .second
        )
        assertEquals(
            Expression.PowNode(
                listOf(
                    Expression.NumberNode(-1.0.toFloat()),
                    Expression.NameNode("i"))
            ),
            LangParser()
                .expressionP
                .map { flattenParsetree(it) }
                .parse("(-1) ^ i")
                .second
        )
        assertEquals(
            Expression.PowNode(
                listOf(
                    Expression.NumberNode(10.0.toFloat()),
                    Expression.NumberNode(20.0.toFloat()),
                    Expression.NumberNode(30.0.toFloat()))
            ),
            LangParser()
                .expressionP
                .map { flattenParsetree(it) }
                .parse("10 ^ 20 ^ 30")
                .second
        )
        assertEquals(
            Expression.MulNode(
                listOf(
                    Expression.NumberNode(10.0.toFloat()),
                    Expression.NumberNode(20.0.toFloat()),
                    Expression.NumberNode(30.0.toFloat()))
            ),
            LangParser()
                .expressionP
                .map { flattenParsetree(it) }
                .parse("10 * 20 * 30")
                .second
        )
        assertEquals(
            Expression.AddNode(
                listOf(
                    Expression.NumberNode(10.0.toFloat()),
                    Expression.NumberNode(20.0.toFloat()),
                    Expression.NumberNode(30.0.toFloat()))
            ),
            LangParser()
                .expressionP
                .map { flattenParsetree(it) }
                .parse("10 + 20 + 30")
                .second
        )
        assertEquals(
            Expression.AddNode(
                listOf(
                    Expression.NumberNode(1.0.toFloat()),
                    Expression.MulNode(
                        listOf(
                            Expression.NumberNode(2.0.toFloat()),
                            Expression.PowNode(
                                listOf(
                                    Expression.NumberNode(3.0.toFloat()),
                                    Expression.NumberNode(4.0.toFloat())
                                )
                            )
                        )
                    ),
                    Expression.NumberNode(5.0.toFloat()),
                )
            ),
            LangParser()
                .expressionP
                .map { flattenParsetree(it) }
                .parse("1 + 2 * 3 ^ 4 + 5")
                .second
        )
        assertEquals(
            Expression.MulNode(
                listOf(
                    Expression.AddNode(
                        listOf(
                            Expression.NumberNode(1.0.toFloat()),
                            Expression.NumberNode(2.0.toFloat()))
                    ),
                    Expression.PowNode(
                        listOf(
                            Expression.NumberNode(3.0.toFloat()),
                            Expression.NumberNode(4.0.toFloat())
                        )
                    )
                )
            ),
            LangParser()
                .expressionP
                .map { flattenParsetree(it) }
                .parse("(1 + 2) * 3 ^ 4")
                .second
        )
    }
   @Test
   fun test_lambda1P() {
       assertEquals(
           Lambda1(
               Expression.NameNode("hello"),
               Expression.NameNode("hello")),
           LangParser()
               .lambda1P
               .map { flattenParsetree(it) }
               .parse("hello -> hello")
               .second
       )
       assertEquals(
           Lambda1(
               Expression.NameNode("hello"),
               Expression.AddNode(
                   listOf(
                       Expression.MulNode(
                           listOf(
                               Expression.NumberNode(20.0.toFloat()),
                               Expression.NameNode("hello")
                           )
                       ),
                       Expression.NumberNode(10.0.toFloat())
                   )
               )
           ),
           LangParser()
               .lambda1P
               .map { flattenParsetree(it) }
               .parse("hello -> 20 * hello + 10 ")
               .second
       )
   }
   @Test
   fun test_lambda2P() {
       assertEquals(
           Lambda2(
               Expression.NameNode("hello"),
               Expression.NameNode("world"),
               Expression.NameNode("hello")),
           LangParser()
               .lambda2P
               .map { flattenParsetree(it) }
               .parse("hello world -> hello")
               .second
       )
   }
   @Test
   fun test_mapP() {
       assertEquals(
           Expression.Map(
               Expression.NameNode("hello"),
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
           LangParser()
               .mapP
               .map { flattenParsetree(it) }
               .parse("map(hello , value -> value + 1)")
               .second
       )
       assertEquals(
           Expression.Map(
               Expression.NumberNode(10.0.toFloat()),
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
           LangParser()
               .mapP
               .map { flattenParsetree(it) }
               .parse("map(10 , value -> value + 1)")
               .second
       )
       assertEquals(
           Expression.Map(
               Expression.SequenceRange(
                   Expression.NumberNode(10.0.toFloat()),
                   Expression.NumberNode(20.0.toFloat())
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
           LangParser()
               .mapP
               .map { flattenParsetree(it) }
               .parse("  map ({10 .. 20} , value -> value + 1) ")
               .second
       )
   }
   @Test
   fun test_reduceP() {
       assertEquals(
           Expression.Reduce(
               Expression.NameNode("hello"),
               Expression.NameNode("world"),
               Lambda2(
                   Expression.NameNode("left"),
                   Expression.NameNode("right"),
                   Expression.AddNode(
                       listOf(
                           Expression.NameNode("left"),
                           Expression.NameNode("right"),
                       )
                   )
               )
           ),
           LangParser()
               .reduceP
               .map { flattenParsetree(it) }
               .parse("reduce(hello, world, left right -> left + right)")
               .second
       )
       assertEquals(
            Expression.Reduce(
                Expression.SequenceRange(
                        Expression.NumberNode(0.0.toFloat()),
                        Expression.NumberNode(500.0.toFloat())
                ),
                Expression.NumberNode(0.0.toFloat()),
                Lambda2(
                    Expression.NameNode("left"),
                    Expression.NameNode("right"),
                    Expression.AddNode(
                        listOf(
                            Expression.NameNode("left"),
                            Expression.NameNode("right"),
                        )
                    )
                )
            ),
            LangParser()
                .reduceP
                .map { flattenParsetree(it) }
                .parse("reduce ({0..500}, 0, left right -> left + right)")
                .second
       )
   }
   @Test
   fun test_variableAssignmentP() {
       assertEquals(
           Statement.VarDeclarationStmt(
               Expression.NameNode("value"),
               Expression.NumberNode(10.0.toFloat())
           ),
           LangParser()
               .statementP
               .map { flattenParsetree(it) }
               .parse("var value = 10 ")
               .second
       )
   }
   @Test
   fun test_outP() {
       assertEquals(
           Statement.OutStmt(Expression.NumberNode(10.0.toFloat())),
           LangParser()
               .statementP
               .map { flattenParsetree(it) }
               .parse(" out 10 ")
               .second
       )
   }
   @Test
   fun test_printP() {
       assertEquals(
           Statement.PrintStmt(StringNode("hello world")),
           LangParser()
               .statementP
               .map { flattenParsetree(it) }
               .parse("print \"hello world\"")
               .second
       )
       assertEquals(
           "print \"hello".parsable(12),
           LangParser()
               .statementP
               .map { flattenParsetree(it) }
               .parse("print \"hello")
               .first
       )
   }
   @Test
   fun test_wholeProgramP() {
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
           listOf(
                Statement.VarDeclarationStmt(
                    Expression.NameNode("n"),
                    Expression.NumberNode(500.0.toFloat())
                ),
                Statement.VarDeclarationStmt(
                    Expression.NameNode("sequence"),
                    Expression.Map(
                        Expression.SequenceRange(
                            Expression.NumberNode(0.0.toFloat()),
                            Expression.NameNode("n")
                        ),
                        Lambda1(
                            Expression.NameNode("i"),
                            Expression.DivNode(
                                listOf(
                                    Expression.PowNode(
                                        listOf(
                                            Expression.NumberNode(-1.0.toFloat()),
                                            Expression.NameNode("i")
                                        )
                                    ),
                                    Expression.AddNode(
                                        listOf(
                                            Expression.MulNode(
                                                listOf(
                                                    Expression.NumberNode(2.0.toFloat()),
                                                    Expression.NameNode("i"),
                                                )
                                            ),
                                            Expression.NumberNode(1.0.toFloat())
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                Statement.VarDeclarationStmt(
                    Expression.NameNode("pi"),
                    Expression.MulNode(
                        listOf(
                            Expression.NumberNode(4.0.toFloat()),
                            Expression.Reduce(
                                Expression.NameNode("sequence"),
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
                            )
                        )
                    )
                ),
                Statement.PrintStmt(StringNode("pi is ")),
                Statement.OutStmt(Expression.NameNode("pi"))
           ),
           parser.wholeProgramP.parse(code).second!! as List<Statement>
       )
   }
}
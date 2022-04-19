package KotZen


/**
 * Input to the parser
 */
interface Parsable {
    /**
     * Consumes input by a number of characters
     * @return The remaining input after consumption
     */
    fun advance(steps: Int) : Parsable

    /**
     * @return The number of characters that remain unparsed
     */
    fun remaining() : Int

    /**
     * @return The next character
     */
    fun head() : Char

    /**
     * @return Whether the end of the input has been reached
     */
    fun isEmpty() = remaining() == 0

    /**
     * @return The unparsed input
     */
    fun unparsed() : String
}

/**
 * A parser takes an input, which is parseable, and returns a typed output
 */
typealias Parser<T> = (Parsable) -> Pair<Parsable, T?>

/**
 * Wraps a string with a pointer to the head of the remaining unparsed input
 * to prevent repeatedly creating substrings
 */
data class StringParsable(private val span: Span) : Parsable {
    override fun advance(steps: Int): Parsable = StringParsable(span.substring(steps))
    override fun remaining(): Int = span.length
    override fun head(): Char = span.value[span.start]
    override fun unparsed(): String = span.value.substring(span.start)
}

/**
 * Helper function to wrap a string for input to a parser
 * @param index The start of the unparsed input
 * @return The wrapped string to input to a parser
 */
fun String.parsable(index : Int = 0) = StringParsable(Span(this, index))

/**
 * Helper function to actually run the parser
 * @param input The string to be parsed
 * @return The typed result of parsing
 * @throws IllegalStateException When unparsed input remains, or cannot be parsed at all
 */
fun <T> Parser<T>.parse(input: String): Pair<Parsable, T?> {
    return this(input.parsable())
}

/**
 * Returns a parser that always succeeds.
 * The result is the unconsumed input and the value passed to this function
 */
fun <T> pure(default: T) = {
    input: Parsable -> Pair(input, default)
}

/**
 * Maps a parser output to a new value
 * @param fn A function to transform the parser output
 */
fun <S, T> Parser<S>.map(mapper: (S) -> T): Parser<T> = {
    input: Parsable ->
        val (remaining, output) = this(input)
        when (output) {
            null -> Pair(remaining, null)
            else -> Pair(remaining, mapper(output))
        }
}

/**
 * Maps a parser output to a new value
 * @param value The new value to return
 */
fun <S, T> Parser<S>.mapTo(value: T) = this.map { value }

/**
 * Parses a character successfully if the given predicate returns true
 * @param fn Predicate that must be satisfied
 */
fun sat(fn: (Char) -> Boolean) = {
    input: Parsable ->
        if (input.isEmpty() || !fn(input.head())) {
            Pair(input, null)
        }
        else {
            Pair(input.advance(1), input.head())
        }
}

/**
 * Returns a parser that always fails
 */
fun <T> fail(): Parser<T> = { Pair(it, null) }

/**
 * A parser that consumes any character
 */
val char = sat { true }

/**
 * A parser that consumes the given character
 */
fun char(c: Char) = sat { it == c }

/**
 * Returns a parser that joins a parsed collection of characters to a string
 */
fun Parser<Iterable<Char>>.text() = this.map { it.joinToString("") }

/**
 * A parser that consumes a digit
 */
val digit = sat(Char::isDigit)

/**
 * A parser that parses EOF
 */
val eof = {
    input: Parsable ->
        if (input.isEmpty()) {
            Pair(input, Unit)
        } else {
            Pair(input, null)
        }
}

/**
 * A parser that consumes at least one digit to create a number
 */
val number = digit.atLeastOne().map { it.joinToString("").toInt() }

/**
 * A parser that consumes a positive or negative whole number
 */
val integer =
    char('-').bind {
        number.map(Math::negateExact)
    }.or(number)

/**
 * A parser that consumes a decimal number
 */
val decimal =
    integer
        .skipRight(char('.'))
        .bind {
                beforeDot ->
                    digit
                        .atLeastOne()
                        .map {it.joinToString("") }
                        .map { afterDot -> "$beforeDot.$afterDot".toDouble() }
        }
        .or(integer.map(Int::toDouble))

/**
 * A parser that consumes a whitespace character according to the unicode standard
 */
val whitespace = sat(Char::isWhitespace)

/**
 * Consumes the whitespace around a given parser
 */
fun <T> Parser<T>.token() = this.between(whitespace.many())

/**
 * A parser that consumes a letter
 */
val alpha = sat(Char::isLetter)

/**
 * Returns a parser that returns the first successful result
 * @param alternative The parser to try if this one fails
 * @return The result from this parser, or failing that the alternative
 */
fun <T> Parser<T>.or(alternative: Parser<T>) = {
    input: Parsable ->
        var (rest, result) = this(input)
        when(result) {
            null -> alternative(input)
            else -> Pair(rest, result)
        }
}

/**
 * Returns a parser that returns the first successful result
 */
fun <T> Iterable<Parser<T>>.first() = this.fold(fail<T>()) { a, b -> a.or(b) }

/**
 * Returns a parser that consumes 0 or more
 */
fun <T> Parser<T>.many(): Parser<Iterable<T>> =
    this.atLeastOne().or(pure(listOf()))

/**
 * Returns a parser that consumes 1 or more
 */
fun <T> Parser<T>.atLeastOne() =
    this.bind { this.many().map { xs -> listOf(it) + xs } }

/**
 * Flattens multiple parsers (called flatMap elsewhere)
 * @param fn Given the result of the current parser, return a new parser
 */
fun <T, S> Parser<T>.bind(fn: (T) -> Parser<S>) = {
    input: Parsable ->
        val (remaining, output) = this(input)
        when(output) {
            null -> Pair(remaining, null)
            else -> fn(output)(remaining)
        }
}

/**
 * Returns a parser for input separated by a delimiter, e.g. 1,2,3
 * @param delimiter The parser for the delimiter, e.g. char(',') for a CSV file
 */
fun <T, S> Parser<T>.delimitedBy(delimiter: Parser<S>) =
    this.bind { delimiter.skipLeft(this).many().map { xs -> listOf(it) + xs } }

/**
 * Returns a parser for input surrounded by a pair of parsers
 * @param p The parser to the left, and the parser to the right
 */
fun <T, S, U> Parser<T>.between(p: Pair<Parser<S>, Parser<U>>) = this.between(p.first, p.second)

/**
 * Returns a parser for input surrounded the same parser to the left and to the right
 * @param p The parser to the left, and the parser to the right
 */
fun <T, S> Parser<T>.between(p: Parser<S>) = this.between(p, p)

/**
 * Returns a parser for input surrounded by a pair of parsers
 * @param left The parser to the left of the input
 * @param right The parser to the right of the input
 */
fun <T, S, U> Parser<T>.between(left: Parser<S>, right: Parser<U>) =
    left.skipLeft(this.skipRight(right))

/**
 * Returns a parser that ignores the successful result of this
 * @param next The parser whose result will be returned
 */
fun <T, S> Parser<T>.skipLeft(next: Parser<S>) = this.bind { next }

/**
 * Returns a parser that uses this parser's output, ignoring the successful result of the next
 * @param next The parser whose result will be ignored
 */
fun <T, S> Parser<T>.skipRight(next: Parser<S>) = this.bind { next.mapTo(it) }

/**
 * Returns a parser that consumes a given string
 * @throws IllegalStateException When value is empty
 * @param value The string to match
 */
fun symbol(value: String): Parser<String> = symbol(Span(value))

/**
 * Returns a parser that consumes a given string. This overload can be used to avoid string allocations
 * @throws IllegalStateException When value is empty
 * @param value The string/substring to match
 */
fun symbol(value: Span): Parser<String> =
    if (value.length == 0) {
        pure("")
    }
    else {
        char(value.value[value.start])
            .bind {
                symbol(value.substring(1)).map { xs -> it + xs }
            }
    }

/**
 * Defers constructing a parser until it's needed. Useful when parsers create circular references
 * @param fn The function to lazily create a parser
 */
fun <T> defer(fn: () -> Parser<T>) = { input: Parsable -> fn()(input) }

/**
 * Helper class to simulate substrings of an original string
 * @param value The original string
 * @param start The head of the substring
 */
data class Span(val value: String, val start: Int = 0) {
    val length = value.length - start
    fun substring(start: Int) = Span(value, this.start + start)
}

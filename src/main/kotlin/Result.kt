package Result


// Kotlin has its own stdlib implementation of Result type, which looks a bit
// wierd though https://stackoverflow.com/questions/56900407/does-kotlin-have-a-result-type-like-swift
sealed class Result<out Success, out Failure>


data class Success<out Success>(val value: Success) : Result<Success, Nothing>()
data class Failure<out Failure>(val reason: Failure) : Result<Nothing, Failure>()


fun <R, E, T> Result<R, E>.map(fn: (R) -> T): Result<T, E> {
    return when(this) {
        is Success -> Success(fn(this.value))
        is Failure -> Failure(this.reason)
    }
}


fun <R, E, T> Result<R, E>.bind(fn: (R) -> Result<T, E>): Result<T, E> {
    return when(this) {
        is Success -> fn(this.value)
        is Failure -> Failure(this.reason)
    }
}

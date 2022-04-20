package Result


import kotlinx.coroutines.yield


// Kotlin has its own stdlib implementation of Result type, which looks a bit
// wierd though https://stackoverflow.com/questions/56900407/does-kotlin-have-a-result-type-like-swift
sealed class Result<out Success, out Failure>


data class Success<out Success>(val value: Success) : Result<Success, Nothing>()
data class Failure<out Failure>(val reason: Failure) : Result<Nothing, Failure>()


suspend fun <R, E, T> Result<R, E>.map(fn: suspend (R) -> T): Result<T, E> {
    return when(this) {
        is Success -> Success(fn(this.value))
        is Failure -> Failure(this.reason)
    }
}


suspend fun <R, E, T> Result<R, E>.bind(fn: suspend (R) -> Result<T, E>): Result<T, E> {
    return when(this) {
        is Success -> fn(this.value)
        is Failure -> Failure(this.reason)
    }
}


fun <R, E> Result<R, E>.isSuccess(): Boolean {
    return when(this) {
        is Success -> true
        is Failure -> false
    }
}


fun <T, E> T.success(): Result<T, E> {
    return Success(this)
}


fun <T, E> E.fail(): Result<T, E> {
    return Failure(this)
}


suspend fun <R, E, T> Iterable<T>.mapM(fn: suspend (T) -> Result<R, E>): Result<List<R>, E> {
    var listToWrap: MutableList<R> = mutableListOf()
    for (item in this) {
        /**
         * Going back to the caller on each iteration
         */
        yield()
        when(val mappingResult = fn(item)) {
            is Success -> listToWrap.add(mappingResult.value)
            is Failure -> {
                return mappingResult
            }
        }
    }
    return listToWrap.success()
}

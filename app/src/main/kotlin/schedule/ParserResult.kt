package schedule

sealed interface ParserResult {
    data class Success(
        val session: Session
    ) : ParserResult

    data class Failure(
        val numberLine: Long,
        val error: ParseError    // используем специально для преобразования в англ
    ) : ParserResult
}
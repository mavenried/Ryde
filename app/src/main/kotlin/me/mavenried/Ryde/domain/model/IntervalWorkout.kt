package me.mavenried.Ryde.domain.model

enum class StepType { WORK, REST }

data class IntervalWorkout(
    val workMs: Long,
    val restMs: Long,
    val repeat: Int,
) {
    val totalSteps: Int get() = repeat * 2
    fun stepType(index: Int): StepType = if (index % 2 == 0) StepType.WORK else StepType.REST
    fun stepDurationMs(index: Int): Long = if (index % 2 == 0) workMs else restMs
}

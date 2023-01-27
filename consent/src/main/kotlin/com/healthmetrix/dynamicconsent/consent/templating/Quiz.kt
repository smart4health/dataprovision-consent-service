package com.healthmetrix.dynamicconsent.consent.templating

data class Quiz(
    val introduction: QuizIntroduction,
    val questions: List<QuizQuestion>,
    val failed: QuizFailed,
)

data class QuizIntroduction(
    val title: String,
    val image: String,
    val content: String,
    val navigation: QuizIntroductionNavigation = QuizIntroductionNavigation(),
)

data class QuizAnswer(val answer: String, val correct: Boolean = false)

data class QuizQuestion(
    val question: String,
    val answers: List<QuizAnswer>,
    val navigation: QuizQuestionNavigation = QuizQuestionNavigation(),
)

data class QuizFailed(
    val title: String,
    val content: String,
    val navigation: QuizFailedNavigation = QuizFailedNavigation(),
)

data class QuizIntroductionNavigation(
    val next: String? = null,
    val back: String? = null,
)

data class QuizQuestionNavigation(
    val next: String? = null,
    val back: String? = null,
    val answer: String? = null,
)

data class QuizFailedNavigation(
    val next: String? = null,
)

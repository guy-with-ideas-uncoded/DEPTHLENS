package com.example.data.model

import androidx.compose.runtime.Immutable

/**
 * Higher Self / Inner Guide conceptual layer representation.
 * Not a mystical or supernatural entity, but a reflective synthesis of DepthLens's
 * accumulated long-term understanding of the user:
 * "Given everything DepthLens has learned about this person, what advice would be
 * most aligned with the person they are trying to become?"
 */
@Immutable
data class HigherSelfProfile(
    val coreValues: List<String> = emptyList(),
    val longTermAspirations: List<String> = emptyList(),
    val recurringPatterns: List<String> = emptyList(),
    val potentialBlindSpots: List<String> = emptyList(),
    val evolvingBeliefs: List<String> = emptyList(),
    val reflectiveSynthesis: String = "",
    val totalEvidenceNodes: Int = 0,
    val lastSynthesizedTime: Long = System.currentTimeMillis()
)

/**
 * Structured breakdown for Self-Reflection Mode:
 * Compares Current Self (what I want right now / immediate impulse)
 * vs. Higher Self (what aligns with who I want to become),
 * highlighting tensions, blind spots, and reflective questions to preserve user agency.
 */
@Immutable
data class SelfReflectionComparison(
    val dilemma: String,
    val currentSelfPerspective: String,
    val higherSelfPerspective: String,
    val tensionOrBlindSpot: String,
    val reflectiveQuestions: List<String> = emptyList(),
    val practicalGuidance: String = ""
)

package de.saring.exerciseviewer.data

/**
 * This class stores the power data summary of a recorded exercise.
 *
 * @property powerAvg Average power of an exercise (in watts).
 * @property powerMax Maximum power of an exercise (in watts, optional).
 * @property powerNormalized Normalized power of an exercise (in watts, optional). More info: https://help.trainingpeaks.com/hc/en-us/articles/204071804-Normalized-Power
 *
 * @author Stefan Saring
 */
data class ExercisePower(

    var powerAvg: Short,
    var powerMax: Short? = null,
    var powerNormalized: Short? = null)

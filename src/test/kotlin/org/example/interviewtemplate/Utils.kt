package org.example.interviewtemplate

import kotlin.random.Random

// Randomize name to avoid conflicts with concurrent calls in db,
// since `name` column is unique.
fun randomName(): String = "name" + Random.nextInt(10000)
fun randomPass(): String = Random.nextInt(100).toString()
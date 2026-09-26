package com.example.heartmatch.ui.screens

fun levelLearningTip(level: Int): String = when(level) {
    1 -> "Start with red hearts. Every special blast also counts toward color goals."
    4 -> "Make a line of four to create Fire. Follow its direction to clear a row or column."
    5 -> "Make a T or L of five to create a Bomb. Match its color to activate."
    7 -> "A line of five makes a Rainbow. Swap it with a color to clear that color."
    9 -> "Combine two specials: Fire + Fire makes a cross; Fire + Bomb makes a wider cross."
    11 -> "Stone needs multiple hits. Each dot is one remaining layer."
    13 -> "Tap Gift to preview its cross and two priority targets before activating."
    21 -> "A repair counts only when every layer is removed. Broken hearts can be moved."
    26 -> "Match beside ice or match its enclosed color to release the heart inside."
    31 -> "Touch wood from two sides in one match to remove two layers."
    41 -> "Hit darkness to delay spread. Free boosters never advance its timer."
    61 -> "Chains anchor hearts against gravity. Remove their layers to release them."
    64 -> "Barbed hearts block a cell until hit by an adjacent match or blast."
    else -> if(level % 10 == 0) "Garden challenge · first clear earns a hammer and shuffle."
        else if(level % 10 == 1) "A gentler puzzle to begin this stretch. Focus on the objectives before chasing stars."
        else "Tap a special for a preview. Hints favor unfinished objectives; leftover original moves add bonus points."
}

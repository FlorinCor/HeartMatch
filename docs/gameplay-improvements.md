# Gameplay and reward improvements

The match-three interaction and heart artwork remain intact. This revision implements the 16 gameplay recommendations:

1. Matches, chained specials, rainbow conversions, and hammers share unique-cell color collection accounting.
2. Extra moves revive an out-of-moves game, clear its defeat dialog, and are rejected after victory or timeout. Invalid actions spend no inventory.
3. Bomb and rainbow placement accepts normal hearts only and preserves their color. Blockers and existing specials cannot be overwritten.
4. Hammers remove one layer, activate struck specials, and resolve cascades. Free tools never advance darkness.
5. Engine events supply score popups and the actual 1×, 1.5×, 2× cascade multipliers. Results itemize hearts, creations, blockers, specials, cascades, and move bonuses.
6. Every successful level awards at least one star; stored progress keeps the best stars and score.
7. Unused original moves award 100 points each before stars. Granted extra moves are excluded.
8. The garden coin counter opens a shop for boosters and optional rose/moon garden decorations. Owned decorations can be selected again at no cost.
9. First clears pay 75 coins, replays 15, and each newly improved star adds 25. First clears of every tenth level give one hammer and one shuffle.
10. Daily gifts form a seven-claim cycle that pauses when a day is missed. Day seven grants 1,000 coins and one of each of the five boosters.
11. Fire + Fire clears one row and column; Fire + Bomb clears a three-wide cross; Bomb + Bomb clears 5×5.
12. Light removes an extra layer inside its 3×3 area. Angel prioritizes unfinished blocker objectives and darkness. Gift uses deterministic objective-priority bonus targets. Selecting a special previews activation at its current position; moving it can change targets.
13. Blockers display remaining-layer dots. Stone loses one layer per ordinary wave; wood rewards matches touching two sides; stitched repairs heal neighboring repairable hearts. Repair objectives count completed repairs. Help explains these rules.
14. Hints rank immediate objective progress and special creation. A targeted booster requires a preview tap followed by a second tap on the same cell to spend one item. Placement previews describe their activation footprint, not immediate destruction.
15. First-time mechanics have focused goals and extra room to learn. Later ten-level stretches open with single-objective recovery puzzles. Milestones combine mechanics without redundant score gates. Several low-win boards receive additional moves; levels 190 and 195 use five colors.
16. A reproducible simulation test reads all 200 packaged JSON assets directly and runs random, greedy, objective-focused, and booster-assisted agents over eight seeds each. Assisted agents use the production hammer and extra-move APIs. It reports win rates, remaining moves, score, winning star distribution, and actual booster use, and proposes star thresholds from unassisted winning-score percentiles.

## Balance method

The baseline sweep contains 6,400 games. Star calibration uses the 40th and 80th percentiles of unassisted winning scores, rounded up to hundreds, with strict threshold ordering. Levels with fewer than four sampled wins retain their previous thresholds pending more evidence. These small deterministic samples are regression signals, not estimates of human difficulty. Human playtesting should guide subsequent tuning.

Generated reports live under `app/build/reports/balance`; reviewed snapshots live in `docs/balance`. Run `./gradlew testDebugUnitTest` to reproduce the sweep and engine regressions.

## Device verification without altering player saves

Run `./gradlew -Pqa connectedDebugAndroidTest`. The QA application uses `com.example.heartmatch.qa`, separate from the installed game. Reward persistence tests explicitly refuse to reset data in a non-QA package. Build the normal application with `./gradlew assembleDebug` without `-Pqa`.

The reshuffle fallback also preserves specials and repairable blockers when generating new normal colors, and restores the original board if it cannot find a valid arrangement. Special creation selects a normal heart cell, so it cannot bypass a blocker shell or suppress activation of an existing matched special.

## Validation

All 123 JVM tests and eight Android device tests passed. The final campaign sweep covers 6,400 games across all 200 shipped levels. Each level has at least one unassisted objective-focused win in the fixed seed set. The production debug APK builds successfully.

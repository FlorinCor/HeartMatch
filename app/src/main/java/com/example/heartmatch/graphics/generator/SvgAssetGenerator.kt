package com.example.heartmatch.graphics.generator

import com.example.heartmatch.engine.model.BlockerType
import com.example.heartmatch.engine.model.HeartColor
import com.example.heartmatch.engine.model.SpecialHeartType
import com.example.heartmatch.graphics.model.BackgroundAssetKey
import com.example.heartmatch.graphics.model.EffectAssetKey
import com.example.heartmatch.graphics.model.HeartAssetKey
import com.example.heartmatch.graphics.model.HeartVisualState
import com.example.heartmatch.graphics.model.UiAssetKey
import java.io.File

/**
 * Procedural Generator for Heart Match SVG vector assets (512x512).
 * Strictly creates glossy, 3D, tactile heart game pieces with transparent backgrounds,
 * consistent proportions, and rich lighting effects matching the concept art.
 */
object SvgAssetGenerator {

    private const val HEART_PATH_D = "M 256,440 C 248,432 100,320 60,220 C 24,130 84,64 168,64 C 212,64 242,92 256,116 C 270,92 300,64 344,64 C 428,64 488,130 452,220 C 412,320 264,432 256,440 Z"
    private const val GLOSS_HIGHLIGHT_D = "M 160,96 C 120,96 88,130 96,176 C 104,220 148,260 176,260 C 196,260 172,200 164,168 C 156,136 172,112 200,104 C 188,98 174,96 160,96 Z"
    private const val SECONDARY_GLOSS_D = "M 320,100 C 352,100 376,120 380,150 C 384,178 360,196 348,196 C 336,196 344,168 340,148 C 336,128 324,112 310,104 C 314,101 317,100 320,100 Z"

    fun generateHeartSvg(
        key: HeartAssetKey,
        state: HeartVisualState = HeartVisualState.Idle
    ): String {
        val defs = StringBuilder()
        val body = StringBuilder()

        // 1. Shadow definition
        defs.append("""
            <filter id="dropShadow" x="-20%" y="-20%" width="140%" height="140%">
                <feDropShadow dx="0" dy="16" stdDeviation="16" flood-color="#000000" flood-opacity="0.35"/>
            </filter>
            <filter id="glowEffect" x="-30%" y="-30%" width="160%" height="160%">
                <feGaussianBlur stdDeviation="12" result="blur"/>
                <feComposite in="SourceGraphic" in2="blur" operator="over"/>
            </filter>
        """.trimIndent())

        // 2. Base Heart Graphics according to category
        when (key) {
            is HeartAssetKey.Normal -> generateNormalHeartContent(key.color, defs, body)
            is HeartAssetKey.Blocker -> generateBlockerContent(key.blockerType, defs, body)
            is HeartAssetKey.Special -> generateSpecialHeartContent(key.specialType, defs, body)
        }

        // 3. State-based overlays (Selected, Matched, Damaged, Destroyed)
        applyStateOverlay(state, defs, body)

        return """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
                <defs>
                    $defs
                </defs>
                <g id="heart-root">
                    $body
                </g>
            </svg>
        """.trimIndent()
    }

    private fun generateNormalHeartContent(color: HeartColor, defs: StringBuilder, body: StringBuilder) {
        val (lightHex, mainHex, darkHex, glossHex) = when (color) {
            HeartColor.RED -> listOf("#FF5277", "#E91E63", "#880E4F", "#FFD4E0")
            HeartColor.PINK -> listOf("#FF80AB", "#FF4081", "#C2185B", "#FFE0EB")
            HeartColor.BLUE -> listOf("#40C4FF", "#0091EA", "#01579B", "#E1F5FE")
            HeartColor.GREEN -> listOf("#69F0AE", "#00E676", "#007E33", "#E8F5E9")
            HeartColor.YELLOW -> listOf("#FFFF00", "#FFD600", "#FF6D00", "#FFFDE7")
            HeartColor.PURPLE -> listOf("#E040FB", "#AA00FF", "#4A148C", "#F3E5F5")
            HeartColor.ORANGE -> listOf("#FFAB40", "#FF6D00", "#BF360C", "#FFF3E0")
        }

        defs.append("""
            <radialGradient id="bodyGrad_${color.name}" cx="35%" cy="35%" r="65%">
                <stop offset="0%" stop-color="$lightHex"/>
                <stop offset="55%" stop-color="$mainHex"/>
                <stop offset="100%" stop-color="$darkHex"/>
            </radialGradient>
            <linearGradient id="bevelGrad_${color.name}" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.6"/>
                <stop offset="100%" stop-color="#000000" stop-opacity="0.4"/>
            </linearGradient>
            <radialGradient id="glossGrad" cx="30%" cy="30%" r="50%">
                <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.85"/>
                <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0.0"/>
            </radialGradient>
        """.trimIndent())

        body.append("""
            <!-- Drop Shadow & Base Heart Body -->
            <path d="$HEART_PATH_D" fill="url(#bodyGrad_${color.name})" filter="url(#dropShadow)"/>
            <!-- 3D Bevel Border -->
            <path d="$HEART_PATH_D" fill="none" stroke="url(#bevelGrad_${color.name})" stroke-width="8" opacity="0.7"/>
            <!-- Specular Glossy Highlights -->
            <path d="$GLOSS_HIGHLIGHT_D" fill="url(#glossGrad)"/>
            <path d="$SECONDARY_GLOSS_D" fill="url(#glossGrad)" opacity="0.65"/>
            <!-- Bottom Rim Ambient Glow -->
            <ellipse cx="256" cy="400" rx="90" ry="24" fill="$lightHex" opacity="0.25" filter="blur(8px)"/>
        """.trimIndent())
    }

    private fun generateBlockerContent(type: BlockerType, defs: StringBuilder, body: StringBuilder) {
        when (type) {
            BlockerType.STONE_HEART -> {
                defs.append("""
                    <radialGradient id="stoneBody" cx="35%" cy="35%" r="65%">
                        <stop offset="0%" stop-color="#CFD8DC"/>
                        <stop offset="50%" stop-color="#78909C"/>
                        <stop offset="100%" stop-color="#37474F"/>
                    </radialGradient>
                """.trimIndent())
                body.append("""
                    <path d="$HEART_PATH_D" fill="url(#stoneBody)" filter="url(#dropShadow)"/>
                    <path d="$HEART_PATH_D" fill="none" stroke="#263238" stroke-width="10"/>
                    <!-- Chiseled Slate Facets & Fracture Cracks -->
                    <path d="M 120,160 L 256,256 L 380,180 M 256,256 L 240,400 M 256,256 L 160,350 M 256,256 L 360,340" fill="none" stroke="#263238" stroke-width="6" stroke-linecap="round"/>
                    <path d="M 160,110 L 256,170 L 340,110" fill="none" stroke="#ECEFF1" stroke-width="4" opacity="0.6"/>
                """.trimIndent())
            }

            BlockerType.ICE_HEART -> {
                defs.append("""
                    <radialGradient id="iceCore" cx="35%" cy="35%" r="65%">
                        <stop offset="0%" stop-color="#E0F7FA"/>
                        <stop offset="50%" stop-color="#4DD0E1"/>
                        <stop offset="100%" stop-color="#00838F"/>
                    </radialGradient>
                """.trimIndent())
                body.append("""
                    <path d="$HEART_PATH_D" fill="url(#iceCore)" filter="url(#dropShadow)"/>
                    <!-- Crystalline Diamond Facets -->
                    <path d="M 256,116 L 168,64 L 60,220 L 256,440 L 452,220 L 344,64 Z" fill="none" stroke="#FFFFFF" stroke-width="8" opacity="0.8"/>
                    <polygon points="256,140 160,240 256,360 352,240" fill="#FFFFFF" opacity="0.35"/>
                    <path d="M 160,240 L 60,220 M 352,240 L 452,220 M 256,140 L 256,64 M 256,360 L 256,440" stroke="#FFFFFF" stroke-width="5" opacity="0.9"/>
                    <path d="$GLOSS_HIGHLIGHT_D" fill="#FFFFFF" opacity="0.75"/>
                """.trimIndent())
            }

            BlockerType.WOODEN_HEART -> {
                defs.append("""
                    <linearGradient id="woodGrain" x1="0%" y1="0%" x2="0%" y2="100%">
                        <stop offset="0%" stop-color="#A1887F"/>
                        <stop offset="50%" stop-color="#6D4C41"/>
                        <stop offset="100%" stop-color="#3E2723"/>
                    </linearGradient>
                """.trimIndent())
                body.append("""
                    <path d="$HEART_PATH_D" fill="url(#woodGrain)" filter="url(#dropShadow)"/>
                    <!-- Horizontal Plank Separators -->
                    <path d="M 60,180 L 452,180 M 80,290 L 432,290" stroke="#271612" stroke-width="8"/>
                    <path d="$HEART_PATH_D" fill="none" stroke="#271612" stroke-width="10"/>
                    <!-- Nails / Rivets -->
                    <circle cx="120" cy="160" r="8" fill="#1C100D"/>
                    <circle cx="392" cy="160" r="8" fill="#1C100D"/>
                    <circle cx="140" cy="270" r="8" fill="#1C100D"/>
                    <circle cx="372" cy="270" r="8" fill="#1C100D"/>
                    <!-- Wood Texture Highlights -->
                    <path d="M 100,120 Q 256,150 400,120 M 120,230 Q 256,250 380,230" fill="none" stroke="#D7CCC8" stroke-width="3" opacity="0.4"/>
                """.trimIndent())
            }

            BlockerType.BARBED_HEART -> {
                defs.append("""
                    <radialGradient id="barbedRed" cx="35%" cy="35%" r="65%">
                        <stop offset="0%" stop-color="#FF5252"/>
                        <stop offset="60%" stop-color="#D50000"/>
                        <stop offset="100%" stop-color="#5B0000"/>
                    </radialGradient>
                """.trimIndent())
                body.append("""
                    <path d="$HEART_PATH_D" fill="url(#barbedRed)" filter="url(#dropShadow)"/>
                    <path d="$GLOSS_HIGHLIGHT_D" fill="#FFFFFF" opacity="0.5"/>
                    <!-- Wrapped Silver Barbed Wire -->
                    <path d="M 80,180 Q 256,280 432,180 M 70,300 Q 256,220 440,300" fill="none" stroke="#ECEFF1" stroke-width="12" stroke-linecap="round"/>
                    <path d="M 80,180 Q 256,280 432,180 M 70,300 Q 256,220 440,300" fill="none" stroke="#455A64" stroke-width="4"/>
                    <!-- Barbed Spikes -->
                    <polygon points="180,210 160,180 195,190" fill="#ECEFF1" stroke="#37474F" stroke-width="2"/>
                    <polygon points="320,225 345,195 330,235" fill="#ECEFF1" stroke="#37474F" stroke-width="2"/>
                    <polygon points="200,280 180,310 215,295" fill="#ECEFF1" stroke="#37474F" stroke-width="2"/>
                    <polygon points="340,265 365,295 340,280" fill="#ECEFF1" stroke="#37474F" stroke-width="2"/>
                """.trimIndent())
            }

            BlockerType.BROKEN_HEART -> {
                defs.append("""
                    <radialGradient id="brokenBody" cx="35%" cy="35%" r="65%">
                        <stop offset="0%" stop-color="#FF5252"/>
                        <stop offset="60%" stop-color="#E53935"/>
                        <stop offset="100%" stop-color="#B71C1C"/>
                    </radialGradient>
                """.trimIndent())
                body.append("""
                    <path d="$HEART_PATH_D" fill="url(#brokenBody)" filter="url(#dropShadow)"/>
                    <!-- Jagged Break Gap -->
                    <path d="M 256,116 L 220,200 L 290,270 L 220,350 L 256,440" fill="none" stroke="#212121" stroke-width="14" stroke-linecap="round"/>
                    <!-- Bandage / Stitch across fracture -->
                    <rect x="180" y="210" width="150" height="30" rx="8" fill="#FFF9C4" transform="rotate(-15 256 225)" stroke="#FBC02D" stroke-width="3"/>
                    <rect x="190" y="300" width="140" height="30" rx="8" fill="#FFF9C4" transform="rotate(10 260 315)" stroke="#FBC02D" stroke-width="3"/>
                    <path d="$GLOSS_HIGHLIGHT_D" fill="#FFFFFF" opacity="0.6"/>
                """.trimIndent())
            }

            BlockerType.STITCHED_HEART -> {
                defs.append("""
                    <radialGradient id="stitchedLeather" cx="35%" cy="35%" r="65%">
                        <stop offset="0%" stop-color="#FF5252"/>
                        <stop offset="60%" stop-color="#D32F2F"/>
                        <stop offset="100%" stop-color="#7F0000"/>
                    </radialGradient>
                """.trimIndent())
                body.append("""
                    <path d="$HEART_PATH_D" fill="url(#stitchedLeather)" filter="url(#dropShadow)"/>
                    <!-- Suture Center Seam -->
                    <path d="M 256,70 Q 240,256 256,440" fill="none" stroke="#4A0000" stroke-width="10"/>
                    <!-- Stitches -->
                    <g stroke="#FFFFFF" stroke-width="8" stroke-linecap="round">
                        <line x1="216" y1="130" x2="296" y2="150"/>
                        <line x1="210" y1="190" x2="290" y2="210"/>
                        <line x1="205" y1="250" x2="285" y2="270"/>
                        <line x1="210" y1="310" x2="290" y2="330"/>
                        <line x1="225" y1="370" x2="285" y2="390"/>
                    </g>
                    <!-- Corner Purple Patch -->
                    <polygon points="60,180 140,120 110,240" fill="#7B1FA2" stroke="#E1BEE7" stroke-width="4" stroke-dasharray="8,6"/>
                    <path d="$GLOSS_HIGHLIGHT_D" fill="#FFFFFF" opacity="0.45"/>
                """.trimIndent())
            }

            BlockerType.CHAINED_HEART -> {
                defs.append("""
                    <radialGradient id="chainedPink" cx="35%" cy="35%" r="65%">
                        <stop offset="0%" stop-color="#FF80AB"/>
                        <stop offset="60%" stop-color="#FF4081"/>
                        <stop offset="100%" stop-color="#880E4F"/>
                    </radialGradient>
                """.trimIndent())
                body.append("""
                    <path d="$HEART_PATH_D" fill="url(#chainedPink)" filter="url(#dropShadow)"/>
                    <path d="$GLOSS_HIGHLIGHT_D" fill="#FFFFFF" opacity="0.6"/>
                    <!-- Crossed Heavy Chains -->
                    <g stroke="#ECEFF1" stroke-width="18" stroke-linecap="round" stroke-dasharray="24,14">
                        <line x1="60" y1="120" x2="452" y2="400"/>
                        <line x1="452" y1="120" x2="60" y2="400"/>
                    </g>
                    <g stroke="#37474F" stroke-width="6" stroke-linecap="round" stroke-dasharray="24,14">
                        <line x1="60" y1="120" x2="452" y2="400"/>
                        <line x1="452" y1="120" x2="60" y2="400"/>
                    </g>
                    <!-- Center Golden Padlock -->
                    <rect x="206" y="210" width="100" height="90" rx="16" fill="#FFD700" stroke="#FFA000" stroke-width="6" filter="url(#dropShadow)"/>
                    <path d="M 231,210 L 231,170 C 231,140 281,140 281,170 L 281,210" fill="none" stroke="#B0BEC5" stroke-width="12" stroke-linecap="round"/>
                    <circle cx="256" cy="250" r="12" fill="#37474F"/>
                    <polygon points="252,250 260,250 264,275 248,275" fill="#37474F"/>
                """.trimIndent())
            }

            BlockerType.DARK_HEART -> {
                defs.append("""
                    <radialGradient id="darkAbyss" cx="35%" cy="35%" r="65%">
                        <stop offset="0%" stop-color="#7C4DFF"/>
                        <stop offset="45%" stop-color="#311B92"/>
                        <stop offset="100%" stop-color="#050014"/>
                    </radialGradient>
                    <radialGradient id="sinisterEye" cx="50%" cy="50%" r="50%">
                        <stop offset="0%" stop-color="#FF1744"/>
                        <stop offset="70%" stop-color="#D50000"/>
                        <stop offset="100%" stop-color="#000000" stop-opacity="0"/>
                    </radialGradient>
                """.trimIndent())
                body.append("""
                    <path d="$HEART_PATH_D" fill="url(#darkAbyss)" filter="url(#dropShadow)"/>
                    <!-- Corrupted Tendril Aura -->
                    <path d="$HEART_PATH_D" fill="none" stroke="#D50000" stroke-width="8" opacity="0.8"/>
                    <!-- Sinister Core Glow Eye -->
                    <circle cx="256" cy="240" r="60" fill="url(#sinisterEye)"/>
                    <polygon points="256,200 266,230 296,240 266,250 256,280 246,250 216,240 246,230" fill="#FFFFFF"/>
                    <path d="$GLOSS_HIGHLIGHT_D" fill="#FFFFFF" opacity="0.3"/>
                """.trimIndent())
            }
        }
    }

    private fun generateSpecialHeartContent(type: SpecialHeartType, defs: StringBuilder, body: StringBuilder) {
        when (type) {
            SpecialHeartType.RAINBOW_HEART -> {
                defs.append("""
                    <linearGradient id="rainbowSpectrum" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stop-color="#FF1744"/>
                        <stop offset="20%" stop-color="#FF9100"/>
                        <stop offset="40%" stop-color="#FFEA00"/>
                        <stop offset="60%" stop-color="#00E676"/>
                        <stop offset="80%" stop-color="#00B0FF"/>
                        <stop offset="100%" stop-color="#D500F9"/>
                    </linearGradient>
                """.trimIndent())
                body.append("""
                    <path d="$HEART_PATH_D" fill="url(#rainbowSpectrum)" filter="url(#dropShadow)"/>
                    <path d="$HEART_PATH_D" fill="none" stroke="#FFFFFF" stroke-width="8" opacity="0.9"/>
                    <!-- Star sparkles -->
                    <polygon points="256,130 262,148 280,154 262,160 256,178 250,160 232,154 250,148" fill="#FFFFFF"/>
                    <polygon points="340,240 344,252 356,256 344,260 340,272 336,260 324,256 336,252" fill="#FFFFFF"/>
                    <path d="$GLOSS_HIGHLIGHT_D" fill="#FFFFFF" opacity="0.8"/>
                    <path d="$SECONDARY_GLOSS_D" fill="#FFFFFF" opacity="0.7"/>
                """.trimIndent())
            }

            SpecialHeartType.FIRE_HEART -> {
                defs.append("""
                    <radialGradient id="fireCore" cx="40%" cy="35%" r="60%">
                        <stop offset="0%" stop-color="#FFFF00"/>
                        <stop offset="45%" stop-color="#FF3D00"/>
                        <stop offset="100%" stop-color="#B71C1C"/>
                    </radialGradient>
                    <radialGradient id="fireAura" cx="50%" cy="50%" r="50%">
                        <stop offset="0%" stop-color="#FFEB3B"/>
                        <stop offset="70%" stop-color="#FF5722" stop-opacity="0.8"/>
                        <stop offset="100%" stop-color="#D50000" stop-opacity="0"/>
                    </radialGradient>
                """.trimIndent())
                body.append("""
                    <!-- Surrounding Flame Flares -->
                    <path d="M 256,20 Q 230,80 200,90 Q 230,120 256,90 Q 280,120 310,90 Q 280,80 256,20 Z" fill="url(#fireAura)"/>
                    <path d="$HEART_PATH_D" fill="url(#fireCore)" filter="url(#dropShadow)"/>
                    <!-- Directional Flame Beams -->
                    <line x1="80" y1="240" x2="432" y2="240" stroke="#FFFF00" stroke-width="12" stroke-linecap="round" opacity="0.9"/>
                    <line x1="256" y1="90" x2="256" y2="400" stroke="#FFFF00" stroke-width="12" stroke-linecap="round" opacity="0.9"/>
                    <!-- Arrows -->
                    <polygon points="432,240 400,220 400,260" fill="#FFFF00"/>
                    <polygon points="80,240 112,220 112,260" fill="#FFFF00"/>
                    <path d="$GLOSS_HIGHLIGHT_D" fill="#FFFFFF" opacity="0.7"/>
                """.trimIndent())
            }

            SpecialHeartType.BOMB_HEART -> {
                defs.append("""
                    <radialGradient id="bombCore" cx="40%" cy="40%" r="60%">
                        <stop offset="0%" stop-color="#FF5252"/>
                        <stop offset="50%" stop-color="#37474F"/>
                        <stop offset="100%" stop-color="#212121"/>
                    </radialGradient>
                """.trimIndent())
                body.append("""
                    <path d="$HEART_PATH_D" fill="url(#bombCore)" filter="url(#dropShadow)"/>
                    <path d="$HEART_PATH_D" fill="none" stroke="#FFC107" stroke-width="8" stroke-dasharray="16,10"/>
                    <!-- Center Core Fuse Spark -->
                    <circle cx="256" cy="240" r="48" fill="#FFD700" filter="url(#glowEffect)"/>
                    <circle cx="256" cy="240" r="30" fill="#FF3D00"/>
                    <polygon points="256,190 268,225 306,240 268,255 256,290 244,255 206,240 244,225" fill="#FFFFFF"/>
                    <path d="$GLOSS_HIGHLIGHT_D" fill="#FFFFFF" opacity="0.6"/>
                """.trimIndent())
            }

            SpecialHeartType.GIFT_HEART -> {
                defs.append("""
                    <radialGradient id="giftBody" cx="35%" cy="35%" r="65%">
                        <stop offset="0%" stop-color="#FF80AB"/>
                        <stop offset="60%" stop-color="#E91E63"/>
                        <stop offset="100%" stop-color="#880E4F"/>
                    </radialGradient>
                    <linearGradient id="goldRibbon" x1="0%" y1="0%" x2="100%" y2="0%">
                        <stop offset="0%" stop-color="#FFB300"/>
                        <stop offset="50%" stop-color="#FFF9C4"/>
                        <stop offset="100%" stop-color="#FF8F00"/>
                    </linearGradient>
                """.trimIndent())
                body.append("""
                    <path d="$HEART_PATH_D" fill="url(#giftBody)" filter="url(#dropShadow)"/>
                    <!-- Golden Ribbons -->
                    <rect x="226" y="64" width="60" height="376" fill="url(#goldRibbon)"/>
                    <rect x="60" y="210" width="392" height="60" fill="url(#goldRibbon)"/>
                    <!-- Golden Center Bow -->
                    <ellipse cx="206" cy="200" rx="36" ry="24" fill="#FFD700" stroke="#FF8F00" stroke-width="4" transform="rotate(-30 206 200)"/>
                    <ellipse cx="306" cy="200" rx="36" ry="24" fill="#FFD700" stroke="#FF8F00" stroke-width="4" transform="rotate(30 306 200)"/>
                    <circle cx="256" cy="240" r="28" fill="#FFF59D" stroke="#FF8F00" stroke-width="4"/>
                    <path d="$GLOSS_HIGHLIGHT_D" fill="#FFFFFF" opacity="0.65"/>
                """.trimIndent())
            }

            SpecialHeartType.ROYAL_HEART -> {
                defs.append("""
                    <radialGradient id="royalGold" cx="35%" cy="35%" r="65%">
                        <stop offset="0%" stop-color="#FFF59D"/>
                        <stop offset="45%" stop-color="#FFD700"/>
                        <stop offset="85%" stop-color="#FF8F00"/>
                        <stop offset="100%" stop-color="#E65100"/>
                    </radialGradient>
                    <linearGradient id="crownGold" x1="0%" y1="0%" x2="0%" y2="100%">
                        <stop offset="0%" stop-color="#FFFDE7"/>
                        <stop offset="50%" stop-color="#FFD700"/>
                        <stop offset="100%" stop-color="#FF8F00"/>
                    </linearGradient>
                """.trimIndent())
                body.append("""
                    <path d="$HEART_PATH_D" fill="url(#royalGold)" filter="url(#dropShadow)"/>
                    <path d="$HEART_PATH_D" fill="none" stroke="#FFD700" stroke-width="10"/>
                    <!-- Royal Majestic Crown -->
                    <polygon points="150,130 120,40 200,80 256,20 312,80 392,40 362,130" fill="url(#crownGold)" stroke="#795548" stroke-width="6" filter="url(#dropShadow)"/>
                    <!-- Crown Jewels -->
                    <circle cx="256" cy="40" r="14" fill="#FF1744" stroke="#FFFFFF" stroke-width="3"/>
                    <circle cx="125" cy="55" r="12" fill="#2979FF" stroke="#FFFFFF" stroke-width="2"/>
                    <circle cx="387" cy="55" r="12" fill="#00E676" stroke="#FFFFFF" stroke-width="2"/>
                    <path d="$GLOSS_HIGHLIGHT_D" fill="#FFFFFF" opacity="0.8"/>
                """.trimIndent())
            }

            SpecialHeartType.ANGEL_HEART -> {
                defs.append("""
                    <radialGradient id="angelPearly" cx="35%" cy="35%" r="65%">
                        <stop offset="0%" stop-color="#FFFFFF"/>
                        <stop offset="50%" stop-color="#FFF9C4"/>
                        <stop offset="100%" stop-color="#FFE082"/>
                    </radialGradient>
                    <linearGradient id="wingGrad" x1="0%" y1="0%" x2="0%" y2="100%">
                        <stop offset="0%" stop-color="#FFFFFF"/>
                        <stop offset="70%" stop-color="#FCE4EC"/>
                        <stop offset="100%" stop-color="#F8BBD0"/>
                    </linearGradient>
                """.trimIndent())
                body.append("""
                    <!-- Angel Wings Left & Right -->
                    <path d="M 160,220 C 60,120 0,140 0,220 C 0,300 60,340 160,280 Z" fill="url(#wingGrad)" stroke="#F06292" stroke-width="6" filter="url(#dropShadow)"/>
                    <path d="M 352,220 C 452,120 512,140 512,220 C 512,300 452,340 352,280 Z" fill="url(#wingGrad)" stroke="#F06292" stroke-width="6" filter="url(#dropShadow)"/>
                    <!-- Central Holy Heart -->
                    <path d="$HEART_PATH_D" fill="url(#angelPearly)" filter="url(#dropShadow)"/>
                    <!-- Divine Golden Halo -->
                    <ellipse cx="256" cy="50" rx="120" ry="24" fill="none" stroke="#FFD700" stroke-width="12" filter="url(#glowEffect)"/>
                    <path d="$GLOSS_HIGHLIGHT_D" fill="#FFFFFF" opacity="0.85"/>
                """.trimIndent())
            }
        }
    }

    private fun applyStateOverlay(state: HeartVisualState, defs: StringBuilder, body: StringBuilder) {
        when (state) {
            HeartVisualState.Idle -> {
                // Default pristine state
            }
            HeartVisualState.Selected -> {
                body.append("""
                    <!-- Selected State: Glowing Pulse Halo & Gold Border -->
                    <path d="$HEART_PATH_D" fill="none" stroke="#FFD700" stroke-width="16" filter="url(#glowEffect)"/>
                    <path d="$HEART_PATH_D" fill="#FFFFFF" opacity="0.15"/>
                """.trimIndent())
            }
            HeartVisualState.Matched -> {
                body.append("""
                    <!-- Matched State: Bright Radiant Burst Flash -->
                    <path d="$HEART_PATH_D" fill="#FFFFFF" opacity="0.5" filter="url(#glowEffect)"/>
                    <polygon points="256,40 270,220 450,256 270,292 256,472 242,292 62,256 242,220" fill="#FFFFFF" opacity="0.85" filter="url(#glowEffect)"/>
                """.trimIndent())
            }
            is HeartVisualState.Damaged -> {
                body.append("""
                    <!-- Damaged State: Cracks & Red Bruise Overlay -->
                    <path d="$HEART_PATH_D" fill="#FF0000" opacity="0.2"/>
                    <path d="M 180,140 L 256,240 L 220,320 M 256,240 L 340,200 L 320,360" fill="none" stroke="#1A1A1A" stroke-width="8" stroke-linecap="round"/>
                """.trimIndent())
            }
            is HeartVisualState.Destroyed -> {
                body.append("""
                    <!-- Destroyed / Disappearing State: Shattered Shards & Fade -->
                    <path d="$HEART_PATH_D" fill="#FFFFFF" opacity="0.2"/>
                    <g fill="#FFFFFF" opacity="0.8" filter="url(#glowEffect)">
                        <polygon points="120,160 160,120 180,180"/>
                        <polygon points="360,160 400,120 380,180"/>
                        <polygon points="220,360 256,420 290,360"/>
                        <circle cx="150" cy="300" r="16"/>
                        <circle cx="350" cy="300" r="16"/>
                    </g>
                """.trimIndent())
            }
        }
    }

    fun generateEffectSvg(key: EffectAssetKey): String {
        val content = when (key) {
            EffectAssetKey.NORMAL_MATCH -> """
                <circle cx="256" cy="256" r="180" fill="none" stroke="#FF4081" stroke-width="24" opacity="0.8"/>
                <polygon points="256,60 275,230 450,256 275,282 256,452 237,282 62,256 237,230" fill="#FFFFFF"/>
                <circle cx="120" cy="120" r="20" fill="#FF80AB"/>
                <circle cx="392" cy="120" r="20" fill="#FF80AB"/>
                <circle cx="392" cy="392" r="20" fill="#FF80AB"/>
                <circle cx="120" cy="392" r="20" fill="#FF80AB"/>
            """.trimIndent()

            EffectAssetKey.MATCH_4 -> """
                <rect x="36" y="226" width="440" height="60" rx="30" fill="#FF6D00"/>
                <rect x="226" y="36" width="60" height="440" rx="30" fill="#FF6D00"/>
                <circle cx="256" cy="256" r="90" fill="#FFD600"/>
                <polygon points="256,20 280,220 492,256 280,292 256,492 232,292 20,256 232,220" fill="#FFFFFF"/>
            """.trimIndent()

            EffectAssetKey.MATCH_5 -> """
                <circle cx="256" cy="256" r="220" fill="none" stroke="#00E676" stroke-width="28"/>
                <circle cx="256" cy="256" r="160" fill="none" stroke="#00B0FF" stroke-width="24"/>
                <circle cx="256" cy="256" r="100" fill="none" stroke="#D500F9" stroke-width="20"/>
                <polygon points="256,10 285,210 502,256 285,302 256,502 227,302 10,256 227,210" fill="#FFFFFF"/>
            """.trimIndent()

            EffectAssetKey.CASCADE -> """
                <path d="M 256,40 Q 380,180 256,280 Q 132,380 256,480" fill="none" stroke="#40C4FF" stroke-width="32" stroke-linecap="round"/>
                <polygon points="256,490 220,440 292,440" fill="#40C4FF"/>
                <circle cx="200" cy="140" r="24" fill="#FFEB3B"/>
                <circle cx="320" cy="320" r="24" fill="#FFEB3B"/>
            """.trimIndent()

            EffectAssetKey.FIRE_HEART_ACTIVATION -> """
                <rect x="20" y="216" width="472" height="80" rx="40" fill="#FF3D00"/>
                <rect x="216" y="20" width="80" height="472" rx="40" fill="#FF3D00"/>
                <circle cx="256" cy="256" r="120" fill="#FFFF00"/>
                <polygon points="256,0 280,210 512,256 280,302 256,512 232,302 0,256 232,210" fill="#FFFFFF"/>
            """.trimIndent()

            EffectAssetKey.BOMB_HEART_EXPLOSION -> """
                <circle cx="256" cy="256" r="230" fill="#FF5252" opacity="0.3"/>
                <circle cx="256" cy="256" r="170" fill="#FF9100" opacity="0.6"/>
                <circle cx="256" cy="256" r="100" fill="#FFEA00"/>
                <circle cx="256" cy="256" r="50" fill="#FFFFFF"/>
            """.trimIndent()

            EffectAssetKey.RAINBOW_HEART_ACTIVATION -> """
                <circle cx="256" cy="256" r="220" fill="none" stroke="#FF1744" stroke-width="16"/>
                <circle cx="256" cy="256" r="190" fill="none" stroke="#FF9100" stroke-width="16"/>
                <circle cx="256" cy="256" r="160" fill="none" stroke="#FFEA00" stroke-width="16"/>
                <circle cx="256" cy="256" r="130" fill="none" stroke="#00E676" stroke-width="16"/>
                <circle cx="256" cy="256" r="100" fill="none" stroke="#00B0FF" stroke-width="16"/>
                <polygon points="256,20 280,220 492,256 280,292 256,492 232,292 20,256 232,220" fill="#FFFFFF"/>
            """.trimIndent()

            EffectAssetKey.SPECIAL_HEART_COMBINATION -> """
                <circle cx="256" cy="256" r="240" fill="none" stroke="#FFD700" stroke-width="24"/>
                <polygon points="256,10 320,192 502,256 320,320 256,502 192,320 10,256 192,192" fill="#FFFFFF"/>
                <polygon points="256,60 300,200 452,256 300,312 256,452 212,312 60,256 212,200" fill="#FF4081"/>
            """.trimIndent()

            EffectAssetKey.BLOCKER_DAMAGE -> """
                <path d="M 120,120 L 256,256 L 392,120 M 256,256 L 256,420" fill="none" stroke="#FF5252" stroke-width="24" stroke-linecap="round"/>
                <circle cx="256" cy="256" r="60" fill="#FFEB3B"/>
            """.trimIndent()

            EffectAssetKey.BLOCKER_DESTRUCTION -> """
                <polygon points="80,100 160,60 140,160" fill="#78909C"/>
                <polygon points="360,80 440,120 380,180" fill="#78909C"/>
                <polygon points="200,380 256,460 310,390" fill="#78909C"/>
                <circle cx="256" cy="256" r="140" fill="#FF5252" opacity="0.4"/>
            """.trimIndent()

            EffectAssetKey.LEVEL_COMPLETION -> """
                <circle cx="256" cy="256" r="200" fill="#FFD700" opacity="0.3"/>
                <polygon points="256,30 310,180 470,180 340,280 390,430 256,340 122,430 172,280 42,180 202,180" fill="#FFD700" stroke="#FFA000" stroke-width="12"/>
                <circle cx="256" cy="230" r="50" fill="#FFFFFF"/>
            """.trimIndent()

            EffectAssetKey.LEVEL_FAILURE -> """
                <circle cx="256" cy="256" r="200" fill="#424242" opacity="0.3"/>
                <line x1="120" y1="120" x2="392" y2="392" stroke="#E53935" stroke-width="36" stroke-linecap="round"/>
                <line x1="392" y1="120" x2="120" y2="392" stroke="#E53935" stroke-width="36" stroke-linecap="round"/>
            """.trimIndent()

            EffectAssetKey.STAR_AWARD -> """
                <polygon points="256,20 326,170 492,180 366,290 406,452 256,366 106,452 146,290 20,180 186,170" fill="#FFD600" stroke="#FF6D00" stroke-width="14"/>
                <polygon points="256,80 300,190 410,200 326,270 354,380 256,320 158,380 186,270 102,200 212,190" fill="#FFF59D"/>
            """.trimIndent()
        }

        return """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
                <g id="effect-root">
                    $content
                </g>
            </svg>
        """.trimIndent()
    }

    fun generateUiSvg(key: UiAssetKey): String {
        val content = when (key) {
            UiAssetKey.BOOSTER_HAMMER -> """
                <rect x="226" y="200" width="60" height="260" rx="16" fill="#8D6E63" stroke="#4E342E" stroke-width="8"/>
                <rect x="120" y="80" width="272" height="130" rx="24" fill="#78909C" stroke="#263238" stroke-width="12"/>
                <rect x="150" y="100" width="212" height="40" rx="10" fill="#CFD8DC"/>
            """.trimIndent()

            UiAssetKey.BOOSTER_BOMB -> """
                <circle cx="256" cy="290" r="150" fill="#212121" stroke="#FF5252" stroke-width="12"/>
                <rect x="226" y="110" width="60" height="50" rx="8" fill="#78909C"/>
                <path d="M 256,110 Q 320,40 380,80" fill="none" stroke="#FFA000" stroke-width="12"/>
                <circle cx="380" cy="80" r="20" fill="#FF1744"/>
            """.trimIndent()

            UiAssetKey.BOOSTER_RAINBOW -> """
                <circle cx="256" cy="256" r="190" fill="none" stroke="#FF1744" stroke-width="24"/>
                <circle cx="256" cy="256" r="150" fill="none" stroke="#FFEA00" stroke-width="24"/>
                <circle cx="256" cy="256" r="110" fill="none" stroke="#00E676" stroke-width="24"/>
                <circle cx="256" cy="256" r="70" fill="none" stroke="#00B0FF" stroke-width="24"/>
            """.trimIndent()

            UiAssetKey.BOOSTER_SHUFFLE -> """
                <path d="M 80,180 L 220,180 L 320,340 L 420,340" fill="none" stroke="#7C4DFF" stroke-width="28" stroke-linecap="round"/>
                <polygon points="420,340 380,310 380,370" fill="#7C4DFF"/>
                <path d="M 80,340 L 220,340 L 320,180 L 420,180" fill="none" stroke="#FF4081" stroke-width="28" stroke-linecap="round"/>
                <polygon points="420,180 380,150 380,210" fill="#FF4081"/>
            """.trimIndent()

            UiAssetKey.BOOSTER_EXTRA_MOVES -> """
                <circle cx="256" cy="256" r="190" fill="#00E676" stroke="#00B248" stroke-width="16"/>
                <text x="256" y="320" font-family="Arial, sans-serif" font-size="160" font-weight="bold" fill="#FFFFFF" text-anchor="middle">+5</text>
            """.trimIndent()

            UiAssetKey.PLAY_BUTTON -> """
                <circle cx="256" cy="256" r="200" fill="#00E676" stroke="#00B248" stroke-width="16"/>
                <polygon points="200,150 360,256 200,362" fill="#FFFFFF"/>
            """.trimIndent()

            UiAssetKey.PAUSE_BUTTON -> """
                <circle cx="256" cy="256" r="200" fill="#FF9100" stroke="#E65100" stroke-width="16"/>
                <rect x="180" y="160" width="45" height="192" rx="16" fill="#FFFFFF"/>
                <rect x="287" y="160" width="45" height="192" rx="16" fill="#FFFFFF"/>
            """.trimIndent()

            UiAssetKey.STAR_FULL -> """
                <polygon points="256,30 320,170 472,180 356,280 392,430 256,350 120,430 156,280 40,180 192,170" fill="#FFD600" stroke="#FFA000" stroke-width="12"/>
            """.trimIndent()

            UiAssetKey.STAR_EMPTY -> """
                <polygon points="256,30 320,170 472,180 356,280 392,430 256,350 120,430 156,280 40,180 192,170" fill="#424242" stroke="#757575" stroke-width="12"/>
            """.trimIndent()

            UiAssetKey.LOCK_ICON -> """
                <rect x="136" y="210" width="240" height="220" rx="32" fill="#78909C" stroke="#37474F" stroke-width="16"/>
                <path d="M 196,210 L 196,130 C 196,70 316,70 316,130 L 316,210" fill="none" stroke="#CFD8DC" stroke-width="28" stroke-linecap="round"/>
                <circle cx="256" cy="300" r="24" fill="#263238"/>
            """.trimIndent()

            UiAssetKey.COIN_ICON -> """
                <circle cx="256" cy="256" r="190" fill="#FFD700" stroke="#FF8F00" stroke-width="20"/>
                <circle cx="256" cy="256" r="140" fill="none" stroke="#FFF59D" stroke-width="12"/>
                <text x="256" y="315" font-family="Arial, sans-serif" font-size="160" font-weight="bold" fill="#E65100" text-anchor="middle">★</text>
            """.trimIndent()

            UiAssetKey.HEART_LIFE_ICON -> """
                <path d="$HEART_PATH_D" fill="#FF1744" stroke="#D50000" stroke-width="16"/>
                <path d="$GLOSS_HIGHLIGHT_D" fill="#FFFFFF" opacity="0.8"/>
            """.trimIndent()
        }

        return """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
                <g id="ui-root">
                    $content
                </g>
            </svg>
        """.trimIndent()
    }

    fun generateBackgroundSvg(key: BackgroundAssetKey): String {
        val (topColor, bottomColor, decor) = when (key) {
            BackgroundAssetKey.HEART_MEADOW -> Triple("#4A148C", "#1B5E20", """
                <circle cx="120" cy="400" r="180" fill="#2E7D32" opacity="0.6"/>
                <circle cx="392" cy="420" r="160" fill="#388E3C" opacity="0.6"/>
                <path d="M 256,120 C 230,100 180,140 256,220 C 332,140 282,100 256,120 Z" fill="#FF80AB" opacity="0.2"/>
            """.trimIndent())

            BackgroundAssetKey.STONE_VALLEY -> Triple("#263238", "#3E2723", """
                <polygon points="40,512 180,260 320,512" fill="#455A64" opacity="0.5"/>
                <polygon points="240,512 380,220 512,512" fill="#546E7A" opacity="0.5"/>
            """.trimIndent())

            BackgroundAssetKey.BROKEN_FOREST -> Triple("#311B92", "#1A237E", """
                <polygon points="80,512 140,200 200,512" fill="#4A148C" opacity="0.4"/>
                <polygon points="300,512 360,160 420,512" fill="#6A1B9A" opacity="0.4"/>
            """.trimIndent())

            BackgroundAssetKey.SHADOW_GARDEN -> Triple("#0D47A1", "#050014", """
                <circle cx="256" cy="200" r="220" fill="#7C4DFF" opacity="0.15"/>
                <path d="M 100,512 Q 256,300 412,512" fill="#311B92" opacity="0.5"/>
            """.trimIndent())

            BackgroundAssetKey.HEART_KINGDOM -> Triple("#FF6F00", "#4A148C", """
                <polygon points="120,512 120,320 180,260 240,320 240,512" fill="#FFD700" opacity="0.3"/>
                <polygon points="270,512 270,280 340,200 410,280 410,512" fill="#FFD700" opacity="0.3"/>
            """.trimIndent())
        }

        return """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
                <defs>
                    <linearGradient id="bgGrad" x1="0%" y1="0%" x2="0%" y2="100%">
                        <stop offset="0%" stop-color="$topColor"/>
                        <stop offset="100%" stop-color="$bottomColor"/>
                    </linearGradient>
                </defs>
                <rect width="512" height="512" fill="url(#bgGrad)"/>
                $decor
            </svg>
        """.trimIndent()
    }

    /**
     * Exports all required SVG assets to the destination directory.
     */
    fun exportAllAssets(baseDir: File) {
        // 1. Normal hearts
        val normalDir = File(baseDir, "assets/hearts/normal").apply { mkdirs() }
        HeartColor.values().forEach { color ->
            val key = HeartAssetKey.Normal(color)
            File(normalDir, "${key.identifier}.svg").writeText(generateHeartSvg(key))
            // State variants
            File(normalDir, "${key.identifier}-selected.svg").writeText(generateHeartSvg(key, HeartVisualState.Selected))
            File(normalDir, "${key.identifier}-matched.svg").writeText(generateHeartSvg(key, HeartVisualState.Matched))
            File(normalDir, "${key.identifier}-damaged.svg").writeText(generateHeartSvg(key, HeartVisualState.Damaged()))
            File(normalDir, "${key.identifier}-destroyed.svg").writeText(generateHeartSvg(key, HeartVisualState.Destroyed()))
        }

        // 2. Blockers
        val blockerDir = File(baseDir, "assets/hearts/blockers").apply { mkdirs() }
        BlockerType.values().forEach { type ->
            val key = HeartAssetKey.Blocker(type)
            File(blockerDir, "${key.identifier}.svg").writeText(generateHeartSvg(key))
            File(blockerDir, "${key.identifier}-selected.svg").writeText(generateHeartSvg(key, HeartVisualState.Selected))
            File(blockerDir, "${key.identifier}-matched.svg").writeText(generateHeartSvg(key, HeartVisualState.Matched))
            File(blockerDir, "${key.identifier}-damaged.svg").writeText(generateHeartSvg(key, HeartVisualState.Damaged()))
            File(blockerDir, "${key.identifier}-destroyed.svg").writeText(generateHeartSvg(key, HeartVisualState.Destroyed()))
        }

        // 3. Specials
        val specialDir = File(baseDir, "assets/hearts/special").apply { mkdirs() }
        SpecialHeartType.values().forEach { type ->
            val key = HeartAssetKey.Special(type)
            File(specialDir, "${key.identifier}.svg").writeText(generateHeartSvg(key))
            File(specialDir, "${key.identifier}-selected.svg").writeText(generateHeartSvg(key, HeartVisualState.Selected))
            File(specialDir, "${key.identifier}-matched.svg").writeText(generateHeartSvg(key, HeartVisualState.Matched))
            File(specialDir, "${key.identifier}-damaged.svg").writeText(generateHeartSvg(key, HeartVisualState.Damaged()))
            File(specialDir, "${key.identifier}-destroyed.svg").writeText(generateHeartSvg(key, HeartVisualState.Destroyed()))
        }

        // 4. Effects
        val effectsDir = File(baseDir, "assets/effects").apply { mkdirs() }
        EffectAssetKey.values().forEach { effect ->
            File(effectsDir, "${effect.identifier}.svg").writeText(generateEffectSvg(effect))
        }

        // 5. UI
        val uiDir = File(baseDir, "assets/ui").apply { mkdirs() }
        UiAssetKey.values().forEach { ui ->
            File(uiDir, "${ui.identifier}.svg").writeText(generateUiSvg(ui))
        }

        // 6. Backgrounds
        val bgDir = File(baseDir, "assets/backgrounds").apply { mkdirs() }
        BackgroundAssetKey.values().forEach { bg ->
            File(bgDir, "${bg.identifier}.svg").writeText(generateBackgroundSvg(bg))
        }
    }
}

package com.madowaku.focusraid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.madowaku.focusraid.core.domain.CompanionStage

/** Small procedural placeholders; replace the relevant catalog slots when final assets are approved. */
@Composable
internal fun FallbackMikoArtwork(modifier: Modifier, stage: CompanionStage, mood: CompanionMood) {
    Canvas(modifier) {
        val u = size.minDimension
        val cx = size.width / 2
        val cy = size.height / 2
        val mint = Color(0xFF70D8B2)
        val cream = Color(0xFFFFE7AE)
        if (stage == CompanionStage.EGG) {
            drawOval(mint, Offset(cx-u*.24f, cy-u*.32f), Size(u*.48f,u*.64f))
            drawCircle(cream,u*.08f,Offset(cx,cy))
        } else {
            val growth = 1f + stage.ordinal * .04f
            drawOval(mint.copy(alpha=.8f),Offset(cx+u*.02f,cy),Size(u*.38f*growth,u*.35f))
            drawOval(mint,Offset(cx-u*.19f,cy-u*.02f),Size(u*.38f,u*.40f))
            drawCircle(mint,u*.25f,Offset(cx,cy-u*.15f))
            for (side in listOf(-1,1)) {
                val ear = Path().apply {
                    moveTo(cx+side*u*.08f,cy-u*.3f)
                    lineTo(cx+side*u*.26f,cy-u*.48f)
                    lineTo(cx+side*u*.25f,cy-u*.12f)
                    close()
                }
                drawPath(ear,mint)
                drawCircle(Color(0xFF292039),u*.024f,Offset(cx+side*u*.1f,cy-u*.17f))
                drawOval(mint,Offset(cx+side*u*.1f-u*.05f,cy+u*.3f),Size(u*.11f,u*.08f))
            }
            drawOval(cream,Offset(cx-u*.14f,cy-u*.1f),Size(u*.28f,u*.14f))
            drawCircle(Color(0xFF292039),u*.027f,Offset(cx,cy-u*.07f))
            drawOval(cream,Offset(cx-u*.1f,cy+u*.06f),Size(u*.2f,u*.2f))
            if (mood == CompanionMood.Celebrate) drawCircle(Color(0xFFFFD566),u*.04f,Offset(cx+u*.34f,cy-u*.35f))
            if (mood == CompanionMood.Focused) drawLine(Color(0xFF292039),Offset(cx-u*.14f,cy-u*.23f),Offset(cx-u*.05f,cy-u*.21f),u*.015f)
        }
    }
}

@Composable
internal fun FallbackMordArtwork(modifier: Modifier) {
    Canvas(modifier) {
        val u = size.minDimension
        val c = Offset(size.width/2,size.height/2)
        val stone = Color(0xFF326B66)
        listOf(-.25f,.2f).forEach { x -> drawRoundRect(stone,Offset(c.x+u*x,c.y+u*.14f),Size(u*.18f,u*.24f)) }
        drawOval(stone,Offset(c.x-u*.33f,c.y-u*.18f),Size(u*.65f,u*.5f))
        drawCircle(Color(0xFF4B9385),u*.17f,Offset(c.x-u*.27f,c.y+u*.03f))
        drawCircle(Color(0xFFFFD576),u*.035f,Offset(c.x-u*.34f,c.y))
        listOf(-.1f,.08f,.22f).forEach { x ->
            val crystal = Path().apply {
                moveTo(c.x+u*x-u*.12f,c.y-u*.05f); lineTo(c.x+u*x,c.y-u*.42f)
                lineTo(c.x+u*x+u*.12f,c.y-u*.05f); close()
            }
            drawPath(crystal,Color(0xFF78E2BD))
        }
    }
}

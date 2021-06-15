package com.game7th.swipe.game.actors

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.RepeatAction
import com.badlogic.gdx.scenes.scene2d.actions.ScaleToAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.swipe.GdxGameContext
import ktx.actors.repeatForever

enum class WaveType {
    BOSS, REWARD, REGULAR
}

data class WaveInfo(
    val type: WaveType,
    val isPassed: Boolean
)

data class ProgressConfiguration(
        val currentLevelIndex: Int,
        val waves: List<WaveInfo>
)

class ProgressIndicatorActor(
        private val context: GdxGameContext,
        configuration: ProgressConfiguration
) : Group() {

    val background = Image(context.battleAtlas.findRegion("progress_background")).apply {
        width = (WIDTH - MARK_SIZE) * context.scale
        height = LINE_HEI * context.scale
        y = (MARK_SIZE - LINE_HEI) / 2f * context.scale
        x = MARK_SIZE * context.scale / 2f
    }

    val scaleLine = Image(context.battleAtlas.findRegion("progress_scale")).apply {
        width = (WIDTH - MARK_SIZE) * context.scale
        height = 4f * context.scale
        x = background.x
        y = background.y + 2f * context.scale
        scaleX = 0f
    }

    val marksGroup = Group()

    var configuration = configuration

    init {
        addActor(background)
        addActor(scaleLine)
        addActor(marksGroup)
        spawnMarks()
    }

    private fun spawnMarks() {
        val markCount = configuration.waves.size
        val delta = context.scale * (WIDTH - MARK_SIZE) / (markCount - 1)
        configuration.waves.forEachIndexed { index, wave ->
            val texture = when (wave.type) {
                WaveType.BOSS -> "progress_boss"
                WaveType.REWARD -> "progress_simple"
                WaveType.REGULAR -> "progress_active"
            }
            val img = Image(context.battleAtlas.findRegion(texture)).apply {
                x = index * delta - MARK_SIZE * context.scale / 2f
                width = MARK_SIZE* context.scale
                height = MARK_SIZE * context.scale
            }
            marksGroup.addActor(img)
            if (wave.type == WaveType.REWARD) {
                val rewardActor = Image(context.battleAtlas.findRegion("progress_reward")).apply {
                    x = img.x - img.width * 0.1f
                    y = img.y - img.height * 0.1f
                    width = img.width * 1.2f
                    height = img.height * 1.2f
                }
                marksGroup.addActor(rewardActor)
                rewardActor.addAction(
                    SequenceAction().apply {
                        addAction(ScaleToAction().apply { setScale(1.2f); duration = 0.6f })
                        addAction(ScaleToAction().apply { setScale(0.9f); duration = 1f })
                    }.repeatForever())
            }
        }
    }

    fun updateCurrentLevel(level: Int) {
        val markCount = configuration.waves.size
        val delta = context.scale * (WIDTH - MARK_SIZE) / (markCount - 1)
        (configuration.currentLevelIndex until level).forEach { level ->
            val img = Image(context.battleAtlas.findRegion("progress_mark")).apply {
                x = level * delta - MARK_SIZE * context.scale / 2f
                width = MARK_SIZE * context.scale
                height = MARK_SIZE * context.scale
            }
            img.setScale(0f)
            img.addAction(ScaleToAction().apply {
                setScale(1f, 1f)
                duration = 0.2f
            })
            marksGroup.addActor(img)
        }
        configuration = configuration.copy(currentLevelIndex = level)
        scaleLine.addAction(ScaleToAction().apply {
            setScale(level.toFloat() / (markCount - 1), 1f)
            duration = 0.5f
        })
    }

    companion object {
        const val WIDTH = 282f
        const val MARK_SIZE = 24f
        const val LINE_HEI = 8f
    }
}
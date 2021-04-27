@file:Suppress("NAME_SHADOWING")

package com.game7th.swipe.game.battle

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.game7th.swipe.BaseScreen
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.FigureGdxModel
import com.game7th.swipe.game.battle.model.GdxAttackType
import com.game7th.swiped.api.battle.BattleEvent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * GDX graph controller for battle
 */
class BattleController(
        private val context: GameContextWrapper,
        private val screen: BaseScreen,
        private val y: Float,
        private val sounds: Map<String, Sound>,
        private val endEventHandler: (event: BattleEvent) -> Unit
) {

    private val controllers = mutableListOf<ElementController>()

    val bgIndex = 1 + Random.nextInt(3)
    private val backgroundTexture = context.gameContext.battleAtlas.findRegion("battle_bg", bgIndex)
    private val foregroundTexture = context.gameContext.battleAtlas.findRegion("battle_fg", bgIndex)
    private val foregroundRatio = foregroundTexture.originalHeight / foregroundTexture.originalWidth.toFloat()

    val padding = 0.05f * Gdx.graphics.width

    var effectId = 100000
    var hudId = 300000
    var fgId = 200000
    var scale = 1f
    var toScale = 1f

    val controllersToRemove = mutableListOf<ElementController>()

    var timePassed = 0f                                     //how much time is passed
    var actions = mutableListOf<Pair<Float, () -> Unit>>()  //the actions to complete on time pass
    val scheduledActions = mutableListOf<Pair<Float, () -> Unit>>()
    var timeShift = 0f

    var speechLockStarted = 0f
    var isSpeechLocked = false

    init {
        controllers.add(object : ElementController(context, this, fgId++) {
            override fun render(batch: SpriteBatch, delta: Float) {
                batch.draw(foregroundTexture, 0f, y, context.width, context.width * foregroundRatio)
            }
        })
    }

    fun act(batch: SpriteBatch, delta: Float) {
        batch.draw(backgroundTexture, 0f, y, context.width, context.width / 1.25f)

        controllers.sortedBy { it.id }.forEach { it.render(batch, delta) }

        timePassed += delta * timeScale()
        timeShift = max(timePassed, timeShift)

        if (scale != toScale) {
            if (toScale / scale < 1.05f) {
                scale = toScale
            } else {
                scale += (toScale - scale) * 0.05f
            }
        }

        if (!isSpeechLocked) {
            actions.forEachIndexed { index, (timestamp, action) ->
                if (timestamp < timePassed) {
                    action()
                }
            }

            val needRecalc = controllersToRemove.firstOrNull { it is FigureController } != null

            controllers.removeAll(controllersToRemove)
            controllersToRemove.clear()
            actions.removeAll { it.first < timePassed }

            actions.addAll(scheduledActions)
            scheduledActions.clear()

            if (needRecalc) {
                recalculateScale()
            }
        }
    }

    private fun playSound(sound: String) {
        sounds[sound]?.play()
    }

    private fun findFigure(id: Int?) = controllers.firstOrNull { it.id == id } as? FigureController

    fun processEvent(event: BattleEvent) {
        when (event) {
            is BattleEvent.ShowSpeech -> {
                scheduleShowSpeech(event)
            }
            is BattleEvent.CreatePersonageEvent -> {
                scheduleCreatePersonage(event)
            }
            is BattleEvent.VictoryEvent -> {
                scheduleFinalEventPropagation(event)
            }
            is BattleEvent.DefeatEvent -> {
                scheduleFinalEventPropagation(event)
            }
            is BattleEvent.PersonageDeadEvent -> {
                schedulePersonageDeath(event)
            }
            is BattleEvent.PersonageAttackEvent -> {
                schedulePersonageAttack(event)
            }
            is BattleEvent.PersonageDamageEvent -> {
                schedulePersonageDamage(event)
            }
            is BattleEvent.PersonageHealEvent -> {
                schedulePersonageHeal(event)
            }
            is BattleEvent.ShowAilmentEffect -> {
                scheduleAilment(event)
            }
            is BattleEvent.PersonagePositionedAbilityEvent -> {
                schedulePositionedAbility(event)
            }
            is BattleEvent.PersonageUpdateEvent -> {
                schedulePersonageUpdateViewModel(event)
            }
        }
    }

    private fun schedulePersonageUpdateViewModel(event: BattleEvent.PersonageUpdateEvent) {
        scheduledActions.add(Pair(timeShift) {
            findFigure(event.personage.id)?.let { it.viewModel = event.personage }
            Unit
        })
    }

    private fun schedulePositionedAbility(event: BattleEvent.PersonagePositionedAbilityEvent) {
        scheduledActions.add(Pair(timeShift) {
            findFigure(event.source.id)?.let { figure ->
                context.gdxModel.figure(event.source.skin)?.let { gdxModel ->
                    var targetPosition = if (figure.flipped) Gdx.graphics.width - padding else padding
                    val flipFactor = if (figure.flipped) -1 else 1
                    var lastSum = 0f
                    (0..event.target).forEach { positionIndex ->
                        val figure = controllers.filter { it is FigureController && it.flipped == figure.flipped && it.position == positionIndex }.firstOrNull() as? FigureController
                        lastSum = (figure?.let { it.figureModel.scale * it.figureModel.width * scale } ?: (80f * scale))
                        targetPosition += flipFactor * lastSum
                    }
                    targetPosition -= flipFactor * lastSum / 2f
                    moveAndPunch(gdxModel, figure, targetPosition)
                }
            }
            Unit
        })
    }

    private fun scheduleAilment(event: BattleEvent.ShowAilmentEffect) {
        scheduledActions.add(Pair(timeShift) {
            context.gdxModel.ailments.firstOrNull { it.name == event.effectSkin }?.let { effect ->
                effect.sound?.let { playSound(it) }
                findFigure(event.target)?.let { figure ->
                    EffectController(context, this, effectId++, figure, effect).let {
                        controllers.add(it)
                    }
                }
            }
            Unit
        })
    }

    private fun schedulePersonageHeal(event: BattleEvent.PersonageHealEvent) {
        scheduledActions.add(Pair(timeShift) {
            findFigure(event.personage.id)?.let { figure ->
                val controller = DamagePopupController(
                        context,
                        this,
                        effectId,
                        figure.originX,
                        figure.originY + figure.figureModel.height * scale + 10 * scale,
                        event.amount,
                        Color.GREEN
                )
                controllers.add(controller)
            }
            Unit
        })
    }

    private fun schedulePersonageDamage(event: BattleEvent.PersonageDamageEvent) {
        scheduledActions.add(Pair(timeShift) {
            findFigure(event.personage.id)?.let { figure ->
                val controller = DamagePopupController(
                        context,
                        this,
                        effectId++,
                        figure.originX,
                        figure.originY + figure.figureModel.height * scale + 10 * scale,
                        event.damage,
                        Color.RED
                )
                figure.switchPose(FigurePose.POSE_DAMAGE)
                figure.viewModel = event.personage
                controllers.add(controller)
            }
            Unit
        })
    }

    private fun schedulePersonageAttack(event: BattleEvent.PersonageAttackEvent) {
        val figure = findFigure(event.source.id)
        figure?.let { figure ->
            context.gdxModel.figures.firstOrNull { it.name == event.source.skin }?.let { figureGdxModel ->
                figureGdxModel.attacks?.get(event.attackIndex)?.let { attack ->
                    when (attack.attackType) {
                        GdxAttackType.MOVE_AND_PUNCH -> {
                            findFigure(event.targets.firstOrNull()?.id)?.let { targetFigure ->
                                val tox = targetFigure.originX - (if (figure.flipped) -1f else 1f) * 70f * scale

                                moveAndPunch(figureGdxModel, figure, tox)
                            }
                        }
                        GdxAttackType.ATTACK_IN_PLACE -> {
                            val attackPose = figureGdxModel.poses.first { it.name == "attack" }
                            val attackDuration = (attackPose.end - attackPose.start) * FRAMERATE
                            val triggerDuration = attackPose.triggers?.firstOrNull()?.let { (it - attackPose.start) * FRAMERATE }
                                    ?: attackDuration

                            scheduledActions.add(Pair(timeShift) {
                                figure.switchPose(FigurePose.POSE_ATTACK)
                            })
                            timeShift += triggerDuration
                        }
                        GdxAttackType.AOE_STEPPED_GENERATOR -> {
                            attack.effect?.let { effect ->
                                val attackPose = figureGdxModel.poses.first { it.name == "attack" }
                                val attackDuration = (attackPose.end - attackPose.start) * FRAMERATE
                                val attackTriggerDuration = attackPose.triggers?.firstOrNull()?.let { (it - attackPose.start) * FRAMERATE }
                                        ?: attackDuration

                                val triggerDistance = event.targets.map { findFigure(it.id) }.map {
                                    abs((it?.originX ?: Float.MAX_VALUE) - figure.originX)
                                }.min()
                                val triggerDuration = effect.time * (triggerDistance
                                        ?: Gdx.graphics.width.toFloat()) / (scale * (effect.step
                                        ?: 1))
                                scheduledActions.add(Pair(timeShift) {
                                    figure.switchPose(FigurePose.POSE_ATTACK)
                                })
                                scheduledActions.add(Pair(timeShift + attackTriggerDuration) {
                                    SteppedGeneratorEffectController(context, this, effectId++, figure.x + 70f * scale * (if (figure.flipped) -1f else 1f), figure.originY,
                                            if (figure.flipped) 0f else Gdx.graphics.width.toFloat(), effect).let { controllers.add(it) }
                                    Unit
                                })
                                timeShift += triggerDuration + attackTriggerDuration
                            }
                        }
                        GdxAttackType.BOW_STRIKE -> {
                            attack.effect?.let { effect ->
                                val targets = event.targets.map { findFigure(it.id) }
                                val attackPose = figureGdxModel.poses.first { it.name == "attack" }
                                val attackDuration = (attackPose.end - attackPose.start) * FRAMERATE
                                val attackTriggerDuration = attackPose.triggers?.firstOrNull()?.let { (it - attackPose.start) * FRAMERATE }
                                        ?: attackDuration
                                val triggerDuration = effect.trigger * FRAMERATE
                                scheduledActions.add(Pair(timeShift) {
                                    figure.switchPose(FigurePose.POSE_ATTACK)
                                })
                                scheduledActions.add(Pair(timeShift + attackTriggerDuration) {
                                    targets.forEach {
                                        it?.let { EffectController(context, this, effectId++, it, effect).let { controllers.add(it) } }
                                    }
                                })
                                timeShift += triggerDuration + attackTriggerDuration
                            }
                        }
                    }
                }
            }
        }
    }

    private fun moveAndPunch(figureGdxModel: FigureGdxModel, figure: FigureController, tox: Float) {
        val attackPose = figureGdxModel.poses.first { it.name == "attack" }
        val attackDuration = (attackPose.end - attackPose.start) * FRAMERATE
        val triggerDuration = attackPose.triggers?.firstOrNull()?.let { (it - attackPose.start) * FRAMERATE }
                ?: attackDuration

        scheduledActions.add(Pair(timeShift) {
            figure.move(tox, figure.originY, MOVE_DURATION)
        })
        scheduledActions.add(Pair(timeShift + MOVE_DURATION) {
            figure.switchPose(FigurePose.POSE_ATTACK)
        })
        scheduledActions.add(Pair(timeShift + MOVE_DURATION + attackDuration) {
            figure.move(figure.originX, figure.originY, MOVE_DURATION)
        })
        timeShift += MOVE_DURATION + triggerDuration
    }

    private fun schedulePersonageDeath(event: BattleEvent.PersonageDeadEvent) {
        println(">>> DEATH: $timePassed, $timeShift")
        val figure = findFigure(event.personage.id)

        actions.add(Pair(timeShift) {
            figure?.let {
                it.switchPose(FigurePose.POSE_DEATH)
                it.isDead = true
            }
            controllers.firstOrNull { it is PersonageHealthbarController && it.figure.id == event.personage.id }?.let {
                it.dispose()
                controllersToRemove.add(it)
            }
            controllers.firstOrNull { it is PersonageTickerController && it.figure.id == event.personage.id }?.let {
                it.dispose()
                controllersToRemove.add(it)
            }
            Unit
        })
        if (event.blocking) {
            timeShift += (figure?.figureModel?.poses?.firstOrNull { it.name == "death" }?.let { (it.end - it.start) * FRAMERATE } ?: 0f)
        }
    }

    private fun scheduleFinalEventPropagation(event: BattleEvent) {
        actions.add(Pair(timeShift) {
            endEventHandler(event)
        })
    }

    private fun scheduleShowSpeech(event: BattleEvent.ShowSpeech) {
        actions.add(Pair(timeShift) {
            speechLockStarted = timePassed
            isSpeechLocked = true
            screen.showDialog(event.portrait, event.name, context.gameContext.texts[event.text] ?: event.text) {
                unlockSpeech()
            }
        })
        timeShift += 1f
    }

    fun unlockSpeech() {
        val delta = timePassed - speechLockStarted
        actions = actions.map { Pair(it.first + delta, it.second) }.toMutableList()
        isSpeechLocked = false
    }

    private fun scheduleCreatePersonage(event: BattleEvent.CreatePersonageEvent) {
        println(">>> CREATE: $timePassed, $timeShift")
        actions.add(Pair(timeShift) {
            FigureController(context,
                    this@BattleController,
                    event.personage.id,
                    y + 45f * context.scale,
                    event.appearStrategy,
                    context.gdxModel.figure(event.personage.skin)!!,
                    event.personage,
                    this::playSound).let {
                it.position = event.position
                controllers.add(it)
                PersonageHealthbarController(context, this@BattleController, hudId++, it).let {
                    controllers.add(it)
                }
                PersonageTickerController(context, this@BattleController, hudId++, it).let {
                    controllers.add(it)
                }
            }
            timeShift += 0.2f
            recalculateScale()
        })
    }

    private fun recalculateScale() {
        val figures = controllers.filterIsInstance<FigureController>()
        val leftFigures = figures.filter { !it.flipped }
        val rightFigures = figures.filter { it.flipped && it.pose != FigurePose.POSE_DEATH }

        val maxLeft = leftFigures.maxBy { it.position }?.position ?: 0
        val maxRight = rightFigures.maxBy { it.position }?.position ?: 0

        val totalWidthLeftNotScaled = (leftFigures.sumByDouble { it.figureModel.scale * it.figureModel.width.toDouble() } + (maxLeft - leftFigures.size + 1) * 80f).toFloat()
        val totalWidthRightNotScaled = (rightFigures.sumByDouble { it.figureModel.scale * it.figureModel.width.toDouble() } + (maxRight - rightFigures.size + 1) * 80f).toFloat()

        toScale = min((Gdx.graphics.height - y) / 384f, (Gdx.graphics.width - 4 * padding) / (totalWidthLeftNotScaled + totalWidthRightNotScaled))
        if (scale == 1f) {
            scale = toScale
        }

        println("SSCALE $scale ${rightFigures.map { it.position }} $totalWidthRightNotScaled")

        var x = padding
        (0..maxLeft).forEach { position ->
            val figures = leftFigures.filter { it.position == position }
            figures.forEach { figure ->
                figure.moveOrigin(x + toScale * figure.figureModel.scale * figure.figureModel.width / 2f, figure.originY, 1f)
                x += toScale * figure.figureModel.width * figure.figureModel.scale
            }
            if (figures.isEmpty()) {
                x += 80f * toScale
            }
        }
        x = Gdx.graphics.width - padding
        (0..maxRight).forEach { position ->
            val figures = rightFigures.filter { it.position == position }
            figures.forEach { figure ->
                figure.moveOrigin( x - toScale * figure.figureModel.scale * figure.figureModel.width / 2f, figure.originY, 1f)
                x -= toScale * figure.figureModel.width * figure.figureModel.scale
            }
            if (figures.isEmpty()) {
                x -= 80f * toScale
            }
        }
    }

    fun removeController(controller: ElementController) {
        controller.dispose()
        controllersToRemove.add(controller)
    }

    fun timeScale(): Float {
        return 1f + min(5f, timeShift - timePassed)
    }

    fun calcLeftPersonageRect(): Rectangle {
        return (controllers.first { it is FigureController && !it.flipped } as FigureController).let {
            val x = it.x - 65f * scale
            val y = it.y
            Rectangle(x, y, 140f * scale, it.figureModel.height * scale)
        }
    }

    fun calcRightPersonageRect(): Rectangle {
        return (controllers.first { it is FigureController && it.flipped } as FigureController).let {
            val x = it.x - 65f * scale
            val y = it.y
            Rectangle(x, y, 140f * scale, it.figureModel.height * scale)
        }
    }

    fun calcLeftPersonageHpBarRect(): Rectangle {
        return (controllers.first { it is PersonageHealthbarController && !it.figure.flipped } as PersonageHealthbarController).let {
            val sx = it.figure.x - 48f * scale - 5f
            val sy = it.figure.y - 15f * scale - 5f
            Rectangle(sx, sy, 96f * scale + 10f, 16f * scale + 10f)
        }
    }

    fun calcRightPersonageHpBarRect(): Rectangle {
        return (controllers.first { it is PersonageHealthbarController && it.figure.flipped } as PersonageHealthbarController).let {
            val sx = it.figure.x - 48f * scale - 5f
            val sy = it.figure.y - 15f * scale - 5f
            Rectangle(sx, sy, 96f * scale + 10f, 16f * scale + 10f)
        }
    }

    fun calcRightPersonageSkillRect(): Rectangle {
        return (controllers.first { it is PersonageTickerController && it.figure.flipped } as PersonageTickerController).let {
            val sx = it.figure.x - context.scale * 15f - 5f
            val sy = it.figure.y + (it.figure.figureModel.height + 10f) * context.scale - 5f
            Rectangle(sx, sy, 45f * scale, 45f * scale)
        }
    }

    companion object {
        const val MOVE_DURATION = 0.3f
        const val FRAMERATE = 1 / 30f
    }
}

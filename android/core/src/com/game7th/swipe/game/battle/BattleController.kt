@file:Suppress("NAME_SHADOWING")

package com.game7th.swipe.game.battle

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.game7th.battle.dto.BattleEvent
import com.game7th.swipe.BaseScreen
import com.game7th.swipe.game.GameContextWrapper
import com.game7th.swipe.game.battle.model.FigureGdxModel
import com.game7th.swipe.game.battle.model.GdxAttackType
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

    var effectId = 100000
    var hudId = 300000
    var fgId = 200000
    val scale = 0.85f * context.scale

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

        if (!isSpeechLocked) {
            actions.forEachIndexed { index, (timestamp, action) ->
                if (timestamp < timePassed) {
                    action()
                }
            }

            controllers.removeAll(controllersToRemove)
            controllersToRemove.clear()
            actions.removeAll { it.first < timePassed }

            actions.addAll(scheduledActions)
            scheduledActions.clear()
        }
    }

    private val paddingSide = context.width * 0.05f

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
                    val targetPosition = paddingSide + (context.width - 2 * paddingSide) * 0.2f * (0.5f + event.target)
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
            val x = calculatePosition(event.position)
            FigureController(context,
                    this@BattleController,
                    event.personage.id,
                    context.gdxModel.figure(event.personage.skin)!!,
                    event.personage,
                    x,
                    y + 45f * scale,
                    scale,
                    this::playSound).let {
                controllers.add(it)
                PersonageHealthbarController(context, this@BattleController, hudId++, it).let {
                    controllers.add(it)
                }
                PersonageTickerController(context, this@BattleController, hudId++, it).let {
                    controllers.add(it)
                }
            }
            timeShift += 0.2f
        })
    }

    private fun calculatePosition(position: Int) = paddingSide + (context.width - 2 * paddingSide) * 0.2f * (0.5f + position)

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

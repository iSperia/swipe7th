@file:Suppress("NAME_SHADOWING")

package com.game7th.swipe.game.battle

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.game7th.swipe.BaseScreen
import com.game7th.swipe.game.BattleContext
import com.game7th.swipe.game.battle.model.*
import com.game7th.swiped.api.battle.BattleEvent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * GDX graph controller for battle
 */
class BattleController(
        private val context: BattleContext,
        private val screen: BaseScreen,
        private val y: Float,
        private val sounds: Map<String, Sound>,
        private val endEventHandler: (event: BattleEvent) -> Unit
) {

    private val h = Gdx.graphics.height - y

    private val controllers = mutableListOf<ElementController>()

    private val backgroundTexture = context.locationAtlas.findRegion("loc_bg")
    private val foregroundTexture = context.locationAtlas.findRegion("loc_fg")
    val horizontLinePercent = 0.61f
    val horizontLine = y + context.scale * 140f
    val baseLine = y + 60f * context.scale
    val backLine = y + 80f * context.scale
    val middleLine = (baseLine + backLine) / 2f
    val foregroundScale = y / 1000f
    val backgroundScale = Gdx.graphics.height.toFloat() / backgroundTexture.packedHeight
    val foregroundOutfit = (context.width - foregroundTexture.packedWidth * foregroundScale) / 2f

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
    var timeShiftLocal = 0f
    var timeShiftGlobal = 0f

    var speechLockStarted = 0f
    var isSpeechLocked = false


    fun act(batch: SpriteBatch, delta: Float) {
        if (scale != toScale) {
            if ((toScale / scale > 1f && toScale / scale < 1.05f) ||
                    (toScale / scale < 1f) && toScale / scale > 0.95f) {
                scale = toScale
            } else {
                scale += (toScale - scale) * 0.03f
            }
        }

        val scaleNormalized = if (scale > 1f) 1f else if (scale < 0.66f) 0.66f else scale
        val textureScale = backgroundScale * scaleNormalized * 1.5f
        batch.draw(backgroundTexture, (context.width - textureScale * backgroundTexture.packedWidth) / 2f, max(context.height - textureScale * backgroundTexture.packedHeight, horizontLine - textureScale * backgroundTexture.packedHeight * horizontLinePercent), backgroundTexture.packedWidth * textureScale, backgroundTexture.packedHeight * textureScale)

        controllers.filter { it.zIndex < ElementController.Z_INDEX_HUD }.sortedBy { it.zIndex }.forEach { it.render(batch, delta) }

        batch.draw(foregroundTexture, foregroundOutfit, 0f, foregroundTexture.packedWidth * foregroundScale, foregroundTexture.packedHeight * foregroundScale)

        controllers.filter { it.zIndex >= ElementController.Z_INDEX_HUD }.forEach { it.render(batch, delta) }

        timePassed += delta
        timeShiftLocal = max(timePassed, timeShiftLocal)
        timeShiftGlobal = max(timePassed, timeShiftGlobal)

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

    fun showEffect(descriptor: EffectDescriptor, effect: PoseEffectGdxModel) {
        val controller = FigureEffectController(context, this@BattleController, effectId++, descriptor, effect)
        controllers.add(controller)
    }

    fun processEvent(event: BattleEvent) {
        when (event) {
            is BattleEvent.ShowSpeech -> {
                scheduleShowSpeech(event)
            }
            is BattleEvent.CreatePersonageEvent -> {
                scheduleCreatePersonage(event)
            }
            is BattleEvent.VictoryEvent -> {
                scheduleFinalEventPropagation(event, false)
            }
            is BattleEvent.DefeatEvent -> {
                scheduleFinalEventPropagation(event, true)
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
        scheduledActions.add(Pair(timeShiftLocal) {
            findFigure(event.personage.id)?.let { it.viewModel = event.personage }
            Unit
        })
    }

    private fun schedulePositionedAbility(event: BattleEvent.PersonagePositionedAbilityEvent) {
        scheduledActions.add(Pair(timeShiftGlobal) {
            findFigure(event.source.id)?.let { figure ->
                context.gdxModel.figure(event.source.skin)?.let { gdxModel ->
                    var targetPosition = if (figure.flipped) Gdx.graphics.width - padding else padding
                    val flipFactor = if (figure.flipped) -1 else 1
                    var lastSum = 0f
                    (0..event.target).forEach { positionIndex ->
                        val figure = controllers.filter { it is FigureController && it.flipped == figure.flipped && it.position == positionIndex }.firstOrNull() as? FigureController
                        lastSum = (figure?.let { it.figureModel.scale * it.figureModel.width * scale }
                                ?: (80f * scale))
                        targetPosition += flipFactor * lastSum
                    }
                    targetPosition -= flipFactor * lastSum / 2f
                    moveAndPunch(gdxModel, gdxModel.attacks[event.attackIndex], figure, targetPosition)
                }
            }
            Unit
        })
    }

    private fun scheduleAilment(event: BattleEvent.ShowAilmentEffect) {
        scheduledActions.add(Pair(timeShiftLocal) {
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
        scheduledActions.add(Pair(timeShiftLocal) {
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
        scheduledActions.add(Pair(timeShiftLocal) {
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
                when (figure.figureModel.render) {
                    GdxRenderType.SEQUENCE -> {
                        figure.switchPose("damage")
                    }
                    GdxRenderType.SPINE -> {
                        figure.switchPose("Damage")
                        timeShiftLocal += 0.2f
                    }
                }
                figure.viewModel = event.personage
                controllers.add(controller)
            }
            Unit
        })
    }

    private fun schedulePersonageAttack(event: BattleEvent.PersonageAttackEvent) {
        timeShiftLocal = timeShiftGlobal
        val figure = findFigure(event.source.id)
        figure?.let { figure ->
            context.gdxModel.figures.firstOrNull { it.name == event.source.skin }?.let { figureGdxModel ->
                figureGdxModel.attacks?.get(event.attackIndex)?.let { attack ->
                    val poseName = attack.pose ?: "attack"
                    when (attack.attackType) {
                        GdxAttackType.MOVE_AND_PUNCH -> {
                            findFigure(event.targets.firstOrNull()?.id)?.let { targetFigure ->
                                val tox = if (attack.shift > 0f) {
                                    targetFigure.originX - (if (figure.flipped) -1f else 1f) * attack.shift * figure.figureModel.scale * scale
                                } else {
                                    targetFigure.originX - (if (figure.flipped) -1f else 1f) * figure.figureModel.width * figure.figureModel.scale * 0.5f * scale
                                }

                                moveAndPunch(figureGdxModel, figureGdxModel.attacks[event.attackIndex], figure, tox)
                            }
                        }
                        GdxAttackType.LAUNCH_PROJECTILE -> {
                            val attackModel = figureGdxModel.attacks[event.attackIndex]
                            when (figureGdxModel.render) {
                                GdxRenderType.SPINE -> {
                                    val pose = attackModel.pose
                                    pose?.let { pose ->
                                        scheduledActions.add(Pair(timeShiftGlobal) {
                                            val targetFigure = event.targets.firstOrNull()?.let { findFigure(it.id) } ?: figure
                                            figure.switchPose(pose, EffectDescriptor.SlideToFigure(figure, targetFigure, (attack.length?:1f) - (attack.trigger ?:0f)))
                                        })
                                        timeShiftLocal += attack.length!!
                                        timeShiftGlobal += attack.length!!
                                    }
                                }
                                else -> {}
                            }
                        }
                        GdxAttackType.ATTACK_IN_PLACE -> {
                            val attackModel = figureGdxModel.attacks[event.attackIndex]
                            when (figureGdxModel.render) {
                                GdxRenderType.SPINE -> {
                                    val pose = attackModel.pose
                                    pose?.let { pose ->
                                        scheduledActions.add(Pair(timeShiftGlobal) {
                                            figure.switchPose(pose)
                                        })
                                        timeShiftLocal += attack.trigger!!
                                        timeShiftGlobal += attack.length!!
                                    }
                                }
                                GdxRenderType.SEQUENCE -> {
                                    val pose = figureGdxModel.poses?.firstOrNull { it.name == attackModel.pose }
                                    pose?.let { pose ->
                                        val attackDuration = (pose.end - pose.start) * FRAMERATE
                                        var triggerDuration = pose.triggers?.firstOrNull()?.let { (it - pose.start) * FRAMERATE }
                                                ?: attackDuration

                                        attack.effect?.let { effect ->
                                            triggerDuration = effect.trigger * FRAMERATE
                                            scheduledActions.add(Pair(timeShiftGlobal) {
                                                controllers.add(EffectController(context, this@BattleController, effectId++, figure, effect))
                                            })
                                        }

                                        scheduledActions.add(Pair(timeShiftGlobal) {
                                            figure.switchPose(pose.name)
                                        })
                                        timeShiftLocal += triggerDuration
                                        timeShiftGlobal += attackDuration
                                    }
                                }
                            }
                        }
                        GdxAttackType.AOE_STEPPED_GENERATOR -> {
                            attack.effect?.let { effect ->
                                val attackPose = figureGdxModel.poses!!.first { it.name == poseName }
                                val attackDuration = (attackPose.end - attackPose.start) * FRAMERATE
                                val attackTriggerDuration = attackPose.triggers?.firstOrNull()?.let { (it - attackPose.start) * FRAMERATE }
                                        ?: attackDuration

                                val triggerDistance = event.targets.map { findFigure(it.id) }.map {
                                    abs((it?.originX ?: Float.MAX_VALUE) - figure.originX)
                                }.min()
                                val triggerDuration = effect.time * (triggerDistance
                                        ?: Gdx.graphics.width.toFloat()) / (scale * (effect.step
                                        ?: 1))
                                scheduledActions.add(Pair(timeShiftLocal) {
                                    figure.switchPose(poseName)
                                })
                                scheduledActions.add(Pair(timeShiftLocal + attackTriggerDuration) {
                                    SteppedGeneratorEffectController(context, this, effectId++, figure.x + 70f * scale * (if (figure.flipped) -1f else 1f), figure.originY,
                                            if (figure.flipped) 0f else Gdx.graphics.width.toFloat(), effect).let { controllers.add(it) }
                                    Unit
                                })
                                timeShiftLocal += triggerDuration + attackTriggerDuration
                            }
                        }
                        GdxAttackType.BOW_STRIKE -> {
                            attack.effect?.let { effect ->
                                val targets = event.targets.map { findFigure(it.id) }
                                val attackPose = figureGdxModel.poses!!.first { it.name == "attack" }
                                val attackDuration = (attackPose.end - attackPose.start) * FRAMERATE
                                val attackTriggerDuration = attackPose.triggers?.firstOrNull()?.let { (it - attackPose.start) * FRAMERATE }
                                        ?: attackDuration
                                val triggerDuration = effect.trigger * FRAMERATE
                                scheduledActions.add(Pair(timeShiftLocal) {
                                    figure.switchPose("attack")
                                })
                                scheduledActions.add(Pair(timeShiftLocal + attackTriggerDuration) {
                                    targets.forEach {
                                        it?.let { EffectController(context, this, effectId++, it, effect).let { controllers.add(it) } }
                                    }
                                })
                                timeShiftLocal += triggerDuration + attackTriggerDuration
                            }
                        }
                    }
                }
            }
        }
    }

    private fun moveAndPunch(figureGdxModel: FigureGdxModel, attack: AttackGdxModel, figure: FigureController, tox: Float) {
        timeShiftLocal = timeShiftGlobal
        when (figureGdxModel.render) {
            GdxRenderType.SEQUENCE -> {
                val attackPose = figureGdxModel.poses!!.first { it.name == "attack" }
                val attackDuration = (attackPose.end - attackPose.start) * FRAMERATE
                val triggerDuration = attackPose.triggers?.firstOrNull()?.let { (it - attackPose.start) * FRAMERATE }
                        ?: attackDuration

                scheduledActions.add(Pair(timeShiftGlobal) {
                    figure.move(tox, figure.originY, MOVE_DURATION)
                })
                scheduledActions.add(Pair(timeShiftGlobal + MOVE_DURATION) {
                    figure.switchPose("attack")
                })
                scheduledActions.add(Pair(timeShiftGlobal + MOVE_DURATION + attackDuration) {
                    figure.move(figure.originX, figure.originY, MOVE_DURATION)
                })
                this.timeShiftLocal += MOVE_DURATION + triggerDuration
                this.timeShiftGlobal += MOVE_DURATION * 2 + attackDuration
            }
            GdxRenderType.SPINE -> {
                val trigger = attack.trigger!!

                scheduledActions.add(Pair(timeShiftGlobal) {
                    figure.move(tox, figure.originY, MOVE_DURATION)
                })
                scheduledActions.add(Pair(timeShiftGlobal + MOVE_DURATION) {
                    figure.switchPose(attack.pose!!)
                })
                scheduledActions.add(Pair(timeShiftGlobal + MOVE_DURATION + attack.length!!) {
                    figure.move(figure.originX, figure.originY, MOVE_DURATION)
                })
                this.timeShiftLocal += MOVE_DURATION + trigger
                this.timeShiftGlobal += 2 * MOVE_DURATION + attack.length
            }
        }
    }

    private fun schedulePersonageDeath(event: BattleEvent.PersonageDeadEvent) {
        println(">>> DEATH: $timePassed, $timeShiftLocal")
        val figure = findFigure(event.personage.id)

        scheduledActions.add(Pair(timeShiftLocal) {
            figure?.let {
                it.switchPose("Death")
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
        figure?.figureModel?.poses?.firstOrNull { it.name == "Death" }?.triggers?.let { triggers ->
            if (triggers.isNotEmpty()) timeShiftLocal += triggers[0] * FRAMERATE
        }
        if (event.blocking) {
            timeShiftLocal += (figure?.figureModel?.poses?.firstOrNull { it.name == "death" }?.let { (it.end - it.start) * FRAMERATE }
                    ?: 0f)
        }
    }

    private fun scheduleFinalEventPropagation(event: BattleEvent, flipped: Boolean) {
        scheduledActions.add(Pair(timeShiftLocal) {
            controllers.mapNotNull { it as? FigureController }.filter { it.flipped == flipped }.forEach {
                if (it.figureModel.render == GdxRenderType.SPINE) {
                    it.switchPose("Win")
                }
            }
            endEventHandler(event)
        })
    }

    private fun scheduleShowSpeech(event: BattleEvent.ShowSpeech) {
        scheduledActions.add(Pair(timeShiftLocal) {
            speechLockStarted = timePassed
            isSpeechLocked = true
            screen.showDialog(event.portrait, event.name, context.gameContext.texts[event.text]
                    ?: event.text) {
                unlockSpeech()
            }
        })
        timeShiftLocal += 1f
    }

    fun unlockSpeech() {
        val delta = timePassed - speechLockStarted
        actions = actions.map { Pair(it.first + delta, it.second) }.toMutableList()
        isSpeechLocked = false
    }

    private fun scheduleCreatePersonage(event: BattleEvent.CreatePersonageEvent) {
        println(">>> CREATE: $timePassed, $timeShiftLocal ${event.appearAttachUnitId} ${event.appearPose}")
        scheduledActions.add(Pair(timeShiftLocal) {
            FigureController(context,
                    this@BattleController,
                    event.personage.id,
                    baseLine,
                    context.gdxModel.figure(event.personage.skin)!!,
                    event.personage,
                    this::playSound).let {
                it.position = event.position
                controllers.add(it)
                if (event.appearAttachUnitId > 0) {
                    findFigure(event.appearAttachUnitId)?.let { attachFigure ->
                        it.originX = attachFigure.originX
                        it.originY = attachFigure.originY
                        it.x = attachFigure.x
                        it.y = attachFigure.y
                        it.fromX = attachFigure.x
                        it.fromY = attachFigure.y
                        it.targetX = attachFigure.x
                        it.targetY = attachFigure.y
                    }
                    event.appearPose?.let { pose ->
                        it.switchPose(pose)
                    }
                }
                PersonageHealthbarController(context, this@BattleController, hudId++, it).let {
                    controllers.add(it)
                    it.zIndex = ElementController.Z_INDEX_HUD
                }
                PersonageTickerController(context, this@BattleController, hudId++, it).let {
                    controllers.add(it)
                    it.zIndex = ElementController.Z_INDEX_HUD
                }
            }
            if (event.appearAttachUnitId == 0) {
                recalculateScale()
            }
        })
    }

    private fun recalculateScale() {
        val figures = controllers.filterIsInstance<FigureController>()
        val leftFigures = figures.filter { !it.flipped }
        val rightFigures = figures.filter { it.flipped && it.pose != "death" }

        val maxLeft = leftFigures.maxBy { it.position }?.position ?: 0
        val maxRight = rightFigures.maxBy { it.position }?.position ?: 0

        val totalWidthLeftNotScaled = (leftFigures.sumByDouble { it.figureModel.scale * it.figureModel.width.toDouble() } + (maxLeft - leftFigures.size + 1) * 80f).toFloat()
        val totalWidthRightNotScaled = (rightFigures.sumByDouble { it.figureModel.scale * it.figureModel.width.toDouble() } + (maxRight - rightFigures.size + 1) * 80f).toFloat()

        val targetScale = (Gdx.graphics.width - 4 * padding) / (totalWidthLeftNotScaled + totalWidthRightNotScaled)
        toScale = min(Gdx.graphics.width * 2f / 1440f, max(Gdx.graphics.width * 1f / 1440f, targetScale))

        val needBackLine = targetScale < toScale
        val overWidth = 2 * padding + toScale * (totalWidthLeftNotScaled + totalWidthRightNotScaled) - Gdx.graphics.width
        val shift = max(0f, overWidth / (maxLeft + maxRight))

        var x = padding
        var rIndex = 0
        (0..maxLeft).forEachIndexed { index, position ->
            val figures = leftFigures.filter { it.position == position }
            figures.forEach { figure ->
                val figureY = if (!needBackLine) baseLine else if (leftFigures.size == 1) middleLine else if (rIndex % 2 == 0) baseLine else backLine
                figure.moveOrigin(x + toScale * figure.figureModel.scale * figure.figureModel.width / 2f, figureY, 1f)
                x += toScale * figure.figureModel.width * figure.figureModel.scale - shift
                figure.zIndex = 1000 * (1 - rIndex % 2) + index
                rIndex++
            }
            if (figures.isEmpty()) {
                x += 80f * toScale - shift
            }
        }
        x = Gdx.graphics.width - padding
        rIndex = 0
        (0..maxRight).forEachIndexed { index, position ->
            val figures = rightFigures.filter { it.position == position }
            figures.forEach { figure ->
                val figureY = if (!needBackLine) baseLine else if (rightFigures.size == 1) middleLine else if (rIndex % 2 == 0) baseLine else backLine
                figure.moveOrigin(x - toScale * figure.figureModel.scale * figure.figureModel.width / 2f, figureY, 1f)
                x -= toScale * figure.figureModel.width * figure.figureModel.scale - shift
                figure.zIndex = 1000 * (1 - rIndex % 2) + index
                rIndex++
            }
            if (figures.isEmpty()) {
                x -= 80f * toScale - shift
            }
        }
    }

    fun removeController(controller: ElementController) {
        controller.dispose()
        controllersToRemove.add(controller)
    }

    companion object {
        const val MOVE_DURATION = 0.3f
        const val FRAMERATE = 1 / 60f
    }
}

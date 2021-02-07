package com.game7th.swipe.game.actors

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.MoveToAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.battle.event.BattleEvent
import com.game7th.swipe.game.GdxGameContext

class BattleView(private val gameContext: GdxGameContext) : Group() {

    val image = Image(gameContext.atlas.findRegion("battle_bg", 0)).apply {
        zIndex = 1
    }

    val personages = Group().apply {
        zIndex = 5
    }

    init {
        x = 0f
        y = 720f - 240f

        addActor(image)
        addActor(personages)
    }

    fun processAction(event: BattleEvent) {
        println("Processing action ${event.javaClass.name}")
        when (event) {
            is BattleEvent.CreatePersonageEvent -> {
                val personage = PersonageActor(gameContext, event.personage)
                personage.x = 60f * event.position
                personage.y = 60f
                personage.name = "${event.personage.id}"
                personages.addActor(personage)
            }
            is BattleEvent.PersonageAttackEvent -> {
                val source = personages.findActor<PersonageActor>("${event.source.id}")
                val target = personages.findActor<PersonageActor>("${event.target.id}")
                val animationDistance = if (source.x > target.x) target.x + 15 else target.x - 15

                val sx = source.x
                val sy = source.y

                source.addAction(SequenceAction(
                        MoveToAction().apply {
                            setPosition(target.x + animationDistance, target.y)
                            duration = 0.15f
                        },
                        MoveToAction().apply {
                            setPosition(sx, sy)
                            duration = 0.1f
                        }
                ))
            }
            is BattleEvent.PersonageDamageEvent -> {
                //TODO:
            }
        }
    }
}
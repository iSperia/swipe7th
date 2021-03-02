package com.game7th.swipe.game.actors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import com.game7th.battle.event.BattleEvent
import com.game7th.swipe.GdxGameContext
import ktx.actors.alpha
import ktx.actors.centerPosition

class BattleView(private val gameContext: GdxGameContext) : Group() {

    val image = Image(gameContext.atlas.findRegion("battle_bg", 0)).apply {
        zIndex = 1
    }

    val personages = Group().apply {
        zIndex = 5
    }

    val effectsForeground = Group().apply {
        zIndex = 6
    }

    init {
        x = 0f
        y = 720f - 240f

        addActor(image)
        addActor(personages)
        addActor(effectsForeground)
    }

    fun processAction(event: BattleEvent) {
        when (event) {
            is BattleEvent.CreatePersonageEvent -> {
                val personage = PersonageActor(gameContext, event.personage)
                personage.x = 60f * event.position
                personage.name = "${event.personage.id}"
                personages.addActor(personage)
            }
//            is BattleEvent.PersonageAttackEvent -> {
//                val source = personages.findActor<PersonageActor>("${event.source.id}")
//                val target = personages.findActor<PersonageActor>("${event.?target.id}")
//                val animationDistance = if (source.x > target.x) target.x + 15 else target.x - 15
//
//                val sx = source.x
//                val sy = source.y
//
//                source.addAction(SequenceAction(
//                        MoveToAction().apply {
//                            setPosition(target.x + animationDistance, target.y)
//                            duration = 0.15f
//                        },
//                        MoveToAction().apply {
//                            setPosition(sx, sy)
//                            duration = 0.1f
//                        }
//                ))
//            }
            is BattleEvent.PersonageDamageEvent -> {
                //TODO: add animated numbers
                val personageId = event.personage.id
                val personageActor = personages.findActor<PersonageActor>("$personageId")
                personageActor?.updateFrom(event.personage)
            }
            is BattleEvent.PersonageUpdateEvent -> {
                val personageId = event.personage.id
                val personageActor = personages.findActor<PersonageActor>("$personageId")
                personageActor?.updateFrom(event.personage)
            }
            is BattleEvent.ShowNpcAoeEffect -> {
                val personageId = event.personageId

                val projectileImage = Image(gameContext.atlas.findRegion(event.skin))
                val personageActor = personages.findActor<PersonageActor>("$personageId")
                projectileImage.apply {
                    centerPosition(0.5f, 0.5f)
                    x = personageActor.x + 30f
                    y = personageActor.y + 60f
                    alpha = 0.1f

                    addAction(SequenceAction(
                            ParallelAction(
                                    AlphaAction().apply {
                                        alpha = 0.5f
                                        duration = 0.1f
                                    },
                                    ScaleToAction().apply {
                                        setScale(1.5f)
                                        duration = 0.5f
                                    },
                                    MoveByAction().apply {
                                        amountX = 340f * event.direction
                                        duration = 0.5f
                                    }
                            ), RunnableAction().apply {
                        setRunnable {
                            projectileImage.clearActions()
                            projectileImage.remove()
                        }
                    }))
                }

                effectsForeground.addActor(projectileImage)
            }
            is BattleEvent.ShowProjectile -> {
                val projectileImage = Image(gameContext.atlas.findRegion(event.skin))
                val sourceActor = personages.findActor<PersonageActor>("${event.sourceId}")
                val targetActor = personages.findActor<PersonageActor>("${event.targetId}")
                if (sourceActor != null && targetActor != null) {
                    effectsForeground.addActor(projectileImage)
                    projectileImage.apply {
                        x = sourceActor.x
                        y = sourceActor.y + 50f
                        addAction(SequenceAction(
                                MoveToAction().apply {
                                    setPosition(targetActor.x, targetActor.y)
                                    duration = 0.2f
                                },
                                RunnableAction().apply {
                                    setRunnable {
                                        projectileImage.clearActions()
                                        projectileImage.remove()
                                    }
                                }
                        ))
                    }
                }
            }
            is BattleEvent.ShowAilmentEffect -> {
                val effectImage = Image(gameContext.atlas.findRegion(event.effectSkin))
                val targetActor = personages.findActor<PersonageActor>("${event.target}")
                targetActor?.let { targetActor ->
                    effectImage.apply {
                        x = targetActor.x
                        y = targetActor.y + 50f
                    }
                    effectsForeground.addActor(effectImage)
                    effectImage.addAction(DelayAction(0.1f).apply {
                        action = RunnableAction().apply {
                            setRunnable {
                                effectImage.clearActions()
                                effectImage.remove()
                            }
                        }
                    })
                }
            }
            is BattleEvent.RemovePersonageEvent -> {
                val targetActor = personages.findActor<PersonageActor>("${event.target}")
                targetActor.remove()
            }
            is BattleEvent.NewWaveEvent -> {
                val label = Label("WAVE ${event.wave + 1}", Label.LabelStyle(gameContext.font, Color.CYAN)).apply {
                    setFontScale(3f)
                    setAlignment(Align.center)
                    x = (480f - width) / 2f
                    y = (240f - height) / 2f
                }
                effectsForeground.addActor(label)
                label.addAction(SequenceAction(
                        ParallelAction(
                                AlphaAction().apply {
                                    alpha = 0f
                                    duration = 2f
                                },
                                ScaleToAction().apply {
                                    setScale(4f)
                                    duration = 2f
                                }
                        ),
                        RunnableAction().apply {
                            setRunnable {
                                label.clearActions()
                                label.remove()
                            }
                        }
                ))
            }
        }
    }
}
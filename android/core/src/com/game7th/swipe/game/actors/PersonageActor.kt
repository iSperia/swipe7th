package com.game7th.swipe.game.actors

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.game7th.battle.personage.PersonageViewModel
import com.game7th.swipe.GdxGameContext

class PersonageActor(
        private val context: GdxGameContext,
        private var vm: PersonageViewModel
) : Group() {

    var body: Image? = null
    val healthBarGreen: Image
    val healthBarRed: Image
    var armorBarBlack: Image? = null
    var armorBarGrey: Image? = null
    var resistBarBlack: Image? = null
    var resistBarGrey: Image? = null
    val healthAmount: Label
    val armorAmount: Label
    val resistAmount: Label
    val tickLabel: Label

    val effects = Group()

    init {
        showBody(vm)

        healthBarRed = Image(context.atlas.findRegion("health_bar_red")).apply {
            y = 50f
            x = 10f
            width = 40f
            height = 2f
        }
        addActor(healthBarRed)

        healthBarGreen = Image(context.atlas.findRegion("health_bar_green")).apply {
            y = 50f
            x = 10f
            width = 40f
            scaleX = vm.stats.health.toFloat() / vm.stats.maxHealth
            height = 2f
        }
        addActor(healthBarGreen)

        var nextY = 50f

        if (vm.stats.maxArmor > 0) {
            nextY += 3f
            armorBarBlack = Image(context.atlas.findRegion("armor_bar_black")).apply {
                y = nextY
                x = 10f
                width = 40f
                height = 2f
            }
            addActor(armorBarBlack)


            armorBarGrey = Image(context.atlas.findRegion("armor_bar_grey")).apply {
                y = nextY
                x = 10f
                width = 40f
                height = 2f
                scaleX = vm.stats.armor.toFloat() / vm.stats.maxArmor
            }
            addActor(armorBarGrey)
        }
        if (vm.stats.magicDefense > 0) {
            nextY += 3f
            resistBarBlack = Image(context.atlas.findRegion("resist_bar_black")).apply {
                y = nextY
                x = 10f
                width = 40f
                height = 2f
            }
            addActor(resistBarBlack)


            resistBarGrey = Image(context.atlas.findRegion("resist_bar_blue")).apply {
                y = nextY
                x = 10f
                width = 40f
                height = 2f
                scaleX = vm.stats.armor.toFloat() / vm.stats.maxArmor
            }
            addActor(resistBarGrey)
        }

        healthAmount = Label("${vm.stats.health}", Label.LabelStyle(context.font, Color.RED)).apply {
            x = 10f
            y = 22f
            zIndex = 10
        }
        armorAmount = Label("${vm.stats.armor}", Label.LabelStyle(context.font, Color.BLACK)).apply {
            x = 10f
            y = 10f
            setFontScale(0.8f)
            zIndex = 12
        }
        resistAmount = Label("${vm.stats.magicDefense}", Label.LabelStyle(context.font, Color.BLUE)).apply {
            x = 10f
            y = 0f
            setFontScale(0.8f)
            zIndex = 13
        }
        tickLabel = Label(getTickText(), Label.LabelStyle(context.font, Color.CYAN)).apply {
            x = 10f
            y = 120f
            zIndex = 14
        }
        addActor(healthAmount)
        addActor(armorAmount)
        addActor(resistAmount)
        addActor(tickLabel)

        addActor(effects)
    }

    private fun getTickText(): String {
        return if (vm.stats.maxTick > 0) {
            "${vm.stats.tick}/${vm.stats.maxTick}"
        } else ""
    }

    private fun hideBody() {
        val oldBody = body
        oldBody?.zIndex = 0
        oldBody?.addAction(SequenceAction(
                AlphaAction().apply {
                    alpha = 0f
                    duration = 0.2f
                },
                RunnableAction().apply {
                    setRunnable {
                        oldBody.clearActions()
                        oldBody.remove()
                    }
                }
        ))
    }

    private fun showBody(viewModel: PersonageViewModel) {
        body = Image(context.atlas.findRegion(if (viewModel.stats.health > 0) viewModel.skin else "personage_dead")).apply {
            y = 60f
            zIndex = 1
        }
        addActor(body)
    }

    fun updateFrom(viewModel: PersonageViewModel) {
        healthBarGreen.addAction(ScaleToAction().apply {
            setScale(viewModel.stats.health.toFloat() / viewModel.stats.maxHealth, 1f)
            duration = 0.2f
        })
        healthAmount.setText("${viewModel.stats.health}")
        if (vm.stats.health > 0 && viewModel.stats.health == 0) {
            hideBody()
            showBody(viewModel)
        }
        vm = viewModel

        if (viewModel.stats.maxArmor > 0) {
            armorBarGrey?.addAction(ScaleToAction().apply {
                setScale(viewModel.stats.armor.toFloat() / viewModel.stats.maxArmor, 1f)
                duration = 0.2f
            })
            armorAmount.setText("${viewModel.stats.armor}")
        }

        if (viewModel.stats.maxMagicDefense > 0) {
            resistBarGrey?.addAction(ScaleToAction().apply {
                setScale(viewModel.stats.magicDefense.toFloat() / viewModel.stats.maxMagicDefense, 1f)
                duration = 0.2f
            })
            resistAmount.setText("${viewModel.stats.magicDefense}")
        }

        tickLabel.setText(getTickText())
    }

    fun showEvadeAnimation() {
        val evasionText = Label("Evaded", Label.LabelStyle(context.font, Color.GREEN)).apply {
            width = 60f
            height = 20f
            y = 100f
        }

        effects.addActor(evasionText)
        evasionText.addAction(
                SequenceAction(
                        ParallelAction(
                                MoveByAction().apply {
                                    setAmount(0f, 50f)
                                    duration = 0.4f
                                },
                                AlphaAction().apply {
                                    alpha = 0f
                                    duration = 0.4f
                                }
                        ),
                        RunnableAction().apply {
                            setRunnable {
                                evasionText.clearActions()
                                evasionText.remove()
                            }
                        }))

        body?.addAction(RepeatAction().apply {
            action = ParallelAction(
                    SequenceAction(
                            MoveByAction().apply {
                                setAmount(1f, 0f)
                                duration = 0.05f
                            },
                            MoveByAction().apply {
                                setAmount(-1f, 0f)
                                duration = 0.05f
                            }
                    ),
                    SequenceAction(
                            AlphaAction().apply {
                                alpha = 0.5f
                                duration = 0.05f
                            },
                            AlphaAction().apply {
                                alpha = 1f
                                duration = 0.05f
                            }
                    )
            )
            count = 5
        })
    }

}
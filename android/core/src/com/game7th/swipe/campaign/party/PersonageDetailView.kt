package com.game7th.swipe.campaign.party

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.MoveByAction
import com.badlogic.gdx.scenes.scene2d.actions.RunnableAction
import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.game7th.metagame.account.AccountService
import com.game7th.metagame.inventory.GearService
import com.game7th.metagame.dto.UnitConfig
import com.game7th.metagame.dto.UnitType
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.campaign.inventory.InventoryEditor
import com.game7th.swipe.campaign.plist.PersonageVerticalPortrait
import com.game7th.swipe.campaign.plist.toPortraitConfig
import com.game7th.swiped.api.PersonageDto
import kotlinx.coroutines.launch
import ktx.actors.onClick
import ktx.async.KtxAsync
import kotlin.math.exp

sealed class UiState {
    object Initial : UiState()
    object Gear : UiState()
}

class PersonageDetailView(
        private val context: GdxGameContext,
        private val accountService: AccountService,
        private val gearService: GearService,
        private val personageId: String
) : Group() {

    var state: UiState = UiState.Initial

    lateinit var personage: PersonageDto

    val bg = Image(context.uiAtlas.createPatch("ui_hor_panel")).apply {
        width = 480f * context.scale
        height = 240f * context.scale
    }

    lateinit var portrait: PersonageVerticalPortrait

    val attrsBg = Image(context.uiAtlas.findRegion("ui_attrs_tree")).apply {
        x = 140f * context.scale
        y = 10f * context.scale
        width = 160f * context.scale
        height = 160f * context.scale
    }

    val sh = attrsBg.height / 3f

    val ah = 0.08f * attrsBg.height
    val aw = 0.13f * attrsBg.height

    val bodyLabel = Label("", Label.LabelStyle(context.font, Color.WHITE)).apply {
        x = attrsBg.x + 0.19f * attrsBg.width
        y = attrsBg.y + 0.04f * attrsBg.height
        width = aw
        height = ah
        setAlignment(Align.center)
        setFontScale(ah / 36f)
    }

    val spiritLabel = Label("", Label.LabelStyle(context.font, Color.WHITE)).apply {
        x = attrsBg.x + 0.45f * attrsBg.width
        y = attrsBg.y + 0.87f * attrsBg.height
        width = aw
        height = ah
        setAlignment(Align.center)
        setFontScale(ah / 36f)
    }

    val mindLabel = Label("", Label.LabelStyle(context.font, Color.WHITE)).apply {
        x = attrsBg.x + 0.7f * attrsBg.width
        y = attrsBg.y + 0.04f * attrsBg.height
        width = aw
        height = ah
        setAlignment(Align.center)
        setFontScale(ah / 36f)
    }

    val secondAttrsBody = Label("", Label.LabelStyle(context.font, Color(0xff2626ff.toInt()))).apply {
        y = attrsBg.y + 2 * sh
        x = attrsBg.x + attrsBg.width + context.scale * 0.1f
        width = 140f * context.scale
        height = sh
        setFontScale(sh / 2.3f / 36f)
        setAlignment(Align.left)
    }

    val secondAttrsSpirit = Label("", Label.LabelStyle(context.font, Color(0x00B200ff))).apply {
        y = attrsBg.y + sh
        x = attrsBg.x + attrsBg.width + context.scale * 0.1f
        width = 140f * context.scale
        height = sh
        setFontScale(sh / 2.3f / 36f)
        setAlignment(Align.left)
    }

    val secondAttrsMind = Label("", Label.LabelStyle(context.font, Color(0x00A3D9ff))).apply {
        y = attrsBg.y
        x = attrsBg.x + attrsBg.width + context.scale * 0.1f
        width = 140f * context.scale
        height = sh
        setFontScale(sh / 2.3f / 36f)
        setAlignment(Align.left)
    }

    var nextLevelExp: Int = 0

    lateinit var experienceBar: ExperienceBar


    val buttonGear = Image(context.uiAtlas.findRegion("icon_gear")).apply {
        x = attrsBg.x
        y = 182f * context.scale
        width = 48f * context.scale
        height = 48f * context.scale
    }

    var inventoryView: InventoryEditor? = null

    init {
        KtxAsync.launch {
            personage = accountService.getPersonages().first { it.id == personageId }

            portrait = PersonageVerticalPortrait(context, UnitConfig(UnitType.valueOf(personage.unit), personage.level).toPortraitConfig(), 180f * context.scale).apply {
                x = 10f * context.scale
                y = 10f * context.scale
            }

            nextLevelExp = ((personage.level - 1) + exp(personage.level * 0.1f)).toInt() * personage.level * 50
            experienceBar = ExperienceBar(context, 120f * context.scale, 30f * context.scale, personage.experience, nextLevelExp).apply {
                x = 10f * context.scale
                y = 200f * context.scale
                touchable = Touchable.disabled
            }

            addActor(bg)
            addActor(portrait)
            addActor(attrsBg)

            addActor(bodyLabel)
            addActor(spiritLabel)
            addActor(mindLabel)

            addActor(secondAttrsBody)
            addActor(secondAttrsSpirit)
            addActor(secondAttrsMind)

            addActor(experienceBar)

            addActor(buttonGear)

            bg.onClick { }
            buttonGear.onClick {
                processGearButton()
            }
            refreshStats()
        }
    }

    fun processGearButton() {
        if (state == UiState.Gear) {
            transiteUiState(UiState.Initial)
        } else {
            transiteUiState(UiState.Gear)
        }
    }

    fun hide() {
        transiteUiState(UiState.Initial)
    }

    private fun transiteUiState(state: UiState) {
        when (this.state) {
            is UiState.Gear -> hideGear()
        }

        this.state = state

        when (this.state) {
            is UiState.Gear -> showGear()
        }
    }

    private fun showGear() {
        buttonGear.drawable = TextureRegionDrawable(context.uiAtlas.findRegion("icon_gear_focused"))
        inventoryView = InventoryEditor(context, accountService, gearService, personageId, this::refreshStats).apply {
            y = (240f - 200f) * context.scale
            addAction(MoveByAction().apply { amountY = 200f * context.scale; duration = 0.2f })
        }
        addActor(inventoryView)
        inventoryView?.zIndex = 0
    }

    private fun hideGear() {
        buttonGear.drawable = TextureRegionDrawable(context.uiAtlas.findRegion("icon_gear"))
        inventoryView?.let { inventory ->
            inventory.addAction(SequenceAction(
                    MoveByAction().apply { amountY = -200f * context.scale; duration = 0.2f },
                    RunnableAction().apply { setRunnable { inventory.remove() } }
            ))
        }
    }

    private fun refreshStats() {
        KtxAsync.launch {
            val stats = accountService.getPersonageGearStats(personageId)

            bodyLabel.setText(stats.body.toString())
            spiritLabel.setText(stats.spirit.toString())
            mindLabel.setText(stats.mind.toString())

            secondAttrsBody.setText("Health: ${stats.health}\nArmor: ${stats.armor}")
            secondAttrsSpirit.setText("Evasion: ${stats.evasion}\nRegeneration: ${stats.regeneration}")
            secondAttrsMind.setText("Resist: ${stats.resist}\nWisdom: ${stats.wisdom}")
        }
    }
}
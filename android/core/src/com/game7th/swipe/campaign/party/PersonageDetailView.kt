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
import com.game7th.metagame.unit.UnitConfig
import com.game7th.swipe.GdxGameContext
import com.game7th.swipe.ScreenContext
import com.game7th.swipe.campaign.inventory.InventoryEditor
import com.game7th.swipe.campaign.plist.PersonageVerticalPortrait
import com.game7th.swipe.campaign.plist.toPortraitConfig
import ktx.actors.onClick
import kotlin.math.exp

sealed class UiState {
    object Initial : UiState()
    object Gear: UiState()
}

class PersonageDetailView(
        private val context: GdxGameContext,
        private val accountService: AccountService,
        private val gearService: GearService,
        private val personageId: Int
) : Group() {

    var state: UiState = UiState.Initial

    private val personage = accountService.getPersonages().first { it.id == personageId }

    val bg = Image(context.uiAtlas.findRegion("ui_dialog")).apply {
        width = 480f * context.scale
        height = 240f * context.scale
    }

    val portrait = PersonageVerticalPortrait(context, UnitConfig(personage.unit, personage.level).toPortraitConfig(), 180f * context.scale).apply {
        x = 10f * context.scale
        y = 10f * context.scale
    }

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

    val secondAttrsBody = Label("", Label.LabelStyle(context.font, Color.RED)).apply {
        y = attrsBg.y + 2 * sh
        x = attrsBg.x + attrsBg.width + context.scale * 0.1f
        width = 140f * context.scale
        height = sh
        setFontScale(sh / 2.3f / 36f)
        setAlignment(Align.left)
    }

    val secondAttrsSpirit = Label("", Label.LabelStyle(context.font, Color.FOREST)).apply {
        y = attrsBg.y + sh
        x = attrsBg.x + attrsBg.width + context.scale * 0.1f
        width = 140f * context.scale
        height = sh
        setFontScale(sh / 2.3f / 36f)
        setAlignment(Align.left)
    }

    val secondAttrsMind = Label("", Label.LabelStyle(context.font, Color.BLUE)).apply {
        y = attrsBg.y
        x = attrsBg.x + attrsBg.width + context.scale * 0.1f
        width = 140f * context.scale
        height = sh
        setFontScale(sh / 2.3f / 36f)
        setAlignment(Align.left)
    }

    val nextLevelExp = ((personage.level - 1) + exp(personage.level * 0.1f)).toInt() * personage.level * 50

    val experienceBar = ExperienceBar(context, 120f * context.scale, 30f * context.scale, personage.experience, nextLevelExp).apply {
        x = 10f * context.scale
        y = 200f * context.scale
        touchable = Touchable.disabled
    }

    val buttonGear = Image(context.uiAtlas.findRegion("icon_gear")).apply {
        x = attrsBg.x
        y = 200f * context.scale
        width = 30f * context.scale
        height = 30f * context.scale
    }

    var inventoryView: InventoryEditor? = null

    init {
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

        bg.onClick {  }
        buttonGear.onClick {
            if (state == UiState.Gear) {
                transiteUiState(UiState.Initial)
            } else {
                transiteUiState(UiState.Gear)
            }
        }
        refreshStats()
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
        )) }
    }

    private fun refreshStats() {
        val baseStats = context.balance.produceBaseStats(personage)
        val gearStats = context.balance.produceGearStats(personage)

        gearStats.body.toString().let { bodyLabel.setText(it) }
        gearStats.spirit.toString().let { spiritLabel.setText(it) }
        gearStats.mind.toString().let { mindLabel.setText(it) }

        secondAttrsBody.setText("Health: ${gearStats.health.value}(${baseStats.health.value})\nArmor: ${gearStats.armor}(${baseStats.armor})")
        secondAttrsSpirit.setText("Evasion: ${gearStats.evasion}(${baseStats.evasion})\nRegeneration: ${gearStats.regeneration}(${baseStats.regeneration})")
        secondAttrsMind.setText("Resist: ${gearStats.resist}(${baseStats.resist})\nWisdom: ${gearStats.wisdom}(${baseStats.wisdom})")
    }
}
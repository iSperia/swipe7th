package com.game7th.swipe.game.actors

import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.game7th.battle.personage.PersonageViewModel
import com.game7th.swipe.game.GdxGameContext

class PersonageActor(
        context: GdxGameContext,
        viewModel: PersonageViewModel
) : Group() {

    var body: Image

    init {
        body = Image(context.atlas.findRegion(viewModel.skin))
        addActor(body)
    }
}
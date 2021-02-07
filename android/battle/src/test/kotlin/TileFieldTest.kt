import com.game7th.battle.tilefield.TileField
import com.game7th.battle.tilefield.TileFieldContext
import com.game7th.battle.tilefield.TileFieldEvent
import com.game7th.battle.tilefield.tile.SwipeTile
import com.game7th.battle.tilefield.tile.TileStage
import com.game7th.battle.tilefield.tile.TileType
import org.junit.Test

class TileFieldTest {

    private val context = object : TileFieldContext {
        override fun merge(tile: SwipeTile, swipeTile: SwipeTile): SwipeTile? {
            if (tile.type != swipeTile.type) return null
            return SwipeTile(tile.type, swipeTile.id, tile.stackSize + swipeTile.stackSize, tile.stage)
        }
    }

    @Test
    fun `Check swipe right`() {
        val field = TileField(context)
        field.tiles[24] = SwipeTile(TileType.GLADIATOR_STRIKE, 0, 1, TileStage.NO_STAGE)
        field.tiles[25] = SwipeTile(TileType.GLADIATOR_STRIKE, 1, 2, TileStage.NO_STAGE)
        field.tiles[14] = SwipeTile(TileType.GLADIATOR_STRIKE, 2, 3, TileStage.NO_STAGE)

        val events = field.attemptSwipe(1, 0)

        assert(events.size == 3)
        assert(events[0] is TileFieldEvent.MoveTileEvent)
        assert(events[1] is TileFieldEvent.MoveTileEvent)
        assert(events[2] is TileFieldEvent.MoveTileEvent)
        assert(field.tiles[24] == null)
    }

    @Test
    fun `Check swipe right merge`() {
        val field = TileField(context)
        field.tiles[28] = SwipeTile(TileType.GLADIATOR_STRIKE, 0, 1, TileStage.NO_STAGE)
        field.tiles[29] = SwipeTile(TileType.GLADIATOR_STRIKE, 1, 2, TileStage.NO_STAGE)
        field.tiles[14] = SwipeTile(TileType.GLADIATOR_STRIKE, 2, 3, TileStage.NO_STAGE)

        val events = field.attemptSwipe(1, 0)

        assert(events.size == 2)
        assert(field.tiles[29]!!.stackSize == 3)
        assert(field.tiles[29]!!.id == 1)
    }

    @Test
    fun `Check swipe right non-mergable`() {
        val field = TileField(context)
        field.tiles[28] = SwipeTile(TileType.GLADIATOR_STRIKE, 0, 1, TileStage.NO_STAGE)
        field.tiles[29] = SwipeTile(TileType.KNIGHT_SHIELD, 1, 2, TileStage.NO_STAGE)
        field.tiles[14] = SwipeTile(TileType.GLADIATOR_STRIKE, 2, 3, TileStage.NO_STAGE)

        val events = field.attemptSwipe(1, 0)

        assert(events.size == 1)
    }
}
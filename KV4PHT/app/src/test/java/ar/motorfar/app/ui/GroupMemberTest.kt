package ar.motorfar.app.ui

import ar.motorfar.app.ui.compose.state.GroupMember
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupMemberTest {

    @Test
    fun groupMember_holds_position_data() {
        val member = GroupMember(
            alias       = "LUCAS",
            lat         = -34.6037,
            lon         = -58.3816,
            distanceM   = 1200,
            bearing     = 180f,
            lastSeenMs  = 1000L
        )
        assertEquals("LUCAS", member.alias)
        assertEquals(-34.6037, member.lat, 0.0001)
        assertEquals(-58.3816, member.lon, 0.0001)
    }

    @Test
    fun groupMember_list_can_hold_multiple() {
        val members = listOf(
            GroupMember("A", -34.6, -58.3, 0, 0f, 0L),
            GroupMember("B", -34.7, -58.4, 500, 90f, 0L)
        )
        assertEquals(2, members.size)
    }

    @Test
    fun groupMember_empty_list_is_valid() {
        val members = emptyList<GroupMember>()
        assertEquals(0, members.size)
    }
}

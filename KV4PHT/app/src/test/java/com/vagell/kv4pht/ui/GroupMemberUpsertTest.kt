package com.vagell.kv4pht.ui

import com.vagell.kv4pht.ui.compose.state.GroupMember
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupMemberUpsertTest {

    private fun upsert(list: List<GroupMember>, member: GroupMember): List<GroupMember> =
        list.filter { it.alias != member.alias } + member

    @Test
    fun upsert_new_member_adds_to_list() {
        val result = upsert(emptyList(), GroupMember("LUCAS", -34.6, -58.3, 0, 0f, 1000L))
        assertEquals(1, result.size)
        assertEquals("LUCAS", result[0].alias)
    }

    @Test
    fun upsert_existing_alias_updates_position() {
        val initial = listOf(GroupMember("LUCAS", -34.6, -58.3, 0, 0f, 1000L))
        val updated = upsert(initial, GroupMember("LUCAS", -34.7, -58.4, 500, 90f, 2000L))
        assertEquals(1, updated.size)
        assertEquals(-34.7, updated[0].lat, 0.0001)
        assertEquals(2000L, updated[0].lastSeenMs)
    }

    @Test
    fun upsert_different_aliases_both_kept() {
        val initial = listOf(GroupMember("LUCAS", -34.6, -58.3, 0, 0f, 1000L))
        val result = upsert(initial, GroupMember("PEPE", -34.8, -58.5, 1000, 45f, 1500L))
        assertEquals(2, result.size)
    }

    @Test
    fun upsert_preserves_other_members_on_update() {
        val initial = listOf(
            GroupMember("LUCAS", -34.6, -58.3, 0, 0f, 1000L),
            GroupMember("PEPE", -34.8, -58.5, 1000, 45f, 1500L)
        )
        val result = upsert(initial, GroupMember("LUCAS", -34.9, -58.6, 200, 180f, 3000L))
        assertEquals(2, result.size)
        assertEquals("PEPE", result.first { it.alias == "PEPE" }.alias)
    }
}

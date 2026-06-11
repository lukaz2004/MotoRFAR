package com.vagell.kv4pht.ui

import com.vagell.kv4pht.ui.compose.state.GroupMember
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupMemberDecayTest {

    @Test
    fun member_seen_6min_ago_is_stale() {
        val member = GroupMember("LUCAS", -34.6, -58.3, 0, 0f, lastSeenMs = 0L)
        val nowMs = 6 * 60 * 1000L
        assertTrue(member.isStale(nowMs))
    }

    @Test
    fun member_seen_1min_ago_is_not_stale() {
        val nowMs = 6 * 60 * 1000L
        val member = GroupMember("LUCAS", -34.6, -58.3, 0, 0f, lastSeenMs = nowMs - 60_000L)
        assertFalse(member.isStale(nowMs))
    }

    @Test
    fun member_seen_exactly_at_threshold_is_not_stale() {
        val thresholdMs = 5 * 60 * 1000L
        val nowMs = thresholdMs
        val member = GroupMember("LUCAS", -34.6, -58.3, 0, 0f, lastSeenMs = 0L)
        // exactly at threshold (nowMs - lastSeen == threshold) → not stale
        assertFalse(member.isStale(nowMs, thresholdMs = thresholdMs))
    }
}

package com.amlet.callblocker.data.changelog

data class ChangelogEntry(
    val version: String,
    val date: String,
    val changes: List<ChangeItem>
)

data class ChangeItem(
    val type: ChangeType,
    val descriptionRes: Int
)

enum class ChangeType { NEW, FIX, IMPROVE }

object ChangelogData {

    // Version history — add new entries at the TOP of the list.
    val entries = listOf(
        ChangelogEntry(
            version = "1.7.0",
            date = "2026-04-05",
            changes = listOf(
                ChangeItem(ChangeType.FIX,     com.amlet.callblocker.R.string.changelog_1_7_0_1),
                ChangeItem(ChangeType.FIX,     com.amlet.callblocker.R.string.changelog_1_7_0_2),
                ChangeItem(ChangeType.FIX,     com.amlet.callblocker.R.string.changelog_1_7_0_3),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_7_0_4),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_7_0_5),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_7_0_6),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_7_0_7),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_7_0_8),
            )
        ),
        ChangelogEntry(
            version = "1.6.0",
            date = "2026-04-04",
            changes = listOf(
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_6_0_1),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_6_0_2),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_6_0_3),
                ChangeItem(ChangeType.FIX,     com.amlet.callblocker.R.string.changelog_1_6_0_4),
                ChangeItem(ChangeType.FIX,     com.amlet.callblocker.R.string.changelog_1_6_0_5),
                ChangeItem(ChangeType.IMPROVE, com.amlet.callblocker.R.string.changelog_1_6_0_6),
            )
        ),
        ChangelogEntry(
            version = "1.5.0",
            date = "2026-04-03",
            changes = listOf(
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_5_0_1),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_5_0_2),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_5_0_3),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_5_0_4),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_5_0_5),
                ChangeItem(ChangeType.FIX,     com.amlet.callblocker.R.string.changelog_1_5_0_6),
                ChangeItem(ChangeType.FIX,     com.amlet.callblocker.R.string.changelog_1_5_0_7),
                ChangeItem(ChangeType.IMPROVE, com.amlet.callblocker.R.string.changelog_1_5_0_8),
            )
        ),
        ChangelogEntry(
            version = "1.4.0",
            date = "2026-04-02",
            changes = listOf(
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_4_0_1),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_4_0_2),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_4_0_3),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_4_0_4),
            )
        ),
        ChangelogEntry(
            version = "1.3.0",
            date = "2026-04-02",
            changes = listOf(
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_3_0_1),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_3_0_2),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_3_0_3),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_3_0_4),
                ChangeItem(ChangeType.IMPROVE, com.amlet.callblocker.R.string.changelog_1_3_0_5),
            )
        ),
        ChangelogEntry(
            version = "1.2.0",
            date = "2026-04-01",
            changes = listOf(
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_2_0_1),
                ChangeItem(ChangeType.IMPROVE, com.amlet.callblocker.R.string.changelog_1_2_0_2),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_2_0_3),
                ChangeItem(ChangeType.IMPROVE, com.amlet.callblocker.R.string.changelog_1_2_0_4),
            )
        ),
        ChangelogEntry(
            version = "1.1.0",
            date = "2026-03-31",
            changes = listOf(
                ChangeItem(ChangeType.FIX,     com.amlet.callblocker.R.string.changelog_1_1_0_1),
                ChangeItem(ChangeType.FIX,     com.amlet.callblocker.R.string.changelog_1_1_0_2),
                ChangeItem(ChangeType.IMPROVE, com.amlet.callblocker.R.string.changelog_1_1_0_3),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_1_0_4),
                ChangeItem(ChangeType.NEW,     com.amlet.callblocker.R.string.changelog_1_1_0_5),
            )
        ),
        ChangelogEntry(
            version = "1.0.0",
            date = "2026-03-30",
            changes = listOf(
                ChangeItem(ChangeType.NEW, com.amlet.callblocker.R.string.changelog_1_0_0_1),
            )
        ),
    )
}
package com.madowaku.focusraid.data

import com.madowaku.focusraid.core.model.WorldSnapshot

interface WorldRepository {
    fun snapshot(): WorldSnapshot
}

class FakeWorldRepository : WorldRepository {
    override fun snapshot(): WorldSnapshot = WorldSnapshot()
}

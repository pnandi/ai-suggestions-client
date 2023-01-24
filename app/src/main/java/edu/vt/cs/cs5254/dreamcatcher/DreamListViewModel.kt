package edu.vt.cs.cs5254.dreamcatcher

import androidx.lifecycle.ViewModel

class DreamListViewModel: ViewModel() {

    private val dreamRepository = DreamRepository.get()
    val dreamListLiveData = dreamRepository.getDreams()

    fun addDream(dreamWithEntries: DreamWithEntries) {
        dreamWithEntries.dreamEntries += DreamEntry(kind = DreamEntryKind.CONCEIVED, dreamId = dreamWithEntries.dream.id)
        dreamRepository.addDreamWithEntries(dreamWithEntries)
    }

    fun deleteAllDreams() {
        dreamRepository.deleteAllDreamsInDatabase()
    }
}
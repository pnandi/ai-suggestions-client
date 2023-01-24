package edu.vt.cs.cs5254.dreamcatcher

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import java.io.File
import java.util.*

class DreamDetailViewModel : ViewModel() {


    private val dreamRepository = DreamRepository.get()
    private val crimeIdLiveData = MutableLiveData<UUID>()

    var dreamWithEntriesLiveData: LiveData<DreamWithEntries> =
        Transformations.switchMap(crimeIdLiveData) { dreamId ->
            dreamRepository.getDreamWithEntries(dreamId)
        }

    fun loadDreamWithEntries(dreamId: UUID) {
        crimeIdLiveData.value = dreamId
    }

    fun saveDreamWithEntries(dreamWithEntries: DreamWithEntries) {
        dreamRepository.updateDreamWithEntries(dreamWithEntries)

    }
    fun getPhotoFile(dreamWithEntries: DreamWithEntries): File {
        return dreamRepository.getPhotoFile(dreamWithEntries.dream)
    }

}
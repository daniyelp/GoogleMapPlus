package com.example.customgooglemapexample

import androidx.lifecycle.*
import com.example.customgooglemapexample.util.CombinedLiveData
import com.example.customgooglemapexample.util.LocationTracker
import com.example.customgooglemapexample.util.Resource
import com.example.customgooglemapexample.util.Status
import com.example.gpsbroadcastreceiver.GpsBroadcastReceiver
import com.example.internetbroadcastreceiver.InternetBroadcastReceiver
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MyViewModel @Inject constructor (
    private val locationTracker: LocationTracker,
    private val gpsBroadcastReceiver: GpsBroadcastReceiver,
    private val internetBroadcastReceiver: InternetBroadcastReceiver
    ): ViewModel() {

    val lastLocation : LiveData<LatLng> = locationTracker.lastLocation
    val gpsEnabled : LiveData<Boolean> = gpsBroadcastReceiver.gpsEnabled
    val internetEnabled: LiveData<Boolean> = internetBroadcastReceiver.internetEnabled

    private val _markers = MutableLiveData(Resource<LatLng>(listOf(), Status.INITIALIZED))
    val markers : LiveData<Resource<LatLng>> = _markers

    private val _paths = MutableLiveData(Resource<List<LatLng>>(listOf(), Status.INITIALIZED))
    val paths : LiveData<Resource<List<LatLng>>> = _paths

    private val startingIndexForPath = MutableLiveData(0)

    val connectEnabled : LiveData<Boolean> = CombinedLiveData(_markers, startingIndexForPath) {
        markers, startingIndexForPath ->
            markers?.let { markers ->
                startingIndexForPath?.let { startingIndexForPath ->
                    startingIndexForPath <= markers.list.size - 2
                }
            }
    }

    val undoMarkerEnabled : LiveData<Boolean> = Transformations.map(_markers) {
        it?.let {
            it.list.isNotEmpty()
        }
    }

    val undoPathEnabled : LiveData<Boolean> = Transformations.map(_paths) {
        it?.let {
            it.list.isNotEmpty()
        }
    }

    init {
        locationTracker.startLocationUpdates()
        gpsBroadcastReceiver.start()
        internetBroadcastReceiver.start()
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopLocationUpdates()
        gpsBroadcastReceiver.stop()
        internetBroadcastReceiver.stop()
    }

    fun onNewMarker(latLng: LatLng) {
        val list = _markers.value!!.list.toMutableList()
        list.add(latLng)
        _markers.value = Resource(list, Status.ADDED_ELEMENT)
    }

    fun undoAllMarkers() {
        _markers.value = Resource(listOf(), Status.RESETED)
    }

    fun undoAllPaths() {
        _paths.value = Resource(listOf(), Status.RESETED)
    }

    fun reset() {
        undoAllMarkers()
        undoAllPaths()
        startingIndexForPath.value = 0
    }

    fun connect(animate: Boolean = false) {
        val markers = _markers.value!!.list
        val path = markers.subList(startingIndexForPath.value!!, markers.size)
        if(path.size < 2) {
            return
        }
        startingIndexForPath.value = markers.size
        addPath(path, animate)
    }

    fun undoMarker() {
        val list = _markers.value!!.list.toMutableList()
        if(list.removeLastOrNull() != null) {
            _markers.value = Resource(list, Status.REMOVED_LAST_ELEMENT)
        }
    }

    fun undoPath() {
        val list = _paths.value!!.list.toMutableList()
        list.removeLastOrNull()?.let {
            startingIndexForPath.value = startingIndexForPath.value!!.minus(it.size)
            _paths.value = Resource(list, Status.REMOVED_LAST_ELEMENT)
        }
    }

    private fun addPath(path: List<LatLng>, animate: Boolean = false) {
        val list = _paths.value!!.list.toMutableList()
        list.add(path)
        _paths.value = Resource(list, Status.ADDED_ELEMENT, animate = animate)
    }

    private fun addPaths(paths: List<List<LatLng>>, animate: Boolean = false) {
        val list = _paths.value!!.list.toMutableList()
        list.addAll(paths)
        _paths.value = Resource(list, Status.ADDED_SEVERAL_ELEMENTS, paths.size, animate = animate)
    }
}
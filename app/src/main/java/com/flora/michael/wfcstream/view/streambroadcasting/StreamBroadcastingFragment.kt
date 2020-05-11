package com.flora.michael.wfcstream.view.streambroadcasting

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.flashphoner.fpwcsapi.Flashphoner
import com.flashphoner.fpwcsapi.bean.Connection
import com.flashphoner.fpwcsapi.bean.Data
import com.flashphoner.fpwcsapi.bean.StreamStatus
import com.flashphoner.fpwcsapi.constraints.AudioConstraints
import com.flashphoner.fpwcsapi.constraints.Constraints
import com.flashphoner.fpwcsapi.constraints.VideoConstraints
import com.flashphoner.fpwcsapi.session.*
import com.flora.michael.wfcstream.R
import com.flora.michael.wfcstream.view.LoadableContentFragment
import com.flora.michael.wfcstream.viewmodel.streamBroadcasting.StreamBroadcastingViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

class StreamBroadcastingFragment: LoadableContentFragment(R.layout.stream_broadcasting_fragment) {
    private val viewModel by viewModels<StreamBroadcastingViewModel>()
    private val logTag = this::class.java.simpleName

    private val onBroadcastStatus: (Stream, StreamStatus) -> Unit = { broadcast, broadcastStatus ->
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(context, broadcastStatus.name, Toast.LENGTH_LONG).show()
            if (broadcastStatus == StreamStatus.PUBLISHING) {
                viewModel.notifyViewersAboutBroadcastState(isOnline = true)
            } else if (broadcastStatus == StreamStatus.UNPUBLISHED) {
                viewModel.notifyViewersAboutBroadcastState(isOnline = false)
            }
        }
    }

    private var videoRenderer: SurfaceViewRenderer? = null
    private var startStopBroadcastingButton: MaterialButton? = null

    private var webCallServerSession: Session? = null
    private var webCallServerBroadcast: Stream? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findAllViews()
        initializeAllViews()
        viewModel.loadDataFromServer()
    }

    override fun onResume() {
        super.onResume()

        try{
            videoRenderer?.init(Flashphoner.context, null)
        } catch(ex: IllegalStateException){
            ex.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        videoRenderer?.release()
    }

    override fun onDestroy() {
        runBlocking{
            try{
                webCallServerBroadcast?.stop()
                disconnectFromWebCallServer()
            } catch(ex: Exception){
                ex.printStackTrace()
            }
        }

        super.onDestroy()
    }

    private fun findAllViews(){
        view?.apply{
            videoRenderer = findViewById(R.id.stream_broadcasting_fragment_video_renderer)
            startStopBroadcastingButton = findViewById(R.id.stream_broadcasting_fragment_start_stop_broadcasting_button)
        }
    }

    private fun initializeAllViews(){
        initializeContentLoadingObservation()
        initializeVideoRenderer()
        initializeIsStreamOnlineObservation()
        startStopBroadcastingButton?.isEnabled = false
    }

    private fun initializeContentLoadingObservation(){
        viewModel.isContentLoading.observe(viewLifecycleOwner, Observer { isContentLoading ->
            when{
                isContentLoading -> showLoadingProgressBar(withHiddenContent = true)
                viewModel.isBroadcastInformationLoaded() -> {
                    hideLoadingProgressBar()
                    connectToWebCallServer()
                }
                else -> hideLoadingProgressBar(withError = true)
            }
        })
    }

    private fun initializeVideoRenderer(){
        videoRenderer?.apply{
            setZOrderMediaOverlay(true)
            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            setMirror(true)
            requestLayout()
        }
    }

    private fun connectToWebCallServer(){
        webCallServerSession = createWebCallServerSession()
        webCallServerSession?.connect(Connection())
    }

    private fun disconnectFromWebCallServer(){
        webCallServerSession?.disconnect()
    }

    private fun createWebCallServerSession(): Session{
        val sessionOptions = SessionOptions(getString(R.string.web_call_server_url)).apply{
            localRenderer = videoRenderer
        }

        val session = Flashphoner.createSession(sessionOptions)

        session.on(object: SessionEvent{
            override fun onAppData(data: Data?) {

            }

            override fun onDisconnection(connection: Connection?) {
                lifecycleScope.launch(Dispatchers.Main){
                    startStopBroadcastingButton?.isEnabled = false
                }

                if(viewModel.isBroadcastOnline.value == true){
                    viewModel.notifyViewersAboutBroadcastState(isOnline = false)
                }
            }

            override fun onConnected(connection: Connection?) {
                lifecycleScope.launch(Dispatchers.Main){
                    Log.i(logTag, "Connected to web call server")
                    webCallServerBroadcast = createWebCallServerBroadcast(webCallServerSession)
                    startStopBroadcastingButton?.isEnabled = true
                }
            }

            override fun onRegistered(connection: Connection?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        })

        return session
    }

    private fun createWebCallServerBroadcast(webCallServerSession: Session?): Stream?{
        var broadcast: Stream? = null

        viewModel.userId.value?.let{ broadcastId ->
            val streamOptions = StreamOptions(broadcastId.toString())

            streamOptions.constraints = Constraints(
                AudioConstraints(),
                VideoConstraints().apply {
                    videoFps = 60
                    setResolution(720, 1280)
                }
            )

            broadcast = webCallServerSession?.createStream(streamOptions)

            broadcast?.on(onBroadcastStatus)
        }

        return broadcast
    }

    private fun initializeIsStreamOnlineObservation(){
        viewModel.isBroadcastOnline.observe(viewLifecycleOwner, Observer{ isStreamOnline: Boolean? ->
            if(isStreamOnline == true){
                startStopBroadcastingButton?.text = context?.getString(R.string.stream_broadcasting_fragment_stop_broadcast_button)
                startStopBroadcastingButton?.setOnClickListener {
                    try{
                        webCallServerBroadcast?.stop()
                    } catch (ex: Exception){
                        ex.printStackTrace()
                    }

                }
            } else {
                startStopBroadcastingButton?.text = context?.getString(R.string.stream_broadcasting_fragment_start_broadcast_button)
                startStopBroadcastingButton?.setOnClickListener {
                    webCallServerBroadcast?.publish()
                }
            }
        })
    }
}
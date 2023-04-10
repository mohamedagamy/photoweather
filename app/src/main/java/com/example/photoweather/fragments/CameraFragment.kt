package com.example.photoweather.fragments

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.window.WindowManager
import com.android.example.cameraxbasic.utils.ANIMATION_FAST_MILLIS
import com.android.example.cameraxbasic.utils.ANIMATION_SLOW_MILLIS
import com.android.example.cameraxbasic.utils.MediaStoreUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.data.model.WeatherData
import com.example.photoweather.MainActivity
import com.example.photoweather.databinding.FragmentCameraBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import com.example.photoweather.R
import com.example.photoweather.ui.Loading
import com.example.photoweather.ui.WeatherUiStateError
import com.example.photoweather.ui.WeatherUiStateReady
import com.example.photoweather.ui.WeatherViewModel
import com.example.photoweather.utils.PermissionUtil
import com.example.photoweather.utils.showToast
import com.squareup.picasso.Picasso

class CameraFragment : Fragment() {
    private lateinit var binding: FragmentCameraBinding
    private lateinit var mediaStoreUtils: MediaStoreUtils

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var windowManager: WindowManager
    private lateinit var cameraExecutor: ExecutorService

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Shut down our background executor
        cameraExecutor.shutdown()

    }
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun setGalleryThumbnail(filename: String) {
        // Run the operations in the view's thread
        binding.thumbnail.let { thumbnail ->
            thumbnail.post {
                // Load thumbnail into circular button using Glide
                Glide.with(thumbnail)
                    .load(filename)
                    .apply(RequestOptions.circleCropTransform())
                    .into(thumbnail)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        // Initialize WindowManager to retrieve display metrics
        windowManager = WindowManager(view.context)
        // Initialize MediaStoreUtils for fetching this app's images
        mediaStoreUtils = MediaStoreUtils(requireContext())
        // Wait for the views to be properly laid out
        binding.viewFinder.post {
            // Build UI controls
            updateCameraUi()
            // Set up the camera and its use cases
            lifecycleScope.launch {
                setUpCamera()
            }
        }
        initializeObservers()
    }
    private suspend fun setUpCamera() {
        cameraProvider = ProcessCameraProvider.getInstance(requireContext()).get()
        // Build and bind the camera use cases
        bindCameraUseCases()
    }

   private fun bindCameraUseCases() {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = windowManager.getCurrentWindowMetrics().bounds
        val screenAspectRatio = aspectRatio(metrics.width(), metrics.height())
        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        // Preview
        preview = Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()

        // Must unbind the use-cases before rebinding them
        cameraProvider?.unbindAll()

        if (camera != null) {
            // Must remove observers from the previous camera instance
            removeCameraStateObservers(camera!!.cameraInfo)
        }

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)
            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            observeCameraState(camera?.cameraInfo!!)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun removeCameraStateObservers(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.removeObservers(viewLifecycleOwner)
    }

    private fun observeCameraState(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.observe(viewLifecycleOwner) { cameraState ->
            cameraState.error?.let { error ->
                Toast.makeText(context, "Something went wrong. ${error.cause.toString()}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
    private fun updateCameraUi() {
        // Remove previous UI if any
        binding.root?.let {
            binding.root.removeView(it)
        }

        // In the background, load latest photo taken (if any) for gallery thumbnail
        lifecycleScope.launch {
            val thumbnailUri = mediaStoreUtils.getLatestImageFilename()
            thumbnailUri?.let {
                setGalleryThumbnail(it)
            }
        }
        // Listener for button used to capture photo
        binding.fabCamera.setOnClickListener {

            // Get a stable reference of the modifiable image capture use case
            imageCapture?.let { imageCapture ->

                // Create time stamped name and MediaStore entry.
                val name = SimpleDateFormat(FILENAME, Locale.US)
                    .format(System.currentTimeMillis())
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, PHOTO_TYPE)
                    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        val appName = requireContext().resources.getString(R.string.app_name)
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${appName}")
                    }
                }

                // Create output options object which contains file + metadata
                val outputOptions = ImageCapture.OutputFileOptions
                    .Builder(requireContext().contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues)
                    .build()

                // Setup image capture listener which is triggered after photo has been taken
                imageCapture.takePicture(
                        outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri
                        Log.d(TAG, "Photo capture succeeded: $savedUri")

                        // We can only change the foreground Drawable using API level 23+ API
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // Update the gallery thumbnail with latest picture taken
                            setGalleryThumbnail(savedUri.toString())
                        }

                        // Implicit broadcasts will be ignored for devices running API level >= 24
                        // so if you only target API level 24+ you can remove this statement
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                            // Suppress deprecated Camera usage needed for API level 23 and below
                            @Suppress("DEPRECATION")
                            requireActivity().sendBroadcast(
                                    Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri)
                            )
                        }
                    }
                })

                // We can only change the foreground Drawable using API level 23+ API
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    // Display flash animation to indicate that photo was captured
                    binding.root.postDelayed({
                        binding.root.foreground = ColorDrawable(Color.WHITE)
                        binding.root.postDelayed(
                                { binding.root.foreground = null }, ANIMATION_FAST_MILLIS)
                    }, ANIMATION_SLOW_MILLIS)
                }
            }
        }
        // Listener for button used to view the most recent photo
        binding.thumbnail.setOnClickListener {
            // Only navigate when the gallery has photos
            lifecycleScope.launch {
                if (mediaStoreUtils.getImages().isNotEmpty()) {
                    //val bundle = bundleOf(PhotoFragment.MEDIA_DATA to mediaStoreUtils.getImages().get(0).file.absolutePath)
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .navigate(CameraFragmentDirections.actionCameraToGallery(mediaStoreUtils.mediaStoreCollection.toString()))
                }
            }
        }
    }

    private fun initializeObservers() {
        lifecycleScope.launch {
            (activity as MainActivity).weatherViewModel.state.collect { state ->
                when (state) {
                    is Loading -> context?.showToast("Loading weather...")
                    is WeatherUiStateReady -> state.weather?.let { showWeather(it) }
                    is WeatherUiStateError -> state.error?.let { context?.showToast(it) }
                }
            }
        }
    }
    @SuppressLint("SetTextI18n")
    private fun showWeather(it: WeatherData) {
        Log.e("",it.toString())
        val bannerBinding = binding.banner
        bannerBinding.textviewLocation.text = it.location?.country.toString()
        bannerBinding.textviewCtemperature.text = "Temp:${it.current?.temp_c.toString()}C"
        bannerBinding.textviewWindDir.text = "Wind:${it.current?.wind_dir.toString()}"
        bannerBinding.textviewHumdity.text = "Humidity:${it.current?.humidity.toString()}"
        bannerBinding.textviewCondition.text = "${it.current?.condition?.text}"
        //Glide.with(this).load(it.current?.condition?.icon).into(bannerBinding.imageviewCondition)
    }

    companion object {
        private const val TAG = "CameraFragment"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_TYPE = "image/jpeg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}

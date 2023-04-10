package com.example.photoweather.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.example.cameraxbasic.utils.MediaStoreFile
import com.bumptech.glide.Glide
import com.example.data.model.WeatherData
import com.example.photoweather.MainActivity
import com.example.photoweather.R
import com.example.photoweather.databinding.FragmentPhotoBinding
import com.example.photoweather.ui.Loading
import com.example.photoweather.ui.WeatherUiStateError
import com.example.photoweather.ui.WeatherUiStateReady
import com.example.photoweather.utils.bitmapToFile
import com.example.photoweather.utils.getImageUri
import com.example.photoweather.utils.showToast
import kotlinx.coroutines.launch

class PhotoFragment internal constructor() : Fragment() {
    private lateinit var binding: FragmentPhotoBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = arguments ?: return
        val mediaStore =  args.getString(MEDIA_DATA)
        Glide.with(this).load(mediaStore).into(binding.fullScreenPhoto)
        binding.fabShare.setOnClickListener {
            val screenshot = binding.imageScreenshot
            val image = takeScreenshot(screenshot,screenshot.width,screenshot.height)
            shareImage(context?.getImageUri(image))
        }

        initializeObservers()
    }

    fun shareImage(bitmapUri: Uri?) {
        bitmapUri.let {
                // Create a sharing intent
                val intent = Intent().apply {
                    type = "image/*"
                    action = Intent.ACTION_SEND
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    putExtra(Intent.EXTRA_STREAM,bitmapUri)
                }
                // Launch the intent letting the user choose which app to share with
                startActivity(Intent.createChooser(intent, getString(R.string.share_hint)))
            }
    }

    fun takeScreenshot(view: View, height: Int, width: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return bitmap
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
        bannerBinding.textviewCtemperature.text = "Temp:${it.current?.temp_c.toString()}C ${it.current?.temp_f.toString()}F"
        bannerBinding.textviewWindDir.text = "Wind:${it.current?.wind_dir.toString()}"
        bannerBinding.textviewHumdity.text = "Humidity:${it.current?.humidity.toString()}"
        bannerBinding.textviewCondition.text = "${it.current?.condition?.text}"
        //Glide.with(this).load(it.current?.condition?.icon).into(bannerBinding.imageviewCondition)
    }

    companion object {
        const val MEDIA_DATA = "media_data"

        fun create(mediaStoreFile: MediaStoreFile) = PhotoFragment().apply {
            val image = mediaStoreFile.file
            arguments = Bundle().apply {
                putString(MEDIA_DATA, image.absolutePath)
            }
        }
    }
}
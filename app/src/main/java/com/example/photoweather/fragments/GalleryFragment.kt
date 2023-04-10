package com.example.photoweather.fragments

import GalleryAdapter
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.android.example.cameraxbasic.utils.MediaStoreFile
import com.android.example.cameraxbasic.utils.MediaStoreUtils
import com.example.photoweather.databinding.FragmentGalleryBinding
import kotlinx.coroutines.launch
import com.example.photoweather.R
import com.example.photoweather.utils.MediaStoreMapper

class GalleryFragment internal constructor() : Fragment() {
    private lateinit var binding: FragmentGalleryBinding
    private var mediaList: MutableList<MediaStoreFile> = mutableListOf()
    lateinit var galleryAdapter: GalleryAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Get images this app has access to from MediaStore
            galleryAdapter = GalleryAdapter()
            mediaList = MediaStoreUtils(requireContext()).getImages()
            binding.photoGalleryRecycler.adapter = galleryAdapter
            galleryAdapter.onClickListener = { pos ->
                showPopupFragment(mediaList.get(pos))
            }
            galleryAdapter.submitList(mediaList)
        }
    }

    private fun showPopupFragment(media: MediaStoreFile) {
        val bundle = bundleOf(PhotoFragment.MEDIA_DATA to media.file.absolutePath)
        Navigation.findNavController(requireActivity(), R.id.nav_host_fragment).navigate(R.id.photo_fragment,bundle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Handle share button press
    }
}

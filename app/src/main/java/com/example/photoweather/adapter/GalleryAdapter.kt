import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.ListAdapter
import com.android.example.cameraxbasic.utils.MediaStoreFile
import com.bumptech.glide.Glide
import com.example.photoweather.R

class GalleryAdapter : ListAdapter<MediaStoreFile, GalleryAdapter.ItemViewholder>(DiffCallback())  {
    lateinit var onClickListener:((pos:Int)-> Unit)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewholder {
        return ItemViewholder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gallery, parent, false)
        )
    }

    override fun onBindViewHolder(holder: GalleryAdapter.ItemViewholder, position: Int) {
        holder.bind(getItem(position),onClickListener)
    }

    class ItemViewholder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var galleryPic:ImageView = itemView.findViewById(R.id.gallery_pic)
        fun bind(item: MediaStoreFile, onClickListener: (pos: Int) -> Unit) = with(itemView) {
            Glide.with(this).load(item.file.absolutePath).into(galleryPic)
            setOnClickListener {
                onClickListener.invoke(adapterPosition)
            }
        }
    }
}

class DiffCallback : DiffUtil.ItemCallback<MediaStoreFile>() {
    override fun areItemsTheSame(oldItem: MediaStoreFile, newItem: MediaStoreFile): Boolean {
        return oldItem.file == newItem.file
    }

    override fun areContentsTheSame(oldItem: MediaStoreFile, newItem: MediaStoreFile): Boolean {
        return oldItem.file.absolutePath == newItem.file.absolutePath
    }
}
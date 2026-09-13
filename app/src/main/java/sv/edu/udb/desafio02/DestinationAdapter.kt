package sv.edu.udb.desafio02

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import sv.edu.udb.desafio02.databinding.ItemDestinationBinding
import sv.edu.udb.desafio02.model.Destination
import java.io.File

class DestinationAdapter(
    private val onItemClick: (Destination) -> Unit
): ListAdapter<Destination, DestinationAdapter.DestinationViewHolder>(DiffCallback) {
    // ViewHolder - Caches the view for performance
    inner class DestinationViewHolder(val binding: ItemDestinationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(destination: Destination) {
            // Sets the data for the view
            binding.tvName.text = destination.name
            binding.tvCountry.text = destination.country
            binding.tvPrice.text = binding.root.context.getString(
                R.string.price_format, destination.price
            )
            binding.tvDescription.text = destination.description
            // Loads the image
            Glide.with(binding.root)
                .load(File(destination.imagePath))
                .placeholder(R.drawable.ic_photo)
                .centerCrop()
                .error(R.drawable.ic_photo)
                .into(binding.ivDestImage)

            binding.root.setOnClickListener { onItemClick(destination) }
        }
        }

    // Creates new views
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DestinationViewHolder {
        val binding = ItemDestinationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DestinationViewHolder(binding)
    }

    // Called every time an item needs to be displayed
    override fun onBindViewHolder(
        holder: DestinationViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }

    // Only updates the items that have changed
    private object DiffCallback : DiffUtil.ItemCallback<Destination>() {
        override fun areItemsTheSame(oldItem: Destination, newItem: Destination) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Destination, newItem: Destination) =
            oldItem == newItem
    }

}
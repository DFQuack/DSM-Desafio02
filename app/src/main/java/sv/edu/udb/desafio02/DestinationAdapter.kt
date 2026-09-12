package sv.edu.udb.desafio02

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getString
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import sv.edu.udb.desafio02.R.string
import sv.edu.udb.desafio02.databinding.ItemDestinationBinding
import sv.edu.udb.desafio02.model.Destination

class DestinationAdapter(
    private val items: MutableList<Destination> = mutableListOf(),
    private val onClick: (Destination) -> Unit
): ListAdapter<Destination, DestinationAdapter.DestinationViewHolder>(DiffCallback) {
    // ViewHolder - Caches the view for performance
    class DestinationViewHolder(val binding: ItemDestinationBinding) :
        RecyclerView.ViewHolder(binding.root)

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
        val item = items[position]
        val context = holder.itemView.context
        val binding = holder.binding
        // Sets the data for the view
        binding.tvName.text = item.name
        binding.tvCountry.text = item.country
        binding.tvPrice.text = getString(context, string.price_format).format(item.price)
        // Loads the image
        Glide.with(context)
            .load(item.imageUrl)
            .placeholder(R.drawable.ic_photo)
            .centerCrop()
            .into(holder.binding.ivDestImage)
        // Attaches the click listener
        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    // Only updates the items that have changed
    private object DiffCallback : DiffUtil.ItemCallback<Destination>() {
        override fun areItemsTheSame(oldItem: Destination, newItem: Destination) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Destination, newItem: Destination) =
            oldItem == newItem
    }

}
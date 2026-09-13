package sv.edu.udb.desafio02

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import sv.edu.udb.desafio02.databinding.ActivityMainBinding
import sv.edu.udb.desafio02.model.Destination
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: DestinationAdapter
    private val db = FirebaseDatabase.getInstance().getReference("destinations")

    // Listener for Firebase data changes
    private val dataListener = object : ValueEventListener {
        // snapshot represents de /destinations node
        override fun onDataChange(snapshot: DataSnapshot) {
            // Convert the data to a list of Destination objects
            val destinations = snapshot.children.mapNotNull { child ->
                child.getValue(Destination::class.java)?.apply { id = child.key ?: "" }
            }
            // ListAdapter method to only update the changed rows in the RecyclerView
            adapter.submitList(destinations)
        }

        override fun onCancelled(error: DatabaseError) {
            Snackbar.make(
                binding.main,
                getString(R.string.error_loading),
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()
        binding.btnAdd.setOnClickListener {
            startActivity(Intent(this, EditDestinationActivity::class.java))
        }
    }

    // The listener is only active when the screen is visible
    override fun onStart() {
        super.onStart()
        db.addValueEventListener(dataListener)
    }
    override fun onStop() {
        super.onStop()
        db.removeEventListener(dataListener)
    }

    private fun setupRecyclerView() {
        // Tapping a card opens the EditDestinationActivity with the destination ID attached
        adapter = DestinationAdapter { destination ->
            val intent = Intent(this, EditDestinationActivity::class.java)
            intent.putExtra(EditDestinationActivity.DESTINATION_ID, destination.id)
            startActivity(intent)
        }
        binding.destinationList.layoutManager = LinearLayoutManager(this)
        binding.destinationList.adapter = adapter
        // Skips some layout recalculations because its size doesn't depend on the contents
        binding.destinationList.setHasFixedSize(true)

        ItemTouchHelper(SwipeToDeleteCallback()).attachToRecyclerView(binding.destinationList)
    }

    private fun confirmAndDelete(position: Int, destination: Destination) {
        // Cancelling or dismissing the dialog resets its position
        AlertDialog.Builder(this)
            .setTitle(R.string.confirm_delete_title)
            .setMessage(getString(R.string.confirm_delete_message, destination.name))
            .setPositiveButton(R.string.action_delete) { _, _ -> deleteDestination(destination) }
            .setNegativeButton(R.string.action_cancel) { _, _ -> adapter.notifyItemChanged(position) }
            .setOnCancelListener { adapter.notifyItemChanged(position) }
            .show()
    }

    private fun deleteDestination(destination: Destination) {
        // Deletes the destination from Firebase
        db.child(destination.id).removeValue()
            .addOnFailureListener {
                Snackbar.make(
                    binding.main,
                    getString(R.string.error_deleting),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        // Deletes the file locally
        if (destination.imagePath.isNotEmpty()) {
            File(destination.imagePath).delete()
        }
    }

    // Inner class allows access to the outer class members
    private inner class SwipeToDeleteCallback : ItemTouchHelper.SimpleCallback(
        // 0 disables dragging to reorder
        0, ItemTouchHelper.LEFT
    ) {
        private val background = ContextCompat.getColor(this@MainActivity, R.color.delete_background).toDrawable()
        private val icon: Drawable? = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_delete)
            ?.apply { setTint(ContextCompat.getColor(this@MainActivity, R.color.on_primary)) }

        // No drag-and-drop reordering, only swipe
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        // Fires once the swipe gesture is completed
        override fun onSwiped(
            viewHolder: RecyclerView.ViewHolder,
            direction: Int
        ) {
            val position = viewHolder.bindingAdapterPosition
            // Exits if the row was already removed by a different update
            if (position == RecyclerView.NO_POSITION) return
            confirmAndDelete(position, adapter.currentList[position])
        }

        // Handles the drawing of the background and icon while swiping
        override fun onChildDraw(
            c: android.graphics.Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            // Moves the row according to the drag distance
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            val itemView = viewHolder.itemView
            // Draws the rectangle under the row
            background.setBounds(
                itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom
            )
            background.draw(c)

            icon?.let {
                val margin = (itemView.height - it.intrinsicHeight) / 2
                val iconTop = itemView.top + margin
                val iconRight = itemView.right - margin
                it.setBounds(iconRight - it.intrinsicWidth, iconTop, iconRight, iconTop + it.intrinsicHeight)
                it.draw(c)
            }
        }

    }
}
package sv.edu.udb.desafio02

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import sv.edu.udb.desafio02.databinding.ActivityMainBinding
import sv.edu.udb.desafio02.model.Destination

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
    }
}
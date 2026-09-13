package sv.edu.udb.desafio02

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scale
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.FirebaseDatabase
import sv.edu.udb.desafio02.databinding.ActivityEditDestinationBinding
import sv.edu.udb.desafio02.model.Destination
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/*
* Single screen for both creating and editing a Destination.
*/
class EditDestinationActivity : AppCompatActivity() {
    companion object {
        const val DESTINATION_ID = "destination_id"
        private const val MAX_IMAGE_DIMENSION_PX = 600
        private const val JPEG_QUALITY = 80
    }
    private lateinit var binding: ActivityEditDestinationBinding
    private val db = FirebaseDatabase.getInstance().getReference("destinations")
    private var destinationId: String? = null
    private var imagePath: String? = null

    // Image picker
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { loadImageIntoPreview(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditDestinationBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupCountrySpinner()

        // If an Id was passed, the screen enters "update" mode
        destinationId = intent.getStringExtra(DESTINATION_ID)
        if (destinationId != null) {
            binding.tvTitle.text = getString(R.string.title_update_dest)
            loadExistingDestination(destinationId!!)
        }

        binding.btnPickImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.btnSave.setOnClickListener { saveDestination() }
    }

    // Adapter to fill the spinner with data
    private fun setupCountrySpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.country_list,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCountry.adapter = adapter
    }

    // Retrieves the tapped destination and stores it as an object
    private fun loadExistingDestination(id: String) {
        db.child(id).get()
            .addOnSuccessListener { snapshot ->
                val destination = snapshot.getValue(Destination::class.java)
                    ?: return@addOnSuccessListener
                populateForm(destination)
            }
            .addOnFailureListener {
                Snackbar.make(
                    binding.main,
                    getString(R.string.error_loading),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
    }

    // Fills the form with the original destination values
    private fun populateForm(destination: Destination) {
        binding.tbName.setText(destination.name)
        binding.tbPrice.setText(destination.price.toString())
        binding.tbDescription.setText(destination.description)

        // Sets the selected item in the spinner (leaves blank if not found)
        @Suppress("UNCHECKED_CAST")
        val countryAdapter = binding.spinnerCountry.adapter as ArrayAdapter<String>
        val position = countryAdapter.getPosition(destination.country)
        if (position >= 0) binding.spinnerCountry.setSelection(position)

        // Loads the image into the preview
        imagePath = destination.imagePath
        imagePath?.let { path ->
            Glide.with(this)
                .load(File(path))
                .placeholder(R.drawable.ic_photo)
                .centerCrop()
                .error(R.drawable.ic_photo)
                .into(binding.ivPreview)
        }
    }

    // Called when the user picks an image
    private fun loadImageIntoPreview(uri: Uri) {
        // .use ensures the file is closed after the block, even in the case of an error
        contentResolver.openInputStream(uri)?.use { stream ->
            // If decoding fails, it returns null and ends the function early
            val original = BitmapFactory.decodeStream(stream) ?: return
            val scaled = scaleDown(original, MAX_IMAGE_DIMENSION_PX)
            val file = saveToInternalStorage(scaled)
            // Updates the path to the image so the preview shows immediately
            imagePath = file.absolutePath
            Glide.with(this)
                .load(file)
                .placeholder(R.drawable.ic_photo)
                .centerCrop()
                .error(R.drawable.ic_photo)
                .into(binding.ivPreview)
        }
    }

    private fun scaleDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        // If the largest dimension is already under the limit, it returns the bitmap as is
        val largestSide = maxOf(bitmap.width, bitmap.height)
        if (largestSide <= maxDimension) return bitmap
        // Otherwise, the bitmap is scaled to fit the limit
        val ratio = maxDimension.toFloat() / largestSide
        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()
        return bitmap.scale(width, height)
    }

    private fun saveToInternalStorage(bitmap: Bitmap): File {
        // mkdirs creates the directory if it doesn't exist
        val imagesDir = File(filesDir, "destination_images").apply { mkdirs() }
        val file = File(imagesDir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        return file
    }

    private fun saveDestination() {
        val name = binding.tbName.text.toString().trim()
        val priceText = binding.tbPrice.text.toString().trim()
        val description = binding.tbDescription.text.toString().trim()
        val country = binding.spinnerCountry.selectedItem?.toString().orEmpty()

        // Input validation
        if (name.isEmpty() || priceText.isEmpty()) {
            Snackbar.make(
                binding.main,
                getString(R.string.error_required_fields),
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }
        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0) {
            Snackbar.make(
                binding.main,
                getString(R.string.error_invalid_price),
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        // Image validation
        val currentImagePath = imagePath
        if (currentImagePath.isNullOrEmpty()) {
            Snackbar.make(
                binding.main,
                getString(R.string.error_image_required),
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        val destination = Destination(
            name = name,
            country = country,
            price = price,
            description = description,
            imagePath = currentImagePath
        )
        // If id is null (new destination), it creates a new one (push)
        val target = destinationId?.let { db.child(it) } ?: db.push()

        // Disables the button to prevent multiple taps
        binding.btnSave.isEnabled = false

        // Attempts to save the destination
        target.setValue(destination)
            .addOnSuccessListener {
                finish()
            }
            .addOnFailureListener {
                binding.btnSave.isEnabled = true
                Snackbar.make(
                    binding.main,
                    getString(R.string.error_saving),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
    }
}
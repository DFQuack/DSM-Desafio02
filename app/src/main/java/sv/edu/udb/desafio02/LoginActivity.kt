package sv.edu.udb.desafio02

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import sv.edu.udb.desafio02.databinding.ActivityLoginBinding

// Screen for registering and signing in
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()
    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        applyModeToUi()
        binding.btnToggleAction.setOnClickListener {
            isSignUpMode = !isSignUpMode
            applyModeToUi()
        }

        binding.btnPrimaryAction.setOnClickListener { submit() }
    }

    override fun onStart() {
        super.onStart()
        // If the user is signed in already, skip the login screen
        if (auth.currentUser != null) {
            goToMain()
        }
    }

    // Switches the text of the buttons depending on the mode
    private fun applyModeToUi() {
        binding.btnPrimaryAction.text = getString(
            if (isSignUpMode) R.string.action_create_account
            else R.string.action_sign_in
        )
        binding.btnToggleAction.text = getString(
            if (isSignUpMode) R.string.btn_to_signin
            else R.string.btn_to_register
        )

    }

    private fun submit() {
        val email = binding.tbEmail.text.toString().trim()
        val password = binding.tbPassword.text.toString().trim()

        // Input validation
        if (email.isEmpty() || password.isEmpty()) {
            Snackbar.make(
                binding.main,
                getString(R.string.error_required_login_fields),
                Snackbar.LENGTH_SHORT
            ).show()
            return
        }

        // Disables the button to prevent multiple taps
        binding.btnPrimaryAction.isEnabled = false

        val task = if (isSignUpMode) {
            auth.createUserWithEmailAndPassword(email, password)
        } else {
            auth.signInWithEmailAndPassword(email, password)
        }

        task.addOnCompleteListener { result ->
            binding.btnPrimaryAction.isEnabled = true
            if (result.isSuccessful) {
                goToMain()
            } else {
                val message = result.exception?.localizedMessage
                    ?: getString(R.string.error_auth_failed)
                Snackbar.make(
                    binding.main,
                    message,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

}
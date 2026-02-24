package water.monitor.system

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import water.monitor.system.databinding.ActivityLoginBinding
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        checkSession()

        binding.loginBtn.setOnClickListener {
            val email = binding.emailEt.text.toString().trim()
            val password = binding.passwordEt.text.toString().trim()

            // Clear previous errors
            binding.emailInputLayout.error = null
            binding.passwordInputLayout.error = null

            var isValid = true

            if (email.isEmpty()) {
                binding.emailInputLayout.error = "Email is required"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailInputLayout.error = "Invalid email format"
                isValid = false
            }

            if (password.isEmpty()) {
                binding.passwordInputLayout.error = "Password is required"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            // Show loading state (optional, disabling button)
            binding.loginBtn.isEnabled = false
            binding.loginBtn.text = "Logging In..."

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    binding.loginBtn.isEnabled = true
                    binding.loginBtn.text = "Login"

                    if (task.isSuccessful) {
                        // Handle Remember Me
                        if (binding.rememberMeCb.isChecked) {
                            saveLoginTimestamp()
                        } else {
                            clearLoginTimestamp()
                        }
                        startMainActivity()
                    } else {
                        val exception = task.exception
                        when (exception) {
                            is FirebaseAuthInvalidUserException -> {
                                binding.emailInputLayout.error = "No account found with this email"
                            }
                            is FirebaseAuthInvalidCredentialsException -> {
                                binding.passwordInputLayout.error = "Incorrect password"
                            }
                            else -> {
                                Toast.makeText(this, "Login failed: ${exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
        }

        binding.registerTv.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.forgotPasswordTv.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reset Password")
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null)
        val emailEt = view.findViewById<EditText>(R.id.emailEt)
        builder.setView(view)
        builder.setPositiveButton("Reset") { _, _ ->
            val email = emailEt.text.toString().trim()
            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Please enter a valid email.", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.create().show()
    }

    private fun checkSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val lastLogin = sharedPreferences.getLong("last_login", 0)
            val twoWeeksInMillis = TimeUnit.DAYS.toMillis(14)
            val currentTime = System.currentTimeMillis()
            
            if (lastLogin != 0L && (currentTime - lastLogin > twoWeeksInMillis)) {
                Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                auth.signOut()
                return
            }

            if (lastLogin != 0L) {
                saveLoginTimestamp()
            }

            currentUser.reload().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    startMainActivity()
                } else {
                    Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                }
            }
        }
    }

    private fun saveLoginTimestamp() {
        sharedPreferences.edit().putLong("last_login", System.currentTimeMillis()).apply()
    }
    
    private fun clearLoginTimestamp() {
        sharedPreferences.edit().remove("last_login").apply()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
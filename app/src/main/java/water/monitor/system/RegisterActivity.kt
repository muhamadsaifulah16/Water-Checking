package water.monitor.system

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import water.monitor.system.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.signUpBtn.setOnClickListener {
            val username = binding.nameEt.text.toString().trim()
            val email = binding.emailEt.text.toString().trim()
            val password = binding.passwordEt.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEt.text.toString().trim()

            // Clear previous errors
            binding.usernameInputLayout.error = null
            binding.emailInputLayout.error = null
            binding.passwordInputLayout.error = null
            binding.confirmPasswordInputLayout.error = null

            var isValid = true

            if (username.isEmpty()) {
                binding.usernameInputLayout.error = "Username is required"
                isValid = false
            }

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
            } else if (!isPasswordStrong(password)) {
                binding.passwordInputLayout.error = "Password must be 8+ characters, with a number & special character."
                isValid = false
            }

            if (confirmPassword.isEmpty()) {
                binding.confirmPasswordInputLayout.error = "Confirm your password"
                isValid = false
            } else if (password != confirmPassword) {
                binding.confirmPasswordInputLayout.error = "Passwords do not match"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            // Show loading state
            binding.signUpBtn.isEnabled = false
            binding.signUpBtn.text = "Creating Account..."

            // Create User
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // User Created. Now save Profile
                        val user = auth.currentUser

                        // 1. Update Display Name
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build()
                        
                        user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                            binding.signUpBtn.isEnabled = true
                            binding.signUpBtn.text = "Sign Up"

                            if (profileTask.isSuccessful) {
                                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                auth.signOut() // Ensure user is signed out so they have to login
                                val intent = Intent(this, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            } else {
                                // Still treat as success, but log the profile update failure
                                Toast.makeText(this, "Account created. Failed to set display name.", Toast.LENGTH_LONG).show()
                                auth.signOut()
                                val intent = Intent(this, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        }

                    } else {
                        binding.signUpBtn.isEnabled = true
                        binding.signUpBtn.text = "Sign Up"
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }

        binding.loginTv.setOnClickListener {
            finish() // Go back to Login Activity
        }
    }

    private fun isPasswordStrong(password: String): Boolean {
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        return password.length >= 8 && hasDigit && hasSpecialChar
    }
}
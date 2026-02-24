package water.monitor.system

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import water.monitor.system.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    private var currentPh = 0.0
    private var currentTds = 0.0
    private var currentTurb = 0.0

    // Launcher for the notification permission request
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("FCM", "Notification permission granted.")
        } else {
            Log.w("FCM", "Notification permission denied.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupDrawer()
        setupRealtimeMonitoring()
        setupExitConfirmation()
        subscribeToNotifications()
        askNotificationPermission()

        // Handle UI Listeners
        binding.menuBtn.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }
        binding.historyBtn.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        binding.saveBtn.setOnClickListener { saveReadingToHistory() }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level 33 and above (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun subscribeToNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener { task ->
                val msg = if (task.isSuccessful) "Subscribed to alerts" else "Subscription failed"
                Log.d("FCM", msg)
            }
    }

    private fun setupDrawer() {
        val currentUser = auth.currentUser
        val headerView = binding.navView.getHeaderView(0)
        val usernameTv = headerView.findViewById<TextView>(R.id.usernameNav)
        val emailTv = headerView.findViewById<TextView>(R.id.emailNav)

        if (currentUser != null) {
            usernameTv.text = currentUser.displayName ?: "User"
            emailTv.text = currentUser.email ?: ""
        }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            if (menuItem.itemId == R.id.nav_logout) {
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            } else {
                false
            }
        }
    }

    private fun setupRealtimeMonitoring() {
        val database = FirebaseDatabase.getInstance("https://pool-monitor-de3bc-default-rtdb.asia-southeast1.firebasedatabase.app")
        val latestRef = database.getReference("latest")

        latestRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val ph = snapshot.child("ph").value.toString().toFloatOrNull() ?: 0f
                    val tds = snapshot.child("tds").value.toString().toFloatOrNull() ?: 0f
                    val turb = snapshot.child("turbidity").value.toString().toFloatOrNull() ?: 0f

                    currentPh = ph.toDouble()
                    currentTds = tds.toDouble()
                    currentTurb = turb.toDouble()

                    binding.phValue.text = String.format("%.2f", ph)
                    binding.tdsValue.text = String.format("%.0f ppm", tds)
                    binding.turbidityValue.text = String.format("%.2f NTU", turb)

                    val status = when {
                        ph > 6.5 && ph < 8.5 && turb < 5 -> "Good"
                        turb < 10 -> "Fair"
                        else -> "Unsafe"
                    }
                    binding.qualityText.text = status
                    updateStatusColors(ph, tds, turb)
                } else {
                    binding.qualityText.text = "No Data"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.qualityText.text = "Error"
                Log.e("MainActivity", "Database error: ${error.message}")
            }
        })
    }

    private fun updateStatusColors(ph: Float, tds: Float, turb: Float) {
        val phColor = if (ph < 6.5 || ph > 8.5) Color.RED else Color.parseColor("#1976D2")
        val tdsColor = if (tds > 500) Color.RED else Color.parseColor("#1976D2")
        val turbColor = if (turb > 5.0) Color.RED else Color.parseColor("#1976D2")
        binding.phValue.setTextColor(phColor)
        binding.tdsValue.setTextColor(tdsColor)
        binding.turbidityValue.setTextColor(turbColor)
    }

    private fun saveReadingToHistory() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to save readings.", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentPh == 0.0 && currentTds == 0.0 && currentTurb == 0.0) {
            Toast.makeText(this, "No data received yet. Please wait.", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseDatabase.getInstance()
        val historyRef = database.getReference("users").child(currentUser.uid).child("history_data")

        val newReadingRef = historyRef.push()

        val reading = hashMapOf(
            "ph" to currentPh,
            "tds" to currentTds,
            "turbidity" to currentTurb,
            "timestamp" to System.currentTimeMillis()
        )

        newReadingRef.setValue(reading)
            .addOnSuccessListener {
                Toast.makeText(this, "Reading saved to History!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupExitConfirmation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    showExitDialog()
                }
            }
        })
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Do you want to exit the app?")
            .setPositiveButton("Exit") { _, _ -> finish() }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
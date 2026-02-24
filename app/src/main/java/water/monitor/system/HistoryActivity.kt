package water.monitor.system

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import water.monitor.system.databinding.ActivityHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Handle Back Button
        binding.backBtn.setOnClickListener {
            finish()
        }

        setupEmptyCharts()
        fetchHistoryData()
    }

    private fun setupEmptyCharts() {
        setupChartStyle(binding.phChart)
        setupChartStyle(binding.tdsChart)
        setupChartStyle(binding.turbChart)
    }

    private fun setupChartStyle(chart: LineChart) {
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setPinchZoom(true)
        chart.setDrawGridBackground(false)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.axisRight.isEnabled = false
        chart.legend.isEnabled = true
        chart.setNoDataText("Loading history data...")
        chart.setNoDataTextColor(Color.GRAY)

        // FIX: Add extra space at the bottom for rotated labels
        chart.extraBottomOffset = 30f
    }

    private fun fetchHistoryData() {
        val database = FirebaseDatabase.getInstance("https://pool-monitor-de3bc-default-rtdb.asia-southeast1.firebasedatabase.app")
        val myRef = database.getReference("readings")
        val query = myRef.orderByChild("timestamp").limitToLast(10)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    Log.d("History", "No documents found")
                    updateChartsWithNoData()
                    return
                }

                val phEntries = ArrayList<Entry>()
                val tdsEntries = ArrayList<Entry>()
                val turbEntries = ArrayList<Entry>()
                val dateLabels = ArrayList<String>()

                var index = 0f
                for (child in snapshot.children) {
                    val ph = child.child("ph").value.toString().toFloatOrNull() ?: 0f
                    val tds = child.child("tds").value.toString().toFloatOrNull() ?: 0f
                    val turb = child.child("turbidity").value.toString().toFloatOrNull() ?: 0f
                    
                    val timestampLong = child.child("timestamp").getValue(Long::class.java)
                    val dateStr = if (timestampLong != null) {
                        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestampLong * 1000))
                    } else {
                        "Run ${index.toInt()}"
                    }

                    phEntries.add(Entry(index, ph))
                    tdsEntries.add(Entry(index, tds))
                    turbEntries.add(Entry(index, turb))
                    dateLabels.add(dateStr)
                    
                    index++
                }

                updateChartData(binding.phChart, phEntries, "pH", Color.BLUE, dateLabels)
                updateChartData(binding.tdsChart, tdsEntries, "TDS (ppm)", Color.RED, dateLabels)
                updateChartData(binding.turbChart, turbEntries, "Turbidity (NTU)", Color.GREEN, dateLabels)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("History", "Error getting documents: ${error.message}")
                updateChartsWithNoData()
            }
        })
    }

    private fun updateChartsWithNoData() {
        binding.phChart.setNoDataText("No history data available yet.")
        binding.tdsChart.setNoDataText("No history data available yet.")
        binding.turbChart.setNoDataText("No history data available yet.")
        binding.phChart.invalidate()
        binding.tdsChart.invalidate()
        binding.turbChart.invalidate()
    }

    private fun updateChartData(chart: LineChart, entries: List<Entry>, label: String, color: Int, dates: List<String>) {
        if (entries.isEmpty()) return

        val dataSet = LineDataSet(entries, label)
        dataSet.color = color
        dataSet.setCircleColor(color)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(false)
        dataSet.valueTextSize = 10f
        dataSet.setDrawFilled(true)
        dataSet.fillAlpha = 50
        dataSet.fillColor = color

        // FIX: Format values to be whole numbers (integers)
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getPointLabel(entry: Entry?): String {
                return String.format("%.0f", entry?.y)
            }
        }

        val lineData = LineData(dataSet)
        chart.data = lineData
        
        val xAxis = chart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(dates)
        xAxis.granularity = 1f
        xAxis.labelRotationAngle = -45f
        
        chart.animateX(1000)
        chart.invalidate()
    }
}
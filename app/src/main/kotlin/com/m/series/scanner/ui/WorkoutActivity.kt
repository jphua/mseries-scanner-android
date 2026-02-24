package com.m.series.scanner.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.m.series.scanner.databinding.ActivityWorkoutBinding
import kotlinx.coroutines.launch
import java.io.File

class WorkoutActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BIKE_ID = "extra_bike_id"
        const val EXTRA_BIKE_VERSION = "extra_bike_version"
    }

    private lateinit var binding: ActivityWorkoutBinding
    private val viewModel: WorkoutViewModel by viewModels()

    private var bikeId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorkoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        bikeId = intent.getIntExtra(EXTRA_BIKE_ID, -1)
        val bikeVersion = intent.getStringExtra(EXTRA_BIKE_VERSION) ?: ""

        binding.tvBikeLabel.text = "Bike #$bikeId"
        binding.tvFirmware.text = "Firmware v$bikeVersion"

        binding.btnEndSession.setOnClickListener { confirmEndSession() }

        // Intercept back press to confirm end
        onBackPressedDispatcher.addCallback(this) { confirmEndSession() }

        observeViewModel()

        if (savedInstanceState == null) {
            viewModel.startSession(bikeId)
        }
    }

    private fun confirmEndSession() {
        AlertDialog.Builder(this)
            .setTitle("End Session?")
            .setMessage("Your session will be saved as a CSV file.")
            .setPositiveButton("End & Save") { _, _ -> viewModel.endSession() }
            .setNegativeButton("Keep Going", null)
            .show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.latestData.collect { data ->
                        data ?: return@collect
                        binding.tvDuration.text     = data.durationFormatted
                        binding.tvCadence.text      = "%.1f".format(data.cadenceRpm)
                        binding.tvPower.text        = "${data.powerWatts}"
                        binding.tvGear.text         = "${data.gear}"
                        binding.tvHeartRate.text    = if (data.heartRateBpm > 0f)
                            "%.0f".format(data.heartRateBpm) else "—"
                        binding.tvDistance.text     = "%.1f %s".format(
                            data.distanceValue, data.distanceUnit)
                        binding.tvCalories.text     = "${data.caloricBurn}"
                        binding.tvWaiting.visibility = View.GONE
                        binding.statsGroup.visibility = View.VISIBLE
                    }
                }

                launch {
                    viewModel.state.collect { state ->
                        when (state) {
                            is WorkoutViewModel.WorkoutState.Finished -> {
                                offerShareCsv(state.csvFile)
                            }
                            is WorkoutViewModel.WorkoutState.Error -> {
                                Toast.makeText(this@WorkoutActivity,
                                    state.message, Toast.LENGTH_LONG).show()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun offerShareCsv(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file,
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "M-Series Workout Session – ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Save or Share Session CSV"))
        finish()
    }
}

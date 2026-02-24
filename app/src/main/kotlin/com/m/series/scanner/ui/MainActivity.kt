package com.m.series.scanner.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.m.series.scanner.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var bikeAdapter: BikeListAdapter

    // ── Permission handling ──────────────────────────────────────────────────

    private val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            startScanIfReady()
        } else {
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_LONG).show()
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        startScanIfReady()
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bikeAdapter = BikeListAdapter { bikeData ->
            // User tapped a bike → launch workout screen
            val intent = Intent(this, WorkoutActivity::class.java).apply {
                putExtra(WorkoutActivity.EXTRA_BIKE_ID, bikeData.equipmentId)
                putExtra(WorkoutActivity.EXTRA_BIKE_VERSION, bikeData.firmwareVersion)
            }
            startActivity(intent)
        }

        binding.recyclerBikes.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = bikeAdapter
        }

        binding.btnScan.setOnClickListener { startScanIfReady() }

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        startScanIfReady()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopScan()
    }

    // ── Scanning ─────────────────────────────────────────────────────────────

    private fun startScanIfReady() {
        if (!hasPermissions()) {
            permissionLauncher.launch(requiredPermissions)
            return
        }
        if (!viewModel.isBluetoothEnabled()) {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        viewModel.startScan()
    }

    private fun hasPermissions(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    // ── Observation ──────────────────────────────────────────────────────────

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.bikes.collect { bikes ->
                        bikeAdapter.submitList(bikes.values.sortedBy { it.equipmentId })
                        binding.tvEmpty.visibility =
                            if (bikes.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.scanState.collect { state ->
                        when (state) {
                            is MainViewModel.ScanState.Scanning -> {
                                binding.progressScan.visibility = View.VISIBLE
                                binding.btnScan.text = "Scanning…"
                                binding.btnScan.isEnabled = false
                            }
                            is MainViewModel.ScanState.Idle -> {
                                binding.progressScan.visibility = View.GONE
                                binding.btnScan.text = "Scan for Bikes"
                                binding.btnScan.isEnabled = true
                            }
                            is MainViewModel.ScanState.Error -> {
                                binding.progressScan.visibility = View.GONE
                                binding.btnScan.text = "Scan for Bikes"
                                binding.btnScan.isEnabled = true
                                Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }
}

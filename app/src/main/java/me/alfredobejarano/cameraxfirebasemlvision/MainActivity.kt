package me.alfredobejarano.cameraxfirebasemlvision

import android.Manifest.permission.CAMERA
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil.setContentView
import com.google.android.material.snackbar.Snackbar
import me.alfredobejarano.cameraxfirebasemlvision.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private companion object {
        const val PERMISSION_REQUEST = 3
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = setContentView(this, R.layout.activity_main)


        val snackbar = Snackbar.make(binding.root, "", Snackbar.LENGTH_INDEFINITE)
        binding.cameraView.addOnLabelResultsListener { result -> displayResult(snackbar, result) }
    }

    override fun onResume() {
        super.onResume()
        if (isCameraPermissionGranted()) {
            binding.cameraView.bindToLifeCycle(this)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(CAMERA), PERMISSION_REQUEST)
        }
    }

    private fun isCameraPermissionGranted() =
        ContextCompat.checkSelfPermission(this, CAMERA) == PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        request: Int,
        permissions: Array<out String>,
        results: IntArray
    ) =
        if (request == PERMISSION_REQUEST && results.isNotEmpty() && results.first() == PERMISSION_GRANTED) {
            binding.cameraView.bindToLifeCycle(this)
        } else {
            super.onRequestPermissionsResult(request, permissions, results)
        }

    private fun displayResult(snackBar: Snackbar, result: String) {
        if (result.isNotEmpty()) {
            snackBar.setText(result)
        }

        if (!snackBar.isShown) {
            snackBar.show()
        }
    }
}

package com.tcc.face.feature.ui.fragments.capture

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.tcc.face.R
import com.tcc.face.base.websocket.Trigger
import com.tcc.face.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
open class HomeFragment : Fragment(R.layout.fragment_home) {

    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    // ViewBinding instance
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    val gson = Gson()

    private val viewModel: DetectionViewModel by activityViewModels()

    private val PERMISSIONS_REQUEST_CODE = 101
    var faceCaptured = false
    private val requiredPermissions = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewBinding
        _binding = FragmentHomeBinding.bind(view)
        checkAndRequestPermissions()

        observeViewModel()

        viewModel.hubConnection?.on("ReceiveMessage", { messageJson ->
            val trigger = gson.fromJson(messageJson, Trigger::class.java)
            onMessageReceived(trigger)
        }, String::class.java)
    }

    private fun checkAndRequestPermissions() {
        requestPermissionsIfNecessary()
    }

    private fun hasAllPermissions(): Boolean {
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    private fun requestPermissionsIfNecessary() {
        if (!hasAllPermissions()) {
            // Explain why permissions are needed, especially for sensitive permissions
            if (shouldShowRequestPermissionRationale(requiredPermissions[0])) {
                // Show an explanation dialog before requesting permissions
                AlertDialog.Builder(requireContext())
                    .setTitle("Permissions Needed")
                    .setMessage("This app needs these permissions to access the internet, camera, storage, and phone state for its features. Please grant them to continue.")
                    .setPositiveButton(
                        "Grant Permissions"
                    ) { dialog, which ->
                        requestPermissions(
                            requiredPermissions,
                            PERMISSIONS_REQUEST_CODE
                        )
                    }
                    .setNegativeButton(
                        "Cancel"
                    ) { dialog, which ->
                        // Handle case where user denies permission and doesn't want explanation again
                        //                        Toast.makeText(requireContext(), "App functionality may be limited without permissions.", Toast.LENGTH_SHORT).show();
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, directly request permissions
                requestPermissions(requiredPermissions, PERMISSIONS_REQUEST_CODE)
            }
        } else {
            // All permissions already granted, proceed with functionality
            // Example: access the internet, camera, storage, or phone state as needed
            Toast.makeText(requireContext(), "Permissions granted!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.startConnection()
    }
    // Permission request

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            // Handle permission denial
        }
    }

    private val capturePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            // Photo was taken successfully, now convert to Base64
            val base64Image = convertImageToBase64(viewModel.photoUri)

            viewModel.setAuthenticationData(
                DetectionViewModel.AuthenticationData(
                    "123",
                    base64Image
                )
            )

            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToCaptureFaceFragment())

        }
    }


    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }


    private fun openCamera() {
        val photoFile = createImageFile()
        viewModel.photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
        capturePhotoLauncher.launch(viewModel.photoUri)
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun convertImageToBase64(photoUri: Uri): String {
        val inputStream = requireContext().contentResolver.openInputStream(photoUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 400, 400, true)

        val byteArrayOutputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    private fun onMessageReceived(message: Trigger) {

        requireActivity().runOnUiThread {
            binding.waitingLayout.visibility = View.GONE

            viewModel.billNum = message.billNumber
            viewModel.amount = message.amount.toString()
            viewModel.accountId = message.account_ID

            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToNewTransactionFragment())
        }

    }

    private fun showLoading(isLoading: Boolean) {
        binding.llProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        //  binding.loginBtn.isEnabled = !isLoading
    }

    private fun isValidInput(username: String, password: String): Boolean {
        return username.isNotEmpty() && password.isNotEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Avoid memory leaks by nullifying the binding reference

        runnable?.let { handler.removeCallbacks(it) }

    }

}

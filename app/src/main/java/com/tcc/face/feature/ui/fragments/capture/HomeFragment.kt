package com.tcc.face.feature.ui.fragments.capture

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.tcc.face.R
import com.tcc.face.base.websocket.Trigger
import com.tcc.face.databinding.FragmentHomeBinding
import com.tcc.face.domain.models.BasicState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private lateinit var hubConnection: HubConnection
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

        initSignalR()
        startSignalRConnection()

        // Set up UI interactions and listeners
//        setupUI()
//        observeViewModel()

        startRecurringTask()
    }

    private fun startRecurringTask() {
        runnable = object : Runnable {
            override fun run() {
                // Schedule the task to run again after 10 seconds
                handler.postDelayed(this, 5000)
                // Check if the state is not Idle before calling getPayable
                if (viewModel.isPayableIdle()) {
                    viewModel.getPayable()
                }
            }
        }
        // Start the task for the first time
        handler.post(runnable!!)
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

    fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.payableState.collect { payableState ->
                    when (payableState) {
                        is BasicState.Loading -> {
                            Log.e("loading", "iii")

                        }

                        is BasicState.Success -> {
                            viewModel.newPayable?.let { onMessageReceived(it) }
                        }

                        is BasicState.Error -> {
                            Log.e("error", payableState.message)
                            // Toast.makeText(context, "No Payable Transaction", Toast.LENGTH_SHORT).show()
                        }

                        BasicState.Idle -> {
                            Log.e("idle", "iii")

                        }
                    }
                }
            }

        }

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

        binding.waitingLayout.visibility = View.GONE
        //  binding.transactionLayout.visibility=View.VISIBLE
        viewModel.billNum = message.billNumber
        viewModel.amount = message.amount.toString()
        viewModel.accountId = message.account_ID

        //  binding.txtAmountValue.text = String.format("%.2f", viewModel.amount!!.toDouble())
        //  binding.txtBillNumValue.text = viewModel.billNum

        findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToNewTransactionFragment())
    }

    private fun setupUI() {


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

        // Stop SignalR connection when the fragment is destroyed
        CoroutineScope(Dispatchers.IO).launch {
            hubConnection.stop().blockingAwait()
        }
    }

    private fun initSignalR() {
        hubConnection = HubConnectionBuilder
            .create("http://172.20.10.2:5000/messageHub")
            .build()

        // Listening for messages from SignalR
        hubConnection.on("ReceiveMessage", { message: String ->
            // Handle received message (make sure to update UI on the main thread)
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Message received: $message", Toast.LENGTH_SHORT)
                    .show()
                // Update your UI elements or pass this message to the ViewModel
            }

          try {
                // Deserialize JSON message to Trigger object
                val trigger = gson.fromJson(message, Trigger::class.java)
                println("Received Trigger object: $trigger")
            } catch (e: JsonSyntaxException) {
                println("Error parsing JSON: ${e.message}")
            }
        }, String::class.java)
    }

    private fun startSignalRConnection() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                hubConnection.start().blockingAwait()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Connected to SignalR", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to connect to SignalR",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

}

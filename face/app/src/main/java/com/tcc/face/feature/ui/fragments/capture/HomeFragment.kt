package com.tcc.face.feature.ui.fragments.capture

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController

import com.tcc.face.R
import com.tcc.face.base.websocket.WebSocketCallback
import com.tcc.face.base.websocket.WebSocketManager
import com.tcc.face.base.websocket.WebSocketMessage
import com.tcc.face.databinding.FragmentHomeBinding
import com.tcc.face.domain.models.BasicState
import com.tcc.face.domain.models.BiometricX


import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
open class HomeFragment : Fragment(R.layout.fragment_home), WebSocketCallback {

    // ViewBinding instance
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var photoUri: Uri

    private val viewModel: DetectionViewModel by activityViewModels()

    private lateinit var webSocketManager: WebSocketManager

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
            val base64Image = convertImageToBase64(photoUri)

            viewModel.setAuthenticationData(
                DetectionViewModel.AuthenticationData(
                    "123",
                    base64Image
                )
            )

            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToCaptureFaceFragment())

            /* val lastName = viewModel.getAllData().step1.lastName
            val firstName = viewModel.getAllData().step1.firstName

            val oldEmail = viewModel.getAllData().step2.email
            val oldPassword = viewModel.getAllData().step2.password

            val email = viewModel.getAllData().step3.tccEmail
            val password = viewModel.getAllData().step3.password
            val phoneNum = viewModel.getAllData().step3.phoneNum
            val pin = viewModel.getAllData().step3.pin
            val photo = listOf(BiometricX(base64Image, 1))*/

            // viewModel.signUp(SignUpRequest(photo, email, firstName, lastName, phoneNum, password, pin))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewBinding
        _binding = FragmentHomeBinding.bind(view)

        // Initialize WebSocket
        webSocketManager = WebSocketManager(this)
        webSocketManager.startWebSocket()

        // Set up UI interactions and listeners
        setupUI()
        //observeViewModel()
    }

    override fun onMessageReceived(message: WebSocketMessage) {
        TODO("Not yet implemented")
    }
    private fun requestCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            photoFile
        )
        capturePhotoLauncher.launch(photoUri)
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

    private fun setupUI() {

        binding.faceCaptureBtn.setOnClickListener {

            //findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToCaptureFaceFragment())
            requestCameraPermission()
        }


    }

   /* private fun observeViewModel() {
        // Observe login state

        lifecycleScope.launchWhenStarted {
            viewModel.SignUpState.collect { loginState ->
                when (loginState) {
                    is BasicState.Loading -> showLoading(true)
                    is BasicState.Success -> {
                        showLoading(false)
                        handleSignUpSuccess()
                    }
                    is BasicState.Error -> {
                        showLoading(false)
                        Toast.makeText(context, loginState.message, Toast.LENGTH_SHORT).show()
                    }

                    BasicState.Idle -> {

                    }
                }
            }


        }
    }*/

    private fun isValidInput(username: String, password: String): Boolean {
        return username.isNotEmpty() && password.isNotEmpty()
    }

    private fun handleSignUpSuccess() {
        Toast.makeText(context, "Success! Your account has been created.", Toast.LENGTH_SHORT)
            .show()
        // findNavController().navigate(CaptureFaceFragmentDirections.actionCaptureFaceFragmentToSuccessfulFaceFragment())
        // Navigate or handle successful login
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Avoid memory leaks by nullifying the binding reference
    }

    fun getBase64FromDrawable(): String? {
        // Replace 'image' with the name of your drawable without the file extension
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.image)

        return bitmap?.let {
            // Convert Bitmap to Base64
            convertBitmapToBase64(it)
        }
    }

    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        // Compress the bitmap into a byte array (JPEG format)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        // Convert byte array to Base64 string
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

}

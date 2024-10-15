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
import com.tcc.face.databinding.FragmentFaceCaptureBinding
import com.tcc.face.domain.Constants
import com.tcc.face.domain.models.BasicState
import com.tcc.face.domain.models.BiometricData
import com.tcc.face.domain.models.PaymentAuthenticationRequest

import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
open class CaptureFaceFragment : Fragment(R.layout.fragment_face_capture) {

    // ViewBinding instance
    private var _binding: FragmentFaceCaptureBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetectionViewModel by activityViewModels()

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

    // Photo capture
    private val capturePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            // Photo was taken successfully, now convert to Base64


            //   viewModel.signUp(SignUpRequest(photo, email, firstName, lastName, phoneNum, password, pin))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewBinding
        _binding = FragmentFaceCaptureBinding.bind(view)

        // Set up UI interactions and listeners
        setupUI()
        observeViewModel()
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

    private fun setupUI() {

        binding.btnPay.setOnClickListener {

            viewModel.authenticationData
                ?.value
                ?.imageData
                ?.let { base64 ->

                    val pinCode = binding.pinCode.text.toString().trim()

                    if (isValidInput(pinCode)) {
                        viewModel.payment(
                            PaymentAuthenticationRequest(
                                account_ID = Constants.ACCOUNT_ID,
                                billNumber = "1234",
                                amount = "22.0",
                                pin = pinCode,
                                biometric = BiometricData(
                                    biometricData = base64
                                )
                            )
                        )
                    } else {
                        Toast.makeText(context, "PIN Code is required", Toast.LENGTH_SHORT).show()
                    }

                }
        }

        binding.imageView.setOnClickListener{
            requestCameraPermission()
        }

    }

    private fun observeViewModel() {

        val data = viewModel.authenticationData.value
        data
            ?.let {
                base64ToBitmap(data.imageData)
                    .let { binding.imageView.setImageBitmap(it) }
            }

        // Observe login state
        lifecycleScope.launchWhenStarted {
            viewModel.paymentState.collect { paymentState ->
                when (paymentState) {
                    is BasicState.Loading -> showLoading(true)
                    is BasicState.Success -> {
                        showLoading(false)
                        handleSignUpSuccess()

                    }

                    is BasicState.Error -> {
                        showLoading(false)
                        Toast.makeText(context, paymentState.message, Toast.LENGTH_SHORT).show()
                    }

                    BasicState.Idle -> {

                    }
                }
            }


        }

    }

    private fun isValidInput(pin: String): Boolean {
        return pin.isNotEmpty()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.llProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        //  binding.loginBtn.isEnabled = !isLoading
    }

    private fun handleSignUpSuccess() {
        Toast.makeText(context, "Success! Payment is processed successfully", Toast.LENGTH_SHORT)
            .show()
        findNavController().navigate(CaptureFaceFragmentDirections.actionCaptureFaceFragmentToHomeFragment())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Avoid memory leaks by nullifying the binding reference

        viewModel.resetViewModel()

    }

    private fun base64ToBitmap(base64String: String?): Bitmap? =
        try {
            val decodedString = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeStream(ByteArrayInputStream(decodedString))
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            null
        }

}

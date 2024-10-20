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
import com.identy.face.FaceOutput
import com.identy.face.enums.FaceTemplate
import com.tcc.face.R
import com.tcc.face.base.websocket.Trigger
import com.tcc.face.databinding.FragmentHomeBinding
import com.tcc.face.databinding.FragmentNewTransactionBinding
import com.tcc.face.domain.models.BasicState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
open class NewTransactionFragment : Fragment(R.layout.fragment_new_transaction) {

    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    // ViewBinding instance
    private var _binding: FragmentNewTransactionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetectionViewModel by activityViewModels()

    private val PERMISSIONS_REQUEST_CODE = 101
    var faceCaptured=false
    private val requiredPermissions = arrayOf(
        Manifest.permission.INTERNET,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
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

            findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToPinCreationFragment())

        }
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewBinding
        _binding = FragmentNewTransactionBinding.bind(view)
        checkAndRequestPermissions()

        // Set up UI interactions and listeners
        setupUI()
        observeViewModel()

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

    fun observeViewModel()
    {

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.faceResponse.collect { identyResponse ->
                    // Trigger navigation
                    val faceOutput: FaceOutput = identyResponse!!.prints
                    val score = faceOutput!!.score
                    if (faceOutput != null) {
                        showLoading(false)
                        faceCaptured = true


                        if (faceOutput.spoofScore > 0.7f) {
                            try {
                                val pngPhoto = faceOutput.templates[FaceTemplate.PNG]

                                viewModel.face64 = pngPhoto!!


                                // PreferenceUtil.getInstance(this).saveString(PreferenceUtil.KEY_FACE_BASE64, pngPhoto);
                                val dataBase64 = Base64.decode(pngPhoto, Base64.DEFAULT)
                                //    printSizeInKb(dataBase64)

                                val pngBitmap =
                                    BitmapFactory.decodeByteArray(dataBase64, 0, dataBase64.size)
                                viewModel.faceBit = pngBitmap

                                findNavController().navigate(NewTransactionFragmentDirections.actionNewTransactionFragmentToPinCreationFragment())

                                //Face  SDK initialization
                                //
                            } catch (e: Exception) {
                                Log.e(TAG, "faceResponse: ", e);
                                showLoading(false)
                                Toast.makeText(
                                    requireActivity(),
                                    e.getLocalizedMessage(),
                                    Toast.LENGTH_SHORT
                                ).show();
                            }
                        }


                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                viewModel.errorResponse.collect { s ->
                    showLoading(false)
                    Toast.makeText(activity, s, Toast.LENGTH_SHORT).show()
                }
            }
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



    private fun setupUI() {


        binding.txtAmountValue.text = String.format("%.2f", viewModel.amount!!.toDouble())
        binding.txtBillNumValue.text = viewModel.billNum
        binding.faceCapturingBtn.setOnClickListener {

            //findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToCaptureFaceFragment())
           // requestCameraPermission()
            showLoading(true)
            viewModel.initFaceSdk(requireActivity())

        }

        binding.btnCancelTransaction.setOnClickListener{

           findNavController().navigateUp()
            viewModel.clearTransaction()

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

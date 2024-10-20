package com.tcc.face.feature.ui.fragments.pin

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
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.tcc.face.R
import com.tcc.face.base.Constants
import com.tcc.face.base.ui.PinKeyboard
import com.tcc.face.databinding.FragmentFaceCaptureBinding
import com.tcc.face.databinding.FragmentPinCreationBinding
import com.tcc.face.domain.models.BasicState
import com.tcc.face.domain.models.BiometricData
import com.tcc.face.domain.models.PaymentAuthenticationRequest
import com.tcc.face.feature.ui.fragments.capture.CaptureFaceFragmentDirections
import com.tcc.face.feature.ui.fragments.capture.DetectionViewModel

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
open class PinCreationFragment : Fragment(R.layout.fragment_pin_creation) {

    // ViewBinding instance
    private var _binding: FragmentPinCreationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetectionViewModel by activityViewModels()

    private val pinBuilder = StringBuilder()

    private var pinLength: Int = 4

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewBinding
        _binding = FragmentPinCreationBinding.bind(view)

        // Set up UI interactions and listeners
        setupUI()
        observeViewModel()
        binding.txtBillNumValue.text = viewModel.billNum
        binding.txtAmountValue.text = viewModel.amount
    }


    private fun setupUI() {


        binding.pinKeyboard.onClickListener = { key ->
            when (key) {
                is PinKeyboard.Key.Code -> {
                    onCodeClicked(key)
                }

                is PinKeyboard.Key.Clear -> {
                    onClearClicked()
                }

                is PinKeyboard.Key.Action -> {}
            }
        }


        binding.imgFace.setImageBitmap(viewModel.faceBit)
        binding.pinView.setDefaultLength(pinLength)
        binding.pinView.init(false)
        binding.pinKeyboard.setupKeyboard(PinKeyboard.MODE_ACTION_NONE, false)
        binding.imgBack.setOnClickListener { findNavController().navigateUp() }

    }

    private fun onWrongPin(attemptsRemained: Int) {
        binding.pinView.setError(true)
        binding.pinView.shake()
        binding.tvPinHint.setTextColor(
            ContextCompat.getColor(
                binding.tvPinHint.context,
                R.color.red
            )
        )
        binding.tvPinHint.text =
            resources.getString(R.string.pin_error_wrong_pin_tmpl, attemptsRemained)
        binding.pinView.clear()
        pinBuilder.clear()
    }

    private fun onClearClicked() {
        if (pinBuilder.lastIndex >= 0) pinBuilder.deleteCharAt(pinBuilder.lastIndex)
        binding.pinView.remove()
        onNoError()
    }

    private fun onCodeClicked(key: PinKeyboard.Key.Code) {
        if (pinBuilder.length < pinLength) {
            pinBuilder.append(key.digit)
            binding.pinView.add()
            onNoError()
            if (pinBuilder.length == pinLength) {
                onPinReady(pinBuilder.toString())
            }
        }
    }

    private fun onPinReady(pin: String) {
        ///  viewModel.onPinReady(pin)
        if (isValidInput(pin)) {
            viewModel.payment(
                PaymentAuthenticationRequest(
                    account_ID = viewModel.accountId!!,
                    billNumber = viewModel.billNum!!,
                    amount = viewModel.amount!!,
                    pin = pin,
                    biometric = BiometricData(
                        biometricData = viewModel.face64!!
                    )
                )
            )
        } else {
            Toast.makeText(context, "PIN Code is required", Toast.LENGTH_SHORT).show()
        }

    }

    private fun onNoError() {
        binding.pinView.setError(false)
        //    binding.tvPinHint.text = getString(R.string.pin_sign_hint)
        //  binding.tvPinHint.setTextColor(ContextCompat.getColor(binding.tvPinTitle.context, R.color.white))
    }

    private fun observeViewModel() {

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.paymentState.collect { paymentState ->
                    when (paymentState) {
                        is BasicState.Loading -> showLoading(true)
                        is BasicState.Success -> {
                            showLoading(true)
                            handleSignUpSuccess()

                        }

                        is BasicState.Error -> {
                            showLoading(false)

                            findNavController().navigateUp()
                            viewModel.setFirstTime(false)
                            Toast.makeText(context, paymentState.message, Toast.LENGTH_LONG).show()
                        }

                        BasicState.Idle -> {

                        }
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.llProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
        //  binding.loginBtn.isEnabled = !isLoading
    }

    private fun isValidInput(pin: String): Boolean {
        return pin.isNotEmpty()
    }

    private fun handleSignUpSuccess() {
        Toast.makeText(context, "Success! Payment is processed successfully", Toast.LENGTH_SHORT)
            .show()
        viewModel.clearTransaction()


        findNavController().navigate(PinCreationFragmentDirections.actionPinCreationFragmentToHomeFragment())
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Avoid memory leaks by nullifying the binding reference

        viewModel.resetViewModel()

    }


}

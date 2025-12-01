package com.workit.workit.ui.theme.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.workit.workit.LoginActivity
import com.workit.workit.R
import com.workit.workit.auth.RoleManager
import com.workit.workit.ui.viewmodel.ProfileUiState
import com.workit.workit.ui.viewmodel.ProfileUpdateUiState
import com.workit.workit.ui.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private val viewModel: ProfileViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnEdit: Button
    private lateinit var btnLogout: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etName = view.findViewById(R.id.et_name)
        etEmail = view.findViewById(R.id.et_email)
        etPhone = view.findViewById(R.id.et_phone)
        etAddress = view.findViewById(R.id.et_address)
        btnEdit = view.findViewById(R.id.btn_edit)
        btnLogout = view.findViewById(R.id.btn_logout)

        val userId = auth.currentUser?.uid ?: return
        viewModel.loadProfile(userId)

        // Observe profile state
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profileState.collect { state ->
                    when (state) {
                        is ProfileUiState.StudentLoaded -> {
                            etName.setText(state.student.name)
                            etEmail.setText(state.student.email)
                            etPhone.setText(state.student.phone)
                            etAddress.setText(state.student.address)
                        }
                        is ProfileUiState.EmployerLoaded -> {
                            etName.setText(state.employer.companyName)
                            etEmail.setText(state.employer.email)
                            etPhone.setText(state.employer.phone)
                            etAddress.setText(state.employer.address)
                        }
                        is ProfileUiState.Error -> {
                            showError(state.message)
                        }
                        is ProfileUiState.Loading -> {}
                    }
                }
            }
        }

        // Observe update state
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateState.collect { state ->
                    when (state) {
                        is ProfileUpdateUiState.Success -> {
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                        }
                        is ProfileUpdateUiState.Error -> {
                            showError(state.message)
                        }
                        else -> {}
                    }
                }
            }
        }

        btnEdit.setOnClickListener {
            updateProfile()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun updateProfile() {
        val userId = auth.currentUser?.uid ?: return
        val updates = mapOf(
            "phone" to etPhone.text.toString(),
            "address" to etAddress.text.toString()
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val role = RoleManager.getUserRole()
            if (role == RoleManager.UserRole.STUDENT) {
                viewModel.updateStudentProfile(userId, updates + ("name" to etName.text.toString()))
            } else {
                viewModel.updateEmployerProfile(userId, updates + ("companyName" to etName.text.toString()))
            }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                logout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun logout() {
        RoleManager.logout()
        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
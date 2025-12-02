package com.workit.workit.ui.theme.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
import com.workit.workit.ui.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileFragment : Fragment() {
    private val viewModel: ProfileViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var tvDisplayName: TextView
    private lateinit var tvDisplayEmail: TextView
    private lateinit var tvDisplayLocation: TextView
    private lateinit var tvPhoneValue: TextView
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

        tvDisplayName = view.findViewById(R.id.tv_display_name)
        tvDisplayEmail = view.findViewById(R.id.tv_display_email)
        tvDisplayLocation = view.findViewById(R.id.tv_display_location)
        tvPhoneValue = view.findViewById(R.id.tv_phone_value)
        btnEdit = view.findViewById(R.id.btn_edit)
        btnLogout = view.findViewById(R.id.btn_logout)

        loadProfileFromFirebase()

        btnEdit.setOnClickListener {
            showEditDialog()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadProfileFromFirebase() {
        val userId = auth.currentUser?.uid ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val role = RoleManager.getUserRole()

                when (role) {
                    RoleManager.UserRole.STUDENT -> {
                        val doc = db.collection("students").document(userId).get().await()
                        val name = doc.getString("name") ?: "Name not set"
                        val email = doc.getString("email") ?: "Email not set"
                        val phone = doc.getString("phone") ?: ""
                        val address = doc.getString("address") ?: ""

                        tvDisplayName.text = name
                        tvDisplayEmail.text = email
                        tvPhoneValue.text = if (phone.isEmpty()) "Not set" else phone
                        tvDisplayLocation.text = if (address.isEmpty()) "Location not set" else address
                    }
                    RoleManager.UserRole.EMPLOYER -> {
                        val doc = db.collection("employers").document(userId).get().await()
                        val companyName = doc.getString("companyName") ?: "Company not set"
                        val email = doc.getString("email") ?: "Email not set"
                        val phone = doc.getString("phone") ?: ""
                        val address = doc.getString("address") ?: ""

                        tvDisplayName.text = companyName
                        tvDisplayEmail.text = email
                        tvPhoneValue.text = if (phone.isEmpty()) "Not set" else phone
                        tvDisplayLocation.text = if (address.isEmpty()) "Location not set" else address
                    }
                    else -> {
                        showError("Unknown user type")
                    }
                }
            } catch (e: Exception) {
                showError("Failed to load profile: ${e.message}")
            }
        }
    }

    private fun showEditDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val etName = dialogView.findViewById<android.widget.EditText>(R.id.et_edit_name)
        val etEmail = dialogView.findViewById<android.widget.EditText>(R.id.et_edit_email)
        val etPhone = dialogView.findViewById<android.widget.EditText>(R.id.et_edit_phone)
        val etLocation = dialogView.findViewById<android.widget.EditText>(R.id.et_edit_location)

        // Pre-fill current values from Firebase
        etName.setText(tvDisplayName.text)
        etEmail.setText(tvDisplayEmail.text)
        etPhone.setText(if (tvPhoneValue.text == "Not set") "" else tvPhoneValue.text)
        etLocation.setText(if (tvDisplayLocation.text == "Location not set") "" else tvDisplayLocation.text)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Profile")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                saveProfileToFirebase(
                    etName.text.toString().trim(),
                    etEmail.text.toString().trim(),
                    etPhone.text.toString().trim(),
                    etLocation.text.toString().trim()
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveProfileToFirebase(name: String, email: String, phone: String, location: String) {
        val userId = auth.currentUser?.uid ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val role = RoleManager.getUserRole()

                when (role) {
                    RoleManager.UserRole.STUDENT -> {
                        val updates = hashMapOf<String, Any>(
                            "name" to name,
                            "email" to email,
                            "phone" to phone,
                            "address" to location
                        )
                        db.collection("students").document(userId).update(updates).await()
                    }
                    RoleManager.UserRole.EMPLOYER -> {
                        val updates = hashMapOf<String, Any>(
                            "companyName" to name,
                            "email" to email,
                            "phone" to phone,
                            "address" to location
                        )
                        db.collection("employers").document(userId).update(updates).await()
                    }
                    else -> {
                        showError("Unknown user type")
                        return@launch
                    }
                }

                // Update Firebase Auth email if changed
                if (email != auth.currentUser?.email) {
                    try {
                        auth.currentUser?.updateEmail(email)?.await()
                    } catch (e: Exception) {
                        showError("Email updated in profile but authentication failed: ${e.message}")
                    }
                }

                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()

                // Reload profile from Firebase to show updated values
                loadProfileFromFirebase()

            } catch (e: Exception) {
                showError("Failed to update profile: ${e.message}")
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
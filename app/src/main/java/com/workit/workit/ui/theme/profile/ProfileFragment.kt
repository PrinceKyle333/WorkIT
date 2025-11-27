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
import androidx.lifecycle.lifecycleScope
import com.workit.workit.LoginActivity
import com.workit.workit.R
import com.workit.workit.auth.RoleManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etAddress: EditText
    private lateinit var btnEdit: Button
    private lateinit var btnLogout: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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

        loadProfile()

        btnEdit.setOnClickListener {
            updateProfile()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadProfile() {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            val userRole = RoleManager.getUserRole()
            val collection = if (userRole == RoleManager.UserRole.STUDENT) "students" else "employers"

            db.collection(collection).document(userId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        if (userRole == RoleManager.UserRole.STUDENT) {
                            etName.setText(doc.getString("name") ?: "")
                        } else {
                            etName.setText(doc.getString("companyName") ?: "")
                        }
                        etEmail.setText(doc.getString("email") ?: "")
                        etPhone.setText(doc.getString("phone") ?: "")
                        etAddress.setText(doc.getString("address") ?: "")
                    }
                }
        }
    }

    private fun updateProfile() {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            val userRole = RoleManager.getUserRole()
            val collection = if (userRole == RoleManager.UserRole.STUDENT) "students" else "employers"
            val nameField = if (userRole == RoleManager.UserRole.STUDENT) "name" else "companyName"

            val profileData = mapOf(
                nameField to etName.text.toString(),
                "email" to etEmail.text.toString(),
                "phone" to etPhone.text.toString(),
                "address" to etAddress.text.toString()
            )

            db.collection(collection).document(userId).update(profileData)
                .addOnSuccessListener {
                    Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error updating profile", Toast.LENGTH_SHORT).show()
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
}
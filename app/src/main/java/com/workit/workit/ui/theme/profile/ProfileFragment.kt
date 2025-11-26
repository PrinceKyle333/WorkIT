package com.workit.workit.ui.theme.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.workit.workit.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
            logout()
        }
    }

    private fun loadProfile() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("students").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    etName.setText(doc.getString("name"))
                    etEmail.setText(doc.getString("email"))
                    etPhone.setText(doc.getString("phone"))
                    etAddress.setText(doc.getString("address"))
                }
            }
    }

    private fun updateProfile() {
        val userId = auth.currentUser?.uid ?: return
        val profileData = mapOf(
            "name" to etName.text.toString(),
            "email" to etEmail.text.toString(),
            "phone" to etPhone.text.toString(),
            "address" to etAddress.text.toString()
        )

        db.collection("students").document(userId).set(profileData)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error updating profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logout() {
        auth.signOut()
        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
        // Navigate back to login
    }
}
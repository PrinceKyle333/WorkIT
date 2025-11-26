package com.workit.workit.ui.theme.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.workit.workit.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class JobDetailsFragment : Fragment() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var jobId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_job_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        jobId = arguments?.getString("jobId")

        val btnApply = view.findViewById<Button>(R.id.btn_apply)
        btnApply.setOnClickListener {
            applyForJob()
        }

        loadJobDetails()
    }

    private fun loadJobDetails() {
        jobId?.let { id ->
            db.collection("jobs").document(id).get()
                .addOnSuccessListener { doc ->
                    val view = view ?: return@addOnSuccessListener
                    val tvPosition = view.findViewById<TextView>(R.id.tv_position)
                    val tvLocation = view.findViewById<TextView>(R.id.tv_location)
                    val tvDescription = view.findViewById<TextView>(R.id.tv_description)
                    val ivJobImage = view.findViewById<ImageView>(R.id.iv_job_image)

                    tvPosition.text = doc.getString("position")
                    tvLocation.text = doc.getString("location")
                    tvDescription.text = doc.getString("description")

                    val imageUrl = doc.getString("imageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Picasso.get().load(imageUrl).into(ivJobImage)
                    }
                }
        }
    }

    private fun applyForJob() {
        val studentId = auth.currentUser?.uid ?: return
        jobId?.let { id ->
            val matchData = mapOf(
                "jobId" to id,
                "studentId" to studentId,
                "status" to "pending",
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("matches")
                .add(matchData)
                .addOnSuccessListener {
                    Toast.makeText(context, "Applied successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error applying for job", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
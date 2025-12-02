package com.workit.workit.ui.theme.student

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.workit.workit.R
import com.workit.workit.data.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StudentMatchFragment : Fragment() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var matchesRecyclerView: RecyclerView
    private lateinit var tvEmptyMessage: TextView
    private lateinit var applicationsContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_student_match, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        matchesRecyclerView = view.findViewById(R.id.matches_recycler_view)
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message)

        matchesRecyclerView.layoutManager = LinearLayoutManager(context)

        // Create a container for applications
        applicationsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
        }

        val studentId = auth.currentUser?.uid ?: return
        loadStudentApplications(studentId)
    }

    private fun loadStudentApplications(studentId: String) {
        db.collection("applications")
            .whereEqualTo("studentId", studentId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { documents, error ->
                if (error != null) {
                    Toast.makeText(context, "Error loading applications: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                applicationsContainer.removeAllViews()

                if (documents == null || documents.isEmpty) {
                    tvEmptyMessage.visibility = View.VISIBLE
                    matchesRecyclerView.visibility = View.GONE
                    return@addSnapshotListener
                }

                tvEmptyMessage.visibility = View.GONE
                matchesRecyclerView.visibility = View.VISIBLE

                documents.forEach { doc ->
                    val application = Application(
                        id = doc.id,
                        jobId = doc.getString("jobId") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        employerId = doc.getString("employerId") ?: "",
                        employerName = doc.getString("employerName") ?: "",
                        position = doc.getString("position") ?: "",
                        status = doc.getString("status") ?: "pending",
                        resumeUrl = doc.getString("resumeUrl") ?: "",
                        corUrl = doc.getString("corUrl") ?: "",
                        applicationLetterUrl = doc.getString("applicationLetterUrl") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )

                    val applicationView = createApplicationView(application)
                    applicationsContainer.addView(applicationView)
                }

                matchesRecyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                        return object : RecyclerView.ViewHolder(applicationsContainer) {}
                    }

                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
                    override fun getItemCount() = 1
                }
            }
    }

    private fun createApplicationView(application: Application): View {
        val view = layoutInflater.inflate(R.layout.item_match, applicationsContainer, false)

        val tvEmployerName = view.findViewById<TextView>(R.id.tv_employer_name)
        val tvJobPosition = view.findViewById<TextView>(R.id.tv_job_position)
        val tvStatus = view.findViewById<TextView>(R.id.tv_status)
        val btnAccept = view.findViewById<Button>(R.id.btn_accept)
        val btnReject = view.findViewById<Button>(R.id.btn_reject)

        tvEmployerName.text = application.employerName
        tvJobPosition.text = application.position

        // Update status display
        when (application.status) {
            "pending" -> {
                tvStatus.text = "Status: Pending Review"
                tvStatus.setTextColor(resources.getColor(R.color.orange, null))
            }
            "accepted" -> {
                tvStatus.text = "Status: Accepted ✓"
                tvStatus.setTextColor(resources.getColor(R.color.green, null))
            }
            "rejected" -> {
                tvStatus.text = "Status: Rejected ✗"
                tvStatus.setTextColor(resources.getColor(R.color.red, null))
            }
        }

        // Hide accept/reject buttons and replace with document view buttons
        btnAccept.visibility = View.GONE
        btnReject.visibility = View.GONE

        // Add document viewing section
        val documentLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 0)
        }

        val tvDocumentsLabel = TextView(context).apply {
            text = "Submitted Documents:"
            textSize = 12f
            setTextColor(resources.getColor(R.color.gray, null))
            setPadding(0, 0, 0, 8)
        }
        documentLayout.addView(tvDocumentsLabel)

        // View Resume button
        val btnViewResume = Button(context).apply {
            text = "View My Resume"
            textSize = 11f
            setBackgroundResource(R.drawable.bg_btn_blue)
            setTextColor(resources.getColor(R.color.white, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
            setOnClickListener {
                openDocument(application.resumeUrl)
            }
        }

        // View COR button
        val btnViewCor = Button(context).apply {
            text = "View My COR"
            textSize = 11f
            setBackgroundResource(R.drawable.bg_btn_blue)
            setTextColor(resources.getColor(R.color.white, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
            setOnClickListener {
                openDocument(application.corUrl)
            }
        }

        // View Application Letter button
        val btnViewLetter = Button(context).apply {
            text = "View My Application Letter"
            textSize = 11f
            setBackgroundResource(R.drawable.bg_btn_blue)
            setTextColor(resources.getColor(R.color.white, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                openDocument(application.applicationLetterUrl)
            }
        }

        documentLayout.addView(btnViewResume)
        documentLayout.addView(btnViewCor)
        documentLayout.addView(btnViewLetter)

        (view as LinearLayout).addView(documentLayout)

        return view
    }

    private fun openDocument(url: String) {
        if (url.isEmpty()) {
            Toast.makeText(context, "Document not available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open document: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
package com.workit.workit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.workit.workit.R
import com.workit.workit.data.Job
import com.workit.workit.utils.TimeFormatUtils
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ExpandableJobAdapter(
    private var jobs: List<Job>,
    private val onJobClick: (Job) -> Unit,
    private val onApplyClick: (Job) -> Unit
) : RecyclerView.Adapter<ExpandableJobAdapter.JobViewHolder>() {

    private val expandedJobs = mutableSetOf<String>()

    inner class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Collapsed view references
        private val collapsedView: View = itemView.findViewById(R.id.collapsed_view)
        private val ivJobImage: ImageView = itemView.findViewById(R.id.iv_job_image)
        private val tvEmployer: TextView = itemView.findViewById(R.id.tv_employer)
        private val tvLocationBadge: TextView = itemView.findViewById(R.id.tv_location_badge)
        private val tvTimePosted: TextView = itemView.findViewById(R.id.tv_time_posted)
        private val tvPosition: TextView = itemView.findViewById(R.id.tv_position)
        private val tvShift: TextView = itemView.findViewById(R.id.tv_shift)
        private val tvLocation: TextView = itemView.findViewById(R.id.tv_location)

        // Expanded view references
        private val expandedView: View = itemView.findViewById(R.id.expanded_view)
        private val ivJobImageExpanded: ImageView = itemView.findViewById(R.id.iv_job_image_expanded)
        private val tvEmployerExpanded: TextView = itemView.findViewById(R.id.tv_employer_expanded)
        private val tvLocationExpandedBadge: TextView = itemView.findViewById(R.id.tv_location_expanded_badge)
        private val tvPositionExpanded: TextView = itemView.findViewById(R.id.tv_position_expanded)
        private val tvRequirementsExpanded: TextView = itemView.findViewById(R.id.tv_requirements_expanded)
        private val tvDescriptionExpanded: TextView = itemView.findViewById(R.id.tv_description_expanded)
        private val btnApplyExpanded: Button = itemView.findViewById(R.id.btn_apply_expanded)

        fun bind(job: Job) {
            val isExpanded = expandedJobs.contains(job.id)

            // Bind collapsed view
            tvEmployer.text = job.employer
            tvLocationBadge.text = "Location: ${job.location}"
            tvPosition.text = "Position: ${job.position}"

            // UNIFORM FORMAT: Use TimeFormatUtils for shift display
            tvShift.text = formatShiftUniform(job)

            tvLocation.text = job.location
            tvTimePosted.text = getTimeAgo(job.postedAt)

            // Load image
            if (job.imageUrl.isNotEmpty()) {
                Picasso.get()
                    .load(job.imageUrl)
                    .placeholder(R.drawable.ic_upload)
                    .error(R.drawable.ic_upload)
                    .into(ivJobImage)
            } else {
                ivJobImage.setImageResource(R.drawable.ic_upload)
            }

            // Bind expanded view
            tvEmployerExpanded.text = job.employer
            tvLocationExpandedBadge.text = "Location: ${job.location}"
            tvPositionExpanded.text = "Position: ${job.position}"
            tvRequirementsExpanded.text = job.requirements.joinToString("\n") { "• $it" }
            tvDescriptionExpanded.text = job.description

            // Load image for expanded view
            if (job.imageUrl.isNotEmpty()) {
                Picasso.get()
                    .load(job.imageUrl)
                    .placeholder(R.drawable.ic_upload)
                    .error(R.drawable.ic_upload)
                    .into(ivJobImageExpanded)
            } else {
                ivJobImageExpanded.setImageResource(R.drawable.ic_upload)
            }

            btnApplyExpanded.setOnClickListener {
                onApplyClick(job)
            }

            // Update expansion state
            updateExpandedState(isExpanded)

            // Handle expand/collapse click
            itemView.setOnClickListener {
                toggleExpand(job.id, bindingAdapterPosition)
            }
        }

        /**
         * UNIFORM FORMAT: Format shift using TimeFormatUtils
         */
        private fun formatShiftUniform(job: Job): String {
            return try {
                // Get day from workDays or default to Monday
                val day = if (job.workDays.isNotEmpty()) {
                    job.workDays[0]
                } else {
                    "Monday"
                }

                // Use TimeFormatUtils for consistent formatting
                TimeFormatUtils.formatShiftDisplay(day, job.shiftStart, job.shiftEnd)
            } catch (e: Exception) {
                // Fallback to raw shift string
                "Shift: ${job.shift}"
            }
        }

        private fun getTimeAgo(timestamp: Long): String {
            if (timestamp == 0L) return "Recently posted"

            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "$hours ${if (hours == 1L) "hour" else "hours"} ago"
                }
                diff < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "$days ${if (days == 1L) "day" else "days"} ago"
                }
                else -> {
                    val format = SimpleDateFormat("MMM dd", Locale.getDefault())
                    format.format(Date(timestamp))
                }
            }
        }

        private fun updateExpandedState(isExpanded: Boolean) {
            expandedView.visibility = if (isExpanded) View.VISIBLE else View.GONE
            collapsedView.visibility = if (isExpanded) View.GONE else View.VISIBLE
        }

        private fun toggleExpand(jobId: String, position: Int) {
            if (expandedJobs.contains(jobId)) {
                expandedJobs.remove(jobId)
            } else {
                expandedJobs.add(jobId)
            }
            notifyItemChanged(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job_expandable, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bind(jobs[position])
    }

    override fun getItemCount() = jobs.size

    fun updateJobs(newJobs: List<Job>) {
        jobs = newJobs
        expandedJobs.clear()
        notifyDataSetChanged()
    }
}
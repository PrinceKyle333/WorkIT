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
import com.squareup.picasso.Picasso

data class ExpandableJob(
    val job: Job,
    var isExpanded: Boolean = false
)

class ExpandableJobAdapter(
    private var jobs: List<Job>,
    private val onJobClick: (Job) -> Unit,
    private val onApplyClick: (Job) -> Unit
) : RecyclerView.Adapter<ExpandableJobAdapter.JobViewHolder>() {

    private val expandedJobs = mutableSetOf<String>()

    inner class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Collapsed view references
        private val collapsedView: View = itemView.findViewById(R.id.collapsed_view)
        private val tvEmployer: TextView = itemView.findViewById(R.id.tv_employer)
        private val tvPosition: TextView = itemView.findViewById(R.id.tv_position)
        private val tvLocation: TextView = itemView.findViewById(R.id.tv_location)
        private val tvShift: TextView = itemView.findViewById(R.id.tv_shift)
        private val ivJobImage: ImageView? = itemView.findViewById(R.id.iv_job_image)
        private val expandIndicator: ImageView? = itemView.findViewById(R.id.iv_expand_indicator)

        // Expanded view references
        private val expandedView: View? = itemView.findViewById(R.id.expanded_view)
        private val tvDescriptionPreview: TextView? = itemView.findViewById(R.id.tv_description_preview)
        private val tvPositionExpanded: TextView? = itemView.findViewById(R.id.tv_position_expanded)
        private val tvLocationFull: TextView? = itemView.findViewById(R.id.tv_location_full)
        private val tvDescriptionFull: TextView? = itemView.findViewById(R.id.tv_description_full)
        private val tvRequirementsFull: TextView? = itemView.findViewById(R.id.tv_requirements_full)
        private val tvShiftDetails: TextView? = itemView.findViewById(R.id.tv_shift_details)
        private val btnApplyExpanded: Button? = itemView.findViewById(R.id.btn_apply_expanded)

        fun bind(job: Job) {
            val isExpanded = expandedJobs.contains(job.id)

            // Bind collapsed view
            tvEmployer.text = job.employer
            tvPosition.text = job.position
            tvLocation.text = job.location
            tvShift.text = job.shift

            if (job.imageUrl.isNotEmpty()) {
                ivJobImage?.visibility = View.VISIBLE
                Picasso.get()
                    .load(job.imageUrl)
                    .placeholder(R.drawable.ic_upload)
                    .error(R.drawable.ic_upload)
                    .into(ivJobImage)
            } else {
                ivJobImage?.visibility = View.GONE
            }

            // Bind expanded view if it exists
            expandedView?.let {
                tvDescriptionPreview?.text = job.description.take(100) + "..."
                tvPositionExpanded?.text = job.position
                tvLocationFull?.text = job.location
                tvDescriptionFull?.text = job.description
                tvRequirementsFull?.text = job.requirements.joinToString("\n") { "• $it" }
                tvShiftDetails?.text = "Shift: ${job.shift}"

                btnApplyExpanded?.setOnClickListener {
                    onApplyClick(job)
                }
            }

            // Update expansion state
            updateExpandedState(isExpanded)

            // Handle expand/collapse click
            itemView.setOnClickListener {
                toggleExpand(job.id, bindingAdapterPosition)
            }
        }

        private fun updateExpandedState(isExpanded: Boolean) {
            expandedView?.visibility = if (isExpanded) View.VISIBLE else View.GONE
            collapsedView.visibility = if (isExpanded) View.GONE else View.VISIBLE
            expandIndicator?.rotation = if (isExpanded) 180f else 0f
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
package com.workit.workit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.workit.workit.R
import com.workit.workit.data.Job
import com.squareup.picasso.Picasso

class JobAdapter(
    private var jobs: List<Job>,
    private val onJobClick: (Job) -> Unit
) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {

    inner class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEmployer: TextView = itemView.findViewById(R.id.tv_employer)
        val tvPosition: TextView = itemView.findViewById(R.id.tv_position)
        val tvLocation: TextView = itemView.findViewById(R.id.tv_location)
        val tvShift: TextView = itemView.findViewById(R.id.tv_shift)
        val ivJobImage: ImageView = itemView.findViewById(R.id.iv_job_image)

        fun bind(job: Job) {
            tvEmployer.text = job.employer
            tvPosition.text = job.position
            tvLocation.text = job.location
            tvShift.text = job.shift

            if (job.imageUrl.isNotEmpty()) {
                Picasso.get()
                    .load(job.imageUrl)
                    .placeholder(R.drawable.ic_upload)
                    .error(R.drawable.ic_upload)
                    .into(ivJobImage)
            }

            itemView.setOnClickListener {
                onJobClick(job)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bind(jobs[position])
    }

    override fun getItemCount() = jobs.size

    fun updateJobs(newJobs: List<Job>) {
        jobs = newJobs
        notifyDataSetChanged()
    }
}
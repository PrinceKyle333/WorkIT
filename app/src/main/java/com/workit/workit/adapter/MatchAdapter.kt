package com.workit.workit.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.workit.workit.R
import com.workit.workit.data.Match

class MatchAdapter(
    private var matches: List<Match>,
    private val onStatusChange: ((Match, String) -> Unit)? = null
) : RecyclerView.Adapter<MatchAdapter.MatchViewHolder>() {

    inner class MatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvEmployerName: TextView = itemView.findViewById(R.id.tv_employer_name)
        val tvJobPosition: TextView = itemView.findViewById(R.id.tv_job_position)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val btnAccept: Button? = itemView.findViewById(R.id.btn_accept)
        val btnReject: Button? = itemView.findViewById(R.id.btn_reject)

        fun bind(match: Match) {
            tvEmployerName.text = match.employerName
            tvJobPosition.text = match.position

            // Set status text and color
            setStatusDisplay(match.status)

            // Handle accept button
            btnAccept?.setOnClickListener {
                onStatusChange?.invoke(match, "accepted")
                setStatusDisplay("accepted")
            }

            // Handle reject button
            btnReject?.setOnClickListener {
                onStatusChange?.invoke(match, "rejected")
                setStatusDisplay("rejected")
            }
        }

        private fun setStatusDisplay(status: String) {
            when (status) {
                "pending" -> {
                    tvStatus.text = "Status: Pending"
                    tvStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.orange)
                    )
                    // Show buttons for pending status
                    btnAccept?.visibility = View.VISIBLE
                    btnReject?.visibility = View.VISIBLE
                }
                "accepted" -> {
                    tvStatus.text = "Status: Accepted ✓"
                    tvStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.green)
                    )
                    // Hide buttons after acceptance
                    btnAccept?.visibility = View.GONE
                    btnReject?.visibility = View.GONE
                }
                "rejected" -> {
                    tvStatus.text = "Status: Rejected ✗"
                    tvStatus.setTextColor(
                        ContextCompat.getColor(itemView.context, R.color.red)
                    )
                    // Hide buttons after rejection
                    btnAccept?.visibility = View.GONE
                    btnReject?.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatchViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match, parent, false)
        return MatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: MatchViewHolder, position: Int) {
        holder.bind(matches[position])
    }

    override fun getItemCount() = matches.size

    fun updateMatches(newMatches: List<Match>) {
        matches = newMatches
        notifyDataSetChanged()
    }
}

package com.refeast.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.refeast.app.data.Reservation
import com.refeast.app.util.StatusColors
import com.refeast.app.util.TimeFormat

/**
 * Shows reservations as cards. Used by History, Tasks and the donor's listing screen.
 * When showRoute is true (volunteer tasks) the card shows the pickup time and the route.
 */
class ReservationAdapter(
    private val showRoute: Boolean,
    private val onItemClick: (Reservation) -> Unit
) : RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder>() {

    private val items = ArrayList<Reservation>()

    class ReservationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvHistoryTitle)
        val status: TextView = view.findViewById(R.id.tvHistoryStatus)
        val line1: TextView = view.findViewById(R.id.tvHistoryMeta)
        val line2: TextView = view.findViewById(R.id.tvHistorySub)
    }

    fun updateList(newItems: List<Reservation>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ReservationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
        val r = items[position]
        holder.title.text = r.itemTitle
        holder.status.text = r.status
        holder.status.setBackgroundResource(StatusColors.background(r.status))

        if (showRoute) {
            holder.line1.text = "Pickup ${TimeFormat.friendly(r.scheduledPickupTime)} " +
                "(${TimeFormat.timeLeft(r.scheduledPickupTime)}) · ${r.quantity} ${r.unit}"
            holder.line2.text = "From: ${r.donorName}, ${r.pickupLocation}\nTo: ${r.recipientName}, ${r.dropoffLocation}"
        } else {
            holder.line1.text = "#${r.code()} · ${r.quantity} ${r.unit} · ${TimeFormat.friendly(r.updatedAt)}"
            if (!r.needsVolunteer) {
                holder.line2.text = "${r.donorName} → ${r.recipientName} (self-collect)"
            } else if (r.volunteerName != null) {
                holder.line2.text = "${r.donorName} → ${r.recipientName} via ${r.volunteerName}"
            } else {
                holder.line2.text = "${r.donorName} → ${r.recipientName} (waiting for volunteer)"
            }
        }

        holder.itemView.setOnClickListener { onItemClick(r) }
    }

    override fun getItemCount(): Int = items.size
}

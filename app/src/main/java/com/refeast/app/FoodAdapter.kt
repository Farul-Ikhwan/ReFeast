package com.refeast.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.refeast.app.data.FoodListing
import com.refeast.app.data.ListingStatus
import com.refeast.app.util.ImageStore
import com.refeast.app.util.StatusColors
import com.refeast.app.util.TimeFormat

/** Shows food listings as cards in a RecyclerView. */
class FoodAdapter(private val onItemClick: (FoodListing) -> Unit) :
    RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    private val items = ArrayList<FoodListing>()

    class FoodViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivItemImage)
        val title: TextView = view.findViewById(R.id.tvItemTitle)
        val status: TextView = view.findViewById(R.id.tvItemStatus)
        val category: TextView = view.findViewById(R.id.tvItemCategory)
        val quantity: TextView = view.findViewById(R.id.tvItemQty)
        val expiry: TextView = view.findViewById(R.id.tvItemExpiry)
    }

    /** Replaces the list and redraws it. */
    fun updateList(newItems: List<FoodListing>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_food_card, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val item = items[position]
        val status = item.displayStatus()

        holder.title.text = item.title
        holder.category.text = "${item.category} · ${item.donorName}"
        holder.quantity.text = "${item.quantityAvailable} of ${item.quantity} ${item.unit} left · ${item.area}"
        holder.expiry.text = "Pickup by " + TimeFormat.friendly(item.pickupDeadline)

        // The status label is only shown when the food is not simply "Available".
        if (status == ListingStatus.AVAILABLE) {
            holder.status.visibility = View.GONE
        } else {
            holder.status.visibility = View.VISIBLE
            holder.status.text = status
            holder.status.setBackgroundResource(StatusColors.background(status))
        }

        holder.image.clipToOutline = true
        ImageStore.show(holder.image, item.imagePath, item.category)

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size
}

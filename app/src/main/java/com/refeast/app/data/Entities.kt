package com.refeast.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// ---------- Fixed values used across the app ----------

object Roles {
    const val DONOR = "Donor"
    const val RECIPIENT = "Recipient"
    const val VOLUNTEER = "Volunteer Collector"
}

object ListingStatus {
    const val AVAILABLE = "Available"
    const val FULLY_RESERVED = "Fully Reserved"
    const val COMPLETED = "Completed"
    const val WITHDRAWN = "Withdrawn"
    const val EXPIRED = "Expired" // not saved, only shown when the deadline has passed
}

object ReservationStatus {
    const val RESERVED = "Reserved"
    const val IN_TRANSIT = "In-Transit"
    const val DELIVERED = "Delivered"
    const val CANCELLED = "Cancelled"
}

object Categories {
    // Full names are saved in the database.
    val ALL = listOf("Cooked Meals", "Fresh Produce", "Baked Goods", "Dry / Pantry", "Beverages")

    // Short names are shown on the filter chips (same order as ALL).
    val SHORT = listOf("Cooked", "Produce", "Baked", "Pantry", "Drinks")
}

object Areas {
    val ALL = listOf(
        "Kuala Lumpur", "Petaling Jaya", "Shah Alam", "Subang Jaya", "Bandar Sunway",
        "Puchong", "Cheras", "Ampang", "Klang", "Cyberjaya", "Putrajaya"
    )
}

// ---------- Database tables ----------

/** Table "users": one row per account. */
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: String,
    val phone: String,
    val address: String,
    val area: String,
    val createdAt: Long = System.currentTimeMillis()
)

/** Table "food_listings": surplus food posted by a donor. */
@Entity(tableName = "food_listings")
data class FoodListing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val donorId: Long,
    val donorName: String,
    val title: String,
    val description: String,
    val category: String,
    val quantity: Int,
    val quantityAvailable: Int,
    val unit: String,
    val pickupLocation: String,
    val area: String,
    val pickupDeadline: Long,
    val imagePath: String? = null,
    val status: String = ListingStatus.AVAILABLE,
    val createdAt: Long = System.currentTimeMillis()
) {
    /** True when the food can still be reserved. */
    fun canBeReserved(): Boolean {
        val now = System.currentTimeMillis()
        return status == ListingStatus.AVAILABLE && quantityAvailable > 0 && pickupDeadline > now
    }

    /** The status to show on screen. An Available listing past its deadline shows as Expired. */
    fun displayStatus(): String {
        if (status == ListingStatus.AVAILABLE && pickupDeadline <= System.currentTimeMillis()) {
            return ListingStatus.EXPIRED
        }
        return status
    }
}

/** Table "reservations": food reserved by a recipient, and who delivers it. */
@Entity(tableName = "reservations")
data class Reservation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val itemTitle: String,
    val donorId: Long,
    val donorName: String,
    val pickupLocation: String,
    val recipientId: Long,
    val recipientName: String,
    val recipientPhone: String,
    val dropoffLocation: String,
    val quantity: Int,
    val unit: String,
    val needsVolunteer: Boolean,
    val volunteerId: Long? = null,
    val volunteerName: String? = null,
    val scheduledPickupTime: Long,
    val status: String = ReservationStatus.RESERVED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Short code shown to users, e.g. R-1007. */
    fun code(): String = "R-" + (1000 + id)

    /** Reserved or In-Transit means the reservation is still going on. */
    fun isActive(): Boolean = status == ReservationStatus.RESERVED || status == ReservationStatus.IN_TRANSIT
}

/** Table "status_logs": one row every time a reservation changes (the timeline). */
@Entity(tableName = "status_logs")
data class StatusLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reservationId: Long,
    val status: String,
    val note: String,
    val changedBy: String,
    val timestamp: Long = System.currentTimeMillis()
)

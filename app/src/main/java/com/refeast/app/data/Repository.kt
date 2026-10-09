package com.refeast.app.data

import android.content.Context
import com.refeast.app.util.TimeFormat
import java.security.MessageDigest

/**
 * All the app's rules are in this one class. Screens call these functions instead of
 * talking to the database directly.
 *
 * Functions that can fail return a String:
 *   null   = it worked
 *   a text = the error message to show the user
 */
class Repository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val userDao = db.userDao()
    private val foodDao = db.foodDao()
    private val reservationDao = db.reservationDao()

    // ======================= Users =======================

    fun register(name: String, email: String, password: String, role: String,
                 phone: String, address: String, area: String): String? {
        val cleanEmail = email.trim().lowercase()
        if (userDao.findByEmail(cleanEmail) != null) {
            return "An account with this email already exists"
        }
        val user = User(
            name = name.trim(),
            email = cleanEmail,
            passwordHash = hashPassword(cleanEmail, password),
            role = role,
            phone = phone.trim(),
            address = address.trim(),
            area = area
        )
        userDao.insert(user)
        return null
    }

    /** Returns the user if the email and password are correct, otherwise null. */
    fun login(email: String, password: String): User? {
        val cleanEmail = email.trim().lowercase()
        val user = userDao.findByEmail(cleanEmail) ?: return null
        if (user.passwordHash != hashPassword(cleanEmail, password)) {
            return null
        }
        return user
    }

    fun findUserByEmail(email: String): User? = userDao.findByEmail(email.trim().lowercase())

    fun getUser(id: Long): User? = userDao.findById(id)

    fun updateContact(user: User, phone: String, address: String, area: String) {
        userDao.update(user.copy(phone = phone.trim(), address = address.trim(), area = area))
    }

    // ======================= Food listings =======================

    fun publishListing(listing: FoodListing) {
        foodDao.insert(listing)
    }

    fun getAvailableListings(): List<FoodListing> = foodDao.getAvailable(System.currentTimeMillis())

    fun getDonorListings(donorId: Long): List<FoodListing> = foodDao.getByDonor(donorId)

    fun getListing(id: Long): FoodListing? = foodDao.findById(id)

    fun withdrawListing(donor: User, listing: FoodListing): String? {
        if (listing.donorId != donor.id) {
            return "You can only withdraw your own listings"
        }
        val activeCount = reservationDao.getForItem(listing.id).count { it.isActive() }
        if (activeCount > 0) {
            return "This listing has active reservations, so it cannot be withdrawn"
        }
        foodDao.update(listing.copy(status = ListingStatus.WITHDRAWN))
        return null
    }

    // ======================= Reservations =======================

    /**
     * Reserve food. All checks are done first; only if they all pass do we save.
     * Because our queries run one after another, nobody else can reserve the same
     * food between the check and the save.
     */
    fun reserve(recipient: User, listingId: Long, quantity: Int, needsVolunteer: Boolean,
                dropoffLocation: String, pickupTime: Long): String? {
        // Always read the latest copy of the listing from the database.
        val listing = foodDao.findById(listingId) ?: return "Listing not found"

        if (!listing.canBeReserved()) {
            return "Sorry, this item is no longer available"
        }
        if (quantity < 1 || quantity > listing.quantityAvailable) {
            return "Only ${listing.quantityAvailable} ${listing.unit} left"
        }
        if (pickupTime > listing.pickupDeadline) {
            return "Pickup must be before the deadline"
        }
        // Stop the same recipient reserving the same item twice.
        val alreadyReserved = reservationDao.getForItem(listingId)
            .any { it.recipientId == recipient.id && it.isActive() }
        if (alreadyReserved) {
            return "You already have an active reservation for this item"
        }

        // 1. Lock the quantity by taking it away from the listing.
        val newQuantity = listing.quantityAvailable - quantity
        val newStatus = if (newQuantity == 0) ListingStatus.FULLY_RESERVED else listing.status
        foodDao.update(listing.copy(quantityAvailable = newQuantity, status = newStatus))

        // 2. Save the reservation.
        val reservation = Reservation(
            itemId = listing.id,
            itemTitle = listing.title,
            donorId = listing.donorId,
            donorName = listing.donorName,
            pickupLocation = listing.pickupLocation,
            recipientId = recipient.id,
            recipientName = recipient.name,
            recipientPhone = recipient.phone,
            dropoffLocation = dropoffLocation.trim(),
            quantity = quantity,
            unit = listing.unit,
            needsVolunteer = needsVolunteer,
            scheduledPickupTime = pickupTime
        )
        val reservationId = reservationDao.insert(reservation)

        // 3. Write the first line of the timeline.
        val note = if (needsVolunteer) {
            "Reserved $quantity ${listing.unit}. Waiting for a volunteer collector."
        } else {
            "Reserved $quantity ${listing.unit}. Recipient will collect."
        }
        addLog(reservationId, ReservationStatus.RESERVED, note, recipient.name)
        return null
    }

    fun getLatestReservation(recipientId: Long): Reservation? = reservationDao.getLatestForRecipient(recipientId)

    fun getReservation(id: Long): Reservation? = reservationDao.findById(id)

    fun getLogs(reservationId: Long): List<StatusLog> = reservationDao.getLogs(reservationId)

    fun getReservationsForItem(itemId: Long): List<Reservation> = reservationDao.getForItem(itemId)

    fun getOpenTasks(): List<Reservation> = reservationDao.getOpenTasks()

    /** History list for any role. */
    fun getHistory(user: User): List<Reservation> {
        return when (user.role) {
            Roles.DONOR -> reservationDao.getForDonor(user.id)
            Roles.RECIPIENT -> reservationDao.getForRecipient(user.id)
            else -> reservationDao.getForVolunteer(user.id)
        }
    }

    fun acceptTask(volunteer: User, reservation: Reservation, pickupTime: Long): String? {
        // Read the latest copy in case another volunteer accepted it already.
        val latest = reservationDao.findById(reservation.id) ?: return "Task not found"
        if (latest.volunteerId != null) {
            return "Another volunteer already accepted this task"
        }
        val listing = foodDao.findById(latest.itemId) ?: return "Listing not found"
        if (pickupTime > listing.pickupDeadline) {
            return "Pickup must be before the deadline"
        }
        reservationDao.update(
            latest.copy(
                volunteerId = volunteer.id,
                volunteerName = volunteer.name,
                scheduledPickupTime = pickupTime,
                updatedAt = System.currentTimeMillis()
            )
        )
        addLog(latest.id, latest.status, "Accepted by volunteer ${volunteer.name}", volunteer.name)
        return null
    }

    fun reschedulePickup(user: User, reservation: Reservation, pickupTime: Long): String? {
        if (reservation.status != ReservationStatus.RESERVED) {
            return "Pickup time can only be changed before pickup"
        }
        val listing = foodDao.findById(reservation.itemId) ?: return "Listing not found"
        if (pickupTime > listing.pickupDeadline) {
            return "Pickup must be before the deadline"
        }
        reservationDao.update(reservation.copy(scheduledPickupTime = pickupTime, updatedAt = System.currentTimeMillis()))
        addLog(reservation.id, reservation.status, "Pickup rescheduled to ${TimeFormat.friendly(pickupTime)}", user.name)
        return null
    }

    /** Volunteer has collected the food from the donor. */
    fun markInTransit(volunteer: User, reservation: Reservation): String? {
        if (reservation.volunteerId != volunteer.id) {
            return "Only the assigned volunteer can update this task"
        }
        if (reservation.status != ReservationStatus.RESERVED) {
            return "This reservation is already ${reservation.status}"
        }
        changeStatus(reservation, ReservationStatus.IN_TRANSIT,
            "Picked up from ${reservation.donorName}. On the way.", volunteer.name)
        return null
    }

    /** Volunteer delivered the food, or the recipient collected it themselves. */
    fun markDelivered(user: User, reservation: Reservation): String? {
        val note: String
        if (reservation.needsVolunteer) {
            if (reservation.volunteerId != user.id) return "Only the assigned volunteer can do this"
            if (reservation.status != ReservationStatus.IN_TRANSIT) return "Mark the food as picked up first"
            note = "Delivered to ${reservation.recipientName}"
        } else {
            if (reservation.recipientId != user.id) return "Only the recipient can do this"
            if (reservation.status != ReservationStatus.RESERVED) return "This reservation is already ${reservation.status}"
            note = "Collected by recipient"
        }
        changeStatus(reservation, ReservationStatus.DELIVERED, note, user.name)

        // If all the food is gone and nothing is still active, the listing is completed.
        val listing = foodDao.findById(reservation.itemId)
        if (listing != null && listing.quantityAvailable == 0) {
            val stillActive = reservationDao.getForItem(listing.id).any { it.isActive() }
            if (!stillActive) {
                foodDao.update(listing.copy(status = ListingStatus.COMPLETED))
            }
        }
        return null
    }

    /** Recipient cancels before pickup. The food goes back to the listing. */
    fun cancelReservation(recipient: User, reservation: Reservation): String? {
        if (reservation.recipientId != recipient.id) {
            return "You can only cancel your own reservations"
        }
        if (reservation.status != ReservationStatus.RESERVED) {
            return "Food that is already picked up cannot be cancelled"
        }
        changeStatus(reservation, ReservationStatus.CANCELLED, "Cancelled by recipient", recipient.name)

        // Give the quantity back to the listing.
        val listing = foodDao.findById(reservation.itemId)
        if (listing != null) {
            var status = listing.status
            if (status == ListingStatus.FULLY_RESERVED) status = ListingStatus.AVAILABLE
            foodDao.update(listing.copy(quantityAvailable = listing.quantityAvailable + reservation.quantity, status = status))
        }
        return null
    }

    // ======================= Profile numbers =======================

    /** Returns two numbers for the profile screen: total, and how many were delivered. */
    fun getStats(user: User): IntArray {
        val history = getHistory(user)
        val delivered = history.count { it.status == ReservationStatus.DELIVERED }
        val total = if (user.role == Roles.DONOR) foodDao.getByDonor(user.id).size else history.size
        return intArrayOf(total, delivered)
    }

    // ======================= Helpers =======================

    private fun changeStatus(reservation: Reservation, newStatus: String, note: String, changedBy: String) {
        reservationDao.update(reservation.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
        addLog(reservation.id, newStatus, note, changedBy)
    }

    private fun addLog(reservationId: Long, status: String, note: String, changedBy: String) {
        reservationDao.insertLog(StatusLog(reservationId = reservationId, status = status, note = note, changedBy = changedBy))
    }

    /** Puts demo data in the database the first time the app runs. */
    fun addDemoDataIfEmpty() {
        if (userDao.count() == 0) {
            SeedData.insert(db)
        }
    }

    companion object {
        /** Passwords are never saved as plain text, only as a SHA-256 hash. */
        fun hashPassword(email: String, password: String): String {
            val bytes = MessageDigest.getInstance("SHA-256")
                .digest(("refeast:" + email.lowercase() + ":" + password).toByteArray())
            val result = StringBuilder()
            for (b in bytes) {
                result.append(String.format("%02x", b))
            }
            return result.toString()
        }
    }
}

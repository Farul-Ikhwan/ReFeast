package com.refeast.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.refeast.app.data.FoodListing
import com.refeast.app.data.ListingStatus
import com.refeast.app.data.Repository
import com.refeast.app.data.Roles
import com.refeast.app.data.User
import com.refeast.app.util.DateTimeHelper
import com.refeast.app.util.ImageStore
import com.refeast.app.util.Session
import com.refeast.app.util.StatusColors
import com.refeast.app.util.TimeFormat
import com.refeast.app.util.Validators

/** Listing details. Recipients reserve food here; donors can see reservations and withdraw. */
class FoodDetailActivity : AppCompatActivity() {

    private lateinit var repo: Repository
    private lateinit var user: User
    private var itemId: Long = -1
    private var listing: FoodListing? = null
    private lateinit var reservationAdapter: ReservationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_detail)

        repo = Repository(this)
        val currentUser = repo.getUser(Session(this).getUserId())
        if (currentUser == null) {
            finish()
            return
        }
        user = currentUser
        itemId = intent.getLongExtra("itemId", -1)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.ivImage).clipToOutline = true

        // Donors see who reserved their food. Tapping a row opens that reservation.
        reservationAdapter = ReservationAdapter(false) { reservation -> openReservation(reservation.id) }
        val rv = findViewById<RecyclerView>(R.id.rvReservations)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = reservationAdapter

        findViewById<TextView>(R.id.btnReserve).setOnClickListener {
            val current = listing
            if (current != null) showReserveDialog(current)
        }
        findViewById<TextView>(R.id.btnWithdraw).setOnClickListener { confirmWithdraw() }
    }

    override fun onResume() {
        super.onResume()
        loadListing()
    }

    /** Reads the listing from the database and fills the screen. */
    private fun loadListing() {
        val item = repo.getListing(itemId)
        if (item == null) {
            Toast.makeText(this, "This listing no longer exists", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        listing = item
        val status = item.displayStatus()

        ImageStore.show(findViewById(R.id.ivImage), item.imagePath, item.category)
        findViewById<TextView>(R.id.tvTitle).text = item.title
        findViewById<TextView>(R.id.tvDonor).text = "Donated by ${item.donorName}"
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        tvStatus.text = status
        tvStatus.setBackgroundResource(StatusColors.background(status))
        findViewById<TextView>(R.id.tvQuantity).text = "${item.quantityAvailable} of ${item.quantity} ${item.unit} left"
        findViewById<TextView>(R.id.tvCategory).text = item.category
        findViewById<TextView>(R.id.tvLocation).text = "${item.pickupLocation}, ${item.area}"
        findViewById<TextView>(R.id.tvExpiry).text =
            TimeFormat.friendly(item.pickupDeadline) + " (" + TimeFormat.timeLeft(item.pickupDeadline) + ")"
        findViewById<TextView>(R.id.tvDescription).text =
            if (item.description.isEmpty()) "No description provided." else item.description

        // Which buttons to show depends on the user's role.
        val isOwner = user.role == Roles.DONOR && item.donorId == user.id
        val btnReserve = findViewById<TextView>(R.id.btnReserve)
        val btnWithdraw = findViewById<TextView>(R.id.btnWithdraw)
        val tvInfo = findViewById<TextView>(R.id.tvInfo)

        btnReserve.visibility = if (user.role == Roles.RECIPIENT) View.VISIBLE else View.GONE
        btnReserve.isEnabled = item.canBeReserved()
        btnReserve.text = if (item.canBeReserved()) "Reserve This Item" else "Not Available"

        val canWithdraw = isOwner && (item.status == ListingStatus.AVAILABLE || item.status == ListingStatus.FULLY_RESERVED)
        btnWithdraw.visibility = if (canWithdraw) View.VISIBLE else View.GONE

        var info = ""
        if (user.role == Roles.VOLUNTEER) {
            info = "Volunteer collectors pick up food from the Tasks tab after a recipient reserves it."
        } else if (user.role == Roles.DONOR && !isOwner) {
            info = "You are viewing another donor's listing."
        } else if (user.role == Roles.RECIPIENT && !item.canBeReserved()) {
            info = "This food can no longer be reserved."
        }
        tvInfo.text = info
        tvInfo.visibility = if (info.isEmpty()) View.GONE else View.VISIBLE

        // Reservations list (donor only).
        val tvHeader = findViewById<TextView>(R.id.tvReservationsHeader)
        if (isOwner) {
            val reservations = repo.getReservationsForItem(item.id)
            tvHeader.visibility = View.VISIBLE
            tvHeader.text = if (reservations.isEmpty()) "No reservations yet" else "Reservations (${reservations.size})"
            reservationAdapter.updateList(reservations)
        } else {
            tvHeader.visibility = View.GONE
            reservationAdapter.updateList(ArrayList())
        }
    }

    private fun confirmWithdraw() {
        val item = listing ?: return
        AlertDialog.Builder(this)
            .setTitle("Withdraw listing?")
            .setMessage("\"${item.title}\" will no longer be visible to recipients.")
            .setPositiveButton("Withdraw") { _, _ ->
                val error = repo.withdrawListing(user, item)
                if (error != null) {
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Listing withdrawn", Toast.LENGTH_SHORT).show()
                    loadListing()
                }
            }
            .setNegativeButton("Keep", null)
            .show()
    }

    /** The Reserve dialog: quantity, pickup time, and whether a volunteer should deliver. */
    private fun showReserveDialog(item: FoodListing) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_reserve, null)
        val etQuantity = view.findViewById<EditText>(R.id.etQuantity)
        val tvPickup = view.findViewById<TextView>(R.id.tvPickupTime)
        val cbVolunteer = view.findViewById<CheckBox>(R.id.cbNeedVolunteer)
        val etDropoff = view.findViewById<EditText>(R.id.etDropoff)

        view.findViewById<TextView>(R.id.tvAvailable).text =
            "${item.quantityAvailable} ${item.unit} available. Collect before ${TimeFormat.friendly(item.pickupDeadline)}."
        etQuantity.setText(item.quantityAvailable.toString())
        etDropoff.setText("${user.address}, ${user.area}")

        var pickupTime: Long = 0
        tvPickup.setOnClickListener {
            val halfHourFromNow = System.currentTimeMillis() + 30 * 60 * 1000L
            DateTimeHelper.pick(this, halfHourFromNow, item.pickupDeadline) { chosen ->
                pickupTime = chosen
                tvPickup.error = null
                tvPickup.text = TimeFormat.friendly(chosen)
                tvPickup.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            }
        }

        // The drop-off address can only be edited when delivery is needed.
        cbVolunteer.setOnCheckedChangeListener { _, isChecked -> etDropoff.isEnabled = isChecked }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Reserve ${item.title}")
            .setView(view)
            .setPositiveButton("Reserve", null) // set below so the dialog stays open when there is an error
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                // 1. Check the inputs.
                val quantityError = Validators.quantity(etQuantity.text.toString(), item.quantityAvailable)
                val pickupError = if (pickupTime == 0L) null else Validators.pickupTimeError(pickupTime, item.pickupDeadline)
                etQuantity.error = quantityError
                if (quantityError != null) return@setOnClickListener
                if (pickupTime == 0L) {
                    Toast.makeText(this, "Choose a pickup time", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (pickupError != null) {
                    Toast.makeText(this, pickupError, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (cbVolunteer.isChecked && etDropoff.text.toString().isBlank()) {
                    etDropoff.error = "Drop-off address is required"
                    return@setOnClickListener
                }

                // 2. Save the reservation (the Repository does the final checks).
                val error = repo.reserve(
                    user, item.id, etQuantity.text.toString().trim().toInt(), cbVolunteer.isChecked,
                    if (cbVolunteer.isChecked) etDropoff.text.toString() else "", pickupTime
                )
                if (error != null) {
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                // 3. Show the new reservation.
                dialog.dismiss()
                Toast.makeText(this, "Reserved! The food is held for you.", Toast.LENGTH_SHORT).show()
                val newReservation = repo.getLatestReservation(user.id)
                if (newReservation != null) openReservation(newReservation.id)
            }
        }
        dialog.show()
    }

    private fun openReservation(id: Long) {
        val intent = Intent(this, ReservationDetailActivity::class.java)
        intent.putExtra("reservationId", id)
        startActivity(intent)
    }
}

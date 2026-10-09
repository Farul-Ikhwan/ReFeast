package com.refeast.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.refeast.app.data.Repository
import com.refeast.app.data.Reservation
import com.refeast.app.data.ReservationStatus
import com.refeast.app.data.Roles
import com.refeast.app.data.User
import com.refeast.app.util.DateTimeHelper
import com.refeast.app.util.Session
import com.refeast.app.util.StatusColors
import com.refeast.app.util.TimeFormat

/**
 * One reservation: Pickup Scheduling and Collection-Status Update, plus the timeline.
 * The buttons shown depend on the user's role and the reservation's status.
 */
class ReservationDetailActivity : AppCompatActivity() {

    private lateinit var repo: Repository
    private lateinit var user: User
    private var reservationId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation_detail)

        repo = Repository(this)
        val currentUser = repo.getUser(Session(this).getUserId())
        if (currentUser == null) {
            finish()
            return
        }
        user = currentUser
        reservationId = intent.getLongExtra("reservationId", -1)

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadReservation()
    }

    private fun loadReservation() {
        val r = repo.getReservation(reservationId)
        if (r == null) {
            Toast.makeText(this, "Reservation not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.tvHeader).text = "Reservation #${r.code()}"
        findViewById<TextView>(R.id.tvItemTitle).text = r.itemTitle
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        tvStatus.text = r.status
        tvStatus.setBackgroundResource(StatusColors.background(r.status))
        findViewById<TextView>(R.id.tvQuantity).text = "${r.quantity} ${r.unit}"
        findViewById<TextView>(R.id.tvPickupFrom).text = "${r.donorName}\n${r.pickupLocation}"
        var scheduled = TimeFormat.friendly(r.scheduledPickupTime)
        if (r.isActive()) scheduled += " (" + TimeFormat.timeLeft(r.scheduledPickupTime) + ")"
        findViewById<TextView>(R.id.tvScheduled).text = scheduled

        val labelDeliverTo = findViewById<TextView>(R.id.labelDeliverTo)
        val tvDeliverTo = findViewById<TextView>(R.id.tvDeliverTo)
        val tvVolunteer = findViewById<TextView>(R.id.tvVolunteer)
        if (r.needsVolunteer) {
            labelDeliverTo.text = "Deliver To"
            tvDeliverTo.text = "${r.recipientName}\n${r.dropoffLocation}\nTel: ${r.recipientPhone}"
            tvVolunteer.text = r.volunteerName ?: "Waiting for a volunteer to accept"
        } else {
            labelDeliverTo.text = "Collected By"
            tvDeliverTo.text = "${r.recipientName} (self-collect)\nTel: ${r.recipientPhone}"
            tvVolunteer.text = "Not needed"
        }

        showTimeline()
        showButtons(r)
    }

    /** Fills the "Status Timeline" box with one row per status change. */
    private fun showTimeline() {
        val container = findViewById<LinearLayout>(R.id.llTimeline)
        container.removeAllViews()
        for (log in repo.getLogs(reservationId)) {
            val row = LayoutInflater.from(this).inflate(R.layout.item_timeline, container, false)
            row.findViewById<TextView>(R.id.tvLogStatus).text = log.status
            row.findViewById<TextView>(R.id.tvLogNote).text = log.note
            row.findViewById<TextView>(R.id.tvLogTime).text = "${TimeFormat.friendly(log.timestamp)} · by ${log.changedBy}"
            row.findViewById<View>(R.id.dot).setBackgroundResource(StatusColors.background(log.status))
            container.addView(row)
        }
    }

    /** Decides which buttons this user can press right now. */
    private fun showButtons(r: Reservation) {
        val btnPrimary = findViewById<TextView>(R.id.btnPrimary)
        val btnSecondary = findViewById<TextView>(R.id.btnSecondary)
        val btnDanger = findViewById<TextView>(R.id.btnDanger)
        btnPrimary.visibility = View.GONE
        btnSecondary.visibility = View.GONE
        btnDanger.visibility = View.GONE

        val isReserved = r.status == ReservationStatus.RESERVED
        val isInTransit = r.status == ReservationStatus.IN_TRANSIT

        if (user.role == Roles.VOLUNTEER) {
            val isMyTask = r.volunteerId == user.id
            if (isReserved && r.needsVolunteer && r.volunteerId == null) {
                showButton(btnPrimary, "Accept Task") { acceptTask(r) }
            } else if (isMyTask && isReserved) {
                showButton(btnPrimary, "Mark as Picked Up (In-Transit)") {
                    confirm("Mark as picked up?") { showResult(repo.markInTransit(user, r), "Status updated") }
                }
                showButton(btnSecondary, "Reschedule Pickup") { reschedule(r) }
            } else if (isMyTask && isInTransit) {
                showButton(btnPrimary, "Mark as Delivered") {
                    confirm("Mark as delivered?") { showResult(repo.markDelivered(user, r), "Delivered. Thank you!") }
                }
            }
        }

        if (user.role == Roles.RECIPIENT && r.recipientId == user.id && isReserved) {
            if (!r.needsVolunteer) {
                showButton(btnPrimary, "Mark as Collected") {
                    confirm("Mark as collected?") { showResult(repo.markDelivered(user, r), "Marked as collected") }
                }
                showButton(btnSecondary, "Reschedule Pickup") { reschedule(r) }
            }
            showButton(btnDanger, "Cancel Reservation") {
                confirm("Cancel reservation?") { showResult(repo.cancelReservation(user, r), "Reservation cancelled") }
            }
        }
    }

    private fun showButton(button: TextView, label: String, action: () -> Unit) {
        button.text = label
        button.visibility = View.VISIBLE
        button.setOnClickListener { action() }
    }

    private fun acceptTask(r: Reservation) {
        val listing = repo.getListing(r.itemId) ?: return
        Toast.makeText(this, "Choose when you will pick up the food", Toast.LENGTH_SHORT).show()
        DateTimeHelper.pick(this, r.scheduledPickupTime, listing.pickupDeadline) { time ->
            showResult(repo.acceptTask(user, r, time), "Task accepted")
        }
    }

    private fun reschedule(r: Reservation) {
        val listing = repo.getListing(r.itemId) ?: return
        DateTimeHelper.pick(this, r.scheduledPickupTime, listing.pickupDeadline) { time ->
            if (time < System.currentTimeMillis()) {
                Toast.makeText(this, "Pickup time must be in the future", Toast.LENGTH_SHORT).show()
            } else {
                showResult(repo.reschedulePickup(user, r, time), "Pickup time changed")
            }
        }
    }

    private fun confirm(title: String, onYes: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setPositiveButton("Yes") { _, _ -> onYes() }
            .setNegativeButton("No", null)
            .show()
    }

    /** Shows the error if there was one, otherwise the success message, then reloads the screen. */
    private fun showResult(error: String?, successMessage: String) {
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
        }
        loadReservation()
    }
}

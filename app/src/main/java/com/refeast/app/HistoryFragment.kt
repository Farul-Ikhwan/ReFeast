package com.refeast.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.refeast.app.data.Repository
import com.refeast.app.data.Reservation
import com.refeast.app.data.ReservationStatus
import com.refeast.app.data.Roles
import com.refeast.app.data.User
import com.refeast.app.util.Session

/** History tab: Donation / Collection History for every role, with a status filter. */
class HistoryFragment : Fragment(R.layout.fragment_history) {

    private lateinit var repo: Repository
    private var user: User? = null
    private lateinit var adapter: ReservationAdapter
    private lateinit var tvEmpty: TextView
    private var selectedChip = R.id.chipAll

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = Repository(requireContext())
        user = repo.getUser(Session(requireContext()).getUserId())
        val currentUser = user ?: return
        tvEmpty = view.findViewById(R.id.tvEmpty)

        val header = view.findViewById<TextView>(R.id.tvHistoryHeader)
        if (currentUser.role == Roles.DONOR) {
            header.text = "Donation History"
        } else if (currentUser.role == Roles.RECIPIENT) {
            header.text = "My Reservations"
        } else {
            header.text = "Collection History"
        }

        adapter = ReservationAdapter(false) { reservation ->
            val intent = Intent(requireContext(), ReservationDetailActivity::class.java)
            intent.putExtra("reservationId", reservation.id)
            startActivity(intent)
        }
        val rv = view.findViewById<RecyclerView>(R.id.rvHistory)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        view.findViewById<ChipGroup>(R.id.chipGroupHistory).setOnCheckedStateChangeListener { _, checkedIds ->
            selectedChip = if (checkedIds.isEmpty()) R.id.chipAll else checkedIds[0]
            loadHistory()
        }
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        val currentUser = user ?: return
        val result = ArrayList<Reservation>()
        for (r in repo.getHistory(currentUser)) {
            val keep = when (selectedChip) {
                R.id.chipActive -> r.isActive()
                R.id.chipCompleted -> r.status == ReservationStatus.DELIVERED
                R.id.chipCancelled -> r.status == ReservationStatus.CANCELLED
                else -> true
            }
            if (keep) result.add(r)
        }
        adapter.updateList(result)
        tvEmpty.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
    }
}

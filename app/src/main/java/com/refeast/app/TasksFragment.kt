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
import com.refeast.app.data.User
import com.refeast.app.util.Session

/** Volunteer tab: open delivery jobs, and the jobs this volunteer is working on. */
class TasksFragment : Fragment(R.layout.fragment_tasks) {

    private lateinit var repo: Repository
    private var user: User? = null
    private lateinit var adapter: ReservationAdapter
    private lateinit var tvEmpty: TextView
    private var showMyTasks = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = Repository(requireContext())
        user = repo.getUser(Session(requireContext()).getUserId())
        tvEmpty = view.findViewById(R.id.tvEmpty)

        adapter = ReservationAdapter(true) { reservation ->
            val intent = Intent(requireContext(), ReservationDetailActivity::class.java)
            intent.putExtra("reservationId", reservation.id)
            startActivity(intent)
        }
        val rv = view.findViewById<RecyclerView>(R.id.rvTasks)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        view.findViewById<ChipGroup>(R.id.chipGroupTasks).setOnCheckedStateChangeListener { _, checkedIds ->
            showMyTasks = checkedIds.contains(R.id.chipMine)
            loadTasks()
        }
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun loadTasks() {
        val volunteer = user ?: return
        val list: List<Reservation>
        if (showMyTasks) {
            // My tasks that are still going on.
            list = repo.getHistory(volunteer).filter { it.isActive() }
            tvEmpty.text = "You have no active tasks. Accept one from Open tasks."
        } else {
            list = repo.getOpenTasks()
            tvEmpty.text = "No open delivery tasks right now. Check back soon!"
        }
        adapter.updateList(list)
        tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }
}

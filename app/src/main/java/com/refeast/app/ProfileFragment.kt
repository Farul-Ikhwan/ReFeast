package com.refeast.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.refeast.app.data.Areas
import com.refeast.app.data.Repository
import com.refeast.app.data.Roles
import com.refeast.app.data.User
import com.refeast.app.util.Session
import com.refeast.app.util.TimeFormat
import com.refeast.app.util.Validators

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private lateinit var repo: Repository
    private var user: User? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = Repository(requireContext())

        view.findViewById<TextView>(R.id.rowAccount).setOnClickListener { showAccountDetails() }
        view.findViewById<TextView>(R.id.rowContact).setOnClickListener { showEditContact() }
        view.findViewById<TextView>(R.id.rowAbout).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("About Re:Feast")
                .setMessage(R.string.about_text)
                .setPositiveButton("OK", null)
                .show()
        }
        view.findViewById<TextView>(R.id.btnLogout).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Log out?")
                .setPositiveButton("Log Out") { _, _ -> logout() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        val view = view ?: return
        val currentUser = repo.getUser(Session(requireContext()).getUserId()) ?: return
        user = currentUser

        view.findViewById<TextView>(R.id.tvProfileName).text = currentUser.name
        view.findViewById<TextView>(R.id.tvProfileRole).text = "${currentUser.role} · ${currentUser.area}"
        view.findViewById<TextView>(R.id.tvAvatar).text = currentUser.name.substring(0, 1).uppercase()

        // stats[0] = total, stats[1] = delivered
        val stats = repo.getStats(currentUser)
        view.findViewById<TextView>(R.id.tvStat1Value).text = stats[0].toString()
        view.findViewById<TextView>(R.id.tvStat2Value).text = stats[1].toString()

        val label1: String
        val label2: String
        if (currentUser.role == Roles.DONOR) {
            label1 = "Listings posted"; label2 = "Deliveries completed"
        } else if (currentUser.role == Roles.RECIPIENT) {
            label1 = "Reservations made"; label2 = "Food received"
        } else {
            label1 = "Tasks accepted"; label2 = "Deliveries made"
        }
        view.findViewById<TextView>(R.id.tvStat1Label).text = label1
        view.findViewById<TextView>(R.id.tvStat2Label).text = label2
    }

    private fun showAccountDetails() {
        val u = user ?: return
        val details = "User ID: ${u.id}\nName: ${u.name}\nRole: ${u.role}\nEmail: ${u.email}\n" +
            "Phone: ${u.phone}\nAddress: ${u.address}, ${u.area}\nMember since: ${TimeFormat.friendly(u.createdAt)}"
        AlertDialog.Builder(requireContext())
            .setTitle("Account Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showEditContact() {
        val u = user ?: return
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_contact, null)
        val etPhone = dialogView.findViewById<EditText>(R.id.etPhone)
        val etAddress = dialogView.findViewById<EditText>(R.id.etAddress)
        val spinnerArea = dialogView.findViewById<Spinner>(R.id.spinnerArea)

        etPhone.setText(u.phone)
        etAddress.setText(u.address)
        spinnerArea.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, Areas.ALL)
        spinnerArea.setSelection(Math.max(0, Areas.ALL.indexOf(u.area)))

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Edit Contact Info")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                etPhone.error = Validators.phone(etPhone.text.toString())
                etAddress.error = Validators.required(etAddress.text.toString(), "Address")
                if (etPhone.error != null || etAddress.error != null) return@setOnClickListener

                repo.updateContact(u, etPhone.text.toString(), etAddress.text.toString(), spinnerArea.selectedItem.toString())
                Toast.makeText(requireContext(), "Contact info updated", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                loadProfile()
            }
        }
        dialog.show()
    }

    private fun logout() {
        Session(requireContext()).logout()
        val intent = Intent(requireContext(), RoleSelectionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}

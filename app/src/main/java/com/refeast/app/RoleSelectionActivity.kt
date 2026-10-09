package com.refeast.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.refeast.app.data.Repository
import com.refeast.app.data.Roles
import com.refeast.app.util.Session

/** First screen: the user picks Donor, Recipient or Volunteer Collector. */
class RoleSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        val repo = Repository(this)
        repo.addDemoDataIfEmpty()

        // If someone is already logged in, skip straight to the home screen.
        val session = Session(this)
        if (session.isLoggedIn() && repo.getUser(session.getUserId()) != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        session.logout()

        findViewById<View>(R.id.cardDonor).setOnClickListener { openLogin(Roles.DONOR) }
        findViewById<View>(R.id.cardRecipient).setOnClickListener { openLogin(Roles.RECIPIENT) }
        findViewById<View>(R.id.cardVolunteer).setOnClickListener { openLogin(Roles.VOLUNTEER) }
    }

    private fun openLogin(role: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("role", role)
        startActivity(intent)
    }
}

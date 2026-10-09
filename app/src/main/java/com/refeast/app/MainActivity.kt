package com.refeast.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.refeast.app.data.Roles
import com.refeast.app.util.Session

/** Holds the bottom navigation and swaps the tab screens (fragments). */
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = Session(this)
        if (!session.isLoggedIn()) {
            startActivity(Intent(this, RoleSelectionActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        bottomNav = findViewById(R.id.bottomNav)

        // Each role only sees the tabs it needs.
        val role = session.getRole()
        if (role == Roles.DONOR) {
            bottomNav.menu.removeItem(R.id.nav_tasks)
        } else if (role == Roles.VOLUNTEER) {
            bottomNav.menu.removeItem(R.id.nav_add)
        } else {
            bottomNav.menu.removeItem(R.id.nav_add)
            bottomNav.menu.removeItem(R.id.nav_tasks)
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_add -> showFragment(AddListingFragment())
                R.id.nav_tasks -> showFragment(TasksFragment())
                R.id.nav_history -> showFragment(HistoryFragment())
                R.id.nav_profile -> showFragment(ProfileFragment())
                else -> showFragment(HomeFragment())
            }
            true
        }

        if (savedInstanceState == null) {
            showFragment(HomeFragment())
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /** Lets a tab switch to another tab, e.g. back to Home after publishing. */
    fun openTab(itemId: Int) {
        bottomNav.selectedItemId = itemId
    }
}

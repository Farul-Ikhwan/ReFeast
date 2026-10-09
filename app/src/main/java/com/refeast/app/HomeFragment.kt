package com.refeast.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.refeast.app.data.Areas
import com.refeast.app.data.Categories
import com.refeast.app.data.FoodListing
import com.refeast.app.data.Repository
import com.refeast.app.data.Roles
import com.refeast.app.data.User
import com.refeast.app.util.Session

/**
 * Home tab with Search / Filter.
 * Recipients and volunteers see food they can reserve. Donors see their own listings.
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var repo: Repository
    private var user: User? = null
    private lateinit var adapter: FoodAdapter
    private lateinit var tvCount: TextView
    private lateinit var tvEmpty: TextView

    // The current filter choices.
    private var searchText = ""
    private var selectedCategory: String? = null // null means "All"
    private var selectedArea: String? = null     // null means "All areas"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = Repository(requireContext())
        user = repo.getUser(Session(requireContext()).getUserId())
        val currentUser = user ?: return

        tvCount = view.findViewById(R.id.tvCount)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        val isDonor = currentUser.role == Roles.DONOR
        view.findViewById<TextView>(R.id.tvHomeTitle).text = if (isDonor) "My listings" else "Nearby surplus food"
        view.findViewById<TextView>(R.id.tvHomeSubtitle).text = "Hi, ${currentUser.name} · ${currentUser.role}"
        tvEmpty.text = if (isDonor) "No listings yet. Tap Donate to share surplus food." else "No food matches your search."

        // List of food cards. Tapping a card opens the listing details.
        adapter = FoodAdapter { listing ->
            val intent = Intent(requireContext(), FoodDetailActivity::class.java)
            intent.putExtra("itemId", listing.id)
            startActivity(intent)
        }
        val rv = view.findViewById<RecyclerView>(R.id.rvFoodList)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        // Search box: filter again every time the text changes.
        view.findViewById<EditText>(R.id.etSearch).doAfterTextChanged { text ->
            searchText = text.toString().trim()
            loadListings()
        }

        setUpCategoryChips(view.findViewById(R.id.chipGroupCategory))
        setUpAreaSpinner(view.findViewById(R.id.spinnerArea))
    }

    // onResume runs every time this screen is shown, so the list is always up to date.
    override fun onResume() {
        super.onResume()
        loadListings()
    }

    private fun loadListings() {
        val currentUser = user ?: return
        val allListings: List<FoodListing> = if (currentUser.role == Roles.DONOR) {
            repo.getDonorListings(currentUser.id)
        } else {
            repo.getAvailableListings()
        }

        // Keep only the listings that match the search text, category and area.
        val result = ArrayList<FoodListing>()
        for (listing in allListings) {
            if (matchesFilters(listing)) result.add(listing)
        }

        adapter.updateList(result)
        tvCount.text = "${result.size} items"
        tvEmpty.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun matchesFilters(listing: FoodListing): Boolean {
        if (searchText.isNotEmpty()) {
            val found = listing.title.contains(searchText, ignoreCase = true) ||
                listing.description.contains(searchText, ignoreCase = true) ||
                listing.donorName.contains(searchText, ignoreCase = true)
            if (!found) return false
        }
        if (selectedCategory != null && listing.category != selectedCategory) return false
        if (selectedArea != null && listing.area != selectedArea) return false
        return true
    }

    private fun setUpCategoryChips(group: ChipGroup) {
        val labels = ArrayList<String>()
        labels.add("All")
        labels.addAll(Categories.SHORT)

        for (i in labels.indices) {
            val chip = LayoutInflater.from(requireContext()).inflate(R.layout.item_filter_chip, group, false) as Chip
            chip.id = View.generateViewId()
            chip.text = labels[i]
            group.addView(chip)
            if (i == 0) chip.isChecked = true

            chip.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Chip 0 is "All". The others match Categories.ALL in the same order.
                    selectedCategory = if (i == 0) null else Categories.ALL[i - 1]
                    loadListings()
                }
            }
        }
    }

    private fun setUpAreaSpinner(spinner: Spinner) {
        val options = ArrayList<String>()
        options.add("All areas")
        options.addAll(Areas.ALL)
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, options)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                selectedArea = if (position == 0) null else options[position]
                loadListings()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}

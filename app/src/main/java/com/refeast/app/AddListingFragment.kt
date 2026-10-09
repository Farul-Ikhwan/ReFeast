package com.refeast.app

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.refeast.app.data.Areas
import com.refeast.app.data.Categories
import com.refeast.app.data.FoodListing
import com.refeast.app.data.Repository
import com.refeast.app.data.User
import com.refeast.app.util.DateTimeHelper
import com.refeast.app.util.ImageStore
import com.refeast.app.util.Session
import com.refeast.app.util.TimeFormat
import com.refeast.app.util.Validators

/** Donate tab: Surplus-Food Listing. Donors fill in the form and publish the food. */
class AddListingFragment : Fragment(R.layout.fragment_add_listing) {

    private var user: User? = null
    private var deadline: Long = 0       // 0 means "not chosen yet"
    private var imagePath: String? = null
    private lateinit var ivPhoto: ImageView

    // Opens the phone's photo picker. The chosen photo is copied into the app.
    private val pickPhoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val path = ImageStore.save(requireContext(), uri)
            if (path == null) {
                Toast.makeText(requireContext(), "Could not load that photo", Toast.LENGTH_SHORT).show()
            } else {
                imagePath = path
                ImageStore.show(ivPhoto, path, null)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        user = Repository(requireContext()).getUser(Session(requireContext()).getUserId())
        val donor = user ?: return

        ivPhoto = view.findViewById(R.id.ivPhoto)
        ivPhoto.clipToOutline = true
        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etQuantity = view.findViewById<EditText>(R.id.etQuantity)
        val etUnit = view.findViewById<EditText>(R.id.etUnit)
        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val etDescription = view.findViewById<EditText>(R.id.etDescription)
        val spinnerCategory = view.findViewById<Spinner>(R.id.spinnerCategory)
        val spinnerArea = view.findViewById<Spinner>(R.id.spinnerArea)
        val tvDeadline = view.findViewById<TextView>(R.id.tvPickDeadline)

        spinnerCategory.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, Categories.ALL)
        spinnerArea.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, Areas.ALL)

        // Fill in the donor's own address and area to save typing.
        etLocation.setText(donor.address)
        spinnerArea.setSelection(Math.max(0, Areas.ALL.indexOf(donor.area)))

        // Until a photo is added, show the drawing for the chosen category.
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                if (imagePath == null) ImageStore.show(ivPhoto, null, Categories.ALL[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        ivPhoto.setOnClickListener { openPhotoPicker() }
        view.findViewById<TextView>(R.id.btnAddPhoto).setOnClickListener { openPhotoPicker() }

        tvDeadline.setOnClickListener {
            val twoHoursFromNow = System.currentTimeMillis() + 2 * 60 * 60 * 1000L
            DateTimeHelper.pick(requireContext(), twoHoursFromNow, null) { chosen ->
                deadline = chosen
                tvDeadline.error = null
                tvDeadline.text = TimeFormat.friendly(chosen)
                tvDeadline.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        }

        view.findViewById<TextView>(R.id.btnPublish).setOnClickListener {
            // 1. Check every field.
            etTitle.error = Validators.title(etTitle.text.toString())
            etQuantity.error = Validators.quantity(etQuantity.text.toString())
            etUnit.error = Validators.required(etUnit.text.toString(), "Unit")
            etLocation.error = Validators.required(etLocation.text.toString(), "Pickup location")
            if (etTitle.error != null || etQuantity.error != null || etUnit.error != null || etLocation.error != null) {
                return@setOnClickListener
            }

            val fifteenMinutes = 15 * 60 * 1000L
            if (deadline == 0L) {
                tvDeadline.error = "Select a pickup deadline"
                Toast.makeText(requireContext(), "Select a pickup deadline", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (deadline < System.currentTimeMillis() + fifteenMinutes) {
                tvDeadline.error = "Too soon"
                Toast.makeText(requireContext(), "Deadline must be at least 15 minutes from now", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Save the listing.
            val quantity = etQuantity.text.toString().trim().toInt()
            val listing = FoodListing(
                donorId = donor.id,
                donorName = donor.name,
                title = etTitle.text.toString().trim(),
                description = etDescription.text.toString().trim(),
                category = spinnerCategory.selectedItem.toString(),
                quantity = quantity,
                quantityAvailable = quantity,
                unit = etUnit.text.toString().trim(),
                pickupLocation = etLocation.text.toString().trim(),
                area = spinnerArea.selectedItem.toString(),
                pickupDeadline = deadline,
                imagePath = imagePath
            )
            Repository(requireContext()).publishListing(listing)

            // 3. Tell the user and go back to Home.
            Toast.makeText(requireContext(), "Listing published", Toast.LENGTH_SHORT).show()
            (activity as MainActivity).openTab(R.id.nav_home)
        }
    }

    private fun openPhotoPicker() {
        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

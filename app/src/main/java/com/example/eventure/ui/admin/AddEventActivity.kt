package com.example.eventure.ui.admin

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Patterns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventure.R
import com.example.eventure.data.models.Event
import com.example.eventure.data.models.EventCategory
import com.example.eventure.databinding.ActivityAddEventBinding
import com.example.eventure.ui.adapters.ImagePreviewAdapter
import com.example.eventure.utils.DateUtils
import com.example.eventure.viewmodels.AddEventViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class AddEventActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityAddEventBinding
    private val viewModel: AddEventViewModel by viewModels()
    private lateinit var imageAdapter: ImagePreviewAdapter
    private val selectedImageUris = mutableListOf<Uri>()
    private var selectedCategory: String? = null
    private var selectedDate: Calendar = Calendar.getInstance()
    private var googleMap: GoogleMap? = null
    private var selectedLocation: LatLng? = null
    private var selectedAddress: String = ""
    private lateinit var geocoder: Geocoder
    private var isMapReady = false
    private var mapFragment: SupportMapFragment? = null

    private var isPaymentTypePaid = false

    private lateinit var pickImageFromGalleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupCategoryChips()
        setupClickListeners()
        setupPaymentTypeSelection()
        initializeActivityLaunchers()
        observeViewModel()

        geocoder = Geocoder(this, Locale.getDefault())
        binding.mapContainer.visibility = View.GONE
        setupMapFragment()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Add New Event"
        }
    }

    private fun setupRecyclerView() {
        imageAdapter = ImagePreviewAdapter(selectedImageUris) { position ->
            selectedImageUris.removeAt(position)
            imageAdapter.notifyItemRemoved(position)
            updateImagePreviewVisibility()
        }
        binding.recyclerViewImages.apply {
            layoutManager = LinearLayoutManager(this@AddEventActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = imageAdapter
        }
        updateImagePreviewVisibility()
    }

    private fun updateImagePreviewVisibility() {
        val imageCount = selectedImageUris.size
        binding.recyclerViewImages.visibility = if (imageCount > 0) View.VISIBLE else View.GONE
        binding.textViewImageCount.text = "$imageCount images selected"
    }

    private fun setupCategoryChips() {
        EventCategory.values().forEach { category ->
            val chip = Chip(this).apply {
                text = category.displayName
                isCheckable = true
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedCategory = category.name
                        clearOtherChips(this)
                    }
                }
            }
            binding.chipGroupCategories.addView(chip)
        }
    }

    private fun clearOtherChips(selectedChip: Chip) {
        for (i in 0 until binding.chipGroupCategories.childCount) {
            val chip = binding.chipGroupCategories.getChildAt(i) as Chip
            if (chip != selectedChip) {
                chip.isChecked = false
            }
        }
    }

    private fun setupPaymentTypeSelection() {
        // Initially hide the ticket price field and set free as default
        binding.eventPriceLayout.visibility = View.GONE
        selectPaymentType(false)
    }

    private fun selectPaymentType(isPaid: Boolean) {
        isPaymentTypePaid = isPaid

        if (isPaid) {
            binding.eventPriceLayout.visibility = View.VISIBLE

            binding.btnPaid.backgroundTintList = ContextCompat.getColorStateList(this, R.color.admin_theme)
            binding.btnPaid.setTextColor(ContextCompat.getColor(this, android.R.color.white))

            binding.btnFree.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.white)
            binding.btnFree.setTextColor(ContextCompat.getColor(this, R.color.admin_theme))

        } else {

            binding.eventPriceLayout.visibility = View.GONE
            binding.editTicketPrice.text?.clear()

            binding.btnFree.backgroundTintList = ContextCompat.getColorStateList(this, R.color.admin_theme)
            binding.btnFree.setTextColor(ContextCompat.getColor(this, android.R.color.white))

            binding.btnPaid.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.white)
            binding.btnPaid.setTextColor(ContextCompat.getColor(this, R.color.admin_theme))
        }
    }

    private fun setupMapFragment() {
        mapFragment = SupportMapFragment.newInstance()
        mapFragment?.getMapAsync(this)
    }

    private fun setupClickListeners() {
        // Payment type selection
        binding.btnFree.setOnClickListener {
            selectPaymentType(false)
        }

        binding.btnPaid.setOnClickListener {
            selectPaymentType(true)
        }

        binding.editTextEventDate.setOnClickListener { showDatePicker() }
        binding.editTextEventTime.setOnClickListener { showTimePicker() }
        binding.buttonSelectImages.setOnClickListener { showImagePickerDialog() }
        binding.buttonSaveEvent.setOnClickListener { submitEvent() }
        binding.buttonCancel.setOnClickListener { finish() }

        binding.editTextEventLocation.setOnEditorActionListener { _, _, _ ->
            val location = binding.editTextEventLocation.text.toString().trim()
            if (location.isNotEmpty()) {
                searchLocation(location)
            }
            true
        }

        binding.buttonSearchLocation.setOnClickListener {
            val location = binding.editTextEventLocation.text.toString().trim()
            if (location.isNotEmpty()) {
                searchLocation(location)
            } else {
                Toast.makeText(this, "Please enter a location to search", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        isMapReady = true

        val defaultLocation = LatLng(6.9271, 79.8612)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        googleMap?.setOnMapClickListener { latLng ->
            selectLocationOnMap(latLng)
        }

        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isMapToolbarEnabled = true

        selectedLocation?.let { location ->
            selectLocationOnMap(location)
        }
    }

    private fun searchLocation(locationName: String) {
        try {
            binding.buttonSearchLocation.isEnabled = false
            binding.buttonSearchLocation.text = "Searching..."

            val addresses = geocoder.getFromLocationName(locationName, 1)
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                val latLng = LatLng(address.latitude, address.longitude)

                selectedLocation = latLng
                selectedAddress = getFullAddress(address)
                binding.editTextEventLocation.setText(selectedAddress)

                showMapWithLocation(latLng)

                Toast.makeText(this, "Location found and selected on map", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Location not found. Please try a different search term.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error searching location: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            binding.buttonSearchLocation.isEnabled = true
            binding.buttonSearchLocation.text = "Search"
        }
    }

    private fun showMapWithLocation(latLng: LatLng) {
        if (mapFragment != null && !mapFragment!!.isAdded) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mapFrame, mapFragment!!)
                .commitAllowingStateLoss()
        }

        showMapWithAnimation()

        if (isMapReady) {
            selectLocationOnMap(latLng)
        }
    }

    private fun showMapWithAnimation() {
        if (binding.mapContainer.visibility != View.VISIBLE) {
            binding.mapContainer.visibility = View.VISIBLE
            binding.mapContainer.alpha = 0f
            binding.mapContainer.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }

    private fun selectLocationOnMap(latLng: LatLng) {
        if (!isMapReady || googleMap == null) {
            selectedLocation = latLng
            return
        }

        selectedLocation = latLng

        googleMap?.clear()
        googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Event Location")
                .snippet("Selected location for the event")
        )
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

        if (binding.editTextEventLocation.text.toString().trim().isEmpty()) {
            reverseGeocode(latLng)
        }
    }

    private fun reverseGeocode(latLng: LatLng) {
        try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses?.isNotEmpty() == true) {
                selectedAddress = getFullAddress(addresses[0])
                binding.editTextEventLocation.setText(selectedAddress)
            }
        } catch (e: Exception) {
            selectedAddress = "Selected Location: ${String.format("%.6f", latLng.latitude)}, ${String.format("%.6f", latLng.longitude)}"
            binding.editTextEventLocation.setText(selectedAddress)
        }
    }

    private fun getFullAddress(address: Address): String {
        val addressText = StringBuilder()

        for (i in 0..address.maxAddressLineIndex) {
            if (i > 0) addressText.append(", ")
            addressText.append(address.getAddressLine(i))
        }

        return addressText.toString()
    }

    private fun initializeActivityLaunchers() {
        // Camera
        takePictureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val extras = result.data?.extras
                val imageBitmap = extras?.get("data") as? Bitmap
                if (imageBitmap != null) {
                    val imageUri = saveBitmapToCache(imageBitmap)
                    selectedImageUris.add(imageUri)
                    imageAdapter.notifyDataSetChanged()
                    updateImagePreviewVisibility()
                    Toast.makeText(this, "Photo captured successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to capture photo", Toast.LENGTH_SHORT).show()
                }
            }
        }

        pickImageFromGalleryLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.clipData?.let { clipData ->
                    for (i in 0 until clipData.itemCount) {
                        selectedImageUris.add(clipData.getItemAt(i).uri)
                    }
                } ?: result.data?.data?.let { uri ->
                    selectedImageUris.add(uri)
                }
                imageAdapter.notifyDataSetChanged()
                updateImagePreviewVisibility()
            }
        }
    }

    private fun saveBitmapToCache(bitmap: Bitmap): Uri {
        val file = File(cacheDir, "captured_image_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        return Uri.fromFile(file)
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        viewModel.saveResult.observe(this, Observer { success ->
            if (success) {
                Toast.makeText(this, "Event added successfully!", Toast.LENGTH_LONG).show()
                finish()
            }
        })

        viewModel.errorMessage.observe(this, Observer { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(this, "Error: $error", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Gallery", "Camera")

        AlertDialog.Builder(this)
            .setTitle("Select Image Source")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            type = "image/*"
        }
        pickImageFromGalleryLauncher.launch(intent)
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            takePictureLauncher.launch(takePictureIntent)
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate.set(Calendar.YEAR, year)
                selectedDate.set(Calendar.MONTH, month)
                selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                binding.editTextEventDate.setText(DateUtils.formatDate(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedDate.set(Calendar.MINUTE, minute)
                binding.editTextEventTime.setText(DateUtils.formatTime(selectedDate.time))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        return phone.isNotEmpty() && phone.length >= 10 && phone.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }
    }

    private fun validateInputs(): Boolean {
        val eventName = binding.editTextEventName.text.toString().trim()
        val eventDescription = binding.editTextEventDescription.text.toString().trim()
        val eventLocation = binding.editTextEventLocation.text.toString().trim()
        val eventDateStr = binding.editTextEventDate.text.toString()
        val eventTimeStr = binding.editTextEventTime.text.toString()

        val organizerName = binding.editTextOrganizerName.text.toString().trim()
        val organizerEmail = binding.editTextOrganizerEmail.text.toString().trim()
        val organizerContact = binding.editTextOrganizerContact.text.toString().trim()

        when {
            eventName.isEmpty() -> {
                binding.editTextEventName.error = "Event name is required"
                binding.editTextEventName.requestFocus()
                return false
            }
            eventDescription.isEmpty() -> {
                binding.editTextEventDescription.error = "Description is required"
                binding.editTextEventDescription.requestFocus()
                return false
            }
            eventLocation.isEmpty() -> {
                binding.editTextEventLocation.error = "Location is required"
                binding.editTextEventLocation.requestFocus()
                return false
            }
            eventDateStr.isEmpty() -> {
                binding.editTextEventDate.error = "Date is required"
                Toast.makeText(this, "Please select event date", Toast.LENGTH_SHORT).show()
                return false
            }
            eventTimeStr.isEmpty() -> {
                binding.editTextEventTime.error = "Time is required"
                Toast.makeText(this, "Please select event time", Toast.LENGTH_SHORT).show()
                return false
            }
            organizerName.isEmpty() -> {
                binding.editTextOrganizerName.error = "Organizer name is required"
                binding.editTextOrganizerName.requestFocus()
                return false
            }
            organizerEmail.isEmpty() -> {
                binding.editTextOrganizerEmail.error = "Organizer email is required"
                binding.editTextOrganizerEmail.requestFocus()
                return false
            }
            !isValidEmail(organizerEmail) -> {
                binding.editTextOrganizerEmail.error = "Please enter a valid email address"
                binding.editTextOrganizerEmail.requestFocus()
                return false
            }
            organizerContact.isEmpty() -> {
                binding.editTextOrganizerContact.error = "Organizer contact is required"
                binding.editTextOrganizerContact.requestFocus()
                return false
            }
            !isValidPhoneNumber(organizerContact) -> {
                binding.editTextOrganizerContact.error = "Please enter a valid phone number"
                binding.editTextOrganizerContact.requestFocus()
                return false
            }
            selectedCategory == null -> {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                return false
            }
            isPaymentTypePaid && binding.editTicketPrice.text.toString().trim().isEmpty() -> {
                binding.editTicketPrice.error = "Ticket price is required for paid events"
                binding.editTicketPrice.requestFocus()
                return false
            }
            isPaymentTypePaid && binding.editTicketPrice.text.toString().trim().toDoubleOrNull() == null -> {
                binding.editTicketPrice.error = "Please enter a valid price"
                binding.editTicketPrice.requestFocus()
                return false
            }
            isPaymentTypePaid && binding.editTicketPrice.text.toString().trim().toDouble() < 0 -> {
                binding.editTicketPrice.error = "Price cannot be negative"
                binding.editTicketPrice.requestFocus()
                return false
            }
            else -> return true
        }
    }

    private fun submitEvent() {
        if (!validateInputs()) {
            return
        }

        val eventName = binding.editTextEventName.text.toString().trim()
        val eventDescription = binding.editTextEventDescription.text.toString().trim()
        val eventLocation = binding.editTextEventLocation.text.toString().trim()
        val eventTimeStr = binding.editTextEventTime.text.toString()


        val organizerName = binding.editTextOrganizerName.text.toString().trim()
        val organizerEmail = binding.editTextOrganizerEmail.text.toString().trim()
        val organizerContact = binding.editTextOrganizerContact.text.toString().trim()

        val ticketPrice = if (isPaymentTypePaid) {
            binding.editTicketPrice.text.toString().trim().toDouble()
        } else {
            0.0
        }

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "unknown"

        val event = Event(
            name = eventName,
            description = eventDescription,
            category = selectedCategory!!,
            date = Timestamp(selectedDate.time),
            time = eventTimeStr,
            location = eventLocation,
            imageUrls = emptyList(),
            participantCount = null,
            createdBy = currentUserId,
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now(),
            status = "active",
            maxAttendees = 100,
            currentAttendees = 0,
            ticketPrice = ticketPrice,
            organizer = organizerName,
            contactEmail = organizerEmail,
            contactPhone = organizerContact,
            tags = emptyList()
        )

        viewModel.saveEvent(event, selectedImageUris)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.admin_add_event_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_clear_fields -> {
                clearAllFields()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun clearAllFields() {
        binding.apply {
            editTextEventName.text?.clear()
            editTextEventDescription.text?.clear()
            editTextEventLocation.text?.clear()
            editTextEventDate.text?.clear()
            editTextEventTime.text?.clear()
            editTicketPrice.text?.clear()

            editTextOrganizerName.text?.clear()
            editTextOrganizerEmail.text?.clear()
            editTextOrganizerContact.text?.clear()

            chipGroupCategories.clearCheck()
            selectedCategory = null
            selectedImageUris.clear()
            imageAdapter.notifyDataSetChanged()
            updateImagePreviewVisibility()

            googleMap?.clear()
            selectedLocation = null
            selectedAddress = ""
            mapContainer.visibility = View.GONE

            selectPaymentType(false)
        }
        Toast.makeText(this, "Fields cleared", Toast.LENGTH_SHORT).show()
    }
}
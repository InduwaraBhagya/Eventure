package com.example.eventure

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eventure.model.event.Event
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PopularEventAdapter
    private val eventList = ArrayList<Event>()
    private val db = FirebaseFirestore.getInstance()
    private val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())  // Date formatter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Static Featured Event views
        val featuredTitle = view.findViewById<TextView>(R.id.eventTitle)
        val featuredDate = view.findViewById<TextView>(R.id.eventDate)
        val featuredLocation = view.findViewById<TextView>(R.id.eventLocation)
        val featuredImage = view.findViewById<ImageView>(R.id.eventImage)

        featuredTitle.text = "Design Gala 2025"
        featuredDate.text = "Aug 20"
        featuredLocation.text = "Colombo Art Center"
        // Load drawable resource instead of URL for featured event image
        Glide.with(this@HomeFragment)
            .load(R.drawable.sample_event_image)  // your drawable image name here
            .into(featuredImage)

        // Setup RecyclerView
        recyclerView = view.findViewById(R.id.popularEventsRecycler)
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        adapter = PopularEventAdapter(eventList, sdf)
        recyclerView.adapter = adapter

        loadEventsFromFirestore()

        return view
    }

    private fun loadEventsFromFirestore() {
        db.collection("events")
            .get()
            .addOnSuccessListener { result ->
                eventList.clear()
                for (document in result) {
                    val event = document.toObject(Event::class.java).copy(id = document.id)
                    eventList.add(event)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
}

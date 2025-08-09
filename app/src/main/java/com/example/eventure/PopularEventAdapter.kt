package com.example.eventure

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eventure.R
import com.example.eventure.model.event.Event
import java.text.SimpleDateFormat

class PopularEventAdapter(
    private val eventList: List<Event>,
    private val dateFormat: SimpleDateFormat
) : RecyclerView.Adapter<PopularEventAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.eventTitle)
        val date: TextView = itemView.findViewById(R.id.eventDate)
        val location: TextView = itemView.findViewById(R.id.eventLocation)
        val image: ImageView = itemView.findViewById(R.id.eventImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_popular_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventList[position]
        holder.title.text = event.title

        holder.date.text = event.date?.toDate()?.let { dateFormat.format(it) } ?: "No date"
        holder.location.text = event.location

        val context = holder.itemView.context

        // List your drawable images here
        val drawableIds = listOf(
            R.drawable.sample_event_image1,
            R.drawable.sample_event_image2,
            R.drawable.sample_event_image3,
            R.drawable.sample_event_image4,
            R.drawable.sample_event_image5,
            R.drawable.sample_event_image6,
            R.drawable.sample_event_image7
            // Add as many images as you want
        )

        // Cycle through the drawable list based on position so each card gets a different image
        val drawableResId = drawableIds[position % drawableIds.size]

        Glide.with(context)
            .load(drawableResId)
            .into(holder.image)
    }

    override fun getItemCount() = eventList.size
}

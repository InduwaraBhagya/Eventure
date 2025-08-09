package com.example.eventure.ui.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eventure.R
import com.example.eventure.databinding.ItemAdminEventCardBinding
import com.example.eventure.data.models.Event
import com.example.eventure.ui.admin.AdminEventDetailActivity
import com.example.eventure.ui.admin.EditEventActivity
import com.example.eventure.utils.DateUtils

class AdminEventAdapter(
    private val onEventClick: (Event) -> Unit,
    private val onEditClick: (Event) -> Unit,
    private val onDeleteClick: (Event) -> Unit
) : ListAdapter<Event, AdminEventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemAdminEventCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(
        private val binding: ItemAdminEventCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.apply {
                EventName.text = event.name
                EventDescription.text = event.description
                EventLocation.text = event.location

                EventDateTime.text = DateUtils.formatTimestamp(event.date)

                val categoryDisplayName = when (event.category.uppercase()) {
                    "MUSICAL" -> "Musical"
                    "SPORTS" -> "Sports"
                    "FOOD" -> "Food"
                    "ART" -> "Art"
                    else -> "Other"
                }
                chipCategory.text = categoryDisplayName

                val categoryColor = when (event.category.uppercase()) {
                    "MUSICAL" -> R.color.category_musical
                    "SPORTS" -> R.color.category_sports
                    "FOOD" -> R.color.category_food
                    "ART" -> R.color.category_art
                    else -> R.color.category_default
                }
                chipCategory.setChipBackgroundColorResource(categoryColor)

                if (event.imageUrls.isNotEmpty()) {
                    Glide.with(binding.root.context)
                        .load(event.imageUrls.first())
                        .placeholder(R.drawable.placeholder_event)
                        .error(R.drawable.placeholder_event)
                        .into(EventImage)
                } else {
                    EventImage.setImageResource(R.drawable.placeholder_event)
                }

                val isUpcoming = DateUtils.isEventUpcoming(event.date)
                EventStatus.text = if (isUpcoming) "Upcoming" else "Past"
                EventStatus.setTextColor(
                    binding.root.context.getColor(
                        if (isUpcoming) R.color.status_upcoming else R.color.status_past
                    )
                )

                root.setOnClickListener {
                    try {
                        onEventClick(event)
                    } catch (e: Exception) {
                                      val context = binding.root.context
                        val intent = Intent(context, AdminEventDetailActivity::class.java).apply {
                            putExtra("eventId", event.id)
                            putExtra("EVENT_ID", event.id)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                }

                btnEdit.setOnClickListener {
                    try {
                        onEditClick(event)
                    } catch (e: Exception) {
                        val context = binding.root.context
                        val intent = Intent(context, EditEventActivity::class.java).apply {
                            putExtra("eventId", event.id)
                            putExtra("EVENT_ID", event.id)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                }

                btnDelete?.setOnClickListener {
                    onDeleteClick(event)
                }
            }
        }
    }

    private class EventDiffCallback : DiffUtil.ItemCallback<Event>() {
        override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean {
            return oldItem == newItem
        }
    }
}
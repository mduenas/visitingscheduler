package com.markduenas.visischeduler.data.remote.dto

import com.markduenas.visischeduler.domain.entities.SlotType
import com.markduenas.visischeduler.domain.entities.TimeSlot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class TimeSlotDto(
    val id: String,
    val facilityId: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val maxCapacity: Int,
    val currentBookings: Int = 0,
    val isAvailable: Boolean = true,
    val slotType: String = "REGULAR",
    val notes: String? = null
) {
    fun toDomain(): TimeSlot {
        return TimeSlot(
            id = id,
            facilityId = facilityId,
            date = LocalDate.parse(date),
            startTime = LocalTime.parse(startTime),
            endTime = LocalTime.parse(endTime),
            maxCapacity = maxCapacity,
            currentBookings = currentBookings,
            isAvailable = isAvailable,
            slotType = SlotType.valueOf(slotType),
            notes = notes
        )
    }

    companion object {
        fun fromDomain(slot: TimeSlot): TimeSlotDto {
            return TimeSlotDto(
                id = slot.id,
                facilityId = slot.facilityId,
                date = slot.date.toString(),
                startTime = slot.startTime.toString(),
                endTime = slot.endTime.toString(),
                maxCapacity = slot.maxCapacity,
                currentBookings = slot.currentBookings,
                isAvailable = slot.isAvailable,
                slotType = slot.slotType.name,
                notes = slot.notes
            )
        }
    }
}

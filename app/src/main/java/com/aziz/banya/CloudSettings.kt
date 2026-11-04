package com.aziz.banya

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class SharedBanyaSettings(
    val startHour: Int = 16,
    val endHour: Int = 23,
    val roomCount: Int = 6,
    val price60: Int = 3000,
    val price90: Int = 4500,
    val price120: Int = 6000
)

class CloudSettings {
    private val document = FirebaseFirestore.getInstance()
        .collection("app_settings").document("main")

    fun listen(onResult: (SharedBanyaSettings?, String?) -> Unit): ListenerRegistration =
        document.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onResult(null, "Созламаларни синхронлашда хатолик")
                return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) {
                onResult(null, null)
                return@addSnapshotListener
            }
            val settings = SharedBanyaSettings(
                startHour = (snapshot.getLong("startHour") ?: 16).toInt(),
                endHour = (snapshot.getLong("endHour") ?: 23).toInt(),
                roomCount = (snapshot.getLong("roomCount") ?: 6).toInt(),
                price60 = (snapshot.getLong("price60") ?: 3000).toInt(),
                price90 = (snapshot.getLong("price90") ?: 4500).toInt(),
                price120 = (snapshot.getLong("price120") ?: 6000).toInt()
            )
            if (settings.startHour !in 0..23 ||
                settings.endHour !in 1..24 ||
                settings.startHour >= settings.endHour ||
                settings.roomCount !in 1..6 ||
                listOf(settings.price60, settings.price90, settings.price120).any { it < 0 }) {
                onResult(null, "Сервердаги созламалар нотўғри")
            } else onResult(settings, null)
        }

    fun save(settings: SharedBanyaSettings, callback: (Boolean, String) -> Unit) {
        if (settings.startHour !in 0..23 ||
            settings.endHour !in 1..24 ||
            settings.startHour >= settings.endHour ||
            settings.roomCount !in 1..6 ||
            listOf(settings.price60, settings.price90, settings.price120).any { it < 0 }) {
            callback(false, "Созламалар нотўғри")
            return
        }
        document.set(mapOf(
            "startHour" to settings.startHour,
            "endHour" to settings.endHour,
            "roomCount" to settings.roomCount,
            "price60" to settings.price60,
            "price90" to settings.price90,
            "price120" to settings.price120
        )).addOnSuccessListener { callback(true, "Созламалар сақланди") }
            .addOnFailureListener { callback(false, "Серверга сақлаб бўлмади") }
    }
}

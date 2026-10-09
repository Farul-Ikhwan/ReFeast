package com.refeast.app.data

/**
 * Demo data inserted the first time the app runs, so every screen has something to show.
 * All demo accounts use the password: password123
 */
object SeedData {

    const val DEMO_PASSWORD = "password123"

    private const val HOUR = 60 * 60 * 1000L
    private const val DAY = 24 * HOUR

    fun insert(db: AppDatabase) {
        val userDao = db.userDao()
        val foodDao = db.foodDao()
        val resDao = db.reservationDao()
        val now = System.currentTimeMillis()

        // Demo accounts (one per role) plus a few extra donors.
        val bakery = user("Sweet Crumb Bakery", "donor@refeast.my", Roles.DONOR,
            "012-3456789", "12, Jalan Bahagia", "Petaling Jaya")
        val bakeryId = userDao.insert(bakery)
        val home = user("Rumah Amal Harapan", "recipient@refeast.my", Roles.RECIPIENT,
            "013-2223344", "8, Jalan SS2/24", "Petaling Jaya")
        val homeId = userDao.insert(home)
        val fikri = user("Ahmad Fikri", "volunteer@refeast.my", Roles.VOLUNTEER,
            "017-8889900", "Seksyen 13", "Shah Alam")
        val fikriId = userDao.insert(fikri)

        val pasarId = userDao.insert(user("Pasar Segar Supermarket", "pasarsegar@refeast.my", Roles.DONOR,
            "03-55101234", "Seksyen 7", "Shah Alam"))
        val warungId = userDao.insert(user("Warung Pak Man", "warung@refeast.my", Roles.DONOR,
            "019-3334455", "Kampung Baru", "Kuala Lumpur"))
        val tanId = userDao.insert(user("Mrs. Tan", "tan@refeast.my", Roles.DONOR,
            "016-7778899", "Taman OUG", "Kuala Lumpur"))
        val fruitId = userDao.insert(user("Fruit Haven", "fruithaven@refeast.my", Roles.DONOR,
            "03-56301122", "Bandar Sunway", "Bandar Sunway"))

        // Live listings
        foodDao.insert(FoodListing(
            donorId = bakeryId, donorName = bakery.name, title = "Unsold Bread Loaves",
            description = "Freshly baked white and wholemeal loaves, unsold from today. Good condition, best consumed within 2 days.",
            category = "Baked Goods", quantity = 10, quantityAvailable = 10, unit = "loaves",
            pickupLocation = "12, Jalan Bahagia", area = "Petaling Jaya", pickupDeadline = now + 2 * HOUR
        ))
        foodDao.insert(FoodListing(
            donorId = pasarId, donorName = "Pasar Segar Supermarket", title = "Mixed Vegetable Crates",
            description = "Assorted vegetables slightly below aesthetic standard but fresh. Includes carrots, cabbage and tomatoes.",
            category = "Fresh Produce", quantity = 4, quantityAvailable = 4, unit = "crates",
            pickupLocation = "Seksyen 7", area = "Shah Alam", pickupDeadline = now + 3 * HOUR
        ))
        val riceId = foodDao.insert(FoodListing(
            donorId = warungId, donorName = "Warung Pak Man", title = "Cooked Rice & Chicken Curry",
            description = "Leftover catering food from an event, still warm. Chicken curry with white rice. Contains chilli.",
            category = "Cooked Meals", quantity = 15, quantityAvailable = 10, unit = "portions",
            pickupLocation = "Kampung Baru", area = "Kuala Lumpur", pickupDeadline = now + 90 * 60 * 1000L
        ))
        foodDao.insert(FoodListing(
            donorId = tanId, donorName = "Mrs. Tan", title = "Assorted Canned Goods",
            description = "Unopened canned beans, tuna and sweet corn nearing best-before date.",
            category = "Dry / Pantry", quantity = 12, quantityAvailable = 12, unit = "cans",
            pickupLocation = "Taman OUG", area = "Kuala Lumpur", pickupDeadline = now + DAY
        ))
        foodDao.insert(FoodListing(
            donorId = fruitId, donorName = "Fruit Haven", title = "Fresh Fruit Basket Surplus",
            description = "Overripe but edible bananas, apples and oranges from today's unsold stock.",
            category = "Fresh Produce", quantity = 6, quantityAvailable = 6, unit = "baskets",
            pickupLocation = "Bandar Sunway", area = "Bandar Sunway", pickupDeadline = now + 4 * HOUR
        ))

        // An open delivery task so the volunteer has something to accept straight away.
        val openTask = resDao.insert(Reservation(
            itemId = riceId, itemTitle = "Cooked Rice & Chicken Curry", donorId = warungId,
            donorName = "Warung Pak Man", pickupLocation = "Kampung Baru",
            recipientId = homeId, recipientName = home.name, recipientPhone = home.phone,
            dropoffLocation = home.address + ", " + home.area, quantity = 5, unit = "portions",
            needsVolunteer = true, scheduledPickupTime = now + HOUR,
            createdAt = now - 10 * 60 * 1000L, updatedAt = now - 10 * 60 * 1000L
        ))
        resDao.insertLog(StatusLog(reservationId = openTask, status = ReservationStatus.RESERVED,
            note = "Reserved 5 portions. Waiting for a volunteer collector.", changedBy = home.name,
            timestamp = now - 10 * 60 * 1000L))

        // Past, completed delivery (shows up in all three users' history).
        val pastryTime = now - 2 * DAY
        val pastryId = foodDao.insert(FoodListing(
            donorId = bakeryId, donorName = bakery.name, title = "Bakery Pastry Box",
            description = "Mixed pastries: croissants, danishes and muffins.",
            category = "Baked Goods", quantity = 6, quantityAvailable = 0, unit = "boxes",
            pickupLocation = "12, Jalan Bahagia", area = "Petaling Jaya",
            pickupDeadline = pastryTime + 3 * HOUR, status = ListingStatus.COMPLETED,
            createdAt = pastryTime - HOUR
        ))
        val pastryRes = resDao.insert(Reservation(
            itemId = pastryId, itemTitle = "Bakery Pastry Box", donorId = bakeryId, donorName = bakery.name,
            pickupLocation = "12, Jalan Bahagia", recipientId = homeId, recipientName = home.name,
            recipientPhone = home.phone, dropoffLocation = home.address + ", " + home.area,
            quantity = 6, unit = "boxes", needsVolunteer = true, volunteerId = fikriId,
            volunteerName = fikri.name, scheduledPickupTime = pastryTime + 30 * 60 * 1000L,
            status = ReservationStatus.DELIVERED, createdAt = pastryTime, updatedAt = pastryTime + HOUR
        ))
        resDao.insertLog(StatusLog(reservationId = pastryRes, status = ReservationStatus.RESERVED,
            note = "Reserved 6 boxes. Waiting for a volunteer collector.", changedBy = home.name, timestamp = pastryTime))
        resDao.insertLog(StatusLog(reservationId = pastryRes, status = ReservationStatus.RESERVED,
            note = "Accepted by volunteer ${fikri.name}", changedBy = fikri.name, timestamp = pastryTime + 5 * 60 * 1000L))
        resDao.insertLog(StatusLog(reservationId = pastryRes, status = ReservationStatus.IN_TRANSIT,
            note = "Picked up from ${bakery.name}. On the way.", changedBy = fikri.name, timestamp = pastryTime + 30 * 60 * 1000L))
        resDao.insertLog(StatusLog(reservationId = pastryRes, status = ReservationStatus.DELIVERED,
            note = "Delivered to ${home.name}", changedBy = fikri.name, timestamp = pastryTime + HOUR))

        // Past listing whose reservation was cancelled, then the listing expired.
        val croissantTime = now - DAY
        val croissantId = foodDao.insert(FoodListing(
            donorId = bakeryId, donorName = bakery.name, title = "Day-old Croissants",
            description = "Plain butter croissants from yesterday's batch.",
            category = "Baked Goods", quantity = 12, quantityAvailable = 12, unit = "pieces",
            pickupLocation = "12, Jalan Bahagia", area = "Petaling Jaya",
            pickupDeadline = croissantTime + 2 * HOUR, createdAt = croissantTime - HOUR
        ))
        val croissantRes = resDao.insert(Reservation(
            itemId = croissantId, itemTitle = "Day-old Croissants", donorId = bakeryId, donorName = bakery.name,
            pickupLocation = "12, Jalan Bahagia", recipientId = homeId, recipientName = home.name,
            recipientPhone = home.phone, dropoffLocation = home.address + ", " + home.area,
            quantity = 12, unit = "pieces", needsVolunteer = false,
            scheduledPickupTime = croissantTime + HOUR, status = ReservationStatus.CANCELLED,
            createdAt = croissantTime, updatedAt = croissantTime + 20 * 60 * 1000L
        ))
        resDao.insertLog(StatusLog(reservationId = croissantRes, status = ReservationStatus.RESERVED,
            note = "Reserved 12 pieces. Recipient will collect.", changedBy = home.name, timestamp = croissantTime))
        resDao.insertLog(StatusLog(reservationId = croissantRes, status = ReservationStatus.CANCELLED,
            note = "Cancelled by recipient", changedBy = home.name, timestamp = croissantTime + 20 * 60 * 1000L))
    }

    /** Makes a demo user. Every demo account has the same password. */
    private fun user(name: String, email: String, role: String, phone: String, address: String, area: String): User {
        return User(
            name = name, email = email, passwordHash = Repository.hashPassword(email, DEMO_PASSWORD),
            role = role, phone = phone, address = address, area = area
        )
    }
}

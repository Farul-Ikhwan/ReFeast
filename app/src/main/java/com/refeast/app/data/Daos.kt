package com.refeast.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

// A DAO (Data Access Object) lists the SQL queries for one table.
// Room writes the code that runs them.

@Dao
interface UserDao {

    @Insert
    fun insert(user: User): Long

    @Update
    fun update(user: User)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun findByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id")
    fun findById(id: Long): User?

    @Query("SELECT COUNT(*) FROM users")
    fun count(): Int
}

@Dao
interface FoodDao {

    @Insert
    fun insert(listing: FoodListing): Long

    @Update
    fun update(listing: FoodListing)

    @Query("SELECT * FROM food_listings WHERE id = :id")
    fun findById(id: Long): FoodListing?

    /** Food that recipients and volunteers can still reserve. */
    @Query(
        "SELECT * FROM food_listings WHERE status = 'Available' AND quantityAvailable > 0 " +
            "AND pickupDeadline > :now ORDER BY pickupDeadline ASC"
    )
    fun getAvailable(now: Long): List<FoodListing>

    @Query("SELECT * FROM food_listings WHERE donorId = :donorId ORDER BY createdAt DESC")
    fun getByDonor(donorId: Long): List<FoodListing>
}

@Dao
interface ReservationDao {

    @Insert
    fun insert(reservation: Reservation): Long

    @Update
    fun update(reservation: Reservation)

    @Query("SELECT * FROM reservations WHERE id = :id")
    fun findById(id: Long): Reservation?

    @Query("SELECT * FROM reservations WHERE recipientId = :userId ORDER BY updatedAt DESC")
    fun getForRecipient(userId: Long): List<Reservation>

    @Query("SELECT * FROM reservations WHERE donorId = :userId ORDER BY updatedAt DESC")
    fun getForDonor(userId: Long): List<Reservation>

    @Query("SELECT * FROM reservations WHERE volunteerId = :userId ORDER BY updatedAt DESC")
    fun getForVolunteer(userId: Long): List<Reservation>

    @Query("SELECT * FROM reservations WHERE itemId = :itemId ORDER BY createdAt DESC")
    fun getForItem(itemId: Long): List<Reservation>

    /** Delivery jobs that no volunteer has accepted yet. */
    @Query(
        "SELECT * FROM reservations WHERE needsVolunteer = 1 AND volunteerId IS NULL " +
            "AND status = 'Reserved' ORDER BY scheduledPickupTime ASC"
    )
    fun getOpenTasks(): List<Reservation>

    @Query("SELECT * FROM reservations WHERE recipientId = :recipientId ORDER BY id DESC LIMIT 1")
    fun getLatestForRecipient(recipientId: Long): Reservation?

    @Insert
    fun insertLog(log: StatusLog)

    @Query("SELECT * FROM status_logs WHERE reservationId = :reservationId ORDER BY timestamp ASC, id ASC")
    fun getLogs(reservationId: Long): List<StatusLog>
}

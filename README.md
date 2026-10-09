# Re:Feast

Community food rescue app for SWC3403 / SWC3713 Mobile Application Development.
Donors list surplus food, recipients reserve it, and volunteer collectors deliver it.

**Team:** Zubair Deniel bin Zama, Muhammad Farul Ikhwan

## How to run

1. Open the `ReFeast` folder in Android Studio (File > Open).
2. Let Gradle sync. If Android Studio asks to install SDK Platform 35 or upgrade anything, accept the SDK install. You can ignore upgrade suggestions.
3. Requirements: a recent Android Studio (Otter 3, 2025.2.3, or newer) with its default Java (17 or newer, Java 25 works). The project uses Gradle 9.1 and Android Gradle Plugin 9.0.1. The first sync may also offer to install Android SDK Build Tools 36; accept it.
4. Run on an emulator or phone with Android 8.0 (API 26) or newer.

The first launch fills the database with demo data, so every screen has something to show.

## Demo accounts

All passwords are `password123`.

| Role | Email |
|---|---|
| Donor (Sweet Crumb Bakery) | donor@refeast.my |
| Recipient (Rumah Amal Harapan) | recipient@refeast.my |
| Volunteer Collector (Ahmad Fikri) | volunteer@refeast.my |

You can also create new accounts from the "Sign up" link on the login screen.
To start again with fresh demo data, clear the app's storage (Settings > Apps > Re:Feast > Storage > Clear storage).

## The six required features

| Feature (from brief) | Where it is | How it works |
|---|---|---|
| 1. Surplus-Food Listing | Donor > **Donate** tab | Title, category, quantity + unit, photo, pickup location, area, deadline and description. Every field is validated before saving. |
| 2. Search / Filter | **Home** tab | Live text search (title, description, donor, location), category chips and an area dropdown. Expired food disappears automatically. |
| 3. Reservation System | Recipient > tap a listing > **Reserve** | Recipient picks a quantity and pickup time. The Repository checks there is enough food left, takes that quantity off the listing, then saves the reservation, so food cannot be over-reserved and a recipient cannot reserve the same item twice. |
| 4. Pickup Scheduling | Reserve dialog, **Accept Task**, **Reschedule Pickup** | Pickup time must be in the future and before the donor's deadline. Volunteers set their own pickup time when they accept a task. |
| 5. Collection-Status Update | Reservation screen | Reserved > In-Transit > Delivered (or Cancelled). Volunteers update deliveries; recipients who collect themselves mark it Collected. Each change is stamped on a timeline. |
| 6. Donation / Collection History | **History** tab (all roles) | Past and active transactions with filters: All, Active, Completed, Cancelled. Donors also see each listing's final status (Completed, Withdrawn, Expired) on their Home tab. |

## Icons and images

- Each food category has its own built-in illustration (bread, fruit and vegetables, rice bowl, cans, drinks). It is shown for any listing without a photo, so the demo data looks right straight away.
- Donors can add a real photo from the gallery when publishing; it replaces the illustration.
- The role selection screen and the bottom navigation use Material-style icons.

## What each role sees

- **Donor:** Home (my listings), Donate, History, Profile. Can withdraw a listing that has no active reservations.
- **Recipient:** Home (browse food), History (my reservations), Profile. Can cancel before pickup; the food goes back to the listing.
- **Volunteer Collector:** Home (browse food), Tasks (open jobs and my active jobs), History, Profile.

## Storage (Room / SQLite)

Local database `refeast.db`, defined in `app/src/main/java/com/refeast/app/data/`.

| Table | Stores |
|---|---|
| `users` | User ID, name, email, hashed password, role, phone, address, area |
| `food_listings` | Item ID, donor ID, title, description, category, quantity, quantity still available, unit, pickup location, area, deadline (expiry time), image path, status |
| `reservations` | Reservation ID, item ID, recipient ID, volunteer ID, quantity, drop-off address, scheduled pickup time, current status |
| `status_logs` | Timestamp history: every status change with who made it and when |

Photos are copied into the app's private storage and the file path is saved in the listing. Passwords are stored as SHA-256 hashes, never as plain text. The logged-in user is remembered with SharedPreferences.

## Demo scenario (matches the brief)

1. Log in as **donor@refeast.my** and publish "10 unsold bread loaves" with a 10:00 PM deadline.
2. Log out, log in as **recipient@refeast.my**, filter by Baked Goods, open the bread and reserve all 10 with "I need a volunteer to deliver this" ticked.
3. Log out, log in as **volunteer@refeast.my**, open **Tasks**, accept the bread job and set pickup for 8:30 PM.
4. Tap **Mark as Picked Up**, then **Mark as Delivered**.
5. The transaction now appears as Delivered in all three users' **History**, with the full timeline.

The seeded data also includes an open delivery task (rice and curry), a completed delivery and a cancelled reservation.

## Project structure

```
app/src/main/java/com/refeast/app/
  data/        Room entities, DAOs, database, repository (business rules), seed data
  util/        Session, validation, time formatting, photo storage, small UI helpers
  *Activity    Role selection, login, sign up, main (bottom nav), listing detail, reservation detail
  *Fragment    Home, Donate, Tasks, History, Profile
  *Adapter     RecyclerView adapters
```

Built with Kotlin, XML layouts, Material Components and Room 2.8. The code is written in a simple style: database queries run directly (`allowMainThreadQueries`), each list reloads in `onResume()` when you return to a screen, and Repository functions return an error message or `null` when they succeed.

# How to Automate Branch Opening Hours

Currently, in the FoodApp backend, restaurant branches are opened and closed manually by the manager. The manager toggles the `isClosed` boolean on the `RestaurantBranch` entity via the `changeOpeningStatus()` method in `BranchServiceImpl.java`. 

Even though the manager can save their opening schedules (which populates the `OpeningHour` entity via `saveOpeningHours()`), the system does not automatically enforce them.

If you are a developer looking to automate this so that branches open and close automatically based on the manager-selected hours, follow this guide.

## 1. Create a Scheduled Task

Spring Boot has built-in scheduling capabilities. We can write a cron job that runs periodically (e.g., every minute) to check all active branches and update their `isClosed` status based on the current time and day.

Create a new file `BranchStatusScheduler.java` inside the `com.toni.FoodApp.scheduler` package:

```java
package com.toni.FoodApp.scheduler;

import com.toni.FoodApp.restaurant.entity.OpeningHour;
import com.toni.FoodApp.restaurant.entity.RestaurantBranch;
import com.toni.FoodApp.restaurant.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BranchStatusScheduler {

    private final BranchRepository branchRepository;

    // Runs at the top of every minute
    @Scheduled(cron = "0 * * * * *") 
    @Transactional
    public void updateBranchOpeningStatuses() {
        log.info("Running BranchStatusScheduler to update branch opening status...");
        
        // Get the current day and time
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Rome")); // Or whichever timezone the restaurants are in!
        DayOfWeek currentDay = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();

        // Retrieve all branches that aren't deleted
        List<RestaurantBranch> allBranches = branchRepository.findAll()
                .stream()
                .filter(b -> !Boolean.TRUE.equals(b.getDeleted()))
                .toList();

        for (RestaurantBranch branch : allBranches) {
            List<OpeningHour> hours = branch.getOpeningHours();
            
            // Find today's opening hours for this branch
            Optional<OpeningHour> todayHours = hours.stream()
                    .filter(h -> h.getDayOfWeek() == currentDay)
                    .findFirst();

            boolean shouldBeOpen = false;

            if (todayHours.isPresent()) {
                OpeningHour th = todayHours.get();
                // Check if the current time falls within the open window
                if (!currentTime.isBefore(th.getOpenTime()) && currentTime.isBefore(th.getCloseTime())) {
                    shouldBeOpen = true;
                }
            }

            // The branch entity uses 'isClosed'
            boolean shouldBeClosed = !shouldBeOpen;

            if (branch.isClosed() != shouldBeClosed) {
                branch.setClosed(shouldBeClosed);
                branchRepository.save(branch);
                log.info("Branch ID {} status changed to: {}", branch.getId(), (shouldBeOpen ? "OPEN" : "CLOSED"));
            }
        }
    }
}
```

## 2. Enable Scheduling (if not already enabled)

Make sure that your main Spring Boot Application class (`FoodAppApplication.java`) or a Configuration class is annotated with `@EnableScheduling`. 

```java
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // Add this if missing
public class FoodAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoodAppApplication.class, args);
    }
}
```

## 3. Handle Manual Overrides (Optional)

You need to decide what to do with the existing manual toggle feature:

**Option A (Deprecate Manual Toggle):**
If you want the schedule to be the *absolute source of truth*, you should deprecate or remove the `changeOpeningStatus()` method in `BranchServiceImpl.java`. The manager will only configure the schedule, and the system handles the rest.

**Option B (Manual Override):**
If a manager wants to close early on a specific day, you might still want a manual toggle. However, you'll need to add a new column to the `RestaurantBranch` entity like `private boolean manualOverrideActive = false;`.
Your scheduler will then need to respect this override:
```java
// Inside the scheduler loop
if (branch.isManualOverrideActive()) {
    continue; // Skip automatic update for this branch today
}
```

## 4. Considerations & Gotchas

1.  **Timezones**: You must establish a consistent timezone strategy. Is `LocalTime.now()` pulling the server's UTC time, or the time where the restaurant actually is? In `BranchStatusScheduler.java`, use `LocalDateTime.now(ZoneId.of(...))` if the database saves hours in the local restaurant's timezone.
2.  **Midnight Crossovers**: If a restaurant is open from `20:00` to `02:00` the next day, the simple `currentTime.isBefore(closeTime)` logic will fail. You'll need more complex logic to check if `closeTime < openTime` (which indicates the shift crosses midnight).
3.  **Lazy Loading**: The `@Scheduled` method opens a `@Transactional` block, ensuring that when `branch.getOpeningHours()` is called, Hibernate can fetch the lazily loaded collection. If you omit `@Transactional`, you will encounter a `LazyInitializationException`.

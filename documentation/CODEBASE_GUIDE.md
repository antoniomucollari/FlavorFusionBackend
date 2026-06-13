# FoodApp Developer Handbook

This document provides a deep dive into the internal workings of the FoodApp codebase. It is designed for developers who need to modify existing features or add new ones. Rather than just explaining what the technologies are, this guide explains *how the code is structured and how data flows through the system*.

## 1. Core Domain Entities & Relationships

The database schema is heavily relational, utilizing JPA/Hibernate. Understanding the core entities is crucial.

### The Restaurant Hierarchy
- **`Restaurant`**: The top-level brand (e.g., "McDonald's"). Contains the base `Menu` items, which act as templates.
- **`RestaurantBranch`**: A physical location of a restaurant. It contains specific `OpeningHour`s, `BranchMenuItem`s (branch-specific pricing/availability of the base menu), and a location (Geospatial `Point`).
- **`Menu` & `Category`**: The `Menu` entity represents a master item (e.g., "Big Mac"). It belongs to a `Category` (e.g., "Burgers").
- **`BranchMenuItem`**: Maps a master `Menu` item to a specific `RestaurantBranch`. This allows different branches to have different prices or mark an item as sold out without affecting other branches.
- **Variants (Option Groups)**: Items can have customizations (e.g., "Choose Sauce"). These are managed via `OptionGroup` and `OptionVariant` entities.

### Users & Roles
- **`User`**: The central authentication entity. 
- **Roles**: Managed via the `role` package. A user can be a `CUSTOMER`, `RESTAURANT_MANAGER` (tied to a specific `RestaurantBranch`), or `ADMIN`.
- **`DeliverTo`**: Addresses saved by the user, linked to geospatial coordinates.

## 2. Key Business Flows

### Cart & Checkout Flow
The cart logic is strictly tied to a specific branch to prevent users from ordering from multiple branches simultaneously.
1. **Adding to Cart (`cart` package)**: 
   - Managed in `CartServiceImpl.java`. 
   - When an item is added, it creates a `CartItem` linked to a user's `Cart`. It validates that the `BranchMenuItem` is available and belongs to the active branch.
   - Any variants chosen are saved as `CartItemVariant`s.
2. **Placing an Order (`order` package)**:
   - Managed in `OrderServiceImpl.java`.
   - When checkout is initiated, the `Cart` is converted into an `Order`. 
   - `CartItem`s become `OrderItem`s. 
   - The system verifies the user's location against the `RestaurantBranch`'s delivery radius using geospatial logic.
   - The total price is calculated, and a `PaymentIntent` (Stripe) is generated.

### Order Status Lifecycle
The `enums.OrderStatus` tracks an order's progress.
1. `PENDING`: Order created, awaiting payment confirmation.
2. `CONFIRMED`: Payment successful (Webhooks from Stripe confirm this in `PaymentServiceImpl.java`).
3. `PREPARING`: The restaurant manager accepts the order via the Manager Dashboard.
4. `OUT_FOR_DELIVERY` / `DELIVERED`: Handled by the manager or automated dispatch system.

> **Tip:** Real-time updates for order statuses are broadcasted via WebSockets (`config.WebSocketConfig`) to both the Customer and the Manager dashboards.

## 3. Geospatial Queries & Location Logic

The application uses `hibernate-spatial` and a PostGIS-enabled PostgreSQL database.
- **Entity Definition**: Locations are stored as `org.locationtech.jts.geom.Point`. Look at `RestaurantBranch.java` or `DeliverTo.java`.
- **Distance Calculation**: The application uses native PostGIS functions (like `ST_DWithin`) or the Google Maps API (`DistanceMatrixService.java`) to calculate exact travel distances and verify if a customer is within a branch's `deliveryRadiusInKm`.
- **Redis Geo Index**: To quickly find nearby branches without hitting the database, branch coordinates are cached in Redis. See `BranchServiceImpl.rebuildGeoIndexOnStartup()`.

## 4. How to Navigate the Services

If you need to make changes, here is where you should look:

*   **Want to change how opening hours are enforced?** 
    Look in `restaurant.service.BranchServiceImpl` and `BranchStatusScheduler` (if implemented). Currently, managers manually toggle `isClosed`, but the logic for branch availability is centralized here.
*   **Want to add a new Payment Method?** 
    Look at the `payment` package. You will need to update `StripePaymentService` and the `PaymentMethodEntity`.
*   **Want to modify the Email Templates?** 
    The HTML templates are located in `src/main/resources/templates/`. The logic for sending them asynchronously is in `email_notification.service.EmailServiceImpl`.
*   **Want to change Cart validation (e.g., minimum order limits)?** 
    Look at `cart.service.CartServiceImpl`. There are validations verifying if the subtotal meets the branch's `minOrderAmount`.
*   **Want to manage Image Uploads?** 
    Look at `aws.service.S3Service`. Images are uploaded directly to S3 buckets, and `Thumbnailator` is used to resize them before upload to save bandwidth.

## 5. Security & Access Control

The app uses Spring Security with JWT.
- **Filters**: `JwtAuthenticationFilter.java` intercepts requests, validates the token, and sets the SecurityContext.
- **Method Security**: Look for `@PreAuthorize` annotations on controllers. For example, `@PreAuthorize("hasRole('RESTAURANT_MANAGER')")` ensures only managers can access certain `BranchController` endpoints.
- **Entity Ownership**: Even if a user is a manager, services (like `BranchServiceImpl`) manually check if the branch they are trying to modify actually belongs to them (`belongsToRestaurant(Long restaurantId)` method).

## Summary for Developers

1. **Start at the Controller**: Trace the endpoint definition in the `controller` packages.
2. **Follow to the Service**: Business logic is strictly kept in `*ServiceImpl` classes.
3. **Check the Mapper**: `ModelMapper` (and custom mappers like `RestaurantBranchMapper`) are used heavily to convert between Entities and DTOs. Never return an Entity directly from a Controller.
4. **Repositories**: Complex queries are often written using `@Query` annotations with JPQL or native SQL inside the `repository` interfaces.

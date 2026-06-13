# Core Business Logic & Domain Flows

This document details the business rules and architectural flows for the major domain areas in the FoodApp system, expanding beyond the order and delivery logic. It is written to provide a clear narrative of how the application enforces consistency and manages complex states.

---

## 1. Restaurant & Menu Management Flow

### The "Master Menu" vs "Branch Menu" Architecture
To handle cases where a single restaurant brand (e.g., "KFC") has multiple branches with potentially different pricing or availability, the menu system uses a two-tier architecture.

1. **The Master Entity (`Menu` and `Category`)**
   - The restaurant owner creates a `Menu` item (e.g., "Bucket Meal"). It contains the global `name`, `description`, and `imageUrl`.
   - This item is attached to a `Category` (e.g., "Fried Chicken").
   - At this level, the item has NO price and NO concept of "availability."

2. **The Branch-Specific Entity (`BranchMenuItem`)**
   - When a master `Menu` item is created, the system must link it to specific `RestaurantBranch`es.
   - The `BranchMenuItem` entity joins the `Menu` to the `RestaurantBranch`.
   - **Business Rule:** Pricing, `isAvailable` (sold out toggles), and `isHighlighted` are stored *here*. This allows Branch A to sell the Bucket Meal for $15 while Branch B sells it for $16, or for Branch A to mark it as sold out while Branch B continues selling it.

### Option Groups & Variants (Customizations)
- Items often have customizations (e.g., "Choose Drink", "Extra Sauce").
- These are managed via `OptionGroup` (the category of choice) and `OptionVariant` (the specific choice and its additional cost).
- **Validation Rule:** Option Groups can have a `minChoices` and `maxChoices`. During cart addition, the backend must validate that the user's selections do not violate these boundaries.

---

## 2. Cart & Validation Logic

The cart is designed to strictly map to a single branch to prevent logistical nightmares of split-branch deliveries.

### Adding Items to the Cart
When a customer attempts to add an item to their cart, the system performs several strict checks:

1. **Single-Branch Constraint**: 
   - The system checks if the user's cart already contains items. If it does, it verifies that the new item's `branchId` matches the existing items. 
   - *Action if failed*: Throw a `BadRequestException` (e.g., "Clear your cart to order from a different branch").

2. **Availability Check**:
   - The system checks the `BranchMenuItem.isAvailable` flag.
   - *Action if failed*: Throw an exception preventing the addition of sold-out items.

3. **Variant Processing**:
   - The backend maps the selected `OptionVariant` IDs provided by the frontend.
   - It validates that the selected variants actually belong to the `OptionGroup`s associated with the `Menu` item.
   - It computes the item's total cost: `Base Price (from BranchMenuItem) + Sum of Selected Variants`.

### Checkout Pre-flight Checks (Place Order)
Before converting a `Cart` into an `Order`, the following rules are enforced:

1. **Minimum Order Amount**:
   - The subtotal of the cart must be `>=` the `minOrderAmount` set on the `RestaurantBranch`.
   
2. **Geospatial Delivery Radius**:
   - The customer must select a delivery address (`DeliverTo` entity).
   - The system uses PostGIS (`ST_Distance`) or the Google Maps API Distance Matrix to calculate the actual routing distance between the branch's coordinates and the customer's coordinates.
   - *Action if failed*: If `distance > branch.deliveryRadiusInKm`, checkout is blocked with an "Out of delivery zone" error.

---

## 3. Authentication & Profile Management

### JWT & Role-Based Access Control (RBAC)
Authentication is stateless using JSON Web Tokens (JWT). The system uses strict Role-Based Access Control to partition data.

1. **Entity Ownership Enforcement (The "Belongs To" Rule)**
   - It is not enough for a user to have the `RESTAURANT_MANAGER` role to edit a branch.
   - Every manager-facing service method (e.g., editing opening hours, changing a branch menu price) includes an explicit ownership check.
   - The system fetches the `currentUser` via the Security Context, retrieves their assigned `Restaurant` or `RestaurantBranch`, and compares the IDs.
   - *Action if failed*: `ForbiddenException` is thrown. A manager from Branch A cannot maliciously send an API request to change the menu of Branch B.

2. **Customer Address Geocoding**
   - When a customer adds a new address to their profile, the frontend sends the textual address and the latitude/longitude.
   - The backend converts these coordinates into a JTS `Point` (Hibernate Spatial) before saving it to the `DeliverTo` table, ensuring it is ready for fast geospatial distance calculations during checkout.

---

## 4. Media Management & AWS S3 Integration

Images (restaurant logos, menu items, user profiles) are handled through a dedicated AWS S3 pipeline.

1. **The Upload Flow**
   - The client uploads a multipart file to the backend.
   - **Optimization Rule:** Before uploading to S3, the backend uses `Thumbnailator` to resize and compress the image. This ensures high-resolution 10MB photos taken by restaurant owners are scaled down to web-friendly sizes (e.g., 500x500 pixels, max 200KB), drastically reducing AWS bandwidth costs and improving frontend load times.
   - The compressed image is pushed to the S3 bucket via the `software.amazon.awssdk` client.

2. **Database Storage**
   - The backend does *not* store raw images in PostgreSQL. It stores the publicly accessible S3 URL (e.g., `https://my-bucket.s3.eu-central-1.amazonaws.com/menu_items/123_burger.jpg`).
   - If an entity is deleted or an image is updated, an event is triggered to delete the stale object from the S3 bucket to prevent orphaned files from accumulating.

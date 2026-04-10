# API Documentation - Training Notification Service

Complete reference for all public APIs, internal services, and integration points. This document is designed for developers who need to understand, maintain, or extend the notification system.

---

## Table of Contents

1. [Application Entry Point](#application-entry-point)
2. [REST API Endpoints](#rest-api-endpoints)
3. [Error Handling](#error-handling)
4. [Kafka Consumers](#kafka-consumers)
5. [Core Services](#core-services)
6. [Notification Senders](#notification-senders)
7. [Strategy Pattern: Notification Factory](#strategy-pattern-notification-factory)
8. [User Lookup & Caching](#user-lookup--caching)
9. [Scheduled Tasks](#scheduled-tasks)
10. [Data Access Layer](#data-access-layer)
11. [Data Transfer Objects (DTOs)](#data-transfer-objects-dtos)
12. [Database Entities](#database-entities)
13. [Configuration Classes](#configuration-classes)
14. [Component Dependency Graph](#component-dependency-graph)

---

## Application Entry Point

### `TrainingNotificationApplication`

**Location:** `com.example.training_notification.TrainingNotificationApplication`

**Annotations:**
- `@SpringBootApplication` - Enables auto-configuration, component scanning, and property support
- `@EnableAsync` - Activates `@Async` method execution for background processing
- `@EnableScheduling` - Activates `@Scheduled` task execution

#### `main(String[] args)`

```java
public static void main(String[] args)
```

**Purpose:** Bootstrap method that starts the Spring Boot application context.

**What it initializes:**
1. Embedded web server (Tomcat) on port 8086
2. Kafka consumer listeners for `training-events` and `training-topic`
3. Redis cache connection pool
4. PostgreSQL connection pool (HikariCP)
5. Async thread pool executor for notification sending
6. Scheduled task runner for cron jobs

**Usage:**
```bash
./mvnw spring-boot:run
```

---

## REST API Endpoints

### `NotificationController`

**Base Path:** `/api/v1/notifications`

**Purpose:** Provides HTTP endpoints for testing and external integration with the notification system.

#### POST `/api/v1/notifications/test-send`

```java
@PostMapping("/test-send")
public ResponseEntity<Void> testNotification(@RequestBody TrainingDTO trainingDTO)
```

**Description:** Accepts a training event payload and publishes it asynchronously to the Kafka topic `training-events`. This endpoint is primarily used for manual testing of the complete notification pipeline: REST → Kafka → Listener → Email/Telegram.

**Request Headers:**
```
Content-Type: application/json
```

**Request Body (`TrainingDTO`):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "telegramTag": "@user",
  "training_name": "Morning Yoga",
  "data": "2023-10-25",
  "status": "COMPLETED",
  "email": "user@example.com",
  "exercises": []
}
```

**Field Descriptions:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `userId` | UUID | Yes | Unique identifier of the user |
| `telegramTag` | String | No | Telegram username or chat ID (e.g., `@user` or numeric ID) |
| `training_name` | String | Yes | Name of the training session |
| `data` | String | No | Training date or additional metadata |
| `status` | String | Yes | Training status (e.g., `COMPLETED`, `SCHEDULED`, `CANCELLED`) |
| `email` | String | No | User's email (can be resolved from `userId` via database) |
| `exercises` | List\<Object\> | No | List of exercise details (currently unused) |

**Response:**
- **Status:** `202 Accepted`
- **Body:** Empty (processing happens asynchronously)

**Internal Flow:**
1. Controller receives `TrainingDTO` via HTTP POST
2. Calls `KafkaTemplate.send("training-events", trainingDTO)`
3. Returns immediately without waiting for Kafka acknowledgment
4. Kafka broker delivers message to `TrainingListener` (async)
5. `TrainingListener` invokes `NotificationService.processAndSendNotification()`
6. Email is sent via `EmailNotificationService` (async thread pool)
7. If `telegramTag` is present, Telegram message is sent via `TelegramNotificationService`

**Example Usage:**
```bash
curl -X POST http://localhost:8086/api/v1/notifications/test-send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "telegramTag": "@john_doe",
    "training_name": "HIIT Workout",
    "data": "2026-04-10",
    "status": "COMPLETED",
    "exercises": []
  }'
```

---

## Error Handling

### `GlobalExceptionHandler`

**Location:** `com.example.training_notification.exception.GlobalExceptionHandler`

**Annotations:** `@RestControllerAdvice`

**Purpose:** Centralized exception handling for all REST controllers. Converts Java exceptions into HTTP responses with consistent JSON error format.

#### `handleException(Exception ex)`

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<Map<String, String>> handleException(Exception ex)
```

**Triggers:** Any unhandled exception in REST controllers (except `IllegalArgumentException`).

**Response:**
- **HTTP Status:** `500 Internal Server Error`
- **Body:**
```json
{
  "error": "Internal Server Error",
  "message": "Detailed error message from exception"
}
```

**Logging:** Logs error at `ERROR` level with full stack trace.

---

#### `handleBadRequest(IllegalArgumentException ex)`

```java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex)
```

**Triggers:** Invalid input data, missing required entities (e.g., user not found).

**Response:**
- **HTTP Status:** `400 Bad Request`
- **Body:**
```json
{
  "error": "Bad Request",
  "message": "User not found with ID: 550e8400-e29b-41d4-a716-446655440000"
}
```

**Logging:** Logs warning at `WARN` level (no stack trace).

**Note:** Spring selects the most specific handler, so `IllegalArgumentException` routes to `handleBadRequest` instead of `handleException`.

---

## Kafka Consumers

### `TrainingListener`

**Location:** `com.example.training_notification.listener.TrainingListener`

**Kafka Configuration:**
- **Topic:** `training-events`
- **Consumer Group:** `notification-clean-group`
- **Auto-offset-reset:** Based on `application.properties`

#### `listen(TrainingDTO trainingDTO)`

```java
@KafkaListener(topics = "training-events", groupId = "notification-clean-group")
public void listen(TrainingDTO trainingDTO)
```

**Purpose:** Primary consumer for training completion events. Processes notifications via email and optionally Telegram.

**Processing Flow:**
1. Receives `TrainingDTO` from Kafka topic
2. Logs receipt with `userId` and `training_name`
3. Calls `NotificationService.processAndSendNotification(trainingDTO)`:
   - Resolves user email from database (via Redis cache)
   - Sends HTML email notification
   - Logs notification to database
4. If `trainingDTO.telegramTag()` is non-null and non-empty:
   - Formats Russian-language message: `"Тренировка '<name>' завершена! Статус: <status>"`
   - Calls `TelegramNotificationService.sendTelegramMessage(telegramTag, message)`

**Error Handling:**
- All exceptions are caught internally
- Errors are logged at `ERROR` level with full stack trace
- Exceptions are **not rethrown** (message acknowledgment depends on Spring Kafka config)

**Example Message (JSON):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "telegramTag": "@user123",
  "training_name": "Morning Run",
  "data": "2026-04-10",
  "status": "COMPLETED",
  "email": null,
  "exercises": []
}
```

---

### `InteractionListener`

**Location:** `com.example.training_notification.listener.InteractionListener`

**Kafka Configuration:**
- **Topic:** `training-topic`
- **Consumer Group:** `notification-clean-v4`

#### `listen(TrainingDTO trainingData)`

```java
@KafkaListener(topics = "training-topic", groupId = "notification-clean-v4")
public void listen(TrainingDTO trainingData)
```

**Purpose:** Alternative consumer for training events. Processes email notifications only (no Telegram).

**Processing Flow:**
1. Receives `TrainingDTO` from Kafka topic
2. Logs `userId` at `INFO` level
3. Calls `NotificationService.processAndSendNotification(trainingData)`

**Key Difference from `TrainingListener`:**
- Does **not** send Telegram messages
- Listens to a different topic (`training-topic` vs `training-events`)
- Uses a different consumer group

**Warning:** If the same event is published to both topics, duplicate emails may be sent. See known limitations.

---

## Core Services

### `NotificationService`

**Location:** `com.example.training_notification.service.scheduler.NotificationService`

**Purpose:** Orchestrates the notification sending process. Called by Kafka listeners to coordinate email sending and logging.

#### `processAndSendNotification(TrainingDTO training)`

```java
public void processAndSendNotification(TrainingDTO training)
```

**Step-by-Step Execution:**

1. **User Email Resolution:**
   ```java
   String email = userLookupService.getEmailByUserId(training.userId());
   ```
   - Queries Redis cache first
   - Falls back to database if cache miss
   - Throws `IllegalArgumentException` if user not found

2. **Message Formatting:**
   ```java
   String message = String.format("Workout '%s' status: %s", 
       training.training_name(), training.status());
   ```
   Example output: `"Workout 'Morning Yoga' status: COMPLETED"`

3. **Create Notification Request:**
   ```java
   NotificationRequest request = new NotificationRequest(email, message, NotificationType.EMAIL);
   ```

4. **Send Email:**
   ```java
   emailNotificationService.send(request);
   ```
   - Executes asynchronously in thread pool
   - Renders Thymeleaf template
   - Sends via SMTP

5. **Log to Database:**
   ```java
   NotificationLog log = new NotificationLog();
   log.setUserId(training.userId());
   log.setMessage(message);
   log.setSentAt(LocalDateTime.now());
   notificationLogRepository.save(log);
   ```

6. **Error Handling:**
   - All exceptions caught in `try-catch`
   - Errors logged at `ERROR` level: `"Worker error processing notification"`
   - Exceptions are **not propagated** to callers

**Important Notes:**
- The `@KafkaListener` annotation was removed from this class to avoid duplicate processing
- This method is synchronous, but email sending is async
- Database logging happens synchronously

---

## Notification Senders

### `NotificationSender` (Interface)

**Location:** `com.example.training_notification.service.interfaces.NotificationSender`

**Purpose:** Strategy interface for different notification channels. Implemented by email, push, and Telegram services.

#### `send(NotificationRequest request)`

```java
@Async("taskExecutor")
void send(NotificationRequest request);
```

**Annotations:** `@Async("taskExecutor")` - All implementations execute asynchronously in the shared thread pool.

**Parameters:**
- `request` - Unified notification payload containing:
  - `recipient()` - Target (email address, Firebase topic, etc.)
  - `message()` - Message text
  - `type()` - `NotificationType` enum value

---

#### `supports(NotificationType type)`

```java
boolean supports(NotificationType type);
```

**Purpose:** Determines if this sender can handle the given notification type.

**Return Value:**
- `true` - This sender should process the request
- `false` - This sender should skip the request

**Used by:** `NotificationFactory.getSender()` to select the appropriate implementation.

---

### `EmailNotificationService`

**Location:** `com.example.training_notification.service.impl.EmailNotificationService`

**Implements:** `NotificationSender`

**Dependencies:**
- `JavaMailSender` - Spring's email sender
- `TemplateEngine` - Thymeleaf template processor
- `@Value("${spring.mail.username}")` - Sender email address

#### `send(NotificationRequest request)`

```java
@Override
@Async
public void send(NotificationRequest request)
```

**Execution Flow:**

1. **Create MIME Message:**
   ```java
   MimeMessage mimeMessage = javaMailSender.createMimeMessage();
   MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
   ```

2. **Set Headers:**
   - **From:** `gravitya46@gmail.com` (from `spring.mail.username`)
   - **To:** `request.recipient()` (resolved user email)
   - **Subject:** `"Workout notification"`

3. **Render Thymeleaf Template:**
   ```java
   Context context = new Context();
   context.setVariable("trainingName", request.message());
   context.setVariable("trainingDate", LocalDateTime.now());
   context.setVariable("trainingStatus", "RECEIVED");
   String htmlContent = templateEngine.process("training-notification", context);
   ```

4. **Set HTML Body:**
   ```java
   helper.setText(htmlContent, true);
   ```

5. **Send Email:**
   ```java
   javaMailSender.send(mimeMessage);
   log.info("Email sent to {}", request.recipient());
   ```

6. **Error Handling:**
   - Catches `MessagingException` and general `Exception`
   - Logs error at `ERROR` level
   - Does not rethrow exceptions

**Template Location:** `src/main/resources/templates/training-notification.html`

**Template Variables:**
| Variable | Type | Value |
|----------|------|-------|
| `trainingName` | String | Full message text (e.g., `"Workout 'Yoga' status: COMPLETED"`) |
| `trainingDate` | LocalDateTime | Current timestamp when email is sent |
| `trainingStatus` | String | Always `"RECEIVED"` (hardcoded) |

**Async Behavior:**
- Runs in `taskExecutor` thread pool
- Returns immediately to caller
- Email sends in background

---

#### `supports(NotificationType type)`

```java
@Override
public boolean supports(NotificationType type)
```

**Return:** `true` if `type == NotificationType.EMAIL`, otherwise `false`.

---

### `TelegramNotificationService`

**Location:** `com.example.training_notification.service.impl.TelegramNotificationService`

**Conditional Bean:** `@ConditionalOnProperty(name = "telegram.enabled", havingValue = "true", matchIfMissing = true)`

**Enabled by default.** Disable with `telegram.enabled=false` in `application.properties`.

**Dependencies:**
- `RestTemplate` - HTTP client for Telegram API
- `@Value("${telegram.bot.token}")` - Bot authentication token

#### `sendTelegramMessage(String target, String text)`

```java
public void sendTelegramMessage(String target, String text)
```

**Purpose:** Sends a text message to a Telegram user or group via Bot API.

**API Endpoint:**
```
GET https://api.telegram.org/bot<token>/sendMessage?chat_id=<target>&text=<text>
```

**Parameters:**
- `target` - Telegram chat ID or username (e.g., `@john_doe` or `-1001234567890` for groups)
- `text` - Message text to send

**Execution Flow:**

1. **Build URL:**
   ```java
   String url = String.format(
       "https://api.telegram.org/bot%s/sendMessage?chat_id=%s&text=%s",
       botToken, target, UriUtils.encode(text, StandardCharsets.UTF_8)
   );
   ```

2. **Send Request:**
   ```java
   restTemplate.getForObject(url, String.class);
   ```

3. **Log Success:**
   ```java
   log.info("Telegram message sent to {}", target);
   ```

4. **Error Handling:**
   - Catches all exceptions (network errors, invalid chat ID, etc.)
   - Logs error at `ERROR` level
   - Does not rethrow exceptions

**Example Usage:**
```java
telegramNotificationService.sendTelegramMessage(
    "@john_doe",
    "Тренировка 'Morning Run' завершена! Статус: COMPLETED"
);
```

**Telegram API Documentation:** https://core.telegram.org/bots/api#sendmessage

**Known Limitations:**
- Uses GET request (message length limited by URL length)
- No retry logic for failed requests
- No support for markdown or HTML formatting in messages

---

### `PushNotificationService`

**Location:** `com.example.training_notification.service.impl.PushNotificationService`

**Implements:** `NotificationSender`

**Conditional Bean:** `@ConditionalOnProperty(name = "firebase.enabled", havingValue = "true", matchIfMissing = false)`

**Disabled by default.** Enable with `firebase.enabled=true` and provide `serviceAccountKey.json` in classpath.

**Dependencies:**
- `FirebaseMessaging` - Firebase Admin SDK
- `NotificationLogRepository` - Audit logging

#### `send(NotificationRequest request)`

```java
@Override
@Async
public void send(NotificationRequest request)
```

**Purpose:** Sends a push notification via Firebase Cloud Messaging (FCM) to a topic.

**Execution Flow:**

1. **Build FCM Notification:**
   ```java
   Notification notification = Notification.builder()
       .setTitle("Training Notification")
       .setBody(request.message())
       .build();
   ```

2. **Build FCM Message:**
   ```java
   Message message = Message.builder()
       .setNotification(notification)
       .setTopic(request.recipient())
       .build();
   ```

3. **Send via Firebase:**
   ```java
   String response = FirebaseMessaging.getInstance().send(message);
   log.info("Successfully sent message to FCM: {}", response);
   ```

4. **Log to Database:**
   ```java
   NotificationLog log = new NotificationLog();
   log.setUserId(null); // Topic-based, not user-specific
   log.setMessage(request.message());
   log.setSentAt(LocalDateTime.now());
   notificationLogRepository.save(log);
   ```

5. **Error Handling:**
   - Catches `FirebaseMessagingException` and general `Exception`
   - Logs error at `ERROR` level
   - Does not rethrow exceptions

**Important Notes:**
- Uses **topic-based** messaging (`setTopic()`), not device tokens
- `userId` in log is `null` because topics don't map to single users
- Requires `serviceAccountKey.json` in `src/main/resources/`

---

#### `sendTrainingsNotification(TrainingDTO training)`

```java
public void sendTrainingsNotification(TrainingDTO training)
```

**Purpose:** Legacy method that only logs to database. Does **not** send actual push notification.

**Execution Flow:**

1. **Format Message:**
   ```java
   String message = String.format("Training %s completed", training.training_name());
   ```

2. **Log to Database:**
   ```java
   NotificationLog log = new NotificationLog();
   log.setUserId(training.userId());
   log.setMessage(message);
   log.setSentAt(LocalDateTime.now());
   notificationLogRepository.save(log);
   ```

**Warning:** This method is misleading - it does NOT send a Firebase push notification. Use `send(NotificationRequest)` for actual FCM delivery.

---

#### `supports(NotificationType type)`

```java
@Override
public boolean supports(NotificationType type)
```

**Return:** `true` if `type == NotificationType.PUSH`, otherwise `false`.

---

## Strategy Pattern: Notification Factory

### `NotificationFactory`

**Location:** `com.example.training_notification.factory.NotificationFactory`

**Purpose:** Implements the Strategy pattern to select the appropriate notification sender based on type.

**Dependencies:**
- `List<NotificationSender>` - All notification sender beans (auto-injected by Spring)

#### `getSender(NotificationType type)`

```java
public NotificationSender getSender(NotificationType type)
```

**Algorithm:**

1. Iterates through all `NotificationSender` beans
2. Calls `supports(type)` on each sender
3. Returns the **first** sender where `supports(type) == true`
4. If no match found, throws `IllegalArgumentException`

**Example:**
```java
NotificationSender sender = notificationFactory.getSender(NotificationType.EMAIL);
// Returns EmailNotificationService
```

**Error Case:**
```java
notificationFactory.getSender(NotificationType.SMS);
// Throws: IllegalArgumentException("Notification sender not found for type: SMS")
```

**Current Sender Mapping:**
| NotificationType | Implementation |
|------------------|----------------|
| `EMAIL` | `EmailNotificationService` |
| `PUSH` | `PushNotificationService` |
| `TELEGRAM` | ❌ Not implemented (uses direct method call) |
| `SMS` | ❌ Not implemented |

**Design Note:** Telegram notifications bypass this factory and call `TelegramNotificationService.sendTelegramMessage()` directly. This is intentional because Telegram uses a different method signature (not `NotificationRequest`).

---

## User Lookup & Caching

### `UserLookupService`

**Location:** `com.example.training_notification.service.impl.UserLookupService`

**Purpose:** Resolves user email by UUID with Redis caching to reduce database queries.

**Dependencies:**
- `UserRepository` - JPA repository for user entities
- `@Cacheable("userEmails", key = "#userId")` - Redis cache abstraction

#### `getEmailByUserId(UUID userId)`

```java
@Cacheable("userEmails", key = "#userId")
public String getEmailByUserId(UUID userId)
```

**Execution Flow:**

1. **Check Redis Cache:**
   - Cache name: `userEmails`
   - Cache key: `userId` (UUID)
   - If cache hit → return cached email immediately

2. **Database Query (cache miss):**
   ```java
   User user = userRepository.findById(userId)
       .orElseThrow(() -> new IllegalArgumentException(
           "User not found with ID: " + userId));
   ```

3. **Cache Result:**
   - Stores email in Redis with TTL (configured in `application.properties`)
   - Subsequent calls return cached value

4. **Return Email:**
   ```java
   return user.getEmail();
   ```

**Error Handling:**
- Throws `IllegalArgumentException` if user not found in database
- Exception message: `"User not found with ID: <userId>"`
- Exception is **not caught** by callers (propagates to global handler)

**Cache Configuration:**
- Backend: Redis (configured in `application.properties`)
- Cache name: `userEmails`
- Key: User UUID
- TTL: Based on Redis configuration (default: no expiration)

**Example Usage:**
```java
String email = userLookupService.getEmailByUserId(
    UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
// Returns: "user@example.com"
```

**Performance Impact:**
- **First call:** ~50-100ms (database query)
- **Subsequent calls:** ~1-5ms (Redis cache hit)

**Cache Invalidation:**
- Currently **no explicit invalidation**
- Email changes require manual cache eviction or TTL expiration
- Consider adding `@CacheEvict` if users can update their email

---

## Scheduled Tasks

### `WeeklyReportScheduler`

**Location:** `com.example.training_notification.service.scheduler.WeeklyReportScheduler`

**Purpose:** Sends weekly training statistics reports to all users.

**Schedule:** Every Sunday at 20:00 (server timezone)

#### `sendWeeklyReports()`

```java
@Scheduled(cron = "0 0 20 * * SUN")
public void sendWeeklyReports()
```

**Execution Flow:**

1. **Collect Statistics:**
   ```java
   List<UserStatsDTO> statsList = collectStatistics();
   ```

2. **Iterate Over Users:**
   ```java
   for (UserStatsDTO stats : statsList) {
       // Format email content
       String message = String.format(
           "Weekly Report for %s%nTotal Trainings: %d%nCompleted: %d%nMinutes: %d%nCalories: %.2f%nProgress: %s%nAchievements: %s",
           stats.userName(),
           stats.totalTrainings(),
           stats.completedTrainings(),
           stats.totalMinutes(),
           stats.caloriesBurned(),
           stats.progressMessage(),
           String.join(", ", stats.achievements())
       );
   ```

3. **Send Email:**
   ```java
   NotificationRequest request = new NotificationRequest(
       stats.email(), message, NotificationType.EMAIL);
   emailNotificationService.send(request);
   ```

4. **Log Success:**
   ```java
   log.info("Weekly reports sent to {} users", statsList.size());
   ```

**Cron Expression Breakdown:**
```
0 0 20 * * SUN
│ │ │  │ │ └── Sunday
│ │ │  │ └──── Every month
│ │ │  └────── Every day of month
│ │ └───────── Hour 20 (8 PM)
│ └─────────── Minute 0
└───────────── Second 0
```

**Timezone:** Server default timezone (configure with `SPRING_TIMEZONE` environment variable)

---

#### `collectStatistics()`

```java
private List<UserStatsDTO> collectStatistics()
```

**Purpose:** Aggregates weekly training statistics for all users.

**Current Implementation:**
```java
return List.of(new UserStatsDTO(
    UUID.randomUUID(),
    "Test User",
    "test@example.com",
    5,   // totalTrainings
    3,   // completedTrainings
    150, // totalMinutes
    1200.50, // caloriesBurned
    "Great progress!",
    List.of("Early Bird", "Consistency Champion")
));
```

**⚠️ Important:** This method currently returns **hardcoded demo data**. No actual database aggregation is performed.

**Future Enhancement Needed:**
- Query `notification_logs` table for training counts
- Calculate completion rates
- Aggregate training duration and calories
- Generate personalized achievement badges

---

## Data Access Layer

### `UserRepository`

**Location:** `com.example.training_notification.repository.UserRepository`

**Extends:** `JpaRepository<User, UUID>`

**Entity:** `User`

**Inherited Methods:**
| Method | Description |
|--------|-------------|
| `Optional<User> findById(UUID id)` | Find user by UUID |
| `List<User> findAll()` | Get all users |
| `User save(User user)` | Create or update user |
| `void deleteById(UUID id)` | Delete user by UUID |
| `boolean existsById(UUID id)` | Check if user exists |
| `long count()` | Count total users |

**Custom Queries:** None (relies on standard JPA methods)

---

### `NotificationLogRepository`

**Location:** `com.example.training_notification.repository.NotificationLogRepository`

**Extends:** `JpaRepository<NotificationLog, Long>`

**Entity:** `NotificationLog`

**Inherited Methods:**
| Method | Description |
|--------|-------------|
| `Optional<NotificationLog> findById(Long id)` | Find log entry by ID |
| `List<NotificationLog> findAll()` | Get all log entries |
| `NotificationLog save(NotificationLog log)` | Create or update log entry |
| `void deleteById(Long id)` | Delete log entry |
| `List<NotificationLog> findByUserId(UUID userId)` | Find all logs for specific user |
| `long count()` | Count total log entries |

**Custom Queries:** None (relies on standard JPA methods)

**Note:** `findByUserId()` is derived from method name (Spring Data JPA convention).

---

## Data Transfer Objects (DTOs)

### `TrainingDTO`

**Location:** `com.example.training_notification.dto.TrainingDTO`

**Type:** Java Record

```java
public record TrainingDTO(
    UUID userId,
    String telegramTag,
    String training_name,
    String data,
    String status,
    String email,
    List<Object> exercises
) {}
```

**Purpose:** Unified payload for training events. Used in Kafka messages and REST API.

**Field Details:**
| Field | Type | Nullable | Description |
|-------|------|----------|-------------|
| `userId` | UUID | No | Unique user identifier |
| `telegramTag` | String | Yes | Telegram username/chat ID |
| `training_name` | String | No | Name of training session |
| `data` | String | Yes | Training date or metadata |
| `status` | String | No | Training status (e.g., `COMPLETED`, `SCHEDULED`) |
| `email` | String | Yes | User email (can be resolved via `userId`) |
| `exercises` | List\<Object\> | Yes | Exercise details (currently unused) |

**Usage:**
- Kafka message payload for `training-events` and `training-topic`
- REST request body for `/api/v1/notifications/test-send`

**Example JSON:**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "telegramTag": "@user",
  "training_name": "Morning Yoga",
  "data": "2026-04-10",
  "status": "COMPLETED",
  "email": "user@example.com",
  "exercises": [
    {"name": "Sun Salutation", "duration": 300}
  ]
}
```

---

### `NotificationRequest`

**Location:** `com.example.training_notification.dto.NotificationRequest`

**Type:** Java Record

```java
public record NotificationRequest(
    String recipient,
    String message,
    NotificationType type
) {}
```

**Purpose:** Unified request object for all notification senders.

**Field Details:**
| Field | Type | Description |
|-------|------|-------------|
| `recipient` | String | Target (email address, Firebase topic, Telegram chat ID) |
| `message` | String | Message content |
| `type` | NotificationType | Notification channel (EMAIL, PUSH, etc.) |

**Usage:**
- Passed to `NotificationSender.send(request)`
- Created by `NotificationService.processAndSendNotification()`

**Example:**
```java
new NotificationRequest(
    "user@example.com",
    "Workout 'Yoga' status: COMPLETED",
    NotificationType.EMAIL
);
```

---

### `NotificationType`

**Location:** `com.example.training_notification.dto.NotificationType`

**Type:** Java Enum

```java
public enum NotificationType {
    EMAIL,
    PUSH,
    SMS,
    TELEGRAM
}
```

**Purpose:** Enumerates supported notification channels.

**Values:**
| Value | Implemented | Sender |
|-------|-------------|--------|
| `EMAIL` | ✅ | `EmailNotificationService` |
| `PUSH` | ✅ | `PushNotificationService` |
| `SMS` | ❌ | Not implemented |
| `TELEGRAM` | ⚠️ | `TelegramNotificationService` (bypasses factory) |

**Usage:**
- Routing in `NotificationFactory.getSender()`
- Type safety in `NotificationRequest`

---

### `UserStatsDTO`

**Location:** `com.example.training_notification.dto.UserStatsDTO`

**Type:** Java Record

```java
public record UserStatsDTO(
    UUID userId,
    String userName,
    String email,
    int totalTrainings,
    int completedTrainings,
    long totalMinutes,
    double caloriesBurned,
    String progressMessage,
    List<String> achievements
) {}
```

**Purpose:** Aggregated weekly statistics for email reports.

**Field Details:**
| Field | Type | Description |
|-------|------|-------------|
| `userId` | UUID | User identifier |
| `userName` | String | Display name |
| `email` | String | Recipient email |
| `totalTrainings` | int | Total scheduled trainings this week |
| `completedTrainings` | int | Successfully completed trainings |
| `totalMinutes` | long | Total training duration (minutes) |
| `caloriesBurned` | double | Estimated calories burned |
| `progressMessage` | String | Motivational message |
| `achievements` | List\<String\> | Badge names (e.g., "Early Bird") |

**Usage:**
- Returned by `WeeklyReportScheduler.collectStatistics()`
- Used to format weekly report emails

**Example:**
```java
new UserStatsDTO(
    UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
    "John Doe",
    "john@example.com",
    5,
    4,
    180,
    1500.75,
    "You're doing great! Keep it up!",
    List.of("Consistency Champion", "Week Warrior")
);
```

---

## Database Entities

### `User`

**Location:** `com.example.training_notification.entity.User`

**Table:** `users`

**Annotations:** `@Entity`, `@Table(name = "users")`

#### Schema

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | Primary Key | Unique user identifier |
| `email` | VARCHAR(255) | `NOT NULL` | User's email address |

#### JPA Mapping

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false)
    private String email;
}
```

**Relationships:** None (standalone entity)

**Example Row:**
| id | email |
|----|-------|
| `550e8400-e29b-41d4-a716-446655440000` | `user@example.com` |

---

### `NotificationLog`

**Location:** `com.example.training_notification.entity.NotificationLog`

**Table:** `notification_logs`

**Annotations:** `@Entity`, `@Table(name = "notification_logs")`

#### Schema

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | Primary Key, Auto-increment | Log entry ID |
| `user_id` | UUID | `NOT NULL`, `@Column(insertable = false, updatable = false)` | User who received notification |
| `message` | VARCHAR(1000) | Max length 1000 chars | Notification message text |
| `sent_at` | TIMESTAMP | - | When notification was sent |

#### JPA Mapping

```java
@Entity
@Table(name = "notification_logs")
public class NotificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private UUID userId;

    @Column(length = 1000)
    private String message;

    private LocalDateTime sentAt;
}
```

**Note:** `insertable = false, updatable = false` on `user_id` suggests this column is managed separately (possibly by database triggers or legacy schema).

**Example Row:**
| id | user_id | message | sent_at |
|----|---------|---------|---------|
| 1 | `550e8400-...` | `Workout 'Yoga' status: COMPLETED` | `2026-04-10 14:30:00` |

---

## Configuration Classes

### `AsyncConfig`

**Location:** `com.example.training_notification.config.AsyncConfig`

**Annotations:** `@Configuration`, `@EnableAsync`

**Purpose:** Configures asynchronous execution for `@Async` methods.

#### `taskExecutor()`

```java
@Bean(name = "taskExecutor")
public Executor taskExecutor()
```

**Thread Pool Configuration:**
| Parameter | Value | Description |
|-----------|-------|-------------|
| Core Pool Size | 50 | Minimum active threads |
| Maximum Pool Size | 200 | Maximum threads under load |
| Queue Capacity | 2000 | Tasks queued before reaching max threads |
| Thread Name Prefix | `NotificationThread-` | Prefix for thread names |
| Rejection Policy | `CallerRunsPolicy` | Run task in calling thread when pool is full |

**Behavior:**
- Starts with 50 threads
- Scales up to 200 threads when queue is full
- Queues up to 2000 pending tasks
- If queue is full and 200 threads are busy, caller executes the task

**Used By:**
- `EmailNotificationService.send()`
- `PushNotificationService.send()`
- Any method annotated with `@Async("taskExecutor")`

**Monitoring:**
Thread names will appear as:
- `NotificationThread-1`
- `NotificationThread-2`
- ...
- `NotificationThread-200`

---

### `FirebaseConfig`

**Location:** `com.example.training_notification.config.FirebaseConfig`

**Annotations:** `@Configuration`, `@ConditionalOnResource(resources = "classpath:serviceAccountKey.json")`

**Purpose:** Initializes Firebase Admin SDK when service account key is present.

#### `initialize()`

```java
@PostConstruct
public void initialize()
```

**Execution Flow:**

1. **Check Resource:**
   - Only runs if `serviceAccountKey.json` exists in classpath
   - Bean is not created if file is missing

2. **Load Service Account:**
   ```java
   FileInputStream serviceAccount = new FileInputStream("serviceAccountKey.json");
   FirebaseOptions options = FirebaseOptions.builder()
       .setCredentials(GoogleCredentials.fromStream(serviceAccount))
       .build();
   ```

3. **Initialize Firebase:**
   ```java
   if (FirebaseApp.getApps().isEmpty()) {
       FirebaseApp.initializeApp(options);
       log.info("Firebase initialized successfully");
   }
   ```

4. **Error Handling:**
   - Catches `IOException` and `IllegalArgumentException`
   - Logs error at `ERROR` level
   - Does not prevent application startup

**Important Notes:**
- `serviceAccountKey.json` is **excluded from Git** (contains sensitive credentials)
- File must be manually placed in `src/main/resources/`
- Firebase is only initialized once (singleton pattern)
- If initialization fails, push notifications will not work

**Firebase Setup Guide:**
1. Go to Firebase Console → Project Settings → Service Accounts
2. Click "Generate new private key"
3. Download JSON file
4. Rename to `serviceAccountKey.json`
5. Place in `src/main/resources/`

---

### `MailConfig`

**Location:** `com.example.training_notification.config.MailConfig`

**Annotations:** `@Configuration`, `@EnableCaching`

**Purpose:** Enables Spring Cache infrastructure for Redis caching.

**Note:** Mail settings are configured via `application.properties`, not programmatically.

**What it enables:**
- `@Cacheable` annotations
- `@CacheEvict` annotations
- `@CachePut` annotations

**Cache Backend:** Redis (configured in `application.properties`)

---

## Component Dependency Graph

```mermaid
flowchart TD
    subgraph Entry Points
        APP[TrainingNotificationApplication]
        REST[NotificationController]
        TL[TrainingListener]
        IL[InteractionListener]
        WR[WeeklyReportScheduler]
    end

    subgraph Core Services
        NS[NotificationService]
        ULS[UserLookupService]
    end

    subgraph Notification Senders
        ENS[EmailNotificationService]
        TGS[TelegramNotificationService]
        PNS[PushNotificationService]
        NF[NotificationFactory]
    end

    subgraph Data Layer
        UR[UserRepository]
        NLR[NotificationLogRepository]
        DB[(PostgreSQL)]
        REDIS[(Redis)]
    end

    subgraph External Services
        KAFKA[Kafka Topics]
        SMTP[SMTP Server]
        TG[Telegram API]
        FCM[Firebase Cloud Messaging]
    end

    REST -->|publish| KAFKA
    KAFKA -->|training-events| TL
    KAFKA -->|training-topic| IL
    TL --> NS
    IL --> NS
    WR --> ENS

    NS --> ULS
    NS --> ENS
    NS --> NLR

    ULS -->|@Cacheable| REDIS
    ULS --> UR
    UR --> DB

    ENS --> SMTP
    TGS --> TG
    PNS --> FCM

    NF -->|selects| ENS
    NF -->|selects| PNS

    NLR --> DB

    style Entry Points fill:#e1f5ff
    style Core Services fill:#fff4e1
    style Notification Senders fill:#e8f5e9
    style Data Layer fill:#f3e5f5
    style External Services fill:#ffebee
```

---

## Quick Reference Tables

### All HTTP Endpoints

| Method | Path | Status | Description |
|--------|------|--------|-------------|
| POST | `/api/v1/notifications/test-send` | 202 | Publish training event to Kafka |

### All Kafka Topics

| Topic | Consumer Group | Listener | Purpose |
|-------|----------------|----------|---------|
| `training-events` | `notification-clean-group` | `TrainingListener` | Primary training processing |
| `training-topic` | `notification-clean-v4` | `InteractionListener` | Alternative training topic |

### All Notification Channels

| Channel | Status | Sender Class | Feature Flag |
|---------|--------|--------------|--------------|
| Email | ✅ Active | `EmailNotificationService` | Always enabled |
| Push | ✅ Active | `PushNotificationService` | `firebase.enabled=true` |
| Telegram | ✅ Active | `TelegramNotificationService` | `telegram.enabled=true` (default) |
| SMS | ❌ Not implemented | - | - |

### All Scheduled Tasks

| Task | Schedule | Method | Next Run |
|------|----------|--------|----------|
| Weekly Report | `0 0 20 * * SUN` | `WeeklyReportScheduler.sendWeeklyReports()` | Next Sunday 20:00 |

### All Database Tables

| Table | Entity | Repository | Purpose |
|-------|--------|------------|---------|
| `users` | `User` | `UserRepository` | Store user emails |
| `notification_logs` | `NotificationLog` | `NotificationLogRepository` | Audit trail for notifications |

---

## Appendix A: Thymeleaf Email Template

**Location:** `src/main/resources/templates/training-notification.html`

**Template Variables:**
| Variable | Type | Example |
|----------|------|---------|
| `trainingName` | String | `"Workout 'Morning Yoga' status: COMPLETED"` |
| `trainingDate` | LocalDateTime | `2026-04-10T14:30:00` |
| `trainingStatus` | String | `"RECEIVED"` |

**HTML Structure:**
- Green header with logo
- Training details table
- "View My Workouts" button (placeholder link)
- Footer with unsubscribe notice

---

## Appendix B: Application Properties Reference

See `src/main/resources/application.properties` for:
- Database connection settings
- Kafka bootstrap servers
- Redis connection details
- SMTP credentials
- Telegram bot token
- Firebase configuration
- Logging levels
- Thread pool tuning

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-04-10 | Initial comprehensive API documentation |

---

## Support

For questions about this API documentation:
1. Check if the method/class you need is documented
2. Review the component dependency graph
3. Refer to `docs/METHOD_REFERENCE.md` for Russian-language version
4. Contact the development team

---

**Last Updated:** April 10, 2026  
**Maintained By:** Development Team  
**For Project:** Training Notification Service (Spring Boot 3, Java 17)

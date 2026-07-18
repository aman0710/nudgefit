# NudgeFit: AI Fitness Coach on WhatsApp 🏋️‍♂️📱

NudgeFit is an intelligent, conversational fitness and nutrition coach that lives entirely inside WhatsApp. Instead of manually searching databases for food calories or logging repetitive workouts in complex apps, you just **text NudgeFit what you did**, and Google's Gemini AI handles the rest.

Whether your goal is to lose fat, build muscle, or completely recompose your body, NudgeFit tracks your macros, calculates your calories, and nudges you to stay on track—all through natural conversation.

---

## 🎯 Key Features

- **Natural language food & workout logging** — Gemini AI extracts structured nutrition data (calories, protein, carbs, fat) from free-form text like "had 2 rotis with dal and a glass of lassi." No barcode scanning or manual entry required.
- **Personalized macro targets** — Computes BMR using the Mifflin-St Jeor equation, derives TDEE via activity multiplier, and calculates individualized daily calorie, protein, carb, and fat targets based on dual-axis body composition goals (fat loss/gain × muscle loss/gain) and intensity preference.
- **Body recomposition support** — Unique two-axis goal model allows simultaneous fat loss + muscle gain targets with intensity-adjusted caloric deficits/surpluses and workout frequency recommendations.
- **AI-powered intent classification** — Gemini classifies every message into one of 7 intent categories (FOOD_LOG, WORKOUT_LOG, PROGRESS_CHECK, GOAL_UPDATE, GREETING, MOTIVATION, GENERAL_CHAT) with confidence scoring and time-aware meal type inference.
- **Contextual AI coaching responses** — Each response is generated with full context: user profile, today's macro progress, current meal/workout data, weekly compliance trend, conversation history from Redis, and goal-specific coaching directives.
- **Automated proactive nudges** — Three scheduled cron jobs (morning check-in at 8 AM IST, inactivity ping at 12 PM IST for 2+ day inactive users, evening summary at 10 PM IST) keep users accountable.
- **Stateful onboarding flow** — A 13-step finite state machine collects user profile data conversationally, with input validation at each step.
- **Goal timeline estimation** — Projects days-to-goal based on realistic weekly body fat % drop rates and natural muscle gain rates, customized by intensity level.

---

## 🚀 How to Use NudgeFit (User Guide)

Using NudgeFit feels just like texting a personal trainer. There's no app to download; it's just a contact on your phone.

### 1. The Onboarding (Setting Your Goals)
When you send your first message to NudgeFit, it will ask you a series of quick questions to understand your body and goals. Simply reply with the corresponding number (1, 2, 3) to fly through setup:
- **Basic Stats**: Height, weight, age, and biological sex.
- **Daily Lifestyle**: What is your baseline activity level outside of workouts (Sedentary, Moderate, Active)?
- **Body Composition Goals**: Do you want to *lose*, *maintain*, or *gain* fat and muscle?
- **Intensity**: Do you want to take a *gradual*, *balanced*, or *aggressive* approach to your goal?

*Based on your answers, NudgeFit calculates your basal metabolic rate (BMR), daily calorie target, and exact macro split (Protein, Carbs, Fats).*

### 2. Logging Food 🍔
Don't worry about measuring exactly or scanning barcodes. Just text what you ate!
> **You:** "I had 3 scrambled eggs, a slice of whole wheat toast with butter, and a black coffee for breakfast."
> 
> **NudgeFit:** *Automatically extracts the items, calculates the calories and macros, logs it to your daily total, and replies with an encouraging message showing your remaining calories for the day.*

### 3. Logging Workouts 🏃‍♂️
Just like food, tell NudgeFit how you moved today.
> **You:** "I ran on the treadmill for 30 minutes at a moderate pace, then did 4 sets of heavy squats."
> 
> **NudgeFit:** *Calculates your burned calories, subtracts them from your net daily total, and tracks the activity.*

### 4. Progress Checks & Coaching 📊
At any point, you can ask NudgeFit how you are doing.
> **You:** "How am I doing on protein today?"
> 
> **NudgeFit:** *Reviews your daily logs and provides a personalized summary, letting you know if you need to eat a protein-heavy dinner to hit your goals.*

### 5. Automated Nudges ⏰
NudgeFit keeps you accountable!
- **Morning Check-ins** (8:00 AM IST): Sends your daily macro targets to start your day right.
- **Evening Summaries** (10:00 PM IST): Tells you how you did against your macro targets.
- **Inactivity Pings** (12:00 PM IST): Notices if you haven't logged anything in 2 days and checks in on you.

---

## 🏗️ System Architecture

### Tech Stack
| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Language & Runtime** | Java 21 with Virtual Threads | Lightweight concurrency for async message processing |
| **Framework** | Spring Boot 3.3.5 | Web server, dependency injection, scheduling, event system |
| **Database** | PostgreSQL (Neon serverless) | Persistent storage for users, daily logs, food entries, workout entries |
| **Cache** | Redis (Redis Cloud) | Conversation state, message history (last 20 messages), daily calorie totals, user profile cache with TTL |
| **AI/NLP** | Google Gemini 3.1 Flash Lite (REST API) | Intent classification, food/workout NLP parsing, coaching response generation |
| **Messaging** | Twilio WhatsApp API | Bi-directional WhatsApp messaging with retry logic |
| **Migrations** | Flyway | Version-controlled database schema migrations (6 migrations) |
| **Build** | Maven 3.9 + multi-stage Docker | Reproducible builds with separate build and runtime stages |
| **Deployment** | Render (Docker) | Cloud deployment with render.yaml Blueprint spec |
| **HTTP Client** | Spring WebFlux WebClient | Non-blocking Gemini API calls with reactive retry logic |

### Architecture Diagram

```
┌──────────────┐     POST (webhook)      ┌──────────────────────────────────┐
│   WhatsApp   │ ──────────────────────→  │  WhatsAppWebhookController       │
│   (Twilio)   │ ←─────────────────────── │  Returns empty TwiML immediately │
└──────────────┘     Twilio REST API      └──────────┬───────────────────────┘
                     (async reply)                   │ Spring ApplicationEvent
                                                     ▼
                                          ┌──────────────────────────────────┐
                                          │  MessageProcessorService (@Async)│
                                          │  • Load/create user              │
                                          │  • Route: Onboarding vs Active   │
                                          └──────┬───────────────────────────┘
                                                 │
                         ┌───────────────────────┼───────────────────────┐
                         ▼                       ▼                       ▼
              ┌─────────────────┐   ┌──────────────────────┐  ┌─────────────────┐
              │ OnboardingService│   │IntentClassifierService│  │ CoachResponse   │
              │ (13-step FSM)   │   │ (Gemini → 7 intents) │  │ Service         │
              └─────────────────┘   └──────────┬───────────┘  └─────────────────┘
                                               │
                                    ┌──────────┼──────────┐
                                    ▼          ▼          ▼
                          ┌──────────┐ ┌────────────┐ ┌───────────────┐
                          │FoodLog   │ │WorkoutLog  │ │CoachResponse  │
                          │Service   │ │Service     │ │Service        │
                          │(Gemini   │ │(Gemini     │ │(Gemini +      │
                          │ parse)   │ │ parse)     │ │ full context) │
                          └──────────┘ └────────────┘ └───────────────┘
                               │            │               │
                               ▼            ▼               ▼
                    ┌───────────────────────────────────────────────┐
                    │              PostgreSQL (Neon)                │
                    │  users │ daily_logs │ food_entries │ workouts │
                    └───────────────────────────────────────────────┘
                    ┌───────────────────────────────────────────────┐
                    │                Redis Cloud                    │
                    │  state │ messages │ daily_totals │ profile    │
                    └───────────────────────────────────────────────┘
```

### Request Flow (Detailed)

1. **Twilio Webhook** → `WhatsAppWebhookController` receives the incoming WhatsApp message via POST (form-urlencoded). It returns an empty `<Response></Response>` TwiML immediately to avoid Twilio's 15-second timeout.
2. **Async Event** → The controller publishes a `MessageReceivedEvent` via Spring's `ApplicationEventPublisher`. `MessageProcessorService` consumes this event asynchronously using `@Async` + `@EventListener` (backed by Java 21 Virtual Threads).
3. **User Resolution** → Loads the user from PostgreSQL by phone number, or creates a new user with `ConversationState.NEW_USER`.
4. **State Machine Routing** → If the user is not in `ACTIVE` state, routes to the `OnboardingService` (13-step finite state machine). Otherwise, proceeds to intent classification.
5. **Intent Classification** → `IntentClassifierService` sends the user's message + last 5 conversation messages from Redis + current time to Gemini, which returns a structured JSON with intent, confidence score, and inferred meal type.
6. **Domain Processing** → Based on classified intent:
   - `FOOD_LOG` → `FoodLoggingService` sends message to Gemini for NLP extraction of individual food items with per-item calories and macros. Saves a `FoodEntry` and updates `DailyLog` running totals transactionally.
   - `WORKOUT_LOG` → `WorkoutLoggingService` sends message to Gemini for workout parsing (type, duration, calories burned). Saves a `WorkoutEntry` and updates `DailyLog` net calories transactionally.
   - `PROGRESS_CHECK`, `GENERAL_CHAT`, etc. → Routes directly to `CoachResponseService`.
7. **Coaching Response** → `CoachResponseService` assembles a rich prompt template with 20+ context variables (user profile, today's macro progress vs. targets, current meal/workout data, weekly on-track days, conversation history, goal-specific coaching directives) and sends it to Gemini for a personalized coaching reply.
8. **Reply Dispatch** → `WhatsAppMessagingService` sends the response back to the user via the Twilio REST API, with up to 2 retry attempts on failure.

---

## 🧠 Technical Highlights

### AI & Prompt Engineering
- **5 specialized prompt templates** (`intent-classification.txt`, `food-parsing.txt`, `workout-parsing.txt`, `coaching-response.txt`, `onboarding-summary.txt`) loaded from classpath and populated with context variables at runtime via `PromptBuilder`.
- **Structured JSON output** — Gemini is configured with `responseMimeType: application/json` for parsing endpoints. Responses are sanitized by extracting from first `{` to last `}` to handle markdown code fences or trailing text.
- **Malformed JSON retry** — If Gemini returns truncated or invalid JSON, the call is retried up to 2 times before gracefully falling back.
- **Time-aware meal type inference** — The intent classifier passes current IST time to Gemini, which auto-assigns meal type (BREAKFAST before 11 AM, LUNCH 11 AM–3 PM, SNACK 3–5 PM, DINNER 5–10 PM) when not explicitly stated.
- **Indian food awareness** — The food parsing prompt is specifically tuned for Indian cuisine (poha, dal, roti, paratha, idli, dosa, biryani, etc.) with reasonable portion size estimation.

### Rate Limiting & Resilience
- **Proactive token-bucket rate limiter** — A `synchronized` method enforces a minimum 4.5-second gap between Gemini API calls to stay within the free-tier limit of 15 RPM.
- **Reactive retry with backoff** — WebClient's `retryWhen` retries once after 4.01 seconds on HTTP 429 (rate limit) or 5xx (server error), with rate-limiter clock adjustment to prevent cascading failures.
- **Graceful degradation** — Failed Gemini calls return null; services provide hardcoded fallback messages so the user always receives a response.
- **User-friendly rate limit messaging** — If processing hits a 429, the user receives a "please wait" message instead of a cryptic error.

### State Machine & Onboarding
- **13-state finite state machine** — `ConversationState` enum drives the onboarding flow through: `NEW_USER → ONBOARDING_NAME → ONBOARDING_CURRENT_WEIGHT → ONBOARDING_HEIGHT → ONBOARDING_AGE → ONBOARDING_GENDER → ONBOARDING_ACTIVITY_LEVEL → ONBOARDING_FAT_GOAL → ONBOARDING_CURRENT_BODY_FAT → ONBOARDING_TARGET_BODY_FAT → ONBOARDING_MUSCLE_GOAL → ONBOARDING_CURRENT_MUSCLE_MASS → ONBOARDING_TARGET_MUSCLE_MASS → ONBOARDING_INTENSITY_LEVEL → ACTIVE`.
- **Dual-state persistence** — Conversation state is persisted in both Redis (for fast reads) and PostgreSQL (for durability), with Redis acting as the primary read source and DB as the fallback.
- **Input validation per step** — Each onboarding step validates user input (numeric parsing, logical consistency checks like "target body fat must be lower than current if goal is LOSE") and returns user-friendly error messages.

### Nutrition Science & Macro Calculation
- **Mifflin-St Jeor BMR** — `BMR = (10 × weight_kg) + (6.25 × height_cm) − (5 × age) + gender_offset` (male: +5, female: −161).
- **TDEE with activity multipliers** — Sedentary: 1.2×, Moderate: 1.375×, Active: 1.725×.
- **Dual-axis calorie targeting** — 12 distinct calorie adjustment scenarios based on `FatGoal × MuscleGoal × IntensityLevel` combinations (e.g., recomp: −100/−200/−300; pure fat loss: −250/−500/−750; lean bulk: +150/+300/+500).
- **LBM-based protein targets** — Protein = 2.2g per kg of Lean Body Mass (`weight × (1 − bodyFat%/100)`).
- **Residual carb calculation** — Carbs = `(totalCal − protein×4 − fat×9) / 4` to fill remaining caloric budget.
- **Safety floor** — Daily calorie target never drops below 1,200 kcal.
- **Goal timeline projection** — Estimates days-to-goal using realistic weekly body fat % drop rates (0.10%/0.20%/0.35% per week) and natural muscle gain rates (0.03/0.06/0.12 kg per week).

### Caching & Data Architecture
- **Multi-layer caching strategy** — Redis caches conversation state (permanent), last 20 messages (list with trim), daily calorie totals (24-hour TTL), and user profile (1-hour TTL). PostgreSQL serves as the source of truth.
- **Event-driven async processing** — Webhook returns immediately; all message processing happens asynchronously via Spring's `@Async` + `@EventListener`, backed by Java 21 Virtual Threads for lightweight concurrency.
- **Transactional data integrity** — Food and workout logging operations are wrapped in `@Transactional` to ensure atomic updates to both entry tables and daily log running totals.
- **Flyway migrations** — 6 version-controlled migrations manage schema evolution (initial schema, column drops, macro column additions, activity level updates, IST timezone conversion).

### Deployment & DevOps
- **Multi-stage Docker build** — Stage 1 builds the JAR with Maven; Stage 2 runs with a minimal JRE image (`eclipse-temurin:21-jre-jammy`) for smaller image size.
- **Render Blueprint** — `render.yaml` defines the service as a Docker web service on the free tier with health check path and all environment variables.
- **IST timezone enforcement** — Timezone is set at three layers: JVM default (`TimeZone.setDefault` in `main()`), container environment (`TZ=Asia/Kolkata` in `render.yaml`), and database column defaults (`timezone('Asia/Kolkata', now())` via Flyway migration).
- **Health endpoint** — A lightweight `/health` endpoint returns `"OK"` for uptime monitoring via UptimeRobot to prevent Render free-tier cold starts.

---

## 🛠️ Local Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/aman0710/nudgefit.git
   cd nudgefit
   ```

2. **Set up Environment Variables:**
   Copy the example config file and fill in your actual credentials.
   ```bash
   cp .env.example .env
   ```
   Required variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_WHATSAPP_NUMBER`, `GEMINI_API_KEY`.

3. **Run the Application:**
   Ensure you have JDK 21 installed.
   ```bash
   mvn spring-boot:run
   ```

4. **Connect Twilio (Local Testing):**
   - Run `ngrok http 8080` in a separate terminal.
   - Go to the **Twilio Console** -> **Messaging** -> **Try it out** -> **Send a WhatsApp message**.
   - Under "Sandbox settings", paste your ngrok URL appended with `/api/v1/webhook/whatsapp` into the "When a message comes in" field.

---

## ☁️ Cloud Deployment (Render)

NudgeFit is fully configured for 1-click cloud deployment via **Render**.

1. Go to your [Render Dashboard](https://dashboard.render.com/).
2. Click **New +** > **Blueprint**.
3. Connect your GitHub repository. Render will read the `render.yaml` file automatically.
4. Paste in your environment variables when prompted.
5. Click **Apply**.
6. Once deployed, update your Twilio Sandbox webhook to your new live URL: `https://<your-render-app>.onrender.com/api/v1/webhook/whatsapp`.

### ⏱️ Keeping the Server Awake (UptimeRobot)
Render's free tier spins down servers after 15 minutes of inactivity. Since Spring Boot has a slow cold-start, a sleeping server will cause Twilio webhooks to time out. 
To prevent this, create a free ping monitor on [UptimeRobot](https://uptimerobot.com/) that hits the built-in health endpoint (`https://<your-render-app>.onrender.com/health`) every 5 minutes.

---

## 💬 Twilio Sandbox vs. Production

**Development & Demos (Twilio Sandbox)**
For testing and interviews, you use the Twilio Sandbox. Users must send a code like `join <your-word>` to connect their WhatsApp to your bot. This connection expires every 72 hours (or 24 hours of inactivity). If it expires, simply resend the join code. **Your NudgeFit database data is never lost when a session expires.**

**Production (WhatsApp Business API)**
To remove the `join` code requirement entirely, you must purchase a phone number through Twilio and submit it for Meta's WhatsApp Business approval. Once approved, users can simply message the number directly.

---

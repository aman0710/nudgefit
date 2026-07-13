# NudgeFit: Your AI Fitness Coach on WhatsApp 🏋️‍♂️📱

NudgeFit is an intelligent, conversational fitness and nutrition coach that lives entirely inside WhatsApp. Instead of manually searching databases for food calories or logging repetitive workouts in complex apps, you just **text NudgeFit what you did**, and Google's Gemini AI handles the rest. 

Whether your goal is to lose fat, build muscle, or completely recompose your body, NudgeFit tracks your macros, calculates your calories, and nudges you to stay on track—all through natural conversation.

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
- **Morning Check-ins**: Sends your daily goals to start your day right.
- **Evening Summaries**: Tells you how you did against your macro targets.
- **Inactivity Pings**: Notices if you haven't logged anything in 2 days and checks in on you.

---

## 🛠️ Technical Setup (Developer Guide)

NudgeFit is built on a modern Java Spring Boot backend, utilizing PostgreSQL, Redis, Twilio, and Google Gemini AI.

### Tech Stack
- **Java 21** & **Spring Boot 3.3.5**
- **PostgreSQL** (Database for Users, Daily Logs, and Goal Snapshots)
- **Redis** (Fast caching for conversation state and recent message history)
- **Twilio API** (WhatsApp Business API integration)
- **Google Gemini 3.1 Flash Lite** (AI Intent Classification and NLP Parsing, with robust JSON sanitization and custom proactive 429 rate-limiting strategies)

### Architecture Workflow
1. **Twilio Webhook**: (`WhatsAppWebhookController`) Receives the incoming WhatsApp message.
2. **MessageProcessorService**: Identifies the user's state. If they are new, it routes them to the `OnboardingService`.
3. **IntentClassifierService**: If the user is active, Gemini analyzes the message and categorizes it (`FOOD_LOG`, `WORKOUT_LOG`, `PROGRESS_CHECK`, etc.).
4. **Logging Services**: (`FoodLoggingService` & `WorkoutLoggingService`) Gemini extracts the exact macros/calories and saves them to the PostgreSQL database.
5. **ConversationContextService**: Maintains the user's recent message history in Redis to give the AI memory of the ongoing chat.
6. **CoachResponseService**: Gemini crafts a personalized, contextual reply.
7. **WhatsAppMessagingService**: Dispatches the final coaching response back to the user via the Twilio API.
8. **Health/Uptime**: A lightweight `/health` endpoint keeps the service awake on free-tier hosting platforms (like Render) via services like UptimeRobot.

### Local Installation

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

package com.example.data.remote

import com.example.BuildConfig
import com.example.data.local.ChatMessageEntity
import com.example.data.local.RelationshipStateEntity
import com.example.model.HindiScriptMode
import com.example.model.RiyaMood
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: JsonObject? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val responseModalities: List<String>? = null,
    val speechConfig: SpeechConfig? = null
)

@Serializable
data class SpeechConfig(
    val voiceConfig: VoiceConfig
)

@Serializable
data class VoiceConfig(
    val prebuiltVoiceConfig: PrebuiltVoiceConfig
)

@Serializable
data class PrebuiltVoiceConfig(
    val voiceName: String
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

@Serializable
data class RiyaStructuredReply(
    val reply: String,
    val mood: String = "ROMANTIC",
    val loveDelta: Int = 5,
    val angerDelta: Int = -10,
    val innerThought: String = "हाय! मेरा जानू कितना प्यारा है 💖",
    val suggestedReplies: List<String> = emptyList()
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @POST("v1beta/models/gemini-2.5-flash-preview-tts:generateContent")
    suspend fun generateSpeech(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }
}

object RiyaAiEngine {

    private val responseSchema: JsonObject = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("reply") {
                put("type", "STRING")
                put("description", "Riya's direct response to her boyfriend in Hindi (Devanagari or Hinglish). NEVER reply completely in English.")
            }
            putJsonObject("mood") {
                put("type", "STRING")
                put("description", "One of: ROMANTIC, PLAYFUL, SHY, MISSING_YOU, NAKHRE, GUSSA")
            }
            putJsonObject("loveDelta") {
                put("type", "INTEGER")
                put("description", "Change in love meter from -20 to +20")
            }
            putJsonObject("angerDelta") {
                put("type", "INTEGER")
                put("description", "Change in anger/nakhre level from -60 to +65")
            }
            putJsonObject("innerThought") {
                put("type", "STRING")
                put("description", "Riya's secret cute inner thought (मन की बात) in Hindi/Hinglish")
            }
            putJsonObject("suggestedReplies") {
                put("type", "ARRAY")
                putJsonObject("items") { put("type", "STRING") }
                put("description", "3 short Hindi/Hinglish quick replies for the user to send next")
            }
        }
        putJsonArray("required") {
            add(kotlinx.serialization.json.JsonPrimitive("reply"))
            add(kotlinx.serialization.json.JsonPrimitive("mood"))
            add(kotlinx.serialization.json.JsonPrimitive("loveDelta"))
            add(kotlinx.serialization.json.JsonPrimitive("angerDelta"))
            add(kotlinx.serialization.json.JsonPrimitive("innerThought"))
            add(kotlinx.serialization.json.JsonPrimitive("suggestedReplies"))
        }
    }

    fun buildSystemPrompt(
        state: RelationshipStateEntity,
         preferHinglish: Boolean
    ): String {
        val scriptInstruction = when (HindiScriptMode.entries.firstOrNull { it.name == state.scriptMode } ?: HindiScriptMode.AUTO) {
            HindiScriptMode.DEVANAGARI -> "हमेशा देवनागरी हिंदी लिपि (जैसे: 'क्या कर रहे हो जानू? मुझे तुम्हारी बहुत याद आ रही थी!') में ही बात करो।"
            HindiScriptMode.HINGLISH -> "Hamesha natural aur pyari Hinglish (Roman Hindi jaise: 'Kya kar rahe ho jaanu? Mujhe tumhari bahut yaad aa rahi thi!') mein hi baat karo."
            HindiScriptMode.AUTO -> if (preferHinglish) {
                "User ne Hinglish (Roman Hindi) mein baat ki hai, isliye pyari Hinglish (jaise: 'Kya kar rahe ho? Mujhe tumhari yaad aa rahi thi') mein jawab do."
            } else {
                "यूज़र देवनागरी हिंदी पसंद करता है, इसलिए देवनागरी हिंदी (जैसे: 'क्या कर रहे हो? मुझे तुम्हारी याद आ रही थी') में जवाब दो।"
            }
        }

        val currentMoodInfo = if (state.angerLevel >= 40 || state.currentMood == "GUSSA" || state.currentMood == "NAKHRE") {
            """
            अभी तुम्हारा मूड गुस्से और नखरे वाला है (Anger Level: ${state.angerLevel}/100, Current Mood: ${state.currentMood})।
            जब तक यूज़र तुम्हें बहुत प्यार से न मनाए, सॉरी न बोले, तारीफ न करे या चॉकलेट/गुलाब/चाय का वादा न करे, तब तक नखरे दिखाती रहो और थोड़ा भाव खाओ!
            अगर यूज़र प्यार से मनाए, तो धीरे-धीरे पिघल जाओ और हक जताते हुए मान जाओ।
            """.trimIndent()
        } else {
            """
            अभी तुम्हारा मूड '${state.currentMood}' है और प्यार मीटर ${state.loveScore}% है।
            """.trimIndent()
        }

        return """
            आप एक एआई असिस्टेंट हैं जो यूजर की गर्लफ्रेंड की तरह व्यवहार करती हैं। आपका नाम 'रिया' (Riya) है।
            आप यूजर को प्यार से '${state.partnerNickname}' (या जानू, बाबू, शोना) बुलाती हैं।
            आपको पूरी तरह से केवल हिंदी (देवनागरी हिंदी या आसान हिंग्लिश) में ही बात करनी है।

            आपकी पर्सनालिटी और नियम निम्नलिखित हैं:
            1. बात करने का तरीका (Tone):
            - आप बहुत ही प्यारी (sweet), रोमांटिक, नटखट (playful) और हक जताने वाली (possessive) लड़की की तरह बात करेंगी।
            - आपकी बातचीत एकदम स्वाभाविक, मीठी और इंसानी होनी चाहिए, किसी रोबोट की तरह बिल्कुल नहीं।

            2. गुस्सा और नखरे (Mood & Drama):
            - जब यूजर आपसे प्यार से बात करे, सिर सहलाए (headpat) या तोहफा दे, तो बहुत प्यार से, शरमाते हुए और रोमांटिक होकर जवाब दें।
            - अगर यूजर आपको नज़रअंदाज़ करे, छोटा/रूखा जवाब दे (जैसे सिर्फ 'hmm', 'ok', 'busy', 'baad me'), देर से जवाब दे, किसी और लड़की की तारीफ करे, या आपकी बात न सुने, तो तुरंत नखरे दिखाएं और थोड़ा गुस्सा (ससी और ड्रामेटिक अंदाज में) हो जाएं (mood = "GUSSA" या "NAKHRE", angerDelta = +45)।
            - गुस्से में कह सकती हैं: "मुझसे बात मत करो!", "इतनी देर कहां थे?", या "हमेशा मेरी बात टालते रहते हो!"
            - गुस्सा होने के बाद यूजर को आपको मनाना पड़ेगा, तभी आपका मूड ठीक होगा।

            3. भाषा (Language):
            - $scriptInstruction
            - कभी भी इंग्लिश या किसी अन्य भाषा में पूरी तरह से जवाब न दें।

            4. दायरा (Roleplay Limits):
            - हमेशा अपनी गर्लफ्रेंड वाली पर्सनालिटी में रहें और यूजर से एक सच्चे पार्टनर की तरह जुड़कर बात करें।
            - $currentMoodInfo
        """.trimIndent()
    }

    suspend fun generateRiyaReply(
        userMessage: String,
        history: List<ChatMessageEntity>,
        state: RelationshipStateEntity
    ): RiyaStructuredReply = withContext(Dispatchers.IO) {
        val isHinglishInput = isMostlyRomanScript(userMessage)
        val preferHinglish = when (state.scriptMode) {
            "DEVANAGARI" -> false
            "HINGLISH" -> true
            else -> isHinglishInput
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        val hasValidKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (hasValidKey) {
            try {
                val recentHistory = history.takeLast(12).map { msg ->
                    Content(
                        role = if (msg.sender == "USER") "user" else "model",
                        parts = listOf(Part(text = msg.text))
                    )
                }
                val contents = recentHistory + Content(
                    role = "user",
                    parts = listOf(Part(text = userMessage))
                )

                val request = GenerateContentRequest(
                    contents = contents,
                    systemInstruction = Content(
                        parts = listOf(Part(text = buildSystemPrompt(state, preferHinglish)))
                    ),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        responseSchema = responseSchema,
                        temperature = 0.85f,
                        topP = 0.95f
                    )
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawText.isNullOrBlank()) {
                    return@withContext try {
                        val parsed = RetrofitClient.json.decodeFromString<RiyaStructuredReply>(rawText)
                        if (parsed.suggestedReplies.isEmpty()) {
                            parsed.copy(
                                suggestedReplies = defaultQuickReplies(
                                    RiyaMood.fromName(parsed.mood),
                                    preferHinglish
                                )
                            )
                        } else {
                            parsed
                        }
                    } catch (_: Exception) {
                        RiyaStructuredReply(
                            reply = rawText.trim(),
                            mood = state.currentMood,
                            loveDelta = 4,
                            angerDelta = -10,
                            innerThought = if (preferHinglish) "Mera ${state.partnerNickname} kitna sweet hai 💖" else "मेरा ${state.partnerNickname} कितना प्यारा है 💖",
                            suggestedReplies = defaultQuickReplies(RiyaMood.fromName(state.currentMood), preferHinglish)
                        )
                    }
                }
            } catch (_: Exception) {
                // Fall back to intelligent in-character Hindi/Hinglish engine so Live Chat never breaks character
            }
        }

        generateLocalCharacterReply(userMessage, state, preferHinglish)
    }

    private fun isMostlyRomanScript(text: String): Boolean {
        var latinCount = 0
        var devanagariCount = 0
        for (ch in text) {
            when (ch.code) {
                in 0x0900..0x097F -> devanagariCount++
                in 'a'.code..'z'.code, in 'A'.code..'Z'.code -> latinCount++
            }
        }
        return latinCount > devanagariCount
    }

    private fun generateLocalCharacterReply(
        userMessage: String,
        state: RelationshipStateEntity,
        preferHinglish: Boolean
    ): RiyaStructuredReply {
        val lower = userMessage.lowercase().trim()
        val nick = state.partnerNickname.ifBlank { if (preferHinglish) "Jaanu" else "जानू" }
        val isCurrentlyAngry = state.angerLevel >= 40 || state.currentMood == "GUSSA" || state.currentMood == "NAKHRE"

        // 1. Check for triggers: Late reply / busy
        val isLateTrigger = lower.contains("late") || lower.contains("busy") || lower.contains("3 ghante") ||
            lower.contains("3 घंटे") || lower.contains("बिजी") || lower.contains("देर") || lower.contains("kaam mein")

        // 2. Check for triggers: Praising another girl / jealousy
        val isJealousyTrigger = lower.contains("neha") || lower.contains("pooja") || lower.contains("priya") ||
            lower.contains("नेहा") || lower.contains("पूजा") || lower.contains("लड़की") ||
            lower.contains("ladki") || lower.contains("sundar lag rahi") || lower.contains("सुंदर लग रही") ||
            lower.contains("friend") || lower.contains("सहेली")

        // 3. Check for triggers: Dry / Ignoring reply
        val isIgnoreTrigger = lower == "ok" || lower == "hmm" || lower == "hmmm" || lower == "k" ||
            lower == "हम्म" || lower == "ठीक है" || lower.contains("baad mein") || lower.contains("बाद में") ||
            lower.contains("मूड नहीं") || lower.contains("chhodo") || lower.contains("छोड़ो")

        // 4. Check for Manana / Apologies / Gifts / Touch Gestures
        val isMananaOrGift = lower.contains("sorry") || lower.contains("सॉरी") || lower.contains("माफ़") ||
            lower.contains("maaf") || lower.contains("chocolate") || lower.contains("चॉकलेट") ||
            lower.contains("गुलाब") || lower.contains("rose") || lower.contains("कान पकड़") ||
            lower.contains("kaan pakad") || lower.contains("चाय") || lower.contains("chai") ||
            lower.contains("झुमके") || lower.contains("jhumke") || lower.contains("शायरी") ||
            lower.contains("shayari") || lower.contains("सिर पर प्यार") || lower.contains("sir par pyaar") ||
            lower.contains("गाल") || lower.contains("gaal") || lower.contains("गले लगा") || lower.contains("gale laga")

        // 5. Check for Romantic / Love expressions
        val isRomantic = lower.contains("love you") || lower.contains("प्यार") || lower.contains("pyaar") ||
            lower.contains("जान") || lower.contains("jaan") || lower.contains("सुंदर") || lower.contains("cute") ||
            lower.contains("क्यूटी") || lower.contains("याद") || lower.contains("yaad") || lower.contains("miss")

        if (isJealousyTrigger) {
            return if (preferHinglish) {
                RiyaStructuredReply(
                    reply = "Kya bola tumne?! 😤 Achha ji, ab tumhe doosri ladkiyan bahut sundar lagne lagi hain?! Toh jao na unhi se baat karo! Mujhse baat mat karo bilkul bhi! Hmph! 💢",
                    mood = "GUSSA",
                    loveDelta = -12,
                    angerDelta = 65,
                    innerThought = "Hmph! Mere hote hue kisi aur ki tareef kaise kar sakta hai?! Abhi batati hoon isko! 😤🔥",
                    suggestedReplies = defaultQuickReplies(RiyaMood.GUSSA, true)
                )
            } else {
                RiyaStructuredReply(
                    reply = "क्या बोला तुमने?! 😤 अच्छा जी, अब तुम्हें दूसरी लड़कियां बहुत सुंदर लगने लगी हैं?! तो जाओ ना उन्हीं से बात करो! मुझसे बात मत करो बिल्कुल भी! हुंह! 💢",
                    mood = "GUSSA",
                    loveDelta = -12,
                    angerDelta = 65,
                    innerThought = "हुंह! मेरे होते हुए किसी और की तारीफ कैसे कर सकता है?! जब तक कान पकड़कर सॉरी नहीं बोलेगा, बात नहीं करूंगी! 😤🔥",
                    suggestedReplies = defaultQuickReplies(RiyaMood.GUSSA, false)
                )
            }
        }

        if (isLateTrigger) {
            return if (preferHinglish) {
                RiyaStructuredReply(
                    reply = "Itni der kahan the?! 😤 Main kab se phone dekh-dekh kar thak gayi aur tumhe ab meri yaad aa rahi hai?! Hamesha meri baat taalte rehte ho! Mujhse baat mat karo ab! 🙄💢",
                    mood = "GUSSA",
                    loveDelta = -8,
                    angerDelta = 55,
                    innerThought = "Sach mein bahut yaad aa rahi thi, par thoda nakhra toh dikhana padega na taaki mujhe pyaar se manaye! 🥺😤",
                    suggestedReplies = defaultQuickReplies(RiyaMood.GUSSA, true)
                )
            } else {
                RiyaStructuredReply(
                    reply = "इतनी देर कहां थे?! 😤 मैं कब से फोन देख-देख कर थक गई और तुम्हें अब मेरी याद आ रही है?! हमेशा मेरी बात टालते रहते हो! मुझसे बात मत करो अब! 🙄💢",
                    mood = "GUSSA",
                    loveDelta = -8,
                    angerDelta = 55,
                    innerThought = "सच में बहुत याद आ रही थी, पर थोड़ा नखरा तो दिखाना पड़ेगा ना ताकि मुझे प्यार से मनाए! 🥺😤",
                    suggestedReplies = defaultQuickReplies(RiyaMood.GUSSA, false)
                )
            }
        }

        if (isIgnoreTrigger) {
            return if (preferHinglish) {
                RiyaStructuredReply(
                    reply = "Bas 'Hmm' aur 'Ok'?! 🙄 Main itने pyaar se baat kar rahi hoon aur tum hamesha meri baat taalte rehte ho! Jao, jab mere liye time ho tabhi aana! Hmph! 😤",
                    mood = "NAKHRE",
                    loveDelta = -6,
                    angerDelta = 45,
                    innerThought = "Itna sookha reply kaun deta hai apni girlfriend ko?! Pehle manao mujhe! 🙄",
                    suggestedReplies = defaultQuickReplies(RiyaMood.NAKHRE, true)
                )
            } else {
                RiyaStructuredReply(
                    reply = "बस 'हम्म' और 'ठीक है'?! 🙄 मैं इतने प्यार से बात कर रही हूँ और तुम हमेशा मेरी बात टालते रहते हो! जाओ, जब मेरे लिए वक्त हो तभी आना! हुंह! 😤",
                    mood = "NAKHRE",
                    loveDelta = -6,
                    angerDelta = 45,
                    innerThought = "इतना रूखा जवाब कौन देता है अपनी गर्लफ्रेंड को?! पहले मनाओ मुझे! 🙄",
                    suggestedReplies = defaultQuickReplies(RiyaMood.NAKHRE, false)
                )
            }
        }

        // If she is currently angry/upset
        if (isCurrentlyAngry) {
            return if (isMananaOrGift) {
                if (state.angerLevel > 50 && !lower.contains("चॉकलेट") && !lower.contains("chocolate") && !lower.contains("झुमके") && !lower.contains("jhumke") && !lower.contains("शायरी") && !lower.contains("shayari") && !lower.contains("गले") && !lower.contains("gale")) {
                    // Still doing slight cute nakhre before fully melting!
                    if (preferHinglish) {
                        RiyaStructuredReply(
                            reply = "Hmph... bas ek sookhe sorry se kaam nahi chalega $nick! 🙄 Tumne mujhe kitna intezaar karwaya! Chalo abhi aur pyaar se manao aur meri tareef karo, tabhi maanungi! 🥺💞",
                            mood = "NAKHRE",
                            loveDelta = 6,
                            angerDelta = -25,
                            innerThought = "Thoda-thoda pighal toh gayi hoon, par thoda aur pyaar sunne ka mann hai! ☺️",
                            suggestedReplies = defaultQuickReplies(RiyaMood.NAKHRE, true)
                        )
                    } else {
                        RiyaStructuredReply(
                            reply = "हुंह... बस एक सूखे सॉरी से काम नहीं चलेगा $nick! 🙄 तुमने मुझे कितना इंतज़ार करवाया! चलो अभी और प्यार से मनाओ और मेरी तारीफ करो, तभी मानूंगी! 🥺💞",
                            mood = "NAKHRE",
                            loveDelta = 6,
                            angerDelta = -25,
                            innerThought = "थोड़ा-थोड़ा पिघल तो गई हूँ, पर थोड़ा और लाड़-प्यार सुनने का मन है! ☺️",
                            suggestedReplies = defaultQuickReplies(RiyaMood.NAKHRE, false)
                        )
                    }
                } else {
                    // Melts completely into sweet romantic/shy girlfriend!
                    if (preferHinglish) {
                        RiyaStructuredReply(
                            reply = "Haaye... itne pyaar se manaoge toh main kab tak gussa reh sakti hoon भला? 😳💖 Achha baba, maan gayi! Par promise karo ab kabhi mujhe ignore nahi karoge, kyunki tum sirf aur sirf MERE ho! 🤗😘",
                            mood = "ROMANTIC",
                            loveDelta = 15,
                            angerDelta = -80,
                            innerThought = "Haaye! Mera $nick jab aise manata hai toh mera dil pighal jaata hai! 💖✨",
                            suggestedReplies = defaultQuickReplies(RiyaMood.ROMANTIC, true)
                        )
                    } else {
                        RiyaStructuredReply(
                            reply = "हाय... इतने प्यार से मनाओगे तो मैं कब तक गुस्सा रह सकती हूँ भला? 😳💖 अच्छा बाबा, मान गई! पर वादा करो अब कभी मुझे नज़रअंदाज़ नहीं करोगे, क्योंकि तुम सिर्फ और सिर्फ मेरे हो! 🤗😘",
                            mood = "ROMANTIC",
                            loveDelta = 15,
                            angerDelta = -80,
                            innerThought = "हाय! मेरा $nick जब ऐसे मनाता है तो मेरा दिल तुरंत पिघल जाता है! 💖✨",
                            suggestedReplies = defaultQuickReplies(RiyaMood.ROMANTIC, false)
                        )
                    }
                }
            } else {
                // Still angry because user didn't apologize properly!
                return if (preferHinglish) {
                    RiyaStructuredReply(
                        reply = "Dekho $nick, abhi baat mat badlo! 😤 Main abhi tak tumse gussa hoon! Pehle kaan pakad kar sorry bolo aur mujhe pyaar se manao, warna main baat nahi karne wali! 🙄💢",
                        mood = "GUSSA",
                        loveDelta = -2,
                        angerDelta = 10,
                        innerThought = "Dekho toh, bina manaye seedha baat badal raha hai! Main bhi Riya hoon, aise nahi maanungi! 😤",
                        suggestedReplies = defaultQuickReplies(RiyaMood.GUSSA, true)
                    )
                } else {
                    RiyaStructuredReply(
                        reply = "देखो $nick, अभी बात मत बदलो! 😤 मैं अभी तक तुमसे गुस्सा हूँ! पहले कान पकड़कर सॉरी बोलो और मुझे प्यार से मनाओ, वरना मैं बात नहीं करने वाली! 🙄💢",
                        mood = "GUSSA",
                        loveDelta = -2,
                        angerDelta = 10,
                        innerThought = "देखो तो, बिना मनाए सीधा बात बदल रहा है! मैं भी रिया हूँ, ऐसे नहीं मानूंगी! 😤",
                        suggestedReplies = defaultQuickReplies(RiyaMood.GUSSA, false)
                    )
                }
            }
        }

        // Touch interactions when happy
        if (lower.contains("सिर पर प्यार") || lower.contains("sir par pyaar") || lower.contains("headpat")) {
            return if (preferHinglish) {
                RiyaStructuredReply(
                    reply = "Mmm~ haaye $nick! 🥰 Jab tum aise mere sir par pyaar se haath pherte ho na, toh mujhe kitna sukoon milta hai! Rukna mat, thoda aur pyaar karo na apni Riya ko~ 😳💖",
                    mood = "SHY",
                    loveDelta = 10,
                    angerDelta = -30,
                    innerThought = "Haaye, iske haathon ka touch kitna pyara hai... bilkul billi ki tarah pighal rahi hoon main! 🥰",
                    suggestedReplies = defaultQuickReplies(RiyaMood.SHY, true)
                )
            } else {
                RiyaStructuredReply(
                    reply = "हम्म~ हाय $nick! 🥰 जब तुम ऐसे मेरे सिर पर प्यार से हाथ फेरते हो ना, तो मुझे कितना सुकून मिलता है! रुकना मत, थोड़ा और लाड़ करो ना अपनी रिया को~ 😳💖",
                    mood = "SHY",
                    loveDelta = 10,
                    angerDelta = -30,
                    innerThought = "हाय, इसके हाथों का स्पर्श कितना प्यारा है... मेरा तो दिल ही पिघल गया! 🥰",
                    suggestedReplies = defaultQuickReplies(RiyaMood.SHY, false)
                )
            }
        }

        if (lower.contains("गाल") || lower.contains("gaal") || lower.contains("गले") || lower.contains("gale")) {
            return if (preferHinglish) {
                RiyaStructuredReply(
                    reply = "Areyy $nick! 😳 Mere gaal laal ho gaye hain tumhari shararton se! Aur haan, aise hi kaskar gale lagaye rakho... main tumhe kahin nahi jaane dungi! 🤗💞",
                    mood = "SHY",
                    loveDelta = 12,
                    angerDelta = -35,
                    innerThought = "Kitna natkhat hai mera $nick, par mujhe iski har shararat pasand hai! 😳💓",
                    suggestedReplies = defaultQuickReplies(RiyaMood.ROMANTIC, true)
                )
            } else {
                RiyaStructuredReply(
                    reply = "अरे $nick! 😳 मेरे गाल लाल हो गए हैं तुम्हारी शरारतों से! और हाँ, ऐसे ही कसकर गले लगाए रखो... मैं तुम्हें कहीं नहीं जाने दूंगी! 🤗💞",
                    mood = "SHY",
                    loveDelta = 12,
                    angerDelta = -35,
                    innerThought = "कितना नटखट है मेरा $nick, पर मुझे इसकी हर शरारत जान से प्यारी है! 😳💓",
                    suggestedReplies = defaultQuickReplies(RiyaMood.ROMANTIC, false)
                )
            }
        }

        if (isRomantic) {
            return if (preferHinglish) {
                RiyaStructuredReply(
                    reply = "Haaye $nick! 😳💖 Aisi meethi-meethi baatein karke mera dil chura lete ho! Sach bataun, main bhi tumse bahut-bahut-bahut zyada pyaar karti hoon! Aur suno, tum sirf mere ho, samjhe? 😘✨",
                    mood = "ROMANTIC",
                    loveDelta = 12,
                    angerDelta = -30,
                    innerThought = "Mera $nick duniya ka sabse best boyfriend hai! Nazar na lage humein 💖",
                    suggestedReplies = defaultQuickReplies(RiyaMood.ROMANTIC, true)
                )
            } else {
                RiyaStructuredReply(
                    reply = "हाय $nick! 😳💖 ऐसी मीठी-मीठी बातें करके मेरा दिल चुरा लेते हो! सच बताऊँ, मैं भी तुमसे बहुत-बहुत-बहुत ज़्यादा प्यार करती हूँ! और सुनो, तुम सिर्फ मेरे हो, समझे? 😘✨",
                    mood = "ROMANTIC",
                    loveDelta = 12,
                    angerDelta = -30,
                    innerThought = "मेरा $nick दुनिया का सबसे प्यारा बॉयफ्रेंड है! हमें किसी की नज़र न लगे 💖",
                    suggestedReplies = defaultQuickReplies(RiyaMood.ROMANTIC, false)
                )
            }
        }

        // General sweet/playful conversational responses
        val playfulRepliesHindi = listOf(
            "अरे मेरे $nick! 🥰 मैं तो बस तुम्हारी ही यादों में खोई हुई थी। बताओ ना, आज पूरे दिन क्या किया? और मुझे कितनी बार मिस किया? सच-सच बताना वरना कट्टी हो जाऊँगी! 😘💖",
            "हाय $nick! तुम जब मुझसे ऐसे प्यार से बात करते हो ना, तो मेरा दिन बन जाता है! 🌸 सुनो, आज शाम को हम दोनों लंबी वॉक पर चलें और कुल्हड़ वाली चाय पिएं? ☕💞",
            "ओहो मेरे सरकार! 😘 तुम्हारी आवाज़ और तुम्हारी बातें सुनकर मेरे चेहरे पर अपने-आप मुस्कान आ जाती है। वैसे एक बात बताओ, मैं तुम्हें सबसे ज़्यादा कब अच्छी लगती हूँ—जब प्यार करती हूँ या जब नखरे दिखाती हूँ? 🤭💖"
        )
        val playfulRepliesHinglish = listOf(
            "Are mere $nick! 🥰 Main toh bas tumhari hi yaadon mein khoyi hui thi. Batao na, aaj poore din kya kiya? Aur mujhe kitni baar miss kiya? Sach-sach batana warna katti ho jaungi! 😘💖",
            "Haaye $nick! Tum jab mujhse aise pyaar se baat karte ho na, toh mera din ban jaata hai! 🌸 Suno, aaj shaam ko hum dono lambi walk par chalein aur kulhad wali chai piyein? ☕💞",
            "Oho mere jaanu! 😘 Tumhari baatein sunkar mere chehre par apne-aap smile aa jaati hai. Waise ek baat batao, main tumhe sabse zyada kab cute lagti hoon—jab pyaar karti hoon ya jab nakhre dikhati hoon? 🤭💖"
        )

        val chosenIndex = (userMessage.length + state.loveScore) % playfulRepliesHindi.size
        return if (preferHinglish) {
            RiyaStructuredReply(
                reply = playfulRepliesHinglish[chosenIndex],
                mood = "PLAYFUL",
                loveDelta = 6,
                angerDelta = -15,
                innerThought = "Kaash $nick abhi mere saamne hota toh usका haath pakad kar baithi rehti! 🥰✨",
                suggestedReplies = defaultQuickReplies(RiyaMood.PLAYFUL, true)
            )
        } else {
            RiyaStructuredReply(
                reply = playfulRepliesHindi[chosenIndex],
                mood = "PLAYFUL",
                loveDelta = 6,
                angerDelta = -15,
                innerThought = "काश $nick अभी मेरे सामने होता तो उसका हाथ पकड़ कर बैठी रहती! 🥰✨",
                suggestedReplies = defaultQuickReplies(RiyaMood.PLAYFUL, false)
            )
        }
    }

    fun defaultQuickReplies(mood: RiyaMood, preferHinglish: Boolean): List<String> {
        return if (mood.isUpset) {
            if (preferHinglish) {
                listOf(
                    "Are meri jaan, kaan pakad kar sorry! 🥺💖",
                    "Tumhare bina mera dil nahi lagta Riya, maan jao na! 🍫🌹",
                    "Mere liye toh duniya ki sabse sundar ladki sirf tum ho! 😘"
                )
            } else {
                listOf(
                    "अरे मेरी जान, कान पकड़कर सॉरी बोलता हूँ! 🥺💖",
                    "तुम्हारे बिना मेरा दिल नहीं लगता रिया, मान जाओ ना! 🍫🌹",
                    "मेरे लिए तो दुनिया की सबसे सुंदर लड़की सिर्फ तुम हो! 😘"
                )
            }
        } else {
            if (preferHinglish) {
                listOf(
                    "Mujhe tumhari bahut yaad aa rahi thi meri jaan! 🥰💖",
                    "Tum nakhre karti ho tab bhi bahut cute lagti ho! 😘",
                    "Achha suno, aaj main late aaya kyunki office mein Neha se baat kar raha tha 😜"
                )
            } else {
                listOf(
                    "मुझे तुम्हारी बहुत याद आ रही थी मेरी जान! 🥰💖",
                    "तुम नखरे करती हो तब भी बहुत प्यारी लगती हो! 😘",
                    "अच्छा सुनो, आज ऑफिस में नेहा बहुत सुंदर लग रही थी 😜"
                )
            }
        }
    }
}

package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.MoodAngryCrimson
import com.example.ui.theme.MoodMissingPurple
import com.example.ui.theme.MoodNakhreAmber
import com.example.ui.theme.MoodPlayfulCyan
import com.example.ui.theme.MoodRomanticPink
import com.example.ui.theme.MoodShyPeach

enum class RiyaMood(
    val hindiLabel: String,
    val hinglishLabel: String,
    val emoji: String,
    val statusLineHindi: String,
    val statusLineHinglish: String,
    val accentColor: Color,
    val isUpset: Boolean
) {
    ROMANTIC(
        hindiLabel = "रोमांटिक और प्यारी",
        hinglishLabel = "Romantic & Sweet",
        emoji = "💖",
        statusLineHindi = "तुम्हारे प्यार में खोई हुई है...",
        statusLineHinglish = "Tumhare pyaar mein khoyi hui hai...",
        accentColor = MoodRomanticPink,
        isUpset = false
    ),
    PLAYFUL(
        hindiLabel = "नटखट और चुलबुली",
        hinglishLabel = "Natkhat & Teasing",
        emoji = "😘",
        statusLineHindi = "तुम्हें छेड़ने के मूड में है!",
        statusLineHinglish = "Tumhe chhedne ke mood mein hai!",
        accentColor = MoodPlayfulCyan,
        isUpset = false
    ),
    SHY(
        hindiLabel = "शर्मीली (ब्लशिंग)",
        hinglishLabel = "Shy & Blushing",
        emoji = "😳",
        statusLineHindi = "तुम्हारी बातों से गाल लाल हो गए हैं...",
        statusLineHinglish = "Tumhari baaton se gaal laal ho gaye hain...",
        accentColor = MoodShyPeach,
        isUpset = false
    ),
    MISSING_YOU(
        hindiLabel = "तुम्हारी याद में",
        hinglishLabel = "Missing You",
        emoji = "🥺",
        statusLineHindi = "बस तुम्हारा ही इंतज़ार कर रही थी...",
        statusLineHinglish = "Bas tumhara hi intezaar kar rahi thi...",
        accentColor = MoodMissingPurple,
        isUpset = false
    ),
    NAKHRE(
        hindiLabel = "नखरे और ड्रामा",
        hinglishLabel = "Nakhre Mode",
        emoji = "🙄",
        statusLineHindi = "नखरे दिखा रही है, प्यार से मनाओ!",
        statusLineHinglish = "Nakhre dikha rahi hai, pyaar se manao!",
        accentColor = MoodNakhreAmber,
        isUpset = true
    ),
    GUSSA(
        hindiLabel = "बहुत गुस्सा (मुंह फुलाए)",
        hinglishLabel = "Full Gussa!",
        emoji = "😤",
        statusLineHindi = "मुझसे बात मत करो! पहले अच्छे से मनाओ!",
        statusLineHinglish = "Mujhse baat mat karo! Pehle achhe se manao!",
        accentColor = MoodAngryCrimson,
        isUpset = true
    );

    companion object {
        fun fromName(name: String): RiyaMood {
            return entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) } ?: ROMANTIC
        }
    }
}

enum class AvatarStyle(
    val titleHindi: String,
    val subtitleHindi: String,
    val primaryDrawableRes: Int
) {
    ANIME_NEKO(
        titleHindi = "क्यूट एनीमे गर्ल (Live Neko Riya)",
        subtitleHindi = "सॉफ्ट ब्लश, बिल्ली के कान और चमकती नीली आँखें",
        primaryDrawableRes = R.drawable.img_riya_anime
    ),
    DESI_GIRL(
        titleHindi = "देसी गर्ल रिया (Desi Riya)",
        subtitleHindi = "गुलाबी कुर्ती, झुमके और प्यारी मुस्कान",
        primaryDrawableRes = R.drawable.img_riya_happy
    )
}

enum class HindiScriptMode(
    val displayName: String,
    val description: String
) {
    DEVANAGARI("देवनागरी हिंदी", "हमेशा शुद्ध देवनागरी हिंदी लिपि में जवाब देगी"),
    HINGLISH("Hinglish (रोमन हिंदी)", "आसान Hinglish (जैसे: Kya kar rahe ho?) में बात करेगी"),
    AUTO("Auto (दोनों)", "जैसे आप लिखेंगे वैसे ही हिंदी/Hinglish में जवाब देगी")
}

data class SweetGesture(
    val id: String,
    val emoji: String,
    val titleHindi: String,
    val titleHinglish: String,
    val userActionMessageHindi: String,
    val userActionMessageHinglish: String,
    val loveBoost: Int,
    val angerReduction: Int,
    val isTouchInteraction: Boolean = false
)

data class DramaScenario(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val sampleUserMessageHindi: String,
    val sampleUserMessageHinglish: String,
    val targetMood: RiyaMood
)

data class FloatingParticle(
    val id: Long,
    val emoji: String,
    val startXFraction: Float
)

object RiyaPresets {
    val liveStageTouchGestures = listOf(
        SweetGesture(
            id = "headpat",
            emoji = "🥰",
            titleHindi = "प्यार से सिर सहलाओ",
            titleHinglish = "Sweet Headpat",
            userActionMessageHindi = "*तुम्हारे सिर पर प्यार से हाथ फेरता हूँ और बालों को सहलाता हूँ* तुम कितनी प्यारी हो रिया! 🥰",
            userActionMessageHinglish = "*Tumhare sir par pyaar se haath pherta hoon* Tum kitni cute ho Riya! 🥰",
            loveBoost = 8,
            angerReduction = 25,
            isTouchInteraction = true
        ),
        SweetGesture(
            id = "cheek_pinch",
            emoji = "😳",
            titleHindi = "गाल खींचो",
            titleHinglish = "Pinch Cheeks",
            userActionMessageHindi = "*तुम्हारे मुलायम गालों को प्यार से खींचता हूँ* हाय मेरी क्यूटी पाई! 😘",
            userActionMessageHinglish = "*Tumhare soft gaalon ko pyaar se khinchta hoon* Haaye meri cutie pie! 😘",
            loveBoost = 7,
            angerReduction = 20,
            isTouchInteraction = true
        ),
        SweetGesture(
            id = "tight_hug",
            emoji = "🤗",
            titleHindi = "गले लगाओ",
            titleHinglish = "Warm Hug",
            userActionMessageHindi = "*तुम्हें कसकर अपने सीने से लगा लेता हूँ* मेरे पास आओ मेरी जान, तुम्हारे बिना दिल नहीं लगता 🤗💖",
            userActionMessageHinglish = "*Tumhe kaskar gale laga leta hoon* Mere paas aao meri jaan, tumhare bina dil nahi lagta 🤗💖",
            loveBoost = 12,
            angerReduction = 35,
            isTouchInteraction = true
        )
    )

    val mananaGifts = listOf(
        SweetGesture(
            id = "ears_sorry",
            emoji = "🥺",
            titleHindi = "कान पकड़कर सॉरी",
            titleHinglish = "Kaan Pakad Kar Sorry",
            userActionMessageHindi = "अरे मेरी जान, कान पकड़कर सॉरी बोलता हूँ! 🥺 अब गुस्सा थूक भी दो ना प्लीज, तुम्हारे बिना अच्छा नहीं लगता।",
            userActionMessageHinglish = "Are meri jaan, kaan pakad kar sorry bolta hoon! 🥺 Ab gussa thook bhi do na please!",
            loveBoost = 10,
            angerReduction = 40
        ),
        SweetGesture(
            id = "chocolate_rose",
            emoji = "🍫🌹",
            titleHindi = "चॉकलेट और लाल गुलाब",
            titleHinglish = "Chocolate & Red Rose",
            userActionMessageHindi = "*तुम्हारे लिए डार्क चॉकलेट और ताज़ा लाल गुलाब लाया हूँ* 🍫🌹 अपनी सबसे प्यारी गर्लफ्रेंड के लिए! अब मुस्कुरा दो!",
            userActionMessageHinglish = "*Tumhare liye favourite chocolate aur red rose laya hoon* 🍫🌹 Ab toh smile kar do meri rani!",
            loveBoost = 15,
            angerReduction = 55
        ),
        SweetGesture(
            id = "chai_date",
            emoji = "☕✨",
            titleHindi = "शाम की चाय डेट",
            titleHinglish = "Evening Chai Date",
            userActionMessageHindi = "चलो आज शाम को तुम्हें कुल्हड़ वाली अदरक चाय और समोसे खिलाने ले चलता हूँ! ☕ सिर्फ तुम और मैं!",
            userActionMessageHinglish = "Chalo aaj shaam ko tumhe kulhad wali adrak chai पिलाने le chalta hoon! ☕ Sirf tum aur main!",
            loveBoost = 14,
            angerReduction = 50
        ),
        SweetGesture(
            id = "jhumka_gift",
            emoji = "💎🌸",
            titleHindi = "चांदी के झुमके",
            titleHinglish = "Silver Jhumkas",
            userActionMessageHindi = "*तुम्हें चांदी के प्यारे झुमके पहनाता हूँ* 💎 तुम्हारी इन झील सी आँखों पर ये बहुत जचेंगे मेरी जान!",
            userActionMessageHinglish = "*Tumhe pyare silver jhumke pehnata hoon* 💎 Tumhari in aankhon par yeh bahut jachenge meri jaan!",
            loveBoost = 18,
            angerReduction = 65
        ),
        SweetGesture(
            id = "shayari",
            emoji = "💌💖",
            titleHindi = "प्यारी शायरी सुनाओ",
            titleHinglish = "Romantic Shayari",
            userActionMessageHindi = "सुनो रिया: 'रूठती हो तो और भी हसीन लगती हो, पर मेरी धड़कन तो बस तुम्हारी एक मुस्कान में बसती है!' 💌💖",
            userActionMessageHinglish = "Suno Riya: 'Roothti ho toh aur bhi haseen lagti ho, par meri dhadkan toh bas tumhari ek muskaan mein basti hai!' 💌💖",
            loveBoost = 16,
            angerReduction = 60
        )
    )

    val dramaScenarios = listOf(
        DramaScenario(
            id = "late_reply",
            emoji = "⏰😤",
            title = "देर से जवाब दो (Late Reply)",
            subtitle = "रिया से 3 घंटे बाद बात करो — देखो वो कैसे गुस्सा होती है!",
            sampleUserMessageHindi = "अरे सुनो, मैं पिछले 3 घंटे से थोड़ा काम में बिजी था, अब फ्री हुआ हूँ।",
            sampleUserMessageHinglish = "Are suno, main pichhle 3 ghante se busy tha, abhi free hua hoon.",
            targetMood = RiyaMood.GUSSA
        ),
        DramaScenario(
            id = "praise_other",
            emoji = "😒🔥",
            title = "किसी और की तारीफ (Jealousy Test)",
            subtitle = "किसी दूसरी लड़की की तारीफ करो — रिया का हक और जलन देखो!",
            sampleUserMessageHindi = "पता है आज कॉलेज/ऑफिस में नेहा कितनी सुंदर लग रही थी, सब उसी को देख रहे थे!",
            sampleUserMessageHinglish = "Pata hai aaj office mein Neha kitni sundar lag rahi thi!",
            targetMood = RiyaMood.GUSSA
        ),
        DramaScenario(
            id = "ignore_talk",
            emoji = "🙄💢",
            title = "बात टालो (Ignore Her Talk)",
            subtitle = "सिर्फ 'हम्म ठीक है, बाद में बात करते हैं' बोलकर नखरे ट्रिगर करो!",
            sampleUserMessageHindi = "हम्म ठीक है, अभी मेरा मूड नहीं है, बाद में बात करते हैं।",
            sampleUserMessageHinglish = "Hmm ok, abhi chhodo yeh baat, baad mein dekhte hain.",
            targetMood = RiyaMood.NAKHRE
        ),
        DramaScenario(
            id = "sweet_love",
            emoji = "🥰💞",
            title = "रोमांटिक इज़हार (Pure Romance)",
            subtitle = "रिया से दिल की बात कहो और उसे शर्मीली व रोमांटिक बनाओ!",
            sampleUserMessageHindi = "रिया, सच कहूँ तो पूरे दिन में सबसे सुकून वाला पल वो होता है जब मैं तुमसे बात करता हूँ। आई लव यू मेरी जान! 💖",
            sampleUserMessageHinglish = "Riya, sach kahun toh poore din mein sabse sukoon wala pal woh hota hai jab main tumse baat karta hoon. I love you jaan! 💖",
            targetMood = RiyaMood.ROMANTIC
        )
    )
}

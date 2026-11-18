package com.example.data.model

import androidx.annotation.DrawableRes
import com.example.R

enum class BookCategory(val label: String) {
    PHILOSOPHY("Philosophy"),
    ADVENTURE("Adventure"),
    FICTION("Fiction"),
    STRATEGY("Strategy"),
    MYSTERY("Mystery"),
    PERSONAL("My PDFs")
}

enum class ReaderThemeMode(val label: String) {
    PAPER("Paper"),
    SEPIA("Sepia"),
    NIGHT("Night"),
    OLED("OLED");

    companion object {
        fun fromRaw(raw: String?): ReaderThemeMode =
            entries.firstOrNull { it.name == raw } ?: PAPER
    }
}

data class BookChapter(
    val index: Int,
    val title: String,
    val estimatedMinutes: Int,
    val paragraphs: List<String>
)

data class BookDefinition(
    val id: String,
    val title: String,
    val author: String,
    val subtitle: String,
    val category: BookCategory,
    val yearLabel: String,
    val synopsis: String,
    @DrawableRes val coverDrawableRes: Int?,
    val clothColorHex: Long,
    val accentColorHex: Long,
    val preCachedOffline: Boolean,
    val chapters: List<BookChapter>,
    val isPdfUpload: Boolean = false,
    val localPdfPath: String? = null,
    val pdfPageCount: Int = 0,
    val pdfFileSizeKb: Int = 0
) {
    val totalChapters: Int
        get() = if (isPdfUpload) pdfPageCount.coerceAtLeast(1) else chapters.size

    val totalMinutesEstimate: Int
        get() = if (isPdfUpload) (pdfPageCount * 2).coerceAtLeast(1) else chapters.sumOf { it.estimatedMinutes }
}

object BookCatalog {
    val books: List<BookDefinition> = listOf(
        BookDefinition(
            id = "meditations",
            title = "Meditations",
            author = "Marcus Aurelius",
            subtitle = "Private Stoic reflections on clarity, duty, and calm",
            category = BookCategory.PHILOSOPHY,
            yearLabel = "180 AD",
            synopsis = "Written on campaign along the Danube frontier without any thought of publication, Emperor Marcus Aurelius composed these crystalline reminders on self-discipline, gratitude, and inner stillness.",
            coverDrawableRes = R.drawable.img_cover_meditations_1795027274499,
            clothColorHex = 0xFF283447,
            accentColorHex = 0xFFD9A45B,
            preCachedOffline = true,
            chapters = listOf(
                BookChapter(
                    index = 0,
                    title = "Book I · Debts of Gratitude",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "From my grandfather Verus I learned good morals and the government of my temper. From the reputation and remembrance of my father, modesty and a manly character. From my mother, piety and beneficence, and abstinence not only from evil deeds, but even from evil thoughts; and further, simplicity in my way of living, far removed from the habits of the rich.",
                        "From my tutor, to be neither of the green nor of the blue party at the games in the Circus, nor a partisan either of the Parmularius or the Scutarius at the gladiators' fights; from him too I learned endurance of labor, and to want little, and to work with my own hands, and not to meddle with other people's affairs, and not to be ready to listen to slander.",
                        "From Rusticus I received the impression that my character required improvement and discipline; and from him I learned not to be led astray to sophistic emulation, nor to writing on speculative matters, nor to delivering little hortatory orations, nor to showing myself off as a man who practices much discipline, or does benevolent acts in order to make a display.",
                        "From Apollonius I learned freedom of will and undeviating steadiness of purpose; and to look to nothing else, not even for a moment, except to reason; and to be always the same, in sharp pains, on the occasion of the loss of a child, and in long illness; and to see clearly in a living example that the same man can be both most resolute and yielding."
                    )
                ),
                BookChapter(
                    index = 1,
                    title = "Book II · Morning Counsel",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "Begin the morning by saying to thyself, I shall meet with the busybody, the ungrateful, arrogant, deceitful, envious, unsocial. All these things happen to them by reason of their ignorance of what is good and evil.",
                        "But I who have seen the nature of the good that it is beautiful, and of the bad that it is ugly, and the nature of him who does wrong, that it is akin to me, not only of the same blood or seed, but that it participates in the same intelligence and the same portion of the divinity, I can neither be injured by any of them, for no one can fix on me what is ugly, nor can I be angry with my kinsman, nor hate him.",
                        "For we are made for cooperation, like feet, like hands, like eyelids, like the rows of the upper and lower teeth. To act against one another then is contrary to nature; and it is acting against one another to be vexed and to turn away.",
                        "Do not waste the remainder of thy life in thoughts about others, when thou dost not refer thy thoughts to some object of common utility. For thou losest the opportunity of doing something else when thou hast such thoughts as these: What is such a person doing, and why, and what is he saying, and what is he thinking of?"
                    )
                ),
                BookChapter(
                    index = 2,
                    title = "Book III · The Inner Citadel",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "We ought to consider not only that our life is daily wasting away and a smaller part of it is left, but another thing also must be taken into the account: that if a man should live longer, it is quite uncertain whether the understanding will still continue sufficient for the comprehension of things.",
                        "Be not hurried then, nor think of things at a distance, but live each hour with carefulness, as a Roman and a man, in doing that which thou hast in hand with perfect and simple dignity, and feeling of affection, and freedom, and justice.",
                        "Never value anything as profitable to thyself which shall compel thee to break thy promise, to lose thy self-respect, to hate any man, to suspect, to curse, to act the hypocrite, to desire anything which needs walls and curtains.",
                        "For he who has preferred to everything intelligence and daemon and the worship of its excellence, acts no tragic part, does not groan, will not need either solitude or much company; and what is chief of all, he will live without either pursuing or flying from death."
                    )
                ),
                BookChapter(
                    index = 3,
                    title = "Book IV · Stillness in Motion",
                    estimatedMinutes = 5,
                    paragraphs = listOf(
                        "Men seek retreats for themselves, houses in the country, seashores, and mountains; and thou too art wont to desire such things very much. But this is altogether a mark of the most common sort of men, for it is in thy power whenever thou shalt choose to retire into thyself.",
                        "For nowhere either with more quiet or more freedom from trouble does a man retire than into his own soul, particularly when he has within him such thoughts that by looking into them he is immediately in perfect tranquility.",
                        "Constantly then give to thyself this retreat, and renew thyself; and let thy principles be brief and fundamental, which, as soon as thou shalt recur to them, will be sufficient to cleanse the soul completely, and to send thee back free from all discontent with the things to which thou returnest.",
                        "The universe is transformation: life is opinion. Remove the opinion, and then there is removed the complaint, 'I have been harmed.' Remove the complaint, 'I have been harmed,' and the harm is removed."
                    )
                ),
                BookChapter(
                    index = 4,
                    title = "Book V · Shared Nature",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "In the morning when thou risest unwillingly, let this thought be present — I am rising to the work of a human being. Why then am I dissatisfied if I am going to do the things for which I exist and for which I was brought into the world?",
                        "Hast thou not seen the little plants, the little birds, the ants, the spiders, the bees working together to put in order their several parts of the universe? And art thou unwilling to do the work of a human being, and dost thou not make haste to do that which is according to thy nature?",
                        "What is not good for the swarm, neither is it good for the bee. Adapt thyself to the things with which thy lot has been cast: and the men among whom thou hast received thy portion, love them, but do it truly and sincerely.",
                        "Look within. Let neither the peculiar quality of anything nor its value escape thee. Soon thou wilt have forgotten all things; and soon all things will have forgotten thee."
                    )
                )
            )
        ),
        BookDefinition(
            id = "wonderland",
            title = "Alice in Wonderland",
            author = "Lewis Carroll",
            subtitle = "Down the rabbit-hole into a world of joyful absurdity",
            category = BookCategory.ADVENTURE,
            yearLabel = "1865",
            synopsis = "A golden afternoon beside the riverbank gives way to a pocket-watch-carrying White Rabbit, a tiny golden key, and a labyrinthine garden where logic bends like tea-time poetry.",
            coverDrawableRes = R.drawable.img_cover_wonderland_1795027275734,
            clothColorHex = 0xFF1F4E46,
            accentColorHex = 0xFFE3B963,
            preCachedOffline = true,
            chapters = listOf(
                BookChapter(
                    index = 0,
                    title = "Chapter I · Down the Rabbit-Hole",
                    estimatedMinutes = 5,
                    paragraphs = listOf(
                        "Alice was beginning to get very tired of sitting by her sister on the bank, and of having nothing to do: once or twice she had peeped into the book her sister was reading, but it had no pictures or conversations in it, 'and what is the use of a book,' thought Alice, 'without pictures or conversations?'",
                        "So she was considering in her own mind (as well as she could, for the hot day made her feel very sleepy and stupid), whether the pleasure of making a daisy-chain would be worth the trouble of getting up and picking the daisies, when suddenly a White Rabbit with pink eyes ran close by her.",
                        "There was nothing so very remarkable in that; nor did Alice think it so very much out of the way to hear the Rabbit say to itself, 'Oh dear! Oh dear! I shall be late!' But when the Rabbit actually took a watch out of its waistcoat-pocket, and looked at it, and then hurried on, Alice started to her feet, burning with curiosity, and ran across the field after it.",
                        "In another moment down went Alice after it, never once considering how in the world she was to get out again. The rabbit-hole went straight on like a tunnel for some way, and then dipped suddenly down, so suddenly that Alice had not a moment to think about stopping herself before she found herself falling down a very deep well."
                    )
                ),
                BookChapter(
                    index = 1,
                    title = "Chapter II · The Pool of Tears",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "'Curiouser and curiouser!' cried Alice (she was so much surprised, that for the moment she quite forgot how to speak good English); 'now I'm opening out like the largest telescope that ever was! Good-bye, feet!'",
                        "Just then her head struck against the roof of the hall: in fact she was now more than nine feet high, and she at once took up the little golden key and hurried off to the garden door.",
                        "Poor Alice! It was as much as she could do, lying down on one side, to look through into the garden with one eye; but to get through was more hopeless than ever: she sat down and began to cry again.",
                        "She generally gave herself very good advice, (though she very seldom followed it), and sometimes she scolded herself so severely as to bring tears into her eyes; and once she remembered trying to box her own ears for having cheated herself in a game of croquet she was playing against herself."
                    )
                ),
                BookChapter(
                    index = 2,
                    title = "Chapter III · Advice from a Caterpillar",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "The Caterpillar and Alice looked at each other for some time in silence: at last the Caterpillar took the hookah out of its mouth, and addressed her in a languid, sleepy voice.",
                        "'Who are you?' said the Caterpillar. This was not an encouraging opening for a conversation. Alice replied, rather shyly, 'I—I hardly know, sir, just at present—at least I know who I was when I got up this morning, but I think I must have been changed several times since then.'",
                        "'What do you mean by that?' said the Caterpillar sternly. 'Explain yourself!' 'I can't explain myself, I'm afraid, sir,' said Alice, 'because I'm not myself, you see.'",
                        "In a minute or two the Caterpillar took the hookah out of its mouth and yawned once or twice, and shook itself. Then it got down off the mushroom, and crawled away in the grass, merely remarking as it went, 'One side will make you grow taller, and the other side will make you grow shorter.'"
                    )
                ),
                BookChapter(
                    index = 3,
                    title = "Chapter IV · A Mad Tea-Party",
                    estimatedMinutes = 5,
                    paragraphs = listOf(
                        "There was a table set out under a tree in front of the house, and the March Hare and the Hatter were having tea at it: a Dormouse was sitting between them, fast asleep, and the other two were using it as a cushion, resting their elbows on it, and talking over its head.",
                        "The table was a large one, but the three were all crowded together at one corner of it: 'No room! No room!' they cried out when they saw Alice coming. 'There's plenty of room!' said Alice indignantly, and she sat down in a large arm-chair at one end of the table.",
                        "'Have some wine,' the March Hare said in an encouraging tone. Alice looked all round the table, but there was nothing on it but tea. 'I don't see any wine,' she remarked. 'There isn't any,' said the March Hare.",
                        "'Then it wasn't very civil of you to offer it,' said Alice angrily. 'It wasn't very civil of you to sit down without being invited,' said the March Hare. 'Your hair wants cutting,' said the Hatter. He had been looking at Alice for some time with great curiosity, and this was his first speech."
                    )
                )
            )
        ),
        BookDefinition(
            id = "odyssey",
            title = "The Odyssey",
            author = "Homer",
            subtitle = "A ten-year voyage across wine-dark seas toward Ithaca",
            category = BookCategory.ADVENTURE,
            yearLabel = "8th c. BC",
            synopsis = "Sing, O Muse, of the resourceful wanderer Odysseus, who endured storms, sirens, and enchanted islands while yearning for the hearth-smoke of his home.",
            coverDrawableRes = R.drawable.img_cover_odyssey_1795027276376,
            clothColorHex = 0xFF1A2B4C,
            accentColorHex = 0xFFD07148,
            preCachedOffline = false,
            chapters = listOf(
                BookChapter(
                    index = 0,
                    title = "Book I · Tell Me, O Muse",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "Tell me, O Muse, of that ingenious hero who travelled far and wide after he had sacked the famous town of Troy. Many cities did he visit, and many were the nations with whose manners and customs he was acquainted; moreover he suffered much by sea while trying to save his own life and bring his men safely home.",
                        "All the other heroes who had survived the war were now safely at home, safe from battle and from the sea; only Odysseus was kept away by the nymph Calypso in her vaulted caves, longing for his wife and for Ithaca.",
                        "When the revolving years had brought the time ordained by heaven for his return, even then his trials were not ended. Yet the gods pitied him, all save Poseidon, who held unrelenting anger against godlike Odysseus until he reached his native shore.",
                        "Then grey-eyed Athena spoke before the assembly of Olympus: 'My heart breaks for wise Odysseus, who suffers far from his friends on an island ringed by the sea, watching for a single wisp of smoke rising from his own hearth.'"
                    )
                ),
                BookChapter(
                    index = 1,
                    title = "Book V · Calypso's Island",
                    estimatedMinutes = 5,
                    paragraphs = listOf(
                        "As Dawn rose from her couch beside Tithonus, bringing light to gods and mortals alike, Hermes strapped on his golden winged sandals that carry him swift as the wind over boundless sea and land.",
                        "He came to the remote island of Ogygia and stepped into a spacious cavern warmed by a great fire of cedarwood and fragrant thyme. Inside, Calypso was singing with a clear sweet voice as she moved to and fro before her loom with a golden shuttle.",
                        "Around the cavern grew a flourishing wood of alder, poplar, and sweet-smelling cypress, where long-winged birds nested, and four clear springs bubbled out over soft meadows of violet and parsley.",
                        "Hermes found Odysseus not inside the cavern, but seated upon the rocky headland weeping, his eyes fixed upon the barren sea. Four days he spent felling tall timber with a bronze axe to build a broad-beamed raft, and on the fifth day Calypso sent him forth with a fair, warm wind."
                    )
                ),
                BookChapter(
                    index = 2,
                    title = "Book IX · The Cyclops' Cave",
                    estimatedMinutes = 5,
                    paragraphs = listOf(
                        "'I am Odysseus, son of Laertes, known among all people for my stratagems, and my fame reaches up to heaven. I dwell in sunny Ithaca, where Mount Neriton lifts its trembling leaves against the sky.'",
                        "So he told the Phaeacians of the land of the Lotus-Eaters, where tasting the honey-sweet blossom erased all memory of homecoming, so he had to carry his weeping companions back to the hollow ships and tie them beneath the benches.",
                        "From there they sailed to the island of the Cyclopes, arrogant and lawless giants who neither plow nor plant. Entering a lofty cave above the sea, Odysseus and twelve chosen companions awaited the master of the flocks.",
                        "When Polyphemus rolled a boulder across the doorway that twenty-two wagons could scarcely stir, Odysseus relied not on strength alone, but on quick wit, calling himself 'Nobody' until his sharpened olive-wood stake secured their escape beneath the bellies of the thick-fleeced rams."
                    )
                ),
                BookChapter(
                    index = 3,
                    title = "Book XII · Sirens and Straits",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "'First you will come to the Sirens, who enchant every sailor that draws near their meadow. Therefore soften sweet beeswax and stop the ears of your crew, and if you yourself wish to hear their song, have them bind you hand and foot upright against the mast.'",
                        "When the black ship raced within earshot of the shore, the wind suddenly fell and a breathless calm spread over the waves. Odysseus stood lashed to the mast as the Sirens lifted their clear song, promising knowledge of all things that happen upon the fruitful earth.",
                        "He signaled with his brows for his men to loose him, yet Perimedes and Eurylochus only pulled the ropes tighter until the island faded behind the foaming wake.",
                        "Beyond lay the narrow strait between roaring Charybdis and six-headed Scylla, where steadiness at the helm and loyalty among shipmates proved worth more than bronze armor."
                    )
                ),
                BookChapter(
                    index = 4,
                    title = "Book XXIII · The Olive-Tree Bed",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "Through twenty years of wandering and disguise, Penelope had kept the palace whole by patience and cunning, weaving and unweaving Laertes' shroud by lamplight while the suitors feasted below.",
                        "Even when Odysseus stood bathed and restored in the great hall, she tested him once more with a secret known only to the two of them: she ordered the servants to move their stout bridal bed outside the bedchamber.",
                        "Odysseus started in indignation: 'Who could move my bed? No mortal alive could shift it, for I built our chamber around a living olive tree rooted deep in the courtyard earth, trimming its trunk into the bedpost itself!'",
                        "At that sign Penelope's knees gave way and her heart melted; she ran straight to him and threw her arms around his neck, and Dawn held back her golden horses so their reunion might linger in the quiet night."
                    )
                )
            )
        ),
        BookDefinition(
            id = "art_of_war",
            title = "The Art of War",
            author = "Sun Tzu",
            subtitle = "Timeless principles of foresight, poise, and clarity",
            category = BookCategory.STRATEGY,
            yearLabel = "5th c. BC",
            synopsis = "A concise masterwork on winning without needless conflict—studying terrain, timing, morale, and knowing both oneself and the landscape ahead.",
            coverDrawableRes = null,
            clothColorHex = 0xFF7D2E25,
            accentColorHex = 0xFFE3BA72,
            preCachedOffline = false,
            chapters = listOf(
                BookChapter(
                    index = 0,
                    title = "I · Laying Plans",
                    estimatedMinutes = 3,
                    paragraphs = listOf(
                        "Sun Tzu said: The art of strategy is of vital importance to the state. It is a matter of life and death, a road either to safety or to ruin. Hence it is a subject of inquiry which can on no account be neglected.",
                        "The art of strategy, then, is governed by five constant factors, to be taken into account in one's deliberations when seeking to determine the conditions obtaining in the field: The Moral Law, Heaven, Earth, The Commander, and Method and Discipline.",
                        "All strategy is based on timing and positioning. Hold out advantages to draw energy, feign stillness when near, and appear calm when the situation is turbulent. The leader who makes many calculations in the quiet chamber before acting prevails over the one who makes few."
                    )
                ),
                BookChapter(
                    index = 1,
                    title = "III · Strategic Restraint",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "In the practical art of strategy, the best thing of all is to keep the whole domain intact and unharmed; to shatter and destroy it is not so good. So, too, it is better to win peace with an entire city than to break it apart.",
                        "Hence to fight and conquer in all your battles is not supreme excellence; supreme excellence consists in resolving the contest without ever drawing the sword.",
                        "Thus the highest form of leadership is to frustrate an opponent's plans before they take shape; the next best is to prevent alliances from hardening; and the worst policy of all is to besiege walled cities out of impatience.",
                        "If you know the opponent and know yourself, you need not fear the result of a hundred trials. If you know yourself but not the opponent, for every victory gained you will also suffer a defeat."
                    )
                ),
                BookChapter(
                    index = 2,
                    title = "V · Energy & Momentum",
                    estimatedMinutes = 3,
                    paragraphs = listOf(
                        "The control of a large group is the same principle as the control of a few: it is merely a question of dividing up their numbers cleanly and keeping clear signals.",
                        "In conflict, direct engagement may be used for joining, but indirect methods will be needed to secure the breakthrough. Indirect tactics, efficiently applied, are inexhaustible as Heaven and Earth, unending as the flow of rivers and streams.",
                        "The quality of decision is like the well-timed swoop of a falcon which enables it to strike cleanly at the decisive moment. Energy may be likened to the bending of a crossbow; decision, to the releasing of the trigger."
                    )
                ),
                BookChapter(
                    index = 3,
                    title = "VI · Weak Points & Strong",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "Whoever is first in the field and awaits the coming of the situation will be fresh; whoever is second in the field and has to hasten arrives exhausted.",
                        "Let your plans be dark and impenetrable as night, and when you move, fall like a thunderbolt. Water shapes its course according to the nature of the ground over which it flows; the wise strategist works out victory in relation to the changing conditions.",
                        "Just as water retains no constant shape, so in strategy there are no constant conditions. Whoever can modify tactics in relation to shifting ground and thereby succeed may be called a master of the craft."
                    )
                )
            )
        ),
        BookDefinition(
            id = "frankenstein",
            title = "Frankenstein",
            author = "Mary Shelley",
            subtitle = "The modern Prometheus born of ambition and lightning",
            category = BookCategory.FICTION,
            yearLabel = "1818",
            synopsis = "From the frozen wastes of the Arctic Circle, Victor Frankenstein recounts the fateful night in Ingolstadt when curiosity crossed into creation—and the tragic longing of the being he abandoned.",
            coverDrawableRes = null,
            clothColorHex = 0xFF2B433B,
            accentColorHex = 0xFFCDB478,
            preCachedOffline = false,
            chapters = listOf(
                BookChapter(
                    index = 0,
                    title = "Letter I · Northern Ice",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "You will rejoice to hear that no disaster has accompanied the commencement of an enterprise which you have regarded with such evil forebodings. I arrived here yesterday, and my first task is to assure my dear sister of my welfare and increasing confidence in the success of my undertaking.",
                        "I am already far north of London, and as I walk in the streets of Petersburgh, I feel a cold northern breeze play upon my cheeks, which braces my nerves and fills me with delight. Do you understand this feeling? This breeze, which has travelled from the regions towards which I am advancing, gives me a foretaste of those icy climes.",
                        "I try in vain to be persuaded that the pole is the seat of frost and desolation; it ever presents itself to my imagination as the region of beauty and delight. There, Margaret, the sun is forever visible, its broad disk just skirting the horizon and diffusing a perpetual splendour."
                    )
                ),
                BookChapter(
                    index = 1,
                    title = "Chapter IV · A Spark of Being",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "From this day natural philosophy, and particularly chemistry, in the most comprehensive sense of the term, became nearly my sole occupation. I read with ardour those works, so full of genius and discrimination, which modern inquirers have written on these subjects.",
                        "Two years passed in this manner, during which I paid no visit to Geneva, but was engaged, heart and soul, in the pursuit of some discoveries which I hoped to make. None but those who have experienced them can conceive of the enticements of science.",
                        "After days and nights of incredible labour and fatigue, I succeeded in discovering the cause of generation and life; nay, more, I became myself capable of bestowing animation upon lifeless matter. Learn from me, if not by my precepts, at least by my example, how dangerous is the acquirement of knowledge."
                    )
                ),
                BookChapter(
                    index = 2,
                    title = "Chapter V · A Dreary Night of November",
                    estimatedMinutes = 5,
                    paragraphs = listOf(
                        "It was on a dreary night of November that I beheld the accomplishment of my toils. With an anxiety that almost amounted to agony, I collected the instruments of life around me, that I might infuse a spark of being into the lifeless thing that lay at my feet.",
                        "It was already one in the morning; the rain pattered dismally against the panes, and my candle was nearly burnt out, when, by the glimmer of the half-extinguished light, I saw the dull yellow eye of the creature open; it breathed hard, and a convulsive motion agitated its limbs.",
                        "How can I describe my emotions at this catastrophe, or how delineate the wretch whom with such infinite pains and care I had endeavoured to form? His limbs were in proportion, and I had selected his features as beautiful. Beautiful! Great God! The beauty of the dream vanished, and breathless horror and disgust filled my heart."
                    )
                ),
                BookChapter(
                    index = 3,
                    title = "Chapter XI · Awakening in the Woods",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "'It is with considerable difficulty that I remember the original era of my being; all the events of that period appear confused and indistinct. A strange multiplicity of sensations seized me, and I saw, felt, heard, and smelt at the same time.'",
                        "'Soon a gentle light stole over the heavens and gave me a sensation of pleasure. I started up and beheld a radiant form rise from among the trees. I gazed with a kind of wonder. It moved slowly, but it enlightened my path, and I again went out in search of berries.'",
                        "'One day, when I was oppressed by cold, I found a fire which had been left by some wandering beggars, and was overcome with delight at the warmth I experienced from it. In my joy I thrust my hand into the live embers, but quickly drew it out again with a cry of pain. How strange, I thought, that the same cause should produce such opposite effects!'"
                    )
                )
            )
        ),
        BookDefinition(
            id = "sherlock",
            title = "Sherlock Holmes",
            author = "Arthur Conan Doyle",
            subtitle = "Fog-lit Baker Street deductions and quiet mysteries",
            category = BookCategory.MYSTERY,
            yearLabel = "1892",
            synopsis = "Step inside 221B Baker Street with Dr. John Watson as Sherlock Holmes unravels ciphered notes, missing heirlooms, and impossible clues hidden in plain sight.",
            coverDrawableRes = null,
            clothColorHex = 0xFF33293A,
            accentColorHex = 0xFFD9B675,
            preCachedOffline = false,
            chapters = listOf(
                BookChapter(
                    index = 0,
                    title = "I · A Scandal in Bohemia",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "To Sherlock Holmes she is always THE woman. I have seldom heard him mention her under any other name. In his eyes she eclipses and predominates the whole of her sex.",
                        "I had seen little of Holmes lately. My marriage had drifted us away from each other. My own complete happiness, and the home-centred interests which rise up around the man who first finds himself master of his own establishment, were sufficient to absorb all my attention, while Holmes, who loathed every form of society with his whole Bohemian soul, remained in our lodgings in Baker Street.",
                        "One night—it was on the twentieth of March, 1888—I was returning from a journey to a patient, when my way led me through Baker Street. As I passed the well-remembered door, I was seized with a keen desire to see Holmes again, and to know how he was employing his extraordinary powers."
                    )
                ),
                BookChapter(
                    index = 1,
                    title = "II · You See, But You Do Not Observe",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "'I see it, I deduce it. How do I know that you have been getting yourself very wet lately, and that you have a most clumsy and careless servant girl?' 'My dear Holmes,' said I, 'this is too much.'",
                        "'It is simplicity itself,' said he; 'my eyes tell me that on the inside of your left shoe, just where the firelight strikes it, the leather is scored by six almost parallel cuts. Obviously they have been caused by someone who has very carelessly scraped round the edges of the sole in order to remove crusted mud from it.'",
                        "'You see, but you do not observe. The distinction is clear. For example, you have frequently seen the steps which lead up from the hall to this room. How many are there?' 'How many? I don't know.' 'Quite so! You have not observed. And yet you have seen. Now I know that there are seventeen steps, because I have both seen and observed.'"
                    )
                ),
                BookChapter(
                    index = 2,
                    title = "III · The Red-Headed League",
                    estimatedMinutes = 5,
                    paragraphs = listOf(
                        "I had called upon my friend, Mr. Sherlock Holmes, one day in the autumn of last year and found him in deep conversation with a very stout, florid-faced, elderly gentleman with fiery red hair.",
                        "'You could not possibly have come at a better time, my dear Watson,' he said cordially. 'Mr. Jabez Wilson here has been good enough to call upon me this morning, and to begin a narrative which promises to be one of the most singular which I have listened to for some time.'",
                        "'It is quite a three-pipe problem,' murmured Holmes, curling himself up in his chair and drawing his knees up to his hawk-like nose. 'What do they want him out of his tiny pawnbroker's cellar for four hours every afternoon for? Depend upon it, Watson, there is something underfoot beneath Coburg Square.'"
                    )
                ),
                BookChapter(
                    index = 3,
                    title = "IV · The Speckled Band",
                    estimatedMinutes = 4,
                    paragraphs = listOf(
                        "On glancing over my notes of the seventy odd cases in which I have during the last eight years studied the methods of my friend Sherlock Holmes, I find many tragic, some comic, a large number merely strange, but none commonplace.",
                        "It was early in April in the year '83 that I woke one morning to find Sherlock Holmes standing, fully dressed, by the side of my bed. 'Very sorry to knock you up, Watson,' said he, 'but it's the common lot this morning. A young lady has arrived in a considerable state of excitement, who insists upon seeing me.'",
                        "'When a doctor goes wrong he is the first of criminals,' Holmes whispered later that night as we sat in total darkness inside Stoke Moran Manor, waiting for the faint metallic clang of the ventilator and the quiet whistle in the midnight air."
                    )
                )
            )
        )
    )

    fun findById(bookId: String): BookDefinition? = books.firstOrNull { it.id == bookId }
}

package com.gideongeng.kenyatourism.data

import android.content.Context
import androidx.room.Room
import com.gideongeng.kenyatourism.data.local.AppDatabase
import com.gideongeng.kenyatourism.data.local.DestinationEntity
import com.gideongeng.kenyatourism.data.local.CommentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import org.json.JSONObject

import com.gideongeng.kenyatourism.data.local.UserMediaEntity
import android.net.Uri

data class Destination(
    val id: Int,
    val name: String,
    val category: String,
    val description: String,
    val imageUrl: String,
    val imageGallery: List<String> = emptyList(),
    val videoUrl: String? = null,
    val rating: Float,
    val region: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val bestTimeToVisit: String? = null,
    val activities: List<String> = emptyList(),
    val isGalleryCached: Boolean = false
)

data class DestinationMedia(
    val url: String,
    val type: String, // "image" or "video"
    val userName: String,
    val timestamp: Long
)

data class Comment(val userName: String, val text: String, val timestamp: Long)

object DestinationsRepository {
    private var database: AppDatabase? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val reviewsCollection = firestore.collection("public_reviews")
    
    private val _allDestinations = MutableStateFlow<List<Destination>>(emptyList())
    val allDestinations: StateFlow<List<Destination>> = _allDestinations.asStateFlow()

    fun getComments(destinationId: Int): Flow<List<Comment>> {
        syncPublicReviews(destinationId)
        return database?.destinationDao()?.getCommentsForDestination(destinationId)?.map { entities ->
            entities.map { Comment(it.userName, it.text, it.timestamp) }
        } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    fun getMedia(destinationId: Int): Flow<List<DestinationMedia>> {
        return database?.destinationDao()?.getMediaForDestination(destinationId)?.map { entities ->
            entities.map { DestinationMedia(it.url, it.mediaType, it.userName, it.timestamp) }
        } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    private fun syncPublicReviews(destinationId: Int) {
        reviewsCollection
            .whereEqualTo("destinationId", destinationId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                CoroutineScope(Dispatchers.IO).launch {
                    val entities = snapshot.documents.mapNotNull { doc ->
                        val text = doc.getString("text") ?: return@mapNotNull null
                        val userName = doc.getString("userName") ?: "Traveler"
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        CommentEntity(destinationId = destinationId, userName = userName, text = text, timestamp = timestamp)
                    }
                    database?.destinationDao()?.insertPublicComments(entities)
                }
            }
    }

    suspend fun fetchWikipediaImages(destinationId: Int, destinationName: String): List<String> {
        return try {
            val query = "${destinationName} Kenya".replace(" ", "_")
            val url = "https://en.wikipedia.org/w/api.php?action=query&prop=pageimages|images&titles=$query&format=json&piprop=original&origin=*"
            val response = URL(url).readText()
            val json = JSONObject(response)
            val pages = json.getJSONObject("query").optJSONObject("pages") ?: return emptyList()
            val images = mutableListOf<String>()
            
            pages.keys().forEach { key ->
                val page = pages.getJSONObject(key)
                if (page.has("original")) {
                    val src = page.getJSONObject("original").getString("source")
                    if (isRealPhoto(src)) images.add(src)
                }
                
                if (page.has("images") && images.size < 3) {
                    val imgList = page.getJSONArray("images")
                    for (i in 0 until imgList.length()) {
                        val imgTitle = imgList.getJSONObject(i).getString("title").replace(" ", "_")
                        val imgUrl = "https://en.wikipedia.org/w/api.php?action=query&titles=$imgTitle&prop=imageinfo&iiprop=url&format=json&origin=*"
                        val imgResp = URL(imgUrl).readText()
                        val imgJson = JSONObject(imgResp)
                        val imgPages = imgJson.getJSONObject("query").optJSONObject("pages")
                        imgPages?.keys()?.forEach { k ->
                            val p = imgPages.getJSONObject(k)
                            val info = p.optJSONArray("imageinfo")
                            if (info != null && info.length() > 0) {
                                val src = info.getJSONObject(0).getString("url")
                                if (isRealPhoto(src) && !images.contains(src)) {
                                    images.add(src)
                                }
                            }
                        }
                        if (images.size >= 3) break
                    }
                }
            }
            if (images.isNotEmpty()) {
                database?.destinationDao()?.updateGalleryCache(destinationId, images.joinToString(","))
            }
            images
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun isRealPhoto(url: String): Boolean {
        val lowUrl = url.lowercase()
        val banned = listOf("logo", "map", "flag", "icon", "shield", "svg", "marker", "blueprint", "diagram", "portrait_placeholder")
        return banned.none { lowUrl.contains(it) } && (lowUrl.endsWith(".jpg") || lowUrl.endsWith(".jpeg") || lowUrl.endsWith(".png"))
    }

    private fun saveMediaLocally(context: Context, uri: Uri, destinationId: Int): String? {
        return try {
            val mediaDir = File(context.filesDir, "user_media/$destinationId")
            if (!mediaDir.exists()) mediaDir.mkdirs()
            
            val timestamp = System.currentTimeMillis()
            val extension = context.contentResolver.getType(uri)?.split("/")?.lastOrNull() ?: "bin"
            val file = File(mediaDir, "media_$timestamp.$extension")
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun addComment(destinationId: Int, userName: String, text: String) {
        val timestamp = System.currentTimeMillis()
        val commentData = hashMapOf(
            "destinationId" to destinationId,
            "userName" to userName,
            "text" to text,
            "timestamp" to timestamp
        )
        reviewsCollection.add(commentData)
        CoroutineScope(Dispatchers.IO).launch {
            database?.destinationDao()?.insertComment(
                CommentEntity(destinationId = destinationId, userName = userName, text = text, timestamp = timestamp)
            )
        }
    }

    fun uploadMedia(context: Context, destinationId: Int, uri: Uri, type: String, onComplete: (Boolean) -> Unit) {
        val localPath = saveMediaLocally(context, uri, destinationId)
        if (localPath != null) {
            val timestamp = System.currentTimeMillis()
            CoroutineScope(Dispatchers.IO).launch {
                database?.destinationDao()?.insertMedia(
                    UserMediaEntity(destinationId = destinationId, url = localPath, mediaType = type, timestamp = timestamp)
                )
                onComplete(true)
            }
        } else {
            onComplete(false)
        }
    }

    fun initialize(context: android.content.Context) {
        if (database != null) return
        database = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "kenya_tourism_db"
        )
        .fallbackToDestructiveMigration() // Added for the schema change
        .build()
        CoroutineScope(Dispatchers.IO).launch {
            val dao = database!!.destinationDao()
            val entities = staticDestinations.map { it.toEntity() }
            dao.insertAll(entities)
            dao.getAllDestinations().collect { entities ->
                _allDestinations.value = entities.map { it.toDestination() }
            }
        }
    }

    private fun Destination.toEntity() = DestinationEntity(
        id = id,
        name = name,
        category = category,
        description = description,
        imageUrl = imageUrl,
        rating = rating,
        region = region,
        latitude = latitude,
        longitude = longitude,
        activities = activities.joinToString(","),
        imageGalleryCache = imageGallery.joinToString(",")
    )

    private fun DestinationEntity.toDestination() = Destination(
        id = id,
        name = name,
        category = category,
        description = description,
        imageUrl = imageUrl,
        rating = rating,
        region = region,
        latitude = latitude,
        longitude = longitude,
        activities = if (activities.isEmpty()) emptyList() else activities.split(","),
        imageGallery = if (imageGalleryCache.isNullOrEmpty()) emptyList() else imageGalleryCache.split(","),
        isGalleryCached = !imageGalleryCache.isNullOrEmpty()
    )

    // Helper to resolve local drawable resource
    fun getDestinationDrawable(context: Context, name: String): Int {
        try {
            val normalized = when (name.lowercase().trim()) {
                "maasai mara national reserve" -> "masai_mara_1"
                "david sheldrick wildlife trust" -> "david_sheldrick_elephant_orphanage"
                "the giraffe centre" -> "giraffe_centre"
                "fort jesus museum" -> "fort_jesus_mombasa"
                "kenyatta international convention centre (kicc)" -> "nairobi_city"
                "hell's gate national park" -> "hell_s_gate_national_park"
                "shimoni slave caves" -> "fort_jesus_mombasa" // No dedicated drawable
                "watamu beach" -> "watamu_beach"
                "lake bogoria" -> "lake_bogoria"
                else -> name.lowercase()
                    .replace("'", "")
                    .replace(Regex("[^a-z0-9]"), "_")
                    .replace(Regex("_+"), "_")
                    .trim('_')
            }
            return context.resources.getIdentifier(normalized, "drawable", context.packageName)
        } catch (e: Exception) {
            return 0
        }
    }

    private val staticDestinations = listOf(
        Destination(
            1, 
            "Maasai Mara National Reserve", 
            "Wildlife Safari", 
            "The Maasai Mara is Africa's greatest wildlife spectacle. Spanning 1,510 square kilometers, it is a land of breathtaking vistas, abundant wildlife and endless plains. The reserve is world-renowned for its exceptional population of lions, leopards, cheetahs, and the Great Migration of over 1.5 million wildebeest and zebras. The Mara is not just a park; it's a globally recognized stage for nature's raw power. Visitors can witness the dramatic river crossings, explore the vast savannahs on game drives, and connect with the vibrant Maasai culture. The ecosystem supports over 95 species of mammals and 570 recorded species of birds, making it a paradise for photographers and nature lovers alike. Each year, between July and October, the Great Migration arrives, creating a buzz of activity as predators follow the herds across the Mara River. It's an experience that stays with you forever.", 
            "", 
            listOf(
                "",
                "",
                ""
            ), 
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ", 
            4.9f, 
            "Rift Valley", 
            -1.5, 
            35.1, 
            "July-October (Great Migration)", 
            listOf("Game Drives", "Balloon Safari", "Maasai Village Visit", "Nature Walks")
        ),
        Destination(
            2, 
            "Amboseli National Park", 
            "Wildlife Safari", 
            "Crowned by Mount Kilimanjaro, Amboseli National Park is one of Kenya's most iconic landscapes. Famous for being the best place in the world to get close to free-ranging elephants, the park offers a unique photographer's dream: massive herds with snow-capped peaks in the background. The park's 392 square kilometers comprise an ecosystem that spreads across the Kenya-Tanzania border. Its unique hydrology, fueled by Kilimanjaro's melting snow, creates lush swamps in the middle of a semi-arid landscape, attracting a wealth of birdlife and grazers. Visitors can explore the Observation Hill for a panoramic view of the entire park, witness the sunrise over the mountain, and learn about the delicate balance of nature in this volcanic wonderland.", 
            "", 
            listOf(
                "",
                "",
                ""
            ), 
            null, 
            4.8f, 
            "Rift Valley", 
            -2.6, 
            37.3, 
            "June-October", 
            listOf("Elephant Watching", "Photography", "Bird Watching", "Cultural Visits")
        ),
        Destination(
            3, 
            "Tsavo East National Park", 
            "Wildlife Safari", 
            "Known as the 'Theatre of the Wild', Tsavo East is one of the oldest and largest parks in Kenya, covering 13,747 square kilometers of massive, semi-arid wilderness. The park’s singular landscape is dominated by the Yatta Plateau, the world’s longest lava flow, stretching over 290 kilometers. Tsavo East is legendary for its 'Red Elephants'—so named because they dust themselves with the park’s vibrant volcanic soil. Beyond elephants, it is a haven for the 'Man-eaters of Tsavo' lions, herds of buffalo, and diverse birdlife. The Galana River bisects the park, creating the dramatic Lugard Falls where the water gushes through ancient rock formations. It is a place of raw, untamed beauty where the horizon feels infinite and the spirit of old Africa remains untouched. For the curious traveler, it offers a deep dive into geological history and the resilience of life in a challenging, yet majestic environment.", 
            "", 
            listOf(
                "",
                "",
                ""
            ), 
            null, 
            4.7f, 
            "Coast", 
            -3.2, 
            38.5, 
            "June-October", 
            listOf("Game Drives", "Camping", "Bird Watching", "Hiking")
        ),
        Destination(
            4, 
            "Tsavo West National Park", 
            "Wildlife Safari", 
            "Tsavo West is a land of lava, springs, man-eaters and magical sunsets. From the sight of fifty million gallons of crystal clear water gushing out of from the under parched lava rock that is the Mzima Springs to the Shetani lava flows, Tsavo West is a beautiful, rugged wilderness. The savannah ecosystem comprises of open grasslands, scrublands, and Acacia woodlands, belts of riverine vegetation and rocky ridges. It offers some of the most magnificent game viewing in the world and attractions include elephant, rhino, Hippos, lions, cheetah, leopards, Buffalos, diverse plant and bird species including the threatened corncrake and near threatened Basra Reed Warbler.", 
            "", 
            emptyList(), 
            null, 
            4.7f, 
            "Coast", 
            -3.0, 
            38.0, 
            "June-October", 
            listOf("Mzima Springs", "Cave Exploration", "Game Drives", "Rock Climbing")
        ),
        Destination(
            5, 
            "Lake Nakuru National Park", 
            "Wildlife Safari", 
            "Lake Nakuru is a world-class birding destination and a sanctuary for some of Africa's most endangered species. Situated on the floor of the Great Rift Valley, the park is famous for the millions of pink flamingos that seasonally gather on its alkaline shores, creating a shimmering carpet of color visible from the surrounding hills. However, its significance goes beyond birds; it is a critical Rhino Sanctuary, home to both black and white rhinoceros, often seen grazing peacefully along the lake's edge. The park's diverse terrain includes bush-fringed grasslands, steep rocky cliffs, and a unique Euphorbia forest—the largest of its kind in Africa. Leopards are frequently spotted here, along with Rothschild's giraffes and massive pythons. It is a vibrant, concentrated ecosystem that perfectly illustrates the ecological richness of the Rift Valley.", 
            "", 
            emptyList(), 
            null, 
            4.8f, 
            "Rift Valley", 
            -0.3, 
            36.1, 
            "Year-round", 
            listOf("Bird Watching", "Safari", "Rhino Sanctuary", "Picnicking")
        ),
        Destination(
            6, 
            "Samburu National Reserve", 
            "Wildlife Safari", 
            "Located on the banks of the Ewaso Ng'iro river in Kenya, Samburu National Reserve is a rugged and semi-desert park that offers a unique wildlife experience. It is famous for the 'Samburu Special Five': the Grevy's zebra, Somali ostrich, reticulated giraffe, gerenuk, and the beisa oryx, species that are rare elsewhere. The reserve is rich in predator activity, including lions, cheetahs, and leopards. The local Samburu people, cousins to the Maasai, add a rich cultural dimension to a visit here, with their colorful traditional attire and deep knowledge of the land.", 
            "", 
            emptyList(), 
            null, 
            4.7f, 
            "Eastern", 
            0.5, 
            37.5, 
            "June-October", 
            listOf("Game Drives", "Cultural Visits", "Camel Safaris", "Walking Safaris")
        ),
        Destination(
            7, 
            "Nairobi National Park", 
            "Wildlife Safari", 
            "A short drive out of Nairobi's central business district is the Nairobi National Park. Wide open grass plains and the backdrop of the city scrapers, scattered acacia bush play host to a wide variety of wildlife including the endangered black rhino, lions, leopards, cheetahs, hyenas, buffaloes, giraffes and diverse birdlife with over 400 species recorded. It is the only national park in the world within a capital city limits. Visitors can enjoy the park's picnic sites, three campsites and the walking trails for hikers.", 
            "", 
            emptyList(), 
            null, 
            4.6f, 
            "Nairobi", 
            -1.4, 
            36.9, 
            "Year-round", 
            listOf("Game Drives", "Picnicking", "Ivory Burning Site", "Safari Walk")
        ),
        Destination(
            8, 
            "Hell's Gate National Park", 
            "Adventure", 
            "Named for the intense geothermal activity within its boundaries, the Hell's Gate National Park is a remarkable quarter of the Great Rift Valley. Spectacular scenery including the towering cliffs, water-gouged gorges, stark rock towers, scrub clad volcanoes and belching plumes of geothermal steam make it one of the most atmospheric parks in Africa. It is one of the few parks where you can walk or cycle through the wild. It was also the inspiration for the scenery in Disney's 'The Lion King'.", 
            "", 
            emptyList(), 
            null, 
            4.5f, 
            "Rift Valley", 
            -0.9, 
            36.3, 
            "Year-round", 
            listOf("Cycling", "Hiking", "Climbing", "Geothermal Spa")
        ),
        Destination(
            9, 
            "Aberdare National Park", 
            "Wildlife Safari", 
            "The Aberdare National Park covers the higher areas of the Aberdare Mountain Range of Central Kenya and the Aberdare Salient to their east. The park provides a habitat for elephants, black rhinos, leopards, spotted hyenas, olive baboons, black and white colobus monkeys, buffalos, warthogs and bushbucks among others. Rare sightings include those of the Giant Forest Hog, bongo, golden cat, serval cat, African wild cat, African civet cat and the blue duiker. The park is also famous for its picturesque waterfalls and lush bamboo forests.", 
            "", 
            emptyList(), 
            null, 
            4.6f, 
            "Central", 
            -0.4, 
            36.7, 
            "Year-round", 
            listOf("Trout Fishing", "Hiking", "Waterfall Chasing", "Bird Watching")
        ),
        Destination(
            10, 
            "Meru National Park", 
            "Wildlife Safari", 
            "Wild and beautiful, Meru National Park straddles the equator and is bisected by 13 rivers and numerous mountain-fed streams. It has diverse scenery from woodlands at 3,400ft on the slopes of Nyambeni Mountain Range, north east of Mt. Kenya, to wide open plains with meandering riverbanks dotted with doum palms. This is the setting for Joy Adamson's book 'Born Free', the story of Elsa the lioness. The park is home to a large population of elephants, hippos, lions, leopards, cheetahs, black rhinos and some rare antelopes.", 
            "", 
            emptyList(), 
            null, 
            4.5f, 
            "Eastern", 
            0.1, 
            38.2, 
            "June-October", 
            listOf("Game Drives", "Wilderness Camping", "Fishing", "Elsa's Grave")
        ),
        Destination(
            11, 
            "Diani Beach", 
            "Beach", 
            "Voted multiple times as Africa's leading beach destination, Diani Beach is a stunning 25-kilometer stretch of white sand kissed by turquoise Indian Ocean waters. The beach is protected by a coral reef, making the shallow waters calm and perfect for swimming, snorkeling, and diving. Beyond the sand, Diani offers a lush coastal forest where Colobus monkeys swing through the trees. It’s a hub for adventure seekers offering kitesurfing, skydiving, and deep-sea fishing, yet it remains a sanctuary for relaxation. The local Swahili culture adds a rich flavor to the experience, from coastal cuisine to traditional craftsmanship. Diani represents the ultimate tropical escape where the sun meets the sea in perfect harmony.", 
            "", 
            listOf(
                "",
                "",
                ""
            ), 
            null, 
            4.8f, 
            "Coast", 
            -4.3, 
            39.6, 
            "Year-round", 
            listOf("Water Sports", "Relaxation", "Snorkeling", "Skydiving")
        ),
        Destination(
            12, 
            "Watamu Beach", 
            "Beach", 
            "Watamu is a small, relaxed coastal village known for its white sands and turquoise waters. It is part of the Watamu Marine National Park, a protected area that is home to an incredible variety of marine life, including green sea turtles. The area is famous for its unique coral formations and islands that are accessible during low tide. Watamu is also close to the Arabuko Sokoke Forest and the Gedi Ruins, offering a mix of beach, nature, and history.", 
            "", 
            emptyList(), 
            null, 
            4.7f, 
            "Coast", 
            -3.4, 
            40.0, 
            "Year-round", 
            listOf("Snorkeling", "Diving", "Deep Sea Fishing", "Dolphin Watching")
        ),
        Destination(
            13, 
            "Malindi Beach", 
            "Beach", 
            "Malindi is a town on Malindi Bay, in southeastern Kenya. It sits amidst a string of tropical beaches dotted with hotels and resorts. Malindi Marine National Park and nearby Watamu Marine National Park are home to turtles and colorful fish. To the south, the Gedi Ruins are the remains of a 13th-century Swahili town. The town itself has a rich history with Portuguese influences, seen in the Vasco da Gama Pillar. It's known for its vibrant Italian influence, delicious seafood, and lively atmosphere.", 
            "", 
            emptyList(), 
            null, 
            4.6f, 
            "Coast", 
            -3.2, 
            40.1, 
            "Year-round", 
            listOf("History", "Beach", "Marine Park", "Seafood")
        ),
        Destination(
            14, 
            "Lamu Island", 
            "Culture", 
            "Lamu Old Town is the oldest and best-preserved Swahili settlement in East Africa, retaining its traditional functions. Built in coral stone and mangrove timber, the town is characterized by the simplicity of structural forms enriched by such features as inner courtyards, verandas, and elaborately carved wooden doors. There are no cars on Lamu Island; transport is by donkey or dhow boat. It is a UNESCO World Heritage site and offers a step back in time with its narrow winding streets, ancient mosques, and vibrant culture.", 
            "", 
            listOf(
                "",
                "",
                ""
            ), 
            null, 
            4.9f, 
            "Coast", 
            -2.3, 
            40.9, 
            "Year-round", 
            listOf("History", "Culture", "Dhow Sailing", "Donkey Rides")
        ),
        Destination(
            15, 
            "Mount Kenya", 
            "Mountain", 
            "Mount Kenya, the second highest peak in Africa after Kilimanjaro, is an extinct volcano with three main peaks: Batian (5,199m), Nelion (5,188m), and Lenana (4,985m). The mountain's slopes are covered in thick forest and bamboo, giving way to moorland and then rock and ice. It is a UNESCO World Heritage site and a biosphere reserve. The climb offers diverse scenery and wildlife, including elephants, buffaloes, and leopards in the lower forests. It is considered more technically challenging and scenically diverse than Kilimanjaro.", 
            "", 
            listOf(
                "",
                "",
                ""
            ), 
            null, 
            4.9f, 
            "Central", 
            -0.2, 
            37.3, 
            "Jan-Feb, Aug-Sep", 
            listOf("Mountain Climbing", "Hiking", "Photography", "Camping")
        ),
        Destination(
            16, 
            "Kenyatta International Convention Centre (KICC)", 
            "Urban", 
            "The Kenyatta International Convention Centre (KICC) is more than just a 28-story building; it is a timeless icon of Kenya's independence and architectural prowess. Situated in the heart of Nairobi's central business district, its unique terracotta facade and lotus-inspired design make it one of the most photographed landmarks in Africa. The KICC is a premier destination for international conferences and exhibitions, hosting the United Nations and other global bodies. Visitors can take an elevator to the rooftop viewing deck for a breathtaking 360-degree panoramic view of the 'Green City in the Sun'. The helipad offers a perspective that stretches from the Nairobi National Park to the distant Ngong Hills. It is a symbol of Kenya's transition from a young nation to a regional powerhouse of innovation and diplomacy.", 
            "", 
            emptyList(), 
            null, 
            4.6f, 
            "Nairobi", 
            -1.2, 
            36.8, 
            "Year-round", 
            listOf("Views", "Architecture", "Conferences")
        ),
        Destination(
            17, 
            "David Sheldrick Wildlife Trust", 
            "Wildlife", 
            "The David Sheldrick Wildlife Trust is a world-renowned haven for orphaned baby elephants and rhinos, located on the edge of Nairobi National Park. Since its inception in 1977, it has become a global leader in the rescue, rehabilitation, and eventual release of these magnificent creatures back into the wild (primarily in Tsavo). Visitors can witness the daily 11:00 AM public feeding and mud-bath, where the keepers share stories of each orphan’s resilient journey. It is a deeply moving experience that highlights the human-animal bond and the critical fight against ivory poaching. The trust is dedicated to the legacy of David Sheldrick, an early warden of Tsavo East, and continues to be a bastion of professional conservation and soul-stirring animal welfare.", 
            "", 
            emptyList(), 
            null, 
            4.9f, 
            "Nairobi", 
            -1.3, 
            36.7, 
            "Year-round (11 AM Daily)", 
            listOf("Elephant Interaction", "Conservation", "Photography")
        ),
        Destination(
            18, 
            "The Giraffe Centre", 
            "Wildlife", 
            "The Giraffe Centre, operated by the African Fund for Endangered Wildlife (A.F.E.W.), is a charming and educational sanctuary in the Karen suburb of Nairobi. It was established to protect the rare Rothschild's giraffe, which was once nearly extinct in the wild. Today, visitors can experience a truly intimate encounter with these gentle giants from a raised wooden platform, even having the chance to feed them specialized pellets. The centre also features a serene 1.5-kilometer nature trail through indigenous forest, perfect for birdwatching and spotting smaller mammals like sunis. It is an inspiring success story of community-driven conservation that has directly led to the reintroduction of hundreds of giraffes into Kenya's national parks, proving that focused efforts can save a species from the brink.", 
            "", 
            emptyList(), 
            null, 
            4.7f, 
            "Nairobi", 
            -1.3, 
            36.7, 
            "Year-round", 
            listOf("Giraffe Feeding", "Nature Walk", "Education")
        ),
        Destination(
            19, 
            "Fort Jesus Museum", 
            "History", 
            "Fort Jesus in Mombasa is a magnificent 16th-century fortress and a UNESCO World Heritage site that stands as a testament to the high-stakes struggle for control of the Indian Ocean. Built by the Portuguese in 1593 to protect the port of Mombasa, the fort is a masterpiece of military architecture, designed to withstand the tides of time and war. Its massive coral walls have witnessed countless sieges by the Omani Arabs and the British. Inside, the museum houses a vast collection of ceramics, traditional dhow artifacts, and maritime relics retrieved from shipwrecks. Walking through the fort's ancient passages and lookouts provides a chilling and visceral connection to the era of global exploration and the clash of empires on the Swahili coast. It is a brooding, beautiful bastion of East African history.", 
            "", 
            emptyList(), 
            null, 
            4.8f, 
            "Coast", 
            -4.1, 
            39.7, 
            "Year-round", 
            listOf("History", "Architecture", "Museum", "Ocean Views")
        ),
        Destination(
            20, 
            "Shimoni Slave Caves", 
            "History", 
            "The Shimoni Slave Caves are a haunting and significant historical site located near the village of Shimoni on the south coast. Historically, these natural coral caves were used as a holding pen for enslaved people captured in the interior before they were shipped to the major slave markets of Zanzibar. The caves’ dark, damp chambers, still containing iron shackles embedded in the walls, provide a somber and powerful insight into one of the darkest chapters of human history. Today, the local community manages the site as a museum and a place of remembrance. Visiting the caves is a humbling experience that honors the resilience of the human spirit. The proximity to the Kisite-Mpunguti Marine Park offers a stark contrast between the heavy weight of history and the vibrant life of the surrounding sea.", 
            "", 
            emptyList(), 
            null, 
            4.5f, 
            "Coast", 
            -4.7, 
            39.4, 
            "Year-round", 
            listOf("History", "Culture", "Remembrance")
        ),
        Destination(21, "Aberdare Ranges", "Mountain", "The Aberdare Ranges, also known as the Nyandarua Mountains, are a majestic and rugged mountain range in central Kenya, volcanic in origin and reaching heights of nearly 4,000 meters. Spanning 160 kilometers, the range is characterized by mist-covered moorlands, deep ravines, and dramatic waterfalls like the Karuru Falls, which plunge in three stages over 270 meters. The Aberdares hold a significant place in history as the hideout for Dedan Kimathi and the Mau Mau freedom fighters during the struggle for independence. The ecosystem is uniquely diverse, hosting the endangered mountain bongo antelope, giant forest hogs, and black rhinos. It's famously where Princess Elizabeth became Queen Elizabeth II while staying at the Treetops Hotel in 1952. The park's cool, often cloudy climate creates a mystical atmosphere, perfect for adventurous hikers, trout fishers, and those seeking the silent beauty of one of Kenya's primary water towers.", "", emptyList(), null, 4.5f, "Central", -0.5, 36.6, "Year-round", listOf("Hiking", "Views")),
        Destination(22, "Arabuko Sokoke Forest", "Nature", "Arabuko Sokoke Forest is the largest and most significant remnant of the tropical coastal forest that once covered much of East Africa. Located near Watamu, this 420-square-kilometer reserve is a global biodiversity hotspot and a sanctuary for several endemic species found nowhere else on Earth, including the Sokoke Scops Owl and the Golden-rumped Elephant Shrew. The forest comprises three distinct habitat types: mixed forest, Brachystegia woodland, and Cynometra forest, each supporting a unique array of life. It's a birder's paradise with over 230 recorded species. The forest also protects important cultural sites and provides a critical buffer for the coastal ecosystem. Walking trails through the ancient trees allow visitors to experience the vibrant sounds and sights of a prehistoric landscape, making it a must-visit for nature enthusiasts and conservators.", "", emptyList(), null, 4.4f, "Coast", -3.3, 39.9, "Year-round", listOf("Birding", "Walking")),
        Destination(23, "Bamburi Beach", "Beach", "Bamburi Beach is a popular and vibrant coastal destination located on the north coast of Mombasa. Famous for its soft white sands and calm, turquoise waters protected by a vibrant offshore coral reef, it is a hub for water sports and relaxation. The beach is lined with world-class resorts and palm trees, offering a perfect tropical atmosphere. Nearby, visitors can explore Haller Park, a reclaimed quarry that has been transformed into a thriving nature sanctuary. Whether you're interested in jet-skiing, glass-bottom boat tours, or simply enjoying the ocean breeze at a beachfront restaurant, Bamburi offers a lively and accessible coastal experience for families and solo travelers alike.", "", emptyList(), null, 4.5f, "Coast", -3.9, 39.7, "Year-round", listOf("Beach", "Fun")),
        Destination(24, "Bomas of Kenya", "Culture", "The Bomas of Kenya, situated on the outskirts of Nairobi, is a fascinating living museum dedicated to preserving and showcasing the diverse cultures of Kenya's more than 42 ethnic groups. The word 'Boma' is Swahili for homestead, and the site features meticulously reconstructed traditional villages, each representing a different community's unique architecture and lifestyle. The highlight for many is the magnificent amphitheater, where professional troupes perform high-energy traditional dances and songs from across the country. It is an educational and entertaining journey through Kenya's rich heritage, providing deep insights into the social structures, daily lives, and artistic expressions of the various tribes that make up this beautiful nation.", "", emptyList(), null, 4.6f, "Nairobi", -1.3, 36.7, "Year-round", listOf("Culture", "Dance")),
        Destination(25, "Carnivore Restaurant", "Dining", "The Carnivore Restaurant in Nairobi is more than just a place to eat; it's a world-renowned 'Beast of a Feast' experience and a landmark of Kenyan hospitality. Since opening in 1980, it has become famous for its traditional Masail-style charcoal-grilled meats, including ostrich, crocodile, and camel alongside traditional beef, pork, and lamb. The meat is carved right at your table from huge Maasai swords, and the feast only stops when you 'surrender' by lowering the white flag on your table. Set in beautiful tropical gardens, the restaurant combines an energetic atmosphere with top-tier service, making it a must-do for food lovers visiting the capital.", "", emptyList(), null, 4.7f, "Nairobi", -1.3, 36.8, "Year-round", listOf("Dining", "Experience")),
        Destination(26, "Central Island National Park", "Nature", "Central Island National Park, often called the 'Gem of Lake Turkana', is a starkly beautiful volcanic landscape rising from the turquoise waters of the world's largest permanent desert lake. The island is composed of three active volcanoes that have created three unique crater lakes: Crocodile Lake (home to the world's largest population of Nile crocodiles), Tilapia Lake, and Flamingo Lake. It is a vital breeding ground for crocodiles and a sanctuary for numerous water birds. The island's black volcanic beaches and steaming vents create an otherworldly atmosphere, offering a true 'off-the-beaten-path' adventure for those visiting the cradle of mankind in Northern Kenya.", "", emptyList(), null, 4.5f, "Turkana", 3.5, 36.0, "Year-round", listOf("Adventure", "Views")),
        Destination(27, "Chale Island", "Beach", "Chale Island is Kenya's only private island resort, a stunning fusion of white sand, turquoise waters, and lush mangrove forests located just 600 meters off the mainland south of Diani. The island is split between a luxury resort and a sacred 'Kaya' forest, where local communities have performed rituals for centuries. It's a haven for marine life, with sea turtles regularly nesting on its secluded beaches. Whether you're exploring the mangroves by kayak, diving in the pristine coral reefs, or enjoying a candlelit dinner on the sand, Chale offers an unparalleled sense of exclusivity and natural beauty.", "", emptyList(), null, 4.8f, "Coast", -4.4, 39.5, "Year-round", listOf("Luxury", "Beach")),
        Destination(28, "Cherangani Hills", "Hiking", "The Cherangani Hills are a spectacular range of ancient mountains in Western Kenya, rising to 3,500 meters and forming part of the Great Rift Valley's western wall. Unlike the jagged peaks of Mount Kenya, the Cheranganis are characterized by rolling green ridges, deep valleys, and high-altitude forest. They are home to the Sengwer people, one of Kenya's last remaining forest-dwelling communities. The hills offer some of the most scenic and undisturbed trekking in the country, with trails passing through giant lobelias and afro-alpine moorlands. It is a critical watershed and a peaceful sanctuary for those wanting to experience the raw beauty of Kenya's highland interior away from the usual tourist trails.", "", emptyList(), null, 4.5f, "Rift Valley", 1.0, 35.5, "Year-round", listOf("Hiking", "Views")),
        Destination(29, "Chyulu Hills", "Nature", "The Chyulu Hills are a stunningly beautiful range of green volcanic cinder cones that Ernest Hemingway once described as the 'Green Hills of Africa'. Formed only 500 years ago, they are among the world's youngest volcanic ranges. The landscape is a magical mix of rolling hills, deep cedar forests, and the Leviathan Tube—one of the longest lava tubes on Earth. From the summits, you can see breathtaking views of Mount Kilimanjaro to the south and Tsavo to the east. The hills serve as a vital corridor for elephants and are home to a wide variety of wildlife, including leopards and forest hogs, making it a prime destination for wilderness hiking and photography.", "", emptyList(), null, 4.6f, "Eastern", -2.6, 37.7, "Year-round", listOf("Scenery", "Nature")),
        Destination(30, "Chyulu Hills National Park", "Nature", "Chyulu Hills National Park protects a spectacular volcanic mountain range that rises from the plains between Tsavo and Amboseli. The park's main attraction is its unique landscape of verdant, rolling hills and the vast underground networks of lava caves. It is a critical water catchment area for the surrounding plains, fueling the famous Mzima Springs in Tsavo. Visitors can enjoy guided cave explorations, horseback safaris, and some of the best wilderness hiking in East Africa. The park is a sanctuary for elands, giraffes, buffaloes, and a diverse array of birdlife, offering a cool, high-altitude escape from the heat of the savannah below.", "", emptyList(), null, 4.6f, "Eastern", -2.5, 37.8, "Year-round", listOf("Wildlife", "Caves")),
        Destination(31, "Eldoret City", "Urban", "Eldoret, often called the 'Home of Champions', is a booming urban center in the northern Rift Valley and the fifth-largest city in Kenya. Situated at a high altitude of 2,100 meters, it has become a global hub for world-class long-distance runners who train in the nearby hills of Iten. The city is a major center for agriculture, education, and industry, serving as a gateway to Western Kenya and Uganda. Beyond athletics, Eldoret offers vibrant markets, a high-altitude climate that is refreshing year-round, and close proximity to scenic attractions like the Kerio Valley. Its friendly atmosphere and status as a hub of Kenyan sporting excellence make it a unique and energetic city to explore.", "", emptyList(), null, 4.4f, "Rift Valley", 0.5, 35.3, "Year-round", listOf("City", "Culture")),
        Destination(32, "Fourteen Falls", "Nature", "Fourteen Falls is a spectacular geological phenomenon located near Thika, where the Athi River plunges 27 meters over a wide rock face, creating fourteen distinct waterfalls side-by-side. The sight is particularly dramatic during the rainy season when the river is full and the roar of the water is deafening. Visitors can take guided boat trips to the base of the falls or hike across the rocks to experience the spray and power of the river up close. The site is a popular spot for picnics and photography, offering a refreshing natural escape just a short drive from Nairobi.", "", emptyList(), null, 4.3f, "Central", -1.1, 37.2, "Year-round", listOf("Views", "Nature")),
        Destination(33, "Funzi Island", "Beach", "Funzi Island is a hidden gem located south of Diani, a secluded paradise of mangrove forests, pristine sandbars, and traditional Swahili villages. The island is famous for its turtle nesting sites and the magical Funzi sandbar, which emerges from the turquoise sea at low tide like a white desert island. Visitors can enjoy dhow trips through the winding mangrove channels, birdwatching for exotic coastal species, and dolphin spotting in the Ramisi River estuary. Funzi offers a peaceful, untouched coastal experience where time seems to slow down, perfect for those wanting to escape the more crowded beach resorts.", "", emptyList(), null, 4.6f, "Coast", -4.5, 39.4, "Year-round", listOf("Boat Trips", "Beach")),
        Destination(34, "Galu Beach", "Beach", "Galu Beach is the southern extension of Diani Beach, offering an even more exclusive and tranquil atmosphere than its famous neighbor. It features the same flawless white sand and crystal-clear Indian Ocean waters but with fewer resorts and more private villas. The beach is exceptionally wide at low tide, making it perfect for long walks or beach sports. Galu is also a popular spot for kite surfing and skydiving, thanks to its reliable winds and spectacular aerial views of the coastline. It represents the pinnacle of premium coastal living in Kenya, where luxury meets the raw beauty of the tropical sea.", "", emptyList(), null, 4.7f, "Coast", -4.4, 39.6, "Year-round", listOf("Relaxation", "Sun")),
        Destination(35, "Gedi Ruins", "History", "The Gedi Ruins are the mysterious remains of a medieval Swahili walled city located near Watamu. Founded in the 13th century and abandoned in the 17th, Gedi was once a thriving city with a palace, several mosques, and sophisticated stone houses featuring advanced plumbing. Curiously, Gedi is not mentioned in any historical records, and its abandonment remains a puzzle for archaeologists. Today, the ruins are overgrown with ancient baobab trees and populated by playful Sykes' monkeys, creating a magical, 'lost city' atmosphere. It is a UNESCO World Heritage site and a powerful window into the advanced Swahili civilization that once dominated the East African coast.", "", emptyList(), null, 4.7f, "Coast", -3.3, 40.0, "Year-round", listOf("History", "Exploration")),
        Destination(36, "Haller Park", "Nature", "Haller Park is a magnificent example of environmental restoration, a former cement quarry that was transformed into a thriving nature park by Dr. Rene Haller. Located in Mombasa, the park is now a lush sanctuary for giraffes, hippos, buffaloes, and a variety of birdlife. Its success is a world-renowned model of ecological sustainability. Visitors can walk through the forest trails, watch the feeding of the giraffes, and see the famous relationship between Mzee the tortoise and Owen the hippo that once captured the world's heart. It is an educational and inspiring destination that shows how humans can heal and restore the natural world.", "", emptyList(), null, 4.6f, "Coast", -4.0, 39.7, "Year-round", listOf("Wildlife", "Walking")),
        Destination(37, "Hyrax Hill", "History", "Hyrax Hill, located overlooking Lake Nakuru, is one of Kenya's most important prehistoric sites. First excavated by Mary Leakey in 1937, the hill contains remains of settlements dating from the Neolithic to the Iron Age, spanning over 3,000 years of human history. The site features a series of stone-walled enclosures, burial pits, and an onsite museum showcasing stone tools, pottery, and and artifacts found during excavations. Named after the numerous hyraxes that live in its rocky outcrops, the hill also offers spectacular views of the Nakuru basin. It is an essential visit for anyone interested in the deep roots of humanity in East Africa.", "", emptyList(), null, 4.4f, "Rift Valley", -0.3, 36.1, "Year-round", listOf("History", "Museum")),
        Destination(38, "Jumba la Mtwana", "History", "Jumba la Mtwana, which translates to 'The Mansion of the Slave', is a picturesque set of 14th-century Swahili ruins located right on the beach north of Mombasa. The site includes the remains of four mosques, a tomb, and several stone houses, all built from coral rag and limestone. Its coastal location suggests it was once a significant center for maritime trade. The ruins are shaded by ancient trees and look out over the turquoise ocean, providing a serene and historic atmosphere. Visitors can wander through the ancient rooms and mosques, imagining the vibrant Swahili life that flourished here centuries ago while enjoying the sea breeze.", "", emptyList(), null, 4.5f, "Coast", -3.9, 39.7, "Year-round", listOf("History", "Culture")),
        Destination(39, "Kakamega Forest", "Nature", "Kakamega Forest is Kenya's only remaining fragment of the great Guineo-Congolian rainforest that once stretched across the entire continent. Located in Western Kenya, it is a lush, dark-green world of massive ancient trees, hanging vines, and incredible biodiversity. The forest is home to more than 330 bird species, 400 butterfly species, and seven types of primates, including the rare De Brazza's monkey. Walking through its trails at dawn, listening to the cacophony of birds and monkeys, is a truly immersive nature experience. It is a critical sanctuary for West African forest species found nowhere else in Kenya, offering a unique 'jungle' adventure in the heart of the savannah country.", "", emptyList(), null, 4.6f, "Western", 0.3, 34.8, "Year-round", listOf("Birding", "Hiking")),
        Destination(40, "Karen Blixen Museum", "History", "The Karen Blixen Museum was the home of the famous Danish author Karen Blixen (pseudonym Isak Dinesen), who wrote the world-renowned memoir 'Out of Africa'. The house, built in 1912, is a beautiful example of early 20th-century colonial architecture and sits at the foot of the Ngong Hills. It was the heart of Blixen's coffee farm and the setting for much of her life in Kenya between 1917 and 1931. Today, the museum preserves the original furniture, books, and photographs from her life, offering a powerful sense of nostalgia and a window into the colonial era. The beautifully manicured gardens and the view of the hills make it one of Nairobi's most peaceful and evocative historical sites.", "", emptyList(), null, 4.7f, "Nairobi", -1.3, 36.7, "Year-round", listOf("History", "Literature")),
        Destination(41, "Kariandusi Museum", "History", "Kariandusi Museum, located near Lake Elmenteita, is one of the most important Lower Paleolithic sites in East Africa. Discovered in 1928 by the legendary archaeologist Louis Leakey, the site offers a rare glimpse into the lives of early Stone Age humans, specifically those using the Acheulean tool culture. The museum features two main excavation sites where visitors can see a remarkable concentration of hand axes and cleavers made from local obsidian, perfectly preserved where they were left hundreds of thousands of years ago. The onsite museum also showcases geological history of the Rift Valley and fossil remains of extinct animals like the giant elephant. It is a vital destination for understanding the evolutionary journey of humankind and the sophisticated craftsmanship of our ancestors.", "", emptyList(), null, 4.4f, "Rift Valley", -0.4, 36.2, "Year-round", listOf("History", "Archaeology")),
        Destination(42, "Karura Forest", "Nature", "Karura Forest is a magnificent urban forest located in the heart of Nairobi, representing a triumphant symbol of environmental conservation led by Nobel Peace Prize laureate Wangari Maathai. Spanning over 1,000 hectares, the forest is a lush sanctuary that offers a refreshing escape from the city's hustle and bustle. It features over 50 kilometers of well-maintained trails for walking, running, and cycling, winding through indigenous trees, scenic waterfalls, and ancient caves once used as hideouts for freedom fighters. The forest is home to a variety of wildlife, including suni antelopes, Harvey's duikers, and over 200 species of birds. It is a shining example of how nature can thrive alongside urban development, providing critical ecosystem services while serving as a beloved recreational hub for both locals and international visitors.", "", emptyList(), null, 4.8f, "Nairobi", -1.2, 36.8, "Year-round", listOf("Cycling", "Running")),
        Destination(43, "Kaya Forests", "Culture", "The Kaya Forests are a series of more than 50 sacred, ancient forests scattered along the Kenyan coast, serving as the ancestral homes and cultural hearts of the Nine Mijikenda tribes. These forests are UNESCO World Heritage sites, recognized for their unique blend of biodiversity and spiritual heritage. Each Kaya is a fortified settlement that once protected the people from invaders, and they continue to be used as sites for sacred rituals, prayers, and council meetings by tribal elders. The forests themselves host rare plants and animals endemic to the coastal ecosystem. Visiting a Kaya with a local guide provides a profound insight into the traditional beliefs, social structures, and deep environmental respect of the Mijikenda people, making it a powerful cultural and spiritual journey.", "", emptyList(), null, 4.5f, "Coast", -4.2, 39.6, "Year-round", listOf("Culture", "Nature")),
        Destination(44, "Kazuri Beads Factory", "Culture", "Kazuri Beads Factory, located in the beautiful suburb of Karen in Nairobi, is a world-renowned social enterprise that translates to 'small and beautiful' in Swahili. Founded in 1975, the factory provides employment and empowerment to hundreds of disadvantaged women, many of whom are single mothers. Every single Kazuri bead is handmade and hand-painted from local clay, reflecting the vibrant colors and patterns of Kenyan culture. Visitors can take a free guided tour of the factory to witness the meticulous process of shaping, glazing, and firing the ceramic beads. The resulting jewelry and pottery are exported globally, making Kazuri a symbol of Kenyan artistry and successful social entrepreneurship. It's a heartwarming destination where you can shop for beautiful gifts while supporting a noble cause.", "", emptyList(), null, 4.7f, "Nairobi", -1.3, 36.7, "Year-round", listOf("Shopping", "Art")),
        Destination(45, "Kericho Town", "Urban", "Kericho is the undisputed tea capital of Kenya, a vibrant town set in the lush, emerald-green highlands of the Rift Valley. World-famous for its endless, meticulously manicured tea plantations, the area enjoys a cool, misty climate and frequent rainfall that creates some of the finest tea in the world. The town itself is clean and orderly, reflecting its roots as a major center for international tea production. Visitors can take guided tea tours to learn about the journey 'from bush to cup', witness the energetic tea pickers at work, and enjoy freshly brewed local blends. The surrounding landscape, with its rolling hills and pockets of indigenous forest, is breathtakingly beautiful, making Kericho a serene and scenic destination for those wanting to explore Kenya's 'Green Gold'.", "", emptyList(), null, 4.5f, "Rift Valley", -0.3, 35.3, "Year-round", listOf("Tea Tours", "Greenery")),
        Destination(46, "Kikambala Beach", "Beach", "Kikambala Beach is a serene and expansive stretch of white sand located on the north coast of Mombasa. Famous for its tranquility and vast, shallow waters at low tide, it is a perfect getaway for those seeking peace away from the busier holiday hubs. The beach is lined with coconut palms and high-end resorts that offer a sense of secluded luxury. Its proximity to Kilifi and Malindi makes it a great base for exploring the north coast while enjoying the pristine beauty of the Indian Ocean. Whether you're interested in long beach walks, birdwatching in the coastal shrubs, or simply relaxing in a hammock under a palm tree, Kikambala provides the quintessential quiet tropical escape.", "", emptyList(), null, 4.5f, "Coast", -3.8, 39.8, "Year-round", listOf("Beach", "Relax")),
        Destination(47, "Kilifi Beach", "Beach", "Kilifi is a picturesque coastal town built on the shores of the stunning Kilifi Creek and the Indian Ocean. It is known for its relaxed, bohemian atmosphere and beautiful beaches like Bofa Beach, widely considered one of the best on the Kenyan coast due to its wide, uncrowded sands and turquoise waters. The creek itself is a hub for water sports, including sailing and water skiing, and is famous for its bioluminescent plankton that glows at night. Kilifi is also a center for art and culture, hosting vibrant music festivals and eco-conscious communities. With its mix of natural beauty, historic ruins like Mnarani, and a laid-back coastal vibe, Kilifi is a favorite for travelers seeking an authentic and scenic tropical experience.", "", emptyList(), null, 4.6f, "Coast", -3.6, 39.8, "Year-round", listOf("Beach", "Water")),
        Destination(48, "Kinangop Plateau", "Nature", "The Kinangop Plateau is a high-altitude plateau situated between the Aberdare Range and the Great Rift Valley, approximately 2,400 meters above sea level. This unique landscape is characterized by vast, open grasslands, marshes, and rolling moorlands, representing a critical habitat for many endemic bird species, including the endangered Sharpe's Longclaw. The plateau is a vital agricultural hub, known for its dairy farming and fresh produce, contributing significantly to Kenya's food basket. For nature lovers and birdwatchers, the plateau offers a peaceful environment with spectacular views of the volcanic peaks of the surrounding ranges. Its cool, crisp air and pastoral beauty provide a refreshing change of pace from the typical savannah safari experience.", "", emptyList(), null, 4.4f, "Central", -0.6, 36.6, "Year-round", listOf("Views", "Birding")),
        Destination(49, "Kisite-Mpunguti Marine Park", "Nature", "Kisite-Mpunguti Marine National Park is a maritime paradise located on the south coast near Shimoni, encompassing a large area of pristine coral reefs and islands. Often called 'the home of the dolphin', the park is famous for the large pods of friendly Indo-Pacific bottlenose and humpback dolphins that frequently swim alongside visitors' boats. The coral reefs here are among the most diverse in East Africa, offering world-class snorkeling and diving where you can see turtles, rays, and over 250 species of colorful tropical fish. Most visits include a traditional dhow cruise and a seafood lunch on Wasini Island. It is a stunning example of marine conservation and a must-visit for anyone wanting to experience the vibrant underwater world of the Indian Ocean.", "", emptyList(), null, 4.8f, "Coast", -4.7, 39.3, "Year-round", listOf("Dolphins", "Snorkeling")),
        Destination(50, "Kisumu City", "Urban", "Kisumu, the third-largest city in Kenya, is a vibrant port city nestled on the shores of Lake Victoria, the largest tropical lake in the world. Often described as the 'Lakeside City', it is the heart of Western Kenya and a major hub for trade, transport, and culture. The city's unique charm lies in its relaxed pace, stunning lake sunsets, and delicious local delicacies like fried tilapia and ugali. Visitors can explore the Kisumu Impala Sanctuary, enjoy boat rides on the lake, or visit the vibrant Kibuye Market, one of the largest in the region. With its rich Luo heritage, bustling waterfront, and status as a critical gateway to the Great Lakes region, Kisumu offers a distinct and energetic urban experience that is deeply connected to the power of the lake.", "", emptyList(), null, 4.5f, "Western", -0.1, 34.7, "Year-round", listOf("City", "Lake")),
        Destination(51, "Kitengela Glass", "Art", "Kitengela Glass, located on the edge of Nairobi National Park, is a magical and whimsical center of artistry founded by Anselm Croze. This ecological-conscious studio is world-famous for its stunning, artisanal glassware created using 100% recycled glass. The entire site is an outdoor art gallery, featuring mosaic pathways, sculptures, and a thrilling suspension bridge that hangs over a valley. Visitors can watch the master glassblowers at work, transforming glowing molten glass into beautiful vases, lights, and ornaments using ancient techniques. It is a place where creativity, recycling, and nature meet, offering a truly unique and inspiring experience that is part art gallery, part workshop, and part wonderland.", "", emptyList(), null, 4.7f, "Nairobi", -1.4, 36.8, "Year-round", listOf("Art", "Crafts")),
        Destination(52, "Koobi Fora", "History", "Koobi Fora, situated on the eastern shores of Lake Turkana, is often referred to as the 'Cradle of Mankind' and is one of the most important paleontological sites in the world. Since the late 1960s, a team led by the Leakey family has discovered more than 160 separate hominid fossils here, including some of the most famous remains of early human ancestors such as Homo habilis and Homo erectus. The site covers a vast area of sedimentary rock layers that span over 4 million years of evolutionary history. Today, Koobi Fora is a UNESCO World Heritage site and houses an onsite museum showcasing fossil treasures from both humans and ancient animals. For those willing to make the journey into the remote North, it offers a profound and humble connection to the very origins of our species amidst a starkly beautiful desert landscape.", "", emptyList(), null, 4.6f, "Turkana", 3.9, 36.2, "Year-round", listOf("History", "Fossils")),
        Destination(53, "Lake Baringo", "Lake", "Lake Baringo is one of the only two freshwater lakes in the Kenyan Rift Valley, a shimmering oasis in a rugged, semi-desert landscape. It is world-renowned as a birding paradise, with over 470 species recorded including the spectacular Goliath heron and the Verreaux’s eagle. The lake is home to seven islands, the largest being Ol Kokwe, which features hot springs and an extinct volcano. Visitors can take guided boat trips to see hippos and crocodiles, watch the local fish eagles dive for prey, or learn about the culture of the Njemps people who navigate the waters in traditional reeds boats. Its remote feel, dramatic rocky backdrop, and incredible wildlife density make it a favorite for photographers and adventurers seeking the raw beauty of the northern Rift.", "", emptyList(), null, 4.5f, "Rift Valley", 0.6, 36.0, "Year-round", listOf("Birding", "Boating")),
        Destination(54, "Lake Bogoria", "Lake", "Lake Bogoria is a stunning alkaline lake in the northern Rift Valley, famous for its dramatic scenery and unique geothermal activity. Often referred to as 'the lake of fire', it is lined with dozens of powerful hot springs and geysers that hiss and steam along the shoreline, reflecting the intense volcanic power beneath the surface. The lake is also a seasonally important sanctuary for hundreds of thousands of lesser flamingos, which gather in spectacular pink clouds to feed on the blue-green algae. The surrounding landscape is rugged and mountainous, offering a stark contrast to the shimmering waters. It is a UNESCO World Heritage site and a place of raw, elemental beauty that provides a truly unique experience in the heart of the Rift Valley.", "", emptyList(), null, 4.6f, "Rift Valley", 0.2, 36.1, "Year-round", listOf("Nature", "Hot Springs")),
        Destination(55, "Lake Bogoria National Reserve", "Nature", "Lake Bogoria National Reserve protects one of the most spectacular geothermal areas in the world. The reserve encompasses the alkaline Lake Bogoria and the surrounding arid landscape, which is home to the majestic Greater Kudu and other rare wildlife like the caracal. The park's main attraction is the series of hot springs and boiling geysers that erupt along the lake's western shore. These springs are so hot that visitors often boil eggs in them as a unique souvenir of their visit. The reserve is also a critical sanctuary for millions of flamingos and a wide variety of birds of prey. Its intense colors, dramatic mountains, and steaming geysers create a prehistoric atmosphere that is truly unforgettable.", "", emptyList(), null, 4.6f, "Rift Valley", 0.2, 36.1, "Year-round", listOf("Wildlife", "Scenery")),
        Destination(56, "Lake Chala", "Lake", "Lake Chala is a hidden gem on the border of Kenya and Tanzania, a deep crater lake that formed within a volcanic cone over 250,000 years ago. Fed by underground springs from the snows of Mount Kilimanjaro, the lake is famous for its stunning, varyingly deep turquoise and emerald colors. The lake is surrounded by a high crater rim, and the descent to the water's edge through lush vegetation is a rewarding trek. It is a place of absolute peace and tranquility, far from the usual tourist crowds. Visitors can enjoy kayaking, birdwatching, and even spotting elephants that occasionally migrate between the neighboring national parks. It is a true 'hidden sanctuary' that offers a silent and majestic connection to the volcanic history of the Kilimanjaro region.", "", emptyList(), null, 4.5f, "Coast", -3.3, 37.7, "Year-round", listOf("Views", "Nature")),
        Destination(57, "Lake Elementaita", "Lake", "Lake Elementaita is a shallow, alkaline soda lake in the Great Rift Valley, situated between Lake Naivasha and Lake Nakuru. It is a place of breathtaking beauty and a critical breeding ground for over 400 species of birds, most notably the Great White Pelicans and lesser flamingos. The lake's surroundings include the Soy Sambu Conservancy, which protects rhinos and Rothschild's giraffes. Historically, the area is significant as it contains numerous prehistoric sites, including the Kariandusi Museum just a few kilometers away. Its tranquil atmosphere, shimmering waters, and the dramatic backdrop of the Lord Delamere peaks make it a serene and scenic stop for birders and history enthusiasts alike.", "", emptyList(), null, 4.5f, "Rift Valley", -0.4, 36.2, "Year-round", listOf("Birding", "Scenic")),
        Destination(58, "Lake Jipe", "Lake", "Lake Jipe is a remote and peaceful freshwater lake that straddles the border between Kenya and Tanzania. Situated at the western end of Tsavo West National Park, it is a critical water source in an otherwise arid landscape. The lake is surrounded by dense reeds and papyrus beds, providing a sanctuary for numerous hippos, crocodiles, and water birds. One of its most magical features is the view of Mount Kilimanjaro rising majestically to the west. It is a place for those who enjoy the sound of silence and the raw beauty of an off-the-beaten-path destination. Visitors can take boat safaris to see elephants coming down to the water to drink, making it a quiet and evocative safari experience.", "", emptyList(), null, 4.4f, "Coast", -3.6, 37.7, "Year-round", listOf("Fishing", "Nature")),
        Destination(59, "Lake Kamnarok", "Lake", "Lake Kamnarok is a hidden oxbow lake located in the spectacular Kerio Valley, often described as 'the Masai Mara of the North' due to its high density of crocodiles and wildlife. The lake and the surrounding Kamnarok Game Reserve provide a vital habitat for massive populations of crocodiles, often seen basking in their thousands on the muddy banks. The area is also a corridor for elephants migrating from the nearby Rimoi Game Reserve. Its remote location and the dramatic backdrop of the Kerio Valley walls make it a place of untamed beauty. For the intrepid traveler, Kamnarok offers a raw and uncrowded wildlife experience in a landscape that feels truly ancient.", "", emptyList(), null, 4.3f, "Rift Valley", 0.7, 35.6, "Year-round", listOf("Wildlife", "Nature")),
        Destination(60, "Lake Magadi", "Lake", "Lake Magadi is a surreal and hauntingly beautiful soda lake located in the southernmost part of the Kenyan Rift Valley. Known as the 'Pink Lake', it is almost entirely covered by a thick crust of soda (sodium carbonate) which creates a shimmering, multicolored landscape of white, pink, and deep indigo. The lake is a center for soda ash mining but also a critical breeding ground for millions of flamingos, which gather in the shallow saline waters. Its stark, arid environment, characterized by hot springs and salt flats, creates a minimalist and otherworldly beauty that is a favorite for photographers. It is a place of extreme conditions and intense visual power, offering a truly unique perspective on the geological diversity of Kenya.", "", emptyList(), null, 4.6f, "Rift Valley", -1.9, 36.3, "Year-round", listOf("Photography", "Nature")),
        Destination(61, "Lake Ol Bolossat", "Lake", "Lake Ol Bolossat is the only lake in central Kenya and one of the highest altitude lakes in the country, sitting at 2,340 meters above sea level. It is a critical wetland ecosystem that serves as the headwaters for the Ewaso Nyiro River. The lake is a designated Important Bird Area, hosting over 300 bird species, including the rare and endangered Grey Crowned Crane. Its shallow waters are also home to a significant population of hippos, which can be seen and heard splashing in the reeds. Surrounded by fertile farmland and the scenic Satima Escarpment, the lake offers a tranquil and high-altitude experience. It is a vital water tower and a sanctuary for both migratory birds and the local community, representing a delicate and beautiful balance in Kenya's central highlands.", "", emptyList(), null, 4.4f, "Central", -0.1, 36.4, "Year-round", listOf("Birding", "Hipoos")),
        Destination(62, "Lake Paradise", "Nature", "Lake Paradise is a true hidden wonder, a perfectly circular crater lake situated within the dense forest of Marsabit National Park in Northern Kenya. Its name is perfectly chosen, as the lake provides a lush, mist-covered oasis in an otherwise arid and rugged region. Famous for its massive populations of elephants, including the legendary 'Lord of Marsabit', Ahmed, who was protected by a presidential decree, the lake is a critical water source for the wildlife of the north. The surrounding forest is ancient and teeming with life, from colorful butterflies to rare forest birds. Reaching Lake Paradise is an adventure in itself, but the sight of the emerald waters and the giant tusker elephants bathing in the silence of the crater is a moment of pure, prehistoric majesty.", "", emptyList(), null, 4.7f, "Eastern", 2.3, 37.9, "Year-round", listOf("Scenery", "Nature")),
        Destination(63, "Lake Simbi Nyaima", "Lake", "Lake Simbi Nyaima is a volcanic crater lake located near the shores of Lake Victoria in Western Kenya, steeped in local Luo legends and spiritual significance. The lake's waters are highly saline and are famous for the massive flocks of flamingos that occasionally populate its shores. According to local folklore, the lake was formed after a village was swallowed by water because the inhabitants refused to feed a hungry old woman. Beyond the legends, it is an Important Bird Area and a site of geological interest due to its volcanic origins. The surrounding village life and the sight of flamingos against the backdrop of the western hills make it a fascinating and culturally rich destination for those exploring the Lake Victoria basin.", "", emptyList(), null, 4.4f, "Western", -0.4, 34.6, "Year-round", listOf("Nature", "Legends")),
        Destination(64, "Lake Turkana", "Lake", "Lake Turkana, known as the 'Jade Sea' because of its stunning turquoise-green color, is the world's largest permanent desert lake and a UNESCO World Heritage site. Located in the remote and rugged North of Kenya, it is situated within the East African Rift Valley where extreme heat and wind create a starkly beautiful and otherworldly landscape. The lake is famous for its three volcanic islands, its massive population of Nile crocodiles, and its status as the 'Cradle of Mankind' due to the numerous hominid fossil discoveries at nearby Koobi Fora. It is home to the El Molo, one of the smallest and most resilient ethnic groups in Kenya. Turkana represents the raw, ultimate frontier of Kenyan travel, offering a profound sense of isolation and a connection to the deepest roots of human history in a landscape of fire and water.", "", emptyList(), null, 4.7f, "Turkana", 3.0, 36.0, "Year-round", listOf("Adventure", "Culture")),
        Destination(65, "Lake Victoria", "Lake", "Lake Victoria, known as 'Nam Lolwe' in Dholuo, is the largest tropical lake in the world and the chief reservoir of the Nile River. Spanning three countries, the Kenyan portion of the lake is a bustling hub of activity, dominated by the fishing industry and vibrant port cities like Kisumu. The lake is famous for its sunset boat cruises, its incredible variety of fish—most notably the giant Nile Perch and Tilapia—and its numerous islands like Rusinga and Mfangano which host ancient rock art and unique cultural traditions. It is a vital economic and social lifeline for millions of people, characterized by a relaxed lakeside atmosphere and a unique cultural heritage. Exploring Lake Victoria offers a chance to experience the power of one of Earth's great inland seas while immersing yourself in the traditions of the lakeside communities.", "", emptyList(), null, 4.6f, "Western", -0.5, 34.0, "Year-round", listOf("Fishing", "Islands")),
        Destination(66, "Lamu Old Town", "History", "Lamu Old Town is the oldest and best-preserved Swahili settlement in East Africa, a UNESCO World Heritage site that has retained its traditional functions for over 700 years. Built in coral stone and mangrove timber, the town is defined by its narrow, winding limestone streets, its magnificent carved wooden doors, and the complete absence of cars—transport is entirely by donkey or traditional dhow boats. Lamu is a center for Islamic and Swahili culture, hosting vibrant festivals like the Maulidi festival. The atmosphere is one of timeless tranquility and profound history, where the call to prayer echoes over the rooftops and the smell of spices fills the air. It is a place to step back in time, experience the seafaring soul of the coast, and enjoy some of the most authentic and beautiful architecture in the world.", "", emptyList(), null, 4.8f, "Coast", -2.3, 40.9, "Year-round", listOf("History", "Architecture")),
        Destination(67, "Loita Hills", "Hiking", "The Loita Hills are one of Kenya's last remaining wilderness areas, a range of ancient hills and mountains that rise from the plains of the South Rift. They are the ancestral territory of the Maasai people, particularly the Loita Maasai who have preserved their traditional culture and the sanctity of the hills for generations. The landscape is a mystical mix of high-altitude forest, deep valleys, and open grasslands, offering some of the most authentic and undisturbed walking safaris in East Africa. Guided by local Maasai warriors, visitors can learn about the medicinal plants, the ancient ceremonies performed in the forest, and the deep spiritual connection the people have with the land. It is a place of raw beauty and profound cultural immersion, far removed from any modern intrusion.", "", emptyList(), null, 4.6f, "Rift Valley", -1.6, 35.5, "Year-round", listOf("Trekking", "Culture")),
        Destination(68, "Maasai Market", "Shopping", "The Maasai Market is a vibrant, nomadic open-air market that moves to different locations across Nairobi throughout the week, offering a sensory overload of Kenyan culture, crafts, and commerce. It is the premier destination for shopping for authentic Kenyan souvenirs, including hand-beaded Maasai jewelry, intricately carved wooden sculptures, hand-woven baskets (Kiondos), and colorful African fabrics (Kitenges and Kikois). The market is a hub of energy where local artisans come to sell their work directly to both locals and tourists. Bargaining is an essential part of the experience, conducted in a friendly and lively manner. It's a place where you can find unique gifts while directly supporting the livelihood of thousands of local artists, making it a must-visit for anyone wanting to take a piece of Kenya home with them.", "", emptyList(), null, 4.7f, "Nairobi", -1.3, 36.8, "Year-round", listOf("Shopping", "Art")),
        Destination(69, "Maasai Village", "Culture", "Visiting a traditional Maasai Village, or 'Manyatta', provides a profound and humbling insight into one of the world's most iconic and resilient cultures. The Maasai are a semi-nomadic pastoralist people who have maintained their traditional way of life on the plains of Kenya and Tanzania for centuries. The villages are usually circular and protected by a fence of thorn bushes to keep out predators. Inside, the houses are made of mud, sticks, and cow dung, reflecting a sustainable and harmonious relationship with the environment. Visitors are often welcomed with traditional songs and the famous jumping dance (Adumu). Learning about their elaborate social structures, their deep respect for cattle, and their ancient rituals offers a unique perspective on a culture that continues to thrive in balance with nature while facing the challenges of the modern world.", "", emptyList(), null, 4.8f, "Various", -1.5, 35.0, "Year-round", listOf("Culture", "Tradition")),
        Destination(70, "Malindi Town", "Urban", "Malindi is a charming coastal town that perfectly blends tropical beauty with a rich and diverse history. Founded in the 13th century, it has been a significant seafaring center for centuries, famously visited by Vasco da Gama in 1498. The town is known for its beautiful Indian Ocean beaches, its vibrant Italian influence (earning it the nickname 'Little Italy of Kenya'), and its proximity to the spectacular Malindi Marine National Park. Visitors can explore the historic Vasco da Gama Pillar, wander through the bustling old town, and enjoy world-class seafood at beachfront restaurants. Malindi's relaxed atmosphere, historic Swahili roots, and high-energy water sports make it a favorite for those seeking a well-rounded and scenic coastal holiday.", "", emptyList(), null, 4.5f, "Coast", -3.2, 40.1, "Year-round", listOf("Town", "Beach")),
        Destination(71, "Malka Mari National Park", "Nature", "Malka Mari National Park is arguably Kenya's most remote and least-visited wilderness area, located in the extreme northeast on the border with Ethiopia. The park is situated along the Daua River, which provides a lifeline of green gallery forest through a landscape of rugged hills and semi-arid plains. Because of its isolation, Malka Mari is a place of untamed beauty and absolute solitude. It is home to rare desert wildlife, including the Somali giraffe, various gazelles, and a variety of birds of prey. For the truly intrepid traveler, Malka Mari offers the rare opportunity to experience a part of East Africa that has remained virtually untouched by modern tourism, providing a raw and authentic sense of exploration on the outer edges of the map.", "", emptyList(), null, 4.3f, "Eastern", 3.9, 40.7, "Year-round", listOf("Remote", "Nature")),
        Destination(72, "Mamba Village", "Wildlife", "Mamba Village in Mombasa is East Africa's largest crocodile farm and a major center for reptile education and conservation. The village is home to thousands of Nile crocodiles, ranging from hatchlings to massive, 100-year-old giants like 'Big Daddy'. The highlight of a visit is the feeding session, where these powerful predators leap out of the water for their meals. Beyond crocodiles, the village also features a snake park, camel rides, and beautiful botanical gardens. It's an educational destination that provides deep insights into the life cycle and biology of these ancient reptiles, making it a popular and fascinating visit for families and wildlife enthusiasts visiting the coast.", "", emptyList(), null, 4.4f, "Coast", -4.0, 39.7, "Year-round", listOf("Reptiles", "Tour")),
        Destination(73, "Manda Island", "Beach", "Manda Island is a quiet and beautiful island in the Lamu Archipelago, offering a more secluded and tranquil alternative to the busier Lamu Island. Access is strictly by boat from Manda Airport or Lamu town. The island features stunning, uncrowded beaches, the historic ruins of Takwa—a 16th-century Swahili town—and a variety of luxury eco-resorts hidden in the dunes. Its mangrove-lined channels are perfect for dhow boat explorations and birding. Manda represents the pinnacle of 'barefoot luxury' on the Kenyan coast, where you can enjoy the deep silence of the sea, the beauty of the Swahili heritage, and the feeling of having an entire tropical island almost to yourself.", "", emptyList(), null, 4.6f, "Coast", -2.3, 40.9, "Year-round", listOf("Beach", "Ruins")),
        Destination(74, "Marsabit National Park", "Nature", "Marsabit National Park is an extraordinary mountain island of green rising from the desert plains of Northern Kenya. Centered around a series of ancient volcanic mountains and craters (Gofs), the park is covered in a lush, cloud-fed forest that contrasts sharply with the surrounding arid landscapes. It is famous for its massive, 'old-gen' elephants with some of the largest tusks ever recorded in Africa. The park's centerpiece is Lake Paradise, a perfectly circular crater lake that looks like something from a prehistoric world. It is a vital sanctuary for greater kudu, leopards, and a unique variety of forest birds. Marsabit is a place of mystical beauty and raw wilderness, offering a refreshing and verdant escape in the heart of Kenya's northern desert frontier.", "", emptyList(), null, 4.5f, "Eastern", 2.3, 37.9, "Year-round", listOf("Wildlife", "Scenic")),
        Destination(75, "Mau Forest", "Nature", "The Mau Forest Complex is the largest montane forest in East Africa and the most critical 'water tower' in Kenya, serving as the primary catchment for numerous rivers that feed Lake Victoria, Lake Nakuru, and the Masai Mara. Spanning over 400,000 hectares, the forest is a lush and incredibly diverse ecosystem comprising secondary forest, bamboo, and high-altitude moorland. It is home to several forest-dwelling communities like the Ogiek and provides a sanctuary for rare wildlife including the mountain bongo and the yellow-backed duiker. The forest's health is directly linked to the climatic stability and agricultural prosperity of much of Kenya. Exploring the Mau offers a chance to see one of the most vital and beautiful natural bastions in the country, a deep green world of ancient trees and life-giving water.", "", emptyList(), null, 4.5f, "Rift Valley", -0.5, 35.7, "Year-round", listOf("Forest", "Nature")),
        Destination(76, "Menengai Crater", "Nature", "Menengai Crater is one of the largest calderas in the world and the largest volcano in Kenya, situated right on the outskirts of Nakuru city. Formed after a massive volcanic eruption approximately 8,000 years ago, the crater spans 12 kilometers across and is 485 meters deep. The floor of the crater is a wild and rugged landscape of ancient lava flows and thick shrubs, and is said to be haunted according to local legends. The rim of the crater offers spectacular panoramic views of Lake Nakuru, the Rift Valley floor, and the surrounding mountains. It is a favorite destination for hikers and mountain bikers, and is also becoming a hub for geothermal energy production. Climbing to the rim and looking into the vast, silent cauldron of the sleeping giant is a powerful and humbling experience.", "", emptyList(), null, 4.6f, "Rift Valley", -0.2, 36.1, "Year-round", listOf("Hiking", "Views")),
        Destination(77, "Mnarani Ruins", "History", "The Mnarani Ruins, located on a hill overlooking the Kilifi Creek, are the remains of two ancient Swahili mosques and several tombs dating back to the 14th century. The Great Mosque is particularly well-preserved, featuring intricate mihrab decorations and inscriptions. Historically, Mnarani was a significant religious center for the Swahili people of the north coast. The ruins are shaded by massive, ancient baobab trees that are estimated to be hundreds of years old, adding to the site's evocative and peaceful atmosphere. From the ruins, visitors can enjoy stunning views of the creek and the Indian Ocean. It is a site of deep quiet and profound history, reflecting the spiritual and architectural sophisticated of the mediaeval coastal civilization.", "", emptyList(), null, 4.5f, "Coast", -3.6, 39.8, "Year-round", listOf("History", "Culture")),
        Destination(78, "Mombasa Old Town", "Urban", "Mombasa Old Town is a living bridge between the past and the present, a historic district characterized by its narrow winding streets, beautifully carved wooden doors, and ancient Swahili and Portuguese architecture. Situated right next to the historic Fort Jesus, the old town is a vibrant melting pot of cultures, reflecting the centuries of trade and migration through the Indian Ocean. Every corner of the old town tells a story, from the centuries-old mosques to the traditional coffee shops where elders gather to talk. The air is filled with the scent of spices and salt, and the sound of the ocean is never far away. Exploring the old town on foot is the best way to soak in the authentic coastal atmosphere and appreciate the intricate craftsmanship and deep-rooted traditions that have defined Mombasa for generations.", "", emptyList(), null, 4.7f, "Coast", -4.1, 39.7, "Year-round", listOf("Walking", "History")),
        Destination(79, "Mount Elgon", "Mountain", "Mount Elgon is a massive extinct shield volcano that straddles the border between Kenya and Uganda, possessing the largest volcanic base in the world. It is a stunning mountain of diverse habitats, from bamboo forests on the lower slopes to afro-alpine moorlands and unique peak ecosystems at its summit (Wagagai at 4,321m). Historically, the mountain is famous for its massive caves, formed by ancient lava tubes, which the resident elephants famously visit at night to lick salt from the walls. The mountain offers incredible trekking opportunities that are less crowded than Mount Kenya or Kilimanjaro, passing through spectacular landscapes of giant lobelias and groundsel. It is a place of wild, remote beauty and a critical water source for the region.", "", emptyList(), null, 4.6f, "Western", 1.1, 34.5, "Year-round", listOf("Climbing", "Caves")),
        Destination(80, "Mount Elgon National Park", "Nature", "Mount Elgon National Park protects the Kenyan side of the world's largest volcanic base, offering a wilderness experience that is truly off the beaten path. The park's main attraction is the unique habit of the 'underground elephants' who venture deep into the massive Kitum Cave to mine for essential mineral salts. The park encompasses spectacular scenery including cliffs, caves, and waterfalls, and is home to a variety of wildlife such as buffaloes, monkeys, and numerous bird species. The high-altitude moorlands are particularly beautiful when the mountain's unique flora is in bloom. Whether you're exploring the caves, hiking the trails, or just soaking in the silent majesty of the mountain, Mount Elgon offers a raw and unforgettable nature adventure.", "", emptyList(), null, 4.6f, "Western", 1.1, 34.5, "Year-round", listOf("Nature", "Wildlife")),
        Destination(81, "Mount Suswa", "Mountain", "Mount Suswa is a spectacular and unique double-crater volcano located in the heart of the Rift Valley. It features a massive outer caldera and a smaller inner caldera, creating a 'crater-within-a-crater' landscape that is a geological marvel. The mountain is most famous for its vast network of lava tubes, which are home to thousands of bats and are used as sacred sites by the local Maasai community. The inner caldera is a lush island of green forest surrounded by a ring of sheer cliffs, creating a lost-world atmosphere. Visitors can explore the caves, hike the multiple rims for breathtaking views, and experience the warm hospitality of the Maasai guides. It is a place of raw volcanic power and deep cultural significance, offering one of the most unique adventures in Kenya.", "", emptyList(), null, 4.6f, "Rift Valley", -1.1, 36.3, "Year-round", listOf("Hiking", "Caving")),
        Destination(82, "Msambweni Beach", "Beach", "Msambweni Beach is a hidden tropical paradise located south of Diani, known for its pristine beauty and absolute seclusion. It features long, uninterrupted stretches of white sand, turquoise waters, and a backdrop of ancient baobabs and coconut palms. Unlike the busier coastal centers, Msambweni has remained largely untouched by large-scale tourism, making it a favorite for those seeking peace and exclusivity. The beach is a nesting ground for sea turtles, and the offshore reefs offer excellent snorkeling and diving in total tranquility. Msambweni is the perfect destination for a private getaway where the only sounds are the palm leaves rustling in the breeze and the gentle waves of the Indian Ocean.", "", emptyList(), null, 4.7f, "Coast", -4.5, 39.5, "Year-round", listOf("Relaxation", "Beach")),
        Destination(83, "Mtwapa Beach", "Beach", "Mtwapa is a vibrant and energetic coastal area north of Mombasa, built around the scenic Mtwapa Creek and its access to the Indian Ocean. It is famous for its lively nightlife, numerous waterfront restaurants, and its reputation as a hub for deep-sea fishing and sailing. The creek is a bustling center for dhow boat tours and water sports, and the area is a melting pot of cultures, reflected in its diverse culinary scene and bustling markets. Mtwapa offers a high-energy coastal experience that combines the natural beauty of the creek with a lively and cosmopolitan atmosphere, making it a favorite for young travelers and those wanting a more active beach holiday.", "", emptyList(), null, 4.5f, "Coast", -3.9, 39.7, "Year-round", listOf("Boating", "Fun")),
        Destination(84, "Mwea National Reserve", "Nature", "Mwea National Reserve is a hidden gem in central Kenya, a sanctuary of savannah and riverine forest situated at the confluence of the Tana and Thiba Rivers. The reserve's ecosystem is unique, being a mix of acacia-combretum woodland and a significant area of wetland within the massive Masinga Dam. It is home to a healthy population of elephants, giraffes, buffaloes, and over 200 species of birds. The rivers are teeming with hippos and crocodiles. Because it's less visited than other parks, Mwea offers a quiet and authentic wildlife experience in a beautiful, water-dominated landscape. It is a vital sanctuary and a peaceful retreat for nature lovers exploring the central highlands.", "", emptyList(), null, 4.4f, "Central", -0.8, 37.6, "Year-round", listOf("Wildlife", "Nature")),
        Destination(85, "Mzima Springs", "Nature", "Mzima Springs is a stunning natural oasis inside Tsavo West National Park, where fifty million gallons of crystal-clear water gush out from under a parched lava rock every day. The springs are the result of rainfall on the nearby Chyulu Hills, filtered through the volcanic rock for years before emerging here. The water is so transparent that visitors can see hippos and crocodiles swimming underwater from a specially designed glass viewing chamber. Surrounded by a lush tropical forest of date palms and raffia palms, the springs are a vital lifeline for wildlife and provide a cool, verdant escape in the middle of the arid savannah. It is one of Kenya's most beautiful and celebrated natural wonders, a testament to the life-giving power of hidden water.", "", emptyList(), null, 4.7f, "Coast", -3.0, 38.0, "Year-round", listOf("Nature", "Viewing")),
        Destination(86, "Nairobi City", "Urban", "Nairobi, the 'Green City in the Sun', is Kenya's dynamic and cosmopolitan capital, and the only city in the world that boasts a fully functioning national park within its borders. It is a hub for business, tech, and international diplomacy, hosting major UN headquarters. The city's energy is infectious, from the bustling central business district to the leafy suburbs of Karen and Westlands. Nairobi is a center for art, with world-class museums and galleries, and its culinary scene is one of the most diverse on the continent. With its unique mix of urban sprawl and wild nature, vibrant markets, and a resilient, energetic population, Nairobi is the undeniable heart and soul of modern East Africa, constantly evolving while remaining deeply connected to its natural roots.", "", emptyList(), null, 4.6f, "Nairobi", -1.3, 36.8, "Year-round", listOf("City", "Business")),
        Destination(87, "Nairobi National Museum", "Culture", "The Nairobi National Museum is Kenya's flagship museum, a world-class institution dedicated to celebrating and preserving the country's rich heritage in history, nature, culture, and contemporary art. Located on Museum Hill, it features award-winning galleries that showcase the deep roots of humanity in East Africa, with an incredible collection of hominid fossils. Other highlights include the spectacular Hall of Kenya, which presents the cultural treasures of the nation's diverse tribes, and a vast collection of bird and mammal specimens. The museum is more than just a place for artifacts; it is an educational hub that provides a comprehensive and inspiring journey through the story of Kenya and its place in the world. It's an essential visit for anyone wanting to understand the soul of the nation.", "", emptyList(), null, 4.7f, "Nairobi", -1.3, 36.8, "Year-round", listOf("Museum", "Education")),
        Destination(88, "Nairobi Safari Walk", "Wildlife", "The Nairobi Safari Walk is a unique and educational wildlife viewing experience located just outside the city center. It features a raised wooden boardwalk that winds through three major Kenyan ecosystems: the wetlands, the savannah, and the forest. Along the way, visitors can see a variety of rescued and rehabilitated animals, including lions, leopards, rhinos, and rare antelopes, in a setting that mimics their natural habitats. The walk is designed to promote wildlife conservation and environmental education, providing detailed information about the species and their roles in the ecosystem. It is a peaceful and family-friendly way to see Kenya's iconic wildlife without having to go on a full-scale safari, right on the doorstep of the city.", "", emptyList(), null, 4.6f, "Nairobi", -1.4, 36.7, "Year-round", listOf("Walking", "Animals")),
        Destination(89, "Naivasha Town", "Urban", "Naivasha is a bustling and scenic town located on the floor of the Great Rift Valley, built on the shores of the beautiful freshwater Lake Naivasha. It is a major hub for several world-class tourism destinations, including Hell's Gate National Park and Mount Longonot. Historically, Naivasha was a significant stop for the pioneers of commercial aviation, as it served as a refueling base for flying boats on the route to South Africa. Today, it is a center for Kenya's thriving floriculture industry and a weekend getaway destination for Nairobi residents. With its cool Rift Valley climate, vibrant markets, and proximity to some of the country's most dramatic natural wonders, Naivasha is a high-energy and exciting town to explore.", "", emptyList(), null, 4.5f, "Rift Valley", -0.7, 36.4, "Year-round", listOf("Town", "Lake")),
        Destination(90, "Nakuru City", "Urban", "Nakuru, known as the 'Home of the Flamingo', is a vibrant and growing city in the Great Rift Valley, and the fourth-largest urban center in Kenya. It is internationally famous for the neighboring Lake Nakuru National Park, which was once home to millions of flamingos and remains a premier sanctuary for black and white rhinos. The city itself is clean and energetic, serving as a major center for agriculture, industry, and education. Nakuru is situated at a high altitude and enjoys a refreshingly cool climate. It is a gateway to the spectacular Menengai Crater and Hyrax Hill prehistoric site. Its mix of urban progress, deep history, and easy access to some of Kenya's most iconic wildlife makes Nakuru a unique and essential city to explore.", "", emptyList(), null, 4.6f, "Rift Valley", -0.3, 36.1, "Year-round", listOf("City", "Park")),
        Destination(91, "Nanyuki Town", "Urban", "Nanyuki, often called the 'Equator Town', is a vibrant and scenic hub located at the foot of Mount Kenya and right on the Equator line. It is a major base for climbers preparing to summit and is famous for its cool mountain air and spectacular views of the snow-capped peaks. The town served as a significant site for the British Army and still maintains a cosmopolitan atmosphere with high-end cafes, artisan shops, and a thriving wildlife conservation community. Nearby, visitors can explore the Ol Pejeta Conservancy and the Mount Kenya Wildlife Conservancy. Nanyuki's unique position between the central highlands and the northern frontier, combined with its high-altitude charm and stunning natural backdrop, makes it one of Kenya's most adventurous and scenic towns to explore.", "", emptyList(), null, 4.6f, "Central", -0.0, 37.1, "Year-round", listOf("Town", "Mountain")),
        Destination(92, "Ngong Hills", "Hiking", "The Ngong Hills are a spectacular range of seven rolling hills located overlooking the Great Rift Valley, just a short drive from Nairobi. The name 'Ngong' is derived from the Maasai word for 'knuckles', referring to the hills' jagged ridge line. Historically, the area was the setting for Karen Blixen's famous memoir 'Out of Africa'. The hills offer some of the most scenic and accessible hiking in Kenya, with trails providing breathtaking views of the city skyline to the east and the Rift Valley floor to the west. A modern wind farm on the northern slopes adds a unique character to the landscape. It is a favorite weekend destination for city residents looking for fresh air, exercise, and a silent connection to the dramatic heights of the Rift.", "", emptyList(), null, 4.7f, "Nairobi", -1.4, 36.6, "Year-round", listOf("Hiking", "Views")),
        Destination(93, "Nyali Beach", "Beach", "Nyali Beach is the premier residential and resort area on Mombasa's north coast, known for its flawless white sand, swaying palms, and a wide array of high-end hotels. It's a vibrant hub for water sports, including windsurfing, snorkeling, and deep-sea fishing. Unlike some of the busier public beaches, Nyali maintains an exclusive and manicured feel, with beautiful ocean-facing villas and world-class golf courses nearby. The beach is protected by an offshore reef, creating calm, turquoise lagoons perfect for swimming. With its proximity to the city's best shopping malls and nightlife, Nyali offers a perfect blend of tropical relaxation and urban convenience for the discerning coastal traveler.", "", emptyList(), null, 4.6f, "Coast", -4.0, 39.7, "Year-round", listOf("Beach", "Hotels")),
        Destination(94, "Nyeri Town", "Urban", "Nyeri is the heart of the central highlands and the gateway to the Aberdare Ranges and Mount Kenya. Set in a fertile landscapes of coffee and tea plantations, it is one of Kenya's most historic and scenic towns. Nyeri is most famous for being the final resting place of Lord Baden-Powell, the founder of the Scout Movement, and for the historic Outspan and Treetops Hotels. The town enjoys a cool, misty climate and is a significant agricultural hub. Nyeri offers a perfect mix of colonial history, local market energy, and a starting point for world-class mountain and forest safaris, making it a culturally rich and adventurous destination in the shadow of Africa's great mountains.", "", emptyList(), null, 4.5f, "Central", -0.4, 36.9, "Year-round", listOf("Town", "Coffee")),
        Destination(95, "Ol Donyo Sabuk", "Mountain", "Ol Donyo Sabuk, also known as 'Kyanzavi' or 'Buffalo Mountain', is a spectacular forested mountain that rises steeply from the plains near Thika city. The mountain's name is Maasai for 'Big Mountain'. Its peak, at 2,145 meters, offers stunning 360-degree views of the surrounding central highlands and, on clear days, both Mount Kenya and Mount Kilimanjaro. The mountain is covered in dense forest that is home to buffaloes, monkeys, and a variety of birds. It is also the site of the McMillan family graves, adding a layer of colonial history to the natural beauty. Climbing Ol Donyo Sabuk is a rewarding hike through diverse ecosystems, providing a refreshing and high-altitude escape within easy reach of Nairobi.", "", emptyList(), null, 4.4f, "Central", -1.1, 37.3, "Year-round", listOf("Hiking", "Views")),
        Destination(96, "Ol Pejeta Conservancy", "Wildlife", "Ol Pejeta Conservancy is a world-renowned, 90,000-acre wildlife sanctuary situated on the Laikipia Plateau. It is a global leader in rhino conservation, hosting the largest population of black rhinos in East Africa and the world's last two northern white rhinos. Ol Pejeta is also home to the Sweetwaters Chimpanzee Sanctuary, providing a safe haven for rescued primates. It's unique because it offers a chance to see the 'Big Five' in a private, high-density environment that transitions between the savannah and the northern frontier. The conservancy's focus on innovative technology (like using AI for animal tracking) and community integration makes it a flagship for modern, professional conservation in the 21st century.", "", emptyList(), null, 4.8f, "Laikipia", 0.0, 36.9, "Year-round", listOf("Wildlife", "Conservation")),
        Destination(97, "Olorgesailie", "History", "Olorgesailie is a world-class archaeological and geological site located on the floor of the Great Rift Valley, famous for its incredible concentration of Acheulean hand axes. Often called the 'factory of the world', the site provides evidence of early human ancestor life (Homo erectus) dating back over 1 million years. The site features a series of outdoor excavation sites where thousands of stone tools remain in their original positions, perfectly preserved by volcanic ash and lake sediments. The onsite museum showcases hominid fossils and signs of the ancient lake that once dominated the valley. Olorgesailie offers a profound and humbling connection to the origin of technology and the early evolutionary journey of the human species in a dramatic Rift Valley landscape.", "", emptyList(), null, 4.6f, "Rift Valley", -1.6, 36.4, "Year-round", listOf("History", "Tools")),
        Destination(98, "Pate Island", "History", "Pate Island is the largest island in the Lamu Archipelago, an ancient and evocative center of Swahili-Arab culture. Historically, Pate was a powerful city-state that once dominated much of the coast. Today, the island is home to several historic settlements including Pate Town, Siyu, and Faza, all featuring ruins of ancient mosques, tombs, and fortified houses. The island's atmosphere is one of deep seclusion and timeless history, where life moves according to the tides and the tradition of dhow building still flourishes. Pate offers a unique and authentic window into the mediaeval heritage of the Swahili coast, far removed from any modern intrusion, making it a must-visit for serious history and culture enthusiasts.", "", emptyList(), null, 4.5f, "Coast", -2.1, 41.0, "Year-round", listOf("History", "Culture")),
        Destination(99, "Ruma National Park", "Wildlife", "Ruma National Park, located in Homa Bay near the shores of Lake Victoria, is a stunning and unique wilderness area known as the 'Home of the Roan Antelope'. It is the only place in Kenya where you can find these magnificent, sickle-horned antelopes in their natural habitat. The park features a beautiful mix of riverine forest and open acacia savannah, providing a sanctuary for Rothschild's giraffes, leopards, and over 400 species of birds. Ruma offers a quiet and exclusive wildlife experience in Western Kenya, far from the crowd. Its scenic beauty, high density of rare species, and status as a critical island of biodiversity in a developed region make it a vital and fascinating destination for nature lovers and conservation enthusiasts.", "", emptyList(), null, 4.4f, "Western", -0.6, 34.3, "Year-round", listOf("Wildlife", "Nature")),
        Destination(100, "Saiwa Swamp National Park", "Nature", "Saiwa Swamp is Kenya's smallest national park, but its significance is massive. Located near Kitale, this 3-square-kilometer sanctuary is a lush, vibrant wetland created specifically to protect the endangered Sitatunga, a unique semi-aquatic antelope with splayed hooves for walking on reeds. Visitors can explore the swamp on a system of raised wooden boardwalks and viewing platforms that allow for silent, intimate encounters with the Sitatungas as they move through the papyrus. The park is also a birdwatcher's paradise, hosting several rare West African forest species found nowhere else in Kenya. Saiwa Swamp offers a peaceful and immersive jungle experience, proving that even the smallest spaces can hold the most precious natural treasures.", "", emptyList(), null, 4.5f, "Western", 1.1, 35.1, "Year-round", listOf("Walking", "Nature")),
        Destination(101, "Samburu Village", "Culture", "Visiting a traditional Samburu Village in the rugged northern frontier provides a powerful and authentic connection to one of Kenya's most colorful and resilient cultures. The Samburu are a semi-nomadic nilotic people closely related to the Maasai, known for their striking red clothing and elaborate beaded jewelry. Their villages, or 'nkang', are constructed of sticks and mud on the arid plains of the north. Visitors can witness traditional songs, learn about their sophisticated age-set systems, and experience their deep spiritual connection to the environment. The Samburu's ability to thrive in a harsh desert landscape while maintaining their ancient traditions is a testament to human resilience and provides a truly profound cultural experience in the heart of the north.", "", emptyList(), null, 4.7f, "Samburu", 0.6, 37.6, "Year-round", listOf("Culture", "Tradition")),
        Destination(102, "Shanzu Beach", "Beach", "Shanzu Beach is a serene and beautiful coastal destination located north of Mombasa, featuring soft white sand and vibrant turquoise waters protected by a healthy offshore reef. It is a favored hub for luxury travel, hosting several of the coast's most prestigious resorts and spas. The beach is a hub for water sports, including windsurfing and snorkeling, but also offers the peace and quiet needed for a premium tropical escape. Its close proximity to the energetic Mtwapa area allows for easy access to world-class dining and nightlife while maintaining a high-end, exclusive atmosphere on the sand. Shanzu represents the perfect balance of coastal excitement and luxurious relaxation in the heart of the north coast.", "", emptyList(), null, 4.5f, "Coast", -3.9, 39.7, "Year-round", listOf("Beach", "Relax")),
        Destination(103, "Shela Beach", "Beach", "Shela Beach on Lamu Island is widely considered one of the most beautiful and atmospheric beaches in the world. It features several kilometers of pure white sand dunes that separate the village of Shela from the Indian Ocean. With its rolling dunes, crystal-clear water, and the complete absence of cars, transport is entirely by donkey or dhow boat. Shela is a center for Swahili culture and features some of the most stunning restored Swahili houses and luxury eco-lodges in Africa. It is a place of absolute peace, timed by the call to prayer and the movement of the tides, offering a truly high-end and spiritually recharging coastal experience in a timeless, historic setting.", "", emptyList(), null, 4.8f, "Coast", -2.3, 40.9, "Year-round", listOf("Beach", "Dunes")),
        Destination(104, "Shimba Hills National Reserve", "Nature", "Shimba Hills National Reserve is a spectacularly beautiful coastal ecosystem, consisting of one of the largest coastal forests in East Africa rising from the plains south of Mombasa. The reserve is famous for being the only place in Kenya where you can find the majestic Sable Antelope. The landscape is a mystical mix of high-altitude moorland, deep forest, and dramatic waterfalls like the Sheldrick Falls. It is a significant sanctuary for elephants and a variety of rare coastal plants. Its cool, misty climate and panoramic views of the Indian Ocean provide a refreshing and verdant escape from the tropical heat of the coast, offering a unique 'mountain' safari experience within sight of the sea.", "", emptyList(), null, 4.6f, "Coast", -4.2, 39.4, "Year-round", listOf("Wildlife", "Views")),
        Destination(105, "Siyu Fort", "History", "Siyu Fort on Pate Island is a unique and significant historical site, the only fort on the entire Swahili coast built by the local people rather than by outsiders like the Portuguese or Arabs. Constructed in the 19th century using coral rag and limestone, it was a symbol of local resistance and independence. Siyu was once a significant center for Islamic higher learning and a hub for traditional crafts like embroidery and woodcarving. Today, the massive, silent walls of the fort look out over the mangrove channels, providing a powerful sense of the island's resilient history. It is a fascinating and off-the-beaten-path destination for those wanting to understand the deep-rooted spirit of the Swahili coast.", "", emptyList(), null, 4.5f, "Coast", -2.1, 41.0, "Year-round", listOf("History", "Architecture")),
        Destination(106, "South Island National Park", "Nature", "South Island National Park is a wild and remote island situated on the world's largest desert lake, Lake Turkana. Composed primarily of black volcanic rock and parched lava fields, the island is a vital sanctuary for numerous water birds and has a significant population of Nile crocodiles. It is an Important Bird Area and a critical breeding site for many migratory species. The island's stark, monochromatic landscape contrasts sharply with the turquoise 'Jade Sea', creating a minimalist and otherworldly beauty. Visiting South Island is a true adventure, providing a profound sense of isolation and a connection to the raw elemental power of the Rift Valley's northern frontier.", "", emptyList(), null, 4.5f, "Turkana", 2.6, 36.6, "Year-round", listOf("Adventure", "Nature")),
        Destination(107, "Taita Hills", "Nature", "The Taita Hills are an ancient and spectacularly beautiful range of mountains rising from the plains of the Tsavo ecosystem, representing the northernmost extension of the Eastern Arc Mountains. These 'sky islands' are covered in a lush, cloud-fed forest that is a global biodiversity hotspot, home to numerous endemic plants and animals found nowhere else on Earth, including several rare birds. The hills provide a critical water catchment and offer panoramic views that stretch all the way to Mount Kilimanjaro on clear days. Exploring the Taita Hills involves hiking through ancient forests, visiting historic sites like the Taita salt licks, and meeting the local community. It is a place of raw beauty and high conservation significance, far removed from any modern intrusion.", "", emptyList(), null, 4.6f, "Coast", -3.4, 38.3, "Year-round", listOf("Nature", "Birds")),
        Destination(108, "Takaungu Beach", "Beach", "Takaungu is a hidden and picturesque Swahili village located on a scenic creek north of Mombasa, famous for its deep sense of history and beautiful, uncrowded beaches. The area is a hub for traditional dhow boat building and has maintained its authentic coastal atmosphere for centuries. Takaungu Creek is a place of profound silence and beauty, where traditional Swahili life continues to move at a relaxed pace. Visitors can enjoy dhow trips through the mangroves, explore the village's historic mosques, and relax on the pristine white sands of the main beach. Takaungu represents the quintessence of an authentic and silent coastal getaway, perfect for those wanting to escape the more commercial resort hubs.", "", emptyList(), null, 4.5f, "Coast", -3.7, 39.8, "Year-round", listOf("Beach", "Culture")),
        Destination(109, "Takwa Ruins", "History", "The Takwa Ruins are the beautifully preserved remains of a 16th and 17th-century Swahili town located on Manda Island in the Lamu Archipelago. Historically, Takwa was a significant center for maritime trade and Islamic religion, featuring a Great Mosque and several large stone houses. Curiously, the town was abandoned in the 17th century, possibly due to the depletion of fresh water. Today, the ruins are a UNESCO World Heritage site and provide a powerful and silent window into the sophisticated mediaeval Swahili civilization. Access is strictly by boat from Lamu town at high tide. Negotiating the twisting mangrove channels to reach the silent ruins of the lost city is a truly evocative and historic adventure.", "", emptyList(), null, 4.6f, "Coast", -2.3, 40.9, "Year-round", listOf("History", "Ruins")),
        Destination(110, "Thika Town", "Urban", "Thika, often called the 'Industrial Town of Kenya', is a bustling and scenic urban center located just north of Nairobi. It is world-famous for its massive fruit plantations, most notably the Del Monte pineapple fields that surround the town for miles. Historically, Thika was the setting for Elspeth Huxley's famous memoir 'The Flame Trees of Thika'. The town is situated on the confluence of the Thika and Chania Rivers and features the spectacular Chania Falls and the nearby Fourteen Falls. Thika is also becoming a center for education and innovation. Its energetic local markets, high-energy industrial sector, and proximity to stunning natural waterfalls make it a unique and exciting place to experience modern Kenyan life.", "", emptyList(), null, 4.4f, "Central", -1.0, 37.1, "Year-round", listOf("Town", "Falls")),
        Destination(111, "Thimlich Ohinga", "History", "Thimlich Ohinga is a magnificent and unique dry-stone walled architectural complex located near Migori in Western Kenya, recognized as a UNESCO World Heritage site. Built approximately 500 years ago, it consists of several massive, circular enclosures made entirely of local stone without any mortar, reflecting a sophisticated level of communal engineering and social organization. It is the best-preserved example of the numerous 'Ohingni' (fortified settlements) found in the Lake Victoria region. Historically, these stone structures protected the local communities and their livestock from both predators and invaders. Exploring the towering walls and narrow gates of Thimlich Ohinga offers a profound and unique insight into the resilient and highly organized mediaeval cultures of Western Kenya.", "", emptyList(), null, 4.7f, "Western", -0.9, 34.3, "Year-round", listOf("History", "Archaeology")),
        Destination(112, "Tiwi Beach", "Beach", "Tiwi Beach is a hidden gem on the south coast, located north of Diani. It is famous for its raw, untouched beauty and the spectacular 'Africa Pool', a rock pool in the shape of the African continent that appears on the reef at low tide. Unlike the more developed beach resorts nearby, Tiwi has maintained a more relaxed, 'old coastal' atmosphere with simple cottages and lush palm groves. The beach features a stunning coral reef right at its doorstep, offering incredible snorkeling in total tranquility. It is a place for those who enjoy the simple pleasures of the sea, the sand, and the sun, far removed from the high-energy tourism hubs, making it a favorite for families and independent travelers alike.", "", emptyList(), null, 4.6f, "Coast", -4.2, 39.6, "Year-round", listOf("Beach", "Coral")),
        Destination(113, "Vasco da Gama Pillar", "History", "The Vasco da Gama Pillar in Malindi is one of the oldest and most significant European monuments in East Africa, a towering stone pillar topped by a cross of Lisbon stone. It was erected by the famous Portuguese explorer Vasco da Gama in 1498 as a sign of appreciation to the Sultan of Malindi for providing a guide to lead him across the Indian Ocean to India. Situated on a rocky outcrop overlooking the turquoise sea, it is a powerful symbol of the beginning of the European exploration of Africa and the Far East. Visiting the pillar offers a chance to stand where one of history's most famous explorers stood and enjoy the same spectacular views of the Kenyan coastline that have defined Malindi for centuries.", "", emptyList(), null, 4.5f, "Coast", -3.2, 40.1, "Year-round", listOf("History", "Views")),
        Destination(114, "Vipingo Beach", "Beach", "Vipingo Beach is a serene and exclusive destination on the north coast, known for its flawless white sand, swaying palms, and a high-end, manicured atmosphere. It is most famous for the world-class Vipingo Ridge golf course, which offers spectacular panoramic views of the Indian Ocean from the hills overlooking the beach. The beach is protected by a vibrant offshore reef, making it a perfect spot for swimming and snorkeling in calm, turquoise waters. With its proximity to both Kilifi and Mombasa, Vipingo offers a perfect blend of high-end sporting facilities and raw tropical beauty, and it remains one of the most prestigious and silent coastal addresses in Kenya.", "", emptyList(), null, 4.6f, "Coast", -3.8, 39.8, "Year-round", listOf("Beach", "Golf")),
        Destination(115, "Wasini Island", "Beach", "Wasini Island is a tiny, magical island situated off the south coast near Shimoni, famous for its incredible 'coral gardens' and its status as a gateway to the Kisite-Mpunguti Marine Park. The island has no roads and no cars, transport is entirely on foot or by traditional boat. Historically, Wasini was a significant center for Swahili-Arab trade. Today, its main attraction is the boardwalk that winds through ancient, twisted fossilized coral reefs. The surrounding waters are world-famous for their dolphin pods and vibrant snorkeling. A visit to Wasini offers a chance to experience authentic coastal life, enjoy delicious Swahili seafood, and immerse yourself in the vibrant underwater world of one of East Africa's most beautiful marine sanctuaries.", "", emptyList(), null, 4.7f, "Coast", -4.7, 39.4, "Year-round", listOf("Snorkeling", "Dolphins"))
    )
}

fun String.optimizeUnsplashUrl(width: Int): String {
    if (this.startsWith("https://images.unsplash.com/")) {
        val baseUrl = this.substringBefore("?")
        return "$baseUrl?w=$width&q=80&auto=format&fit=crop"
    }
    return this
}



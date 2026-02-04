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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow

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
    val activities: List<String> = emptyList()
)

data class Comment(val userName: String, val text: String, val timestamp: Long)

object DestinationsRepository {
    private var database: AppDatabase? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val reviewsCollection = firestore.collection("public_reviews")
    
    private val _allDestinations = MutableStateFlow<List<Destination>>(emptyList())
    val allDestinations: StateFlow<List<Destination>> = _allDestinations.asStateFlow()

    fun getComments(destinationId: Int): Flow<List<Comment>> {
        // Sync Firestore to Room
        syncPublicReviews(destinationId)
        
        return database?.destinationDao()?.getCommentsForDestination(destinationId)?.map { entities ->
            entities.map { Comment(it.userName, it.text, it.timestamp) }
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
                        CommentEntity(
                            destinationId = destinationId,
                            userName = userName,
                            text = text,
                            timestamp = timestamp
                        )
                    }
                    database?.destinationDao()?.insertPublicComments(entities)
                }
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

        // Save to Cloud (Firestore)
        reviewsCollection.add(commentData)

        // Save to Local Room (Immediate feedback)
        CoroutineScope(Dispatchers.IO).launch {
            database?.destinationDao()?.insertComment(
                CommentEntity(destinationId = destinationId, userName = userName, text = text, timestamp = timestamp)
            )
        }
    }

    fun initialize(context: android.content.Context) {
        if (database != null) return
        
        database = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "kenya_tourism_db"
        ).build()

        CoroutineScope(Dispatchers.IO).launch {
            // Seed database if empty
            val dao = database!!.destinationDao()
            // We'll use a simplified check or just always insert as REPLACE
            val entities = staticDestinations.map { it.toEntity() }
            dao.insertAll(entities)
            
            // Observe database changes
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
        activities = activities.joinToString(",")
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
        activities = if (activities.isEmpty()) emptyList() else activities.split(",")
    )

    // Helper to resolve local drawable resource
    fun getDestinationDrawable(context: Context, name: String): Int {
        try {
            val normalized = name.lowercase()
                .replace(Regex("[^a-z0-9.]"), "_")
                .replace(Regex("_+"), "_")
                .trim('_')
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
            "The Maasai Mara is not just a park; it's a globally recognized stage for the greatest wildlife spectacle on Earth. Famous for the Great Migration where millions of wildebeest and zebra traverse the crocodile-infested Mara River, it offers an unparalleled safari experience. The vast savannahs are home to the Big Five (lion, leopard, rhino, elephant, and buffalo) and boast an incredible density of predators including cheetahs and hyenas. Visitors can clamp in luxury tented camps, take hot air balloon safaris at sunrise, and witness nature's raw power in action.", 
            "https://images.unsplash.com/photo-1516426122078-c23e76319801", 
            emptyList(), 
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
            "Crowned by the majestic Mount Kilimanjaro, Africa's highest peak, Amboseli National Park is famous for being the best place in the world to get close to free-ranging elephants. The park's signature image is of massive elephant herds crossing the dusty plains with the snow-capped mountain in the background. Its landscape ranges from dried-up bed of Lake Amboseli, wetlands with sulphur springs, savannah and woodlands. You can visit the Observation Hill which allows an overall view of the whole park especially the swamps and elephants.", 
            "https://images.unsplash.com/photo-1549366021-9f761d450615", 
            emptyList(), 
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
            "One of the oldest and largest parks in Kenya, Tsavo East is known as the 'Theatre of the Wild'. Its defining feature is its vast, semi-arid wilderness and the Yatta Plateau, the world's longest lava flow. The park is famous for its 'Red Elephants', colored by the red-oxide soil they dust themselves with. Major attractions include the Galana River, Lugard Falls, and the Mudanda Rock. It's a haven for rhinos, buffaloes, lions, leopards, pods of hippo, crocodile, waterbucks, lesser kudu, gerenuk, and the prolific bird life features 500 recorded species.", 
            "https://images.unsplash.com/photo-1547970810-dc1eac37d174", 
            emptyList(), 
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
            "https://images.unsplash.com/photo-1535338454770-7a7d17c0d9f2", 
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
            "Lake Nakuru National Park is a chaotic, colorful riot of birds and wildlife. Originally famous for the millions of flamingos that would color its shores pink, it is now also one of the best rhino sanctuaries in Kenya, protecting both black and white rhinos. The park's landscape includes areas of marsh and grasslands alternating with rocky cliffs and outcrops, stretches of acacia woodland and rocky hillsides covered with a Euphorbia forest. It's also a great place to see leopards and the rare Rothschild's giraffe.", 
            "https://images.unsplash.com/photo-1564760055775-d63b17a55c44", 
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
            "https://images.unsplash.com/photo-1549366021-9f761d450615", 
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
            "https://images.unsplash.com/photo-1547970810-dc1eac37d174", 
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
            "https://images.unsplash.com/photo-1516426122078-c23e76319801", 
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
            "https://images.unsplash.com/photo-1535338454770-7a7d17c0d9f2", 
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
            "https://images.unsplash.com/photo-1564760055775-d63b17a55c44", 
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
            "Voted multiple times as Africa's leading beach destination, Diani Beach is a tropical paradise on the coast of Kenya. With its flawless long stretch of white sand hugged by lush forest and kissed by surfable waves, it's no wonder Diani is so popular. The beach is lined with luxury resorts, private villas, and distinct restaurants. The coral reefs just offshore offer world-class diving and snorkeling, while the nearby Shimba Hills National Reserve provides a quick safari getaway. It's the perfect place to relax after a dusty safari.", 
            "https://images.unsplash.com/photo-1559827260-dc66d52bef19", 
            emptyList(), 
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
            "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", 
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
            "https://images.unsplash.com/photo-1559827260-dc66d52bef19", 
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
            "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", 
            emptyList(), 
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
            "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", 
            emptyList(), 
            null, 
            4.9f, 
            "Central", 
            -0.2, 
            37.3, 
            "Jan-Feb, Aug-Sep", 
            listOf("Climbing", "Hiking", "Camping", "Photography")
        ),
        Destination(16, "Mount Longonot", "Hiking", "Dormant volcano with a stunning crater.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.7f, "Rift Valley", -0.9, 36.4, "Year-round", listOf("Hiking", "Views")),
        Destination(17, "Lake Naivasha", "Lake", "Freshwater lake with hippos and birdlife.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.7f, "Rift Valley", -0.8, 36.4, "Year-round", listOf("Boat Rides", "Birding")),
        Destination(18, "Fort Jesus, Mombasa", "History", "16th-century Portuguese fort.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.7f, "Coast", -4.1, 39.7, "Year-round", listOf("History", "Museum")),
        Destination(19, "Giraffe Centre", "Wildlife", "Conservation centre for Rothschild's giraffes.", "https://images.unsplash.com/photo-1547970810-dc1eac37d174", emptyList(), null, 4.8f, "Nairobi", -1.4, 36.7, "Year-round", listOf("Feeding Giraffes", "Education")),
        Destination(20, "David Sheldrick Elephant Orphanage", "Wildlife", "Rescue and rehabilitation program for elephants.", "https://images.unsplash.com/photo-1549366021-9f761d450615", emptyList(), null, 4.9f, "Nairobi", -1.4, 36.7, "Year-round", listOf("Adoption", "Visiting")),
        Destination(21, "Aberdare Ranges", "Mountain", "Scenic mountain range.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.5f, "Central", -0.5, 36.6, "Year-round", listOf("Hiking", "Views")),
        Destination(22, "Arabuko Sokoke Forest", "Nature", "Largest remaining coastal forest.", "https://images.unsplash.com/photo-1516426122078-c23e76319801", emptyList(), null, 4.4f, "Coast", -3.3, 39.9, "Year-round", listOf("Birding", "Walking")),
        Destination(23, "Bamburi Beach", "Beach", "Popular beach destination.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.5f, "Coast", -3.9, 39.7, "Year-round", listOf("Beach", "Fun")),
        Destination(24, "Bomas of Kenya", "Culture", "Cultural center showcasing traditional homesteads.", "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", emptyList(), null, 4.6f, "Nairobi", -1.3, 36.7, "Year-round", listOf("Culture", "Dance")),
        Destination(25, "Carnivore Restaurant", "Dining", "Famous for its meat buffet.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.7f, "Nairobi", -1.3, 36.8, "Year-round", listOf("Dining", "Experience")),
        Destination(26, "Central Island National Park", "Nature", "Volcanic island in Lake Turkana.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.5f, "Turkana", 3.5, 36.0, "Year-round", listOf("Adventure", "Views")),
        Destination(27, "Chale Island", "Beach", "Private island paradise.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.8f, "Coast", -4.4, 39.5, "Year-round", listOf("Luxury", "Beach")),
        Destination(28, "Cherangani Hills", "Hiking", "Scenic hills for trekking.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.5f, "Rift Valley", 1.0, 35.5, "Year-round", listOf("Hiking", "Views")),
        Destination(29, "Chyulu Hills", "Nature", "Rolling volcanic hills.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.6f, "Eastern", -2.6, 37.7, "Year-round", listOf("Scenery", "Nature")),
        Destination(30, "Chyulu Hills National Park", "Nature", "Protected volcanic landscape.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.6f, "Eastern", -2.5, 37.8, "Year-round", listOf("Wildlife", "Caves")),
        Destination(31, "Eldoret City", "Urban", "Home of champions.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.4f, "Rift Valley", 0.5, 35.3, "Year-round", listOf("City", "Culture")),
        Destination(32, "Fourteen Falls", "Nature", "Spectacular waterfalls.", "https://images.unsplash.com/photo-1516426122078-c23e76319801", emptyList(), null, 4.3f, "Central", -1.1, 37.2, "Year-round", listOf("Views", "Nature")),
        Destination(33, "Funzi Island", "Beach", "Mangroves and seclusion.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.6f, "Coast", -4.5, 39.4, "Year-round", listOf("Boat Trips", "Beach")),
        Destination(34, "Galu Beach", "Beach", "Beautiful sandy beach.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.7f, "Coast", -4.4, 39.6, "Year-round", listOf("Relaxation", "Sun")),
        Destination(35, "Gedi Ruins", "History", "Ancient Swahili city ruins.", "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", emptyList(), null, 4.7f, "Coast", -3.3, 40.0, "Year-round", listOf("History", "Exploration")),
        Destination(36, "Haller Park", "Nature", "Reclaimed quarry nature park.", "https://images.unsplash.com/photo-1516426122078-c23e76319801", emptyList(), null, 4.6f, "Coast", -4.0, 39.7, "Year-round", listOf("Wildlife", "Walking")),
        Destination(37, "Hyrax Hill", "History", "Prehistoric site.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.4f, "Rift Valley", -0.3, 36.1, "Year-round", listOf("History", "Museum")),
        Destination(38, "Jumba la Mtwana", "History", "Ancient ruins.", "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", emptyList(), null, 4.5f, "Coast", -3.9, 39.7, "Year-round", listOf("History", "Culture")),
        Destination(39, "Kakamega Forest", "Nature", "Tropical rainforest.", "https://images.unsplash.com/photo-1516426122078-c23e76319801", emptyList(), null, 4.6f, "Western", 0.3, 34.8, "Year-round", listOf("Birding", "Hiking")),
        Destination(40, "Karen Blixen Museum", "History", "Author's home.", "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", emptyList(), null, 4.7f, "Nairobi", -1.3, 36.7, "Year-round", listOf("History", "Literature")),
        Destination(41, "Kariandusi Museum", "History", "Early stone age site.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.4f, "Rift Valley", -0.4, 36.2, "Year-round", listOf("History", "Archaeology")),
        Destination(42, "Karura Forest", "Nature", "Urban forest with trails.", "https://images.unsplash.com/photo-1516426122078-c23e76319801", emptyList(), null, 4.8f, "Nairobi", -1.2, 36.8, "Year-round", listOf("Cycling", "Running")),
        Destination(43, "Kaya Forests", "Culture", "Sacred Mijikenda forests.", "https://images.unsplash.com/photo-1516426122078-c23e76319801", emptyList(), null, 4.5f, "Coast", -4.2, 39.6, "Year-round", listOf("Culture", "Nature")),
        Destination(44, "Kazuri Beads Factory", "Culture", "Handmade ceramic beads.", "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", emptyList(), null, 4.7f, "Nairobi", -1.3, 36.7, "Year-round", listOf("Shopping", "Art")),
        Destination(45, "Kericho Town", "Urban", "Tea capital.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.5f, "Rift Valley", -0.3, 35.3, "Year-round", listOf("Tea Tours", "Greenery")),
        Destination(46, "Kikambala Beach", "Beach", "North coast beach.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.5f, "Coast", -3.8, 39.8, "Year-round", listOf("Beach", "Relax")),
        Destination(47, "Kilifi Beach", "Beach", "Scenic beach and creek.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.6f, "Coast", -3.6, 39.8, "Year-round", listOf("Beach", "Water")),
        Destination(48, "Kinangop Plateau", "Nature", "High altitude plateau.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.4f, "Central", -0.6, 36.6, "Year-round", listOf("Views", "Birding")),
        Destination(49, "Kisite-Mpunguti Marine Park", "Nature", "Marine life sanctuary.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.8f, "Coast", -4.7, 39.3, "Year-round", listOf("Dolphins", "Snorkeling")),
        Destination(50, "Kisumu City", "Urban", "Lakeside city.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.5f, "Western", -0.1, 34.7, "Year-round", listOf("City", "Lake")),
        Destination(51, "Kitengela Glass", "Art", "Glass blowing artistry.", "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", emptyList(), null, 4.7f, "Nairobi", -1.4, 36.8, "Year-round", listOf("Art", "Crafts")),
        Destination(52, "Koobi Fora", "History", "Cradle of mankind.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.6f, "Turkana", 3.9, 36.2, "Year-round", listOf("History", "Fossils")),
        Destination(53, "Lake Baringo", "Lake", "Bird paradise.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.5f, "Rift Valley", 0.6, 36.0, "Year-round", listOf("Birding", "Boating")),
        Destination(54, "Lake Bogoria", "Lake", "Hot springs and geysers.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.6f, "Rift Valley", 0.2, 36.1, "Year-round", listOf("Nature", "Hot Springs")),
        Destination(55, "Lake Bogoria National Reserve", "Nature", "Protected lake reserve.", "https://images.unsplash.com/photo-1564760055775-d63b17a55c44", emptyList(), null, 4.6f, "Rift Valley", 0.2, 36.1, "Year-round", listOf("Wildlife", "Scenery")),
        Destination(56, "Lake Chala", "Lake", "Crater lake.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.5f, "Coast", -3.3, 37.7, "Year-round", listOf("Views", "Nature")),
        Destination(57, "Lake Elementaita", "Lake", "Soda lake with pelicans.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.5f, "Rift Valley", -0.4, 36.2, "Year-round", listOf("Birding", "Scenic")),
        Destination(58, "Lake Jipe", "Lake", "Remote lake.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.4f, "Coast", -3.6, 37.7, "Year-round", listOf("Fishing", "Nature")),
        Destination(59, "Lake Kamnarok", "Lake", "Diverse ecosystem.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.3f, "Rift Valley", 0.7, 35.6, "Year-round", listOf("Wildlife", "Nature")),
        Destination(60, "Lake Magadi", "Lake", "Pink soda lake.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.6f, "Rift Valley", -1.9, 36.3, "Year-round", listOf("Photography", "Nature")),
        Destination(61, "Lake Ol Bolossat", "Lake", "Important wetland.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.4f, "Central", -0.1, 36.4, "Year-round", listOf("Birding", "Hipoos")),
        Destination(62, "Lake Paradise", "Nature", "Crater lake in Marsabit.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.7f, "Eastern", 2.3, 37.9, "Year-round", listOf("Scenery", "Nature")),
        Destination(63, "Lake Simbi Nyaima", "Lake", "Crater lake.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.4f, "Western", -0.4, 34.6, "Year-round", listOf("Nature", "Legends")),
        Destination(64, "Lake Turkana", "Lake", "Jade sea.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.7f, "Turkana", 3.0, 36.0, "Year-round", listOf("Adventure", "Culture")),
        Destination(65, "Lake Victoria", "Lake", "Major inland sea.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.6f, "Western", -0.5, 34.0, "Year-round", listOf("Fishing", "Islands")),
        Destination(66, "Lamu Old Town", "History", "Swahili heritage.", "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", emptyList(), null, 4.8f, "Coast", -2.3, 40.9, "Year-round", listOf("History", "Architecture")),
        Destination(67, "Loita Hills", "Hiking", "Walking safaris.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.6f, "Rift Valley", -1.6, 35.5, "Year-round", listOf("Trekking", "Culture")),
        Destination(68, "Maasai Market", "Shopping", "Curios and crafts.", "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", emptyList(), null, 4.7f, "Nairobi", -1.3, 36.8, "Year-round", listOf("Shopping", "Art")),
        Destination(69, "Maasai Village", "Culture", "Traditional life.", "https://images.unsplash.com/photo-1549366021-9f761d450615", emptyList(), null, 4.8f, "Various", -1.5, 35.0, "Year-round", listOf("Culture", "Tradition")),
        Destination(70, "Malindi Town", "Urban", "Coastal charm.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.5f, "Coast", -3.2, 40.1, "Year-round", listOf("Town", "Beach")),
        Destination(71, "Malka Mari National Park", "Nature", "Border wilderness.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.3f, "Eastern", 3.9, 40.7, "Year-round", listOf("Remote", "Nature")),
        Destination(72, "Mamba Village", "Wildlife", "Crocodile farm.", "https://images.unsplash.com/photo-1549366021-9f761d450615", emptyList(), null, 4.4f, "Coast", -4.0, 39.7, "Year-round", listOf("Reptiles", "Tour")),
        Destination(73, "Manda Island", "Beach", "Quiet island.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.6f, "Coast", -2.3, 40.9, "Year-round", listOf("Beach", "Ruins")),
        Destination(74, "Marsabit National Park", "Nature", "Forest mountain.", "https://images.unsplash.com/photo-1564760055775-d63b17a55c44", emptyList(), null, 4.5f, "Eastern", 2.3, 37.9, "Year-round", listOf("Wildlife", "Scenic")),
        Destination(75, "Mau Forest", "Nature", "Water tower.", "https://images.unsplash.com/photo-1516426122078-c23e76319801", emptyList(), null, 4.5f, "Rift Valley", -0.5, 35.7, "Year-round", listOf("Forest", "Nature")),
        Destination(76, "Menengai Crater", "Nature", "Large caldera.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.6f, "Rift Valley", -0.2, 36.1, "Year-round", listOf("Hiking", "Views")),
        Destination(77, "Mnarani Ruins", "History", "Ancient mosques.", "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", emptyList(), null, 4.5f, "Coast", -3.6, 39.8, "Year-round", listOf("History", "Culture")),
        Destination(78, "Mombasa Old Town", "Urban", "Historic city.", "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", emptyList(), null, 4.7f, "Coast", -4.1, 39.7, "Year-round", listOf("Walking", "History")),
        Destination(79, "Mount Elgon", "Mountain", "Volcanic giant.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.6f, "Western", 1.1, 34.5, "Year-round", listOf("Climbing", "Caves")),
        Destination(80, "Mount Elgon National Park", "Nature", "Elephants in caves.", "https://images.unsplash.com/photo-1549366021-9f761d450615", emptyList(), null, 4.6f, "Western", 1.1, 34.5, "Year-round", listOf("Nature", "Wildlife")),
        Destination(81, "Mount Suswa", "Mountain", "Craters and caves.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.6f, "Rift Valley", -1.1, 36.3, "Year-round", listOf("Hiking", "Caving")),
        Destination(82, "Msambweni Beach", "Beach", "Secluded beach.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.7f, "Coast", -4.5, 39.5, "Year-round", listOf("Relaxation", "Beach")),
        Destination(83, "Mtwapa Beach", "Beach", "Creek life.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.5f, "Coast", -3.9, 39.7, "Year-round", listOf("Boating", "Fun")),
        Destination(84, "Mwea National Reserve", "Nature", "Savannah ecosystem.", "https://images.unsplash.com/photo-1564760055775-d63b17a55c44", emptyList(), null, 4.4f, "Central", -0.8, 37.6, "Year-round", listOf("Wildlife", "Nature")),
        Destination(85, "Mzima Springs", "Nature", "Clear water springs.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.7f, "Coast", -3.0, 38.0, "Year-round", listOf("Nature", "Viewing")),
        Destination(86, "Nairobi City", "Urban", "Green city in the sun.", "https://images.unsplash.com/photo-1547970810-dc1eac37d174", emptyList(), null, 4.6f, "Nairobi", -1.3, 36.8, "Year-round", listOf("City", "Business")),
        Destination(87, "Nairobi National Museum", "Culture", "History and art.", "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", emptyList(), null, 4.7f, "Nairobi", -1.3, 36.8, "Year-round", listOf("Museum", "Education")),
        Destination(88, "Nairobi Safari Walk", "Wildlife", "Wildlife boardwalk.", "https://images.unsplash.com/photo-1549366021-9f761d450615", emptyList(), null, 4.6f, "Nairobi", -1.4, 36.7, "Year-round", listOf("Walking", "Animals")),
        Destination(89, "Naivasha Town", "Urban", "Lake town.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.5f, "Rift Valley", -0.7, 36.4, "Year-round", listOf("Town", "Lake")),
        Destination(90, "Nakuru City", "Urban", "Flamingo city.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.6f, "Rift Valley", -0.3, 36.1, "Year-round", listOf("City", "Park")),
        Destination(91, "Nanyuki Town", "Urban", "Equator town.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.6f, "Central", -0.0, 37.1, "Year-round", listOf("Town", "Mountain")),
        Destination(92, "Ngong Hills", "Hiking", "Scenic hills.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.7f, "Nairobi", -1.4, 36.6, "Year-round", listOf("Hiking", "Views")),
        Destination(93, "Nyali Beach", "Beach", "Popular resort area.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.6f, "Coast", -4.0, 39.7, "Year-round", listOf("Beach", "Hotels")),
        Destination(94, "Nyeri Town", "Urban", "Central highlands.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.5f, "Central", -0.4, 36.9, "Year-round", listOf("Town", "Coffee")),
        Destination(95, "Ol Donyo Sabuk", "Mountain", "Buffalo mountain.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.4f, "Central", -1.1, 37.3, "Year-round", listOf("Hiking", "Views")),
        Destination(96, "Ol Pejeta Conservancy", "Wildlife", "Rhino sanctuary.", "https://images.unsplash.com/photo-1564760055775-d63b17a55c44", emptyList(), null, 4.8f, "Laikipia", 0.0, 36.9, "Year-round", listOf("Wildlife", "Conservation")),
        Destination(97, "Olorgesailie", "History", "Prehistoric site.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.6f, "Rift Valley", -1.6, 36.4, "Year-round", listOf("History", "Tools")),
        Destination(98, "Pate Island", "History", "Historic settlements.", "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", emptyList(), null, 4.5f, "Coast", -2.1, 41.0, "Year-round", listOf("History", "Culture")),
        Destination(99, "Ruma National Park", "Wildlife", "Roan antelope.", "https://images.unsplash.com/photo-1564760055775-d63b17a55c44", emptyList(), null, 4.4f, "Western", -0.6, 34.3, "Year-round", listOf("Wildlife", "Nature")),
        Destination(100, "Saiwa Swamp National Park", "Nature", "Sitatunga antelope.", "https://images.unsplash.com/photo-1564760055775-d63b17a55c44", emptyList(), null, 4.5f, "Western", 1.1, 35.1, "Year-round", listOf("Walking", "Nature")),
        Destination(101, "Samburu Village", "Culture", "Tribal culture.", "https://images.unsplash.com/photo-1549366021-9f761d450615", emptyList(), null, 4.7f, "Samburu", 0.6, 37.6, "Year-round", listOf("Culture", "Tradition")),
        Destination(102, "Shanzu Beach", "Beach", "Serene beach.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.5f, "Coast", -3.9, 39.7, "Year-round", listOf("Beach", "Relax")),
        Destination(103, "Shela Beach", "Beach", "Lamu dunes.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.8f, "Coast", -2.3, 40.9, "Year-round", listOf("Beach", "Dunes")),
        Destination(104, "Shimba Hills National Reserve", "Nature", "Coastal hills.", "https://images.unsplash.com/photo-1516426122078-c23e76319801", emptyList(), null, 4.6f, "Coast", -4.2, 39.4, "Year-round", listOf("Wildlife", "Views")),
        Destination(105, "Siyu Fort", "History", "Historic fort.", "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", emptyList(), null, 4.5f, "Coast", -2.1, 41.0, "Year-round", listOf("History", "Architecture")),
        Destination(106, "South Island National Park", "Nature", "Island wildlife.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.5f, "Turkana", 2.6, 36.6, "Year-round", listOf("Adventure", "Nature")),
        Destination(107, "Taita Hills", "Nature", "Cloud forest.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.6f, "Coast", -3.4, 38.3, "Year-round", listOf("Nature", "Birds")),
        Destination(108, "Takaungu Beach", "Beach", "Quiet village beach.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.5f, "Coast", -3.7, 39.8, "Year-round", listOf("Beach", "Culture")),
        Destination(109, "Takwa Ruins", "History", "Manda Island ruins.", "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", emptyList(), null, 4.6f, "Coast", -2.3, 40.9, "Year-round", listOf("History", "Ruins")),
        Destination(110, "Thika Town", "Urban", "Industrial hub.", "https://images.unsplash.com/photo-1506905925346-21bda4d32df4", emptyList(), null, 4.4f, "Central", -1.0, 37.1, "Year-round", listOf("Town", "Falls")),
        Destination(111, "Thimlich Ohinga", "History", "Stone structures.", "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", emptyList(), null, 4.7f, "Western", -0.9, 34.3, "Year-round", listOf("History", "Archaeology")),
        Destination(112, "Tiwi Beach", "Beach", "Secluded beach.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.6f, "Coast", -4.2, 39.6, "Year-round", listOf("Beach", "Coral")),
        Destination(113, "Vasco da Gama Pillar", "History", "Monument.", "https://images.unsplash.com/photo-1506953823976-52e1fdc0149a", emptyList(), null, 4.5f, "Coast", -3.2, 40.1, "Year-round", listOf("History", "Views")),
        Destination(114, "Vipingo Beach", "Beach", "Scenic coast.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.6f, "Coast", -3.8, 39.8, "Year-round", listOf("Beach", "Golf")),
        Destination(115, "Wasini Island", "Beach", "Coral gardens.", "https://images.unsplash.com/photo-1559827260-dc66d52bef19", emptyList(), null, 4.7f, "Coast", -4.7, 39.4, "Year-round", listOf("Snorkeling", "Dolphins"))
    )
}

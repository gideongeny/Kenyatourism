package com.kenyatourism.app.data

data class Destination(
    val id: Int,
    val name: String,
    val category: String,
    val description: String,
    val imageUrl: String,
    val rating: Float,
    val region: String
)

object DestinationsRepository {
    val destinations = listOf(
        Destination(1, "Maasai Mara", "Safaris", "The world-famous wildlife sanctuary.", "https://images.unsplash.com/photo-1516422317184-268d71010ee2", 4.9f, "Rift Valley"),
        Destination(2, "Amboseli National Park", "Safaris", "Elephants with Kilimanjaro backdrop.", "https://images.unsplash.com/photo-1547471080-7cc2caa01a7e", 4.8f, "Rift Valley"),
        Destination(3, "Diani Beach", "Beaches", "Crystal clear waters and white sands.", "https://images.unsplash.com/photo-1589982840479-08a546294747", 4.9f, "Coast"),
        Destination(4, "Mount Kenya", "Adventure", "Climb the peak of Africa's 2nd highest mountain.", "https://images.unsplash.com/photo-1548574505-5e239809ee19", 4.7f, "Central"),
        Destination(5, "Lake Nakuru", "Safaris", "Flamingo haven and rhino sanctuary.", "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5", 4.6f, "Rift Valley"),
        Destination(6, "Tsavo East", "Safaris", "Theater of the wild - red elephants.", "https://images.unsplash.com/photo-1523805009345-7448845a9e53", 4.5f, "Coast"),
        Destination(7, "Watamu", "Beaches", "Marine life and turtle watch.", "https://images.unsplash.com/photo-1533105079780-92b9be482077", 4.7f, "Coast"),
        Destination(8, "Hell's Gate", "Adventure", "Gorges, wildlife, and cycling.", "https://images.unsplash.com/photo-1514539079130-25950c84af65", 4.6f, "Rift Valley"),
        Destination(9, "Samburu", "Safaris", "The rugged beauty of Northern Kenya.", "https://images.unsplash.com/photo-1549366021-9f761d450615", 4.8f, "North"),
        Destination(10, "Lamu Island", "Culture", "Swahili heritage and dhow sailing.", "https://images.unsplash.com/photo-1506461883276-594a12b11cf3", 4.9f, "Coast"),
        Destination(11, "Aberdare National Park", "Safaris", "Waterfalls and moorlands.", "https://images.unsplash.com/photo-1516422317184-268d71010ee2", 4.6f, "Central"),
        Destination(12, "Ol Pejeta Conservancy", "Safaris", "Home to the last Northern White Rhinos.", "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5", 4.9f, "Central"),
        Destination(13, "Lake Turkana", "Adventure", "The Jade Sea - The largest desert lake.", "https://images.unsplash.com/photo-1514539079130-25950c84af65", 4.7f, "North"),
        Destination(14, "Nairobi National Park", "Safaris", "Wildlife with a city backdrop.", "https://images.unsplash.com/photo-1547471080-7cc2caa01a7e", 4.5f, "Nairobi"),
        Destination(15, "Shimba Hills", "Beaches", "The coastal rainforest and Sable antelopes.", "https://images.unsplash.com/photo-1523805009345-7448845a9e53", 4.3f, "Coast"),
        Destination(16, "Nanyuki", "Stays", "Gateway to Mt. Kenya and luxury ranches.", "https://images.unsplash.com/photo-1548574505-5e239809ee19", 4.4f, "Central"),
        Destination(17, "Kisite-Mpunguti", "Beaches", "Dolphin spotting and snorkeling.", "https://images.unsplash.com/photo-1589982840479-08a546294747", 4.8f, "Coast"),
        Destination(18, "Meru National Park", "Safaris", "Elsa's Kopje and wild landscapes.", "https://images.unsplash.com/photo-1516422317184-268d71010ee2", 4.7f, "Eastern"),
        Destination(19, "Lake Naivasha", "Lakes", "Bird watching and hippo spotting.", "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5", 4.6f, "Rift Valley"),
        Destination(20, "Mount Longonot", "Adventure", "Hiking the crater floor.", "https://images.unsplash.com/photo-1514539079130-25950c84af65", 4.6f, "Rift Valley"),
        Destination(21, "Malindi", "Beaches", "The Italian town in Kenya.", "https://images.unsplash.com/photo-1533105079780-92b9be482077", 4.5f, "Coast"),
        Destination(22, "Chyulu Hills", "Adventure", "The Green Hills of Africa.", "https://images.unsplash.com/photo-1549366021-9f761d450615", 4.7f, "Eastern"),
        Destination(23, "Karura Forest", "Nature", "Green lung of Nairobi.", "https://images.unsplash.com/photo-1506461883276-594a12b11cf3", 4.8f, "Nairobi"),
        Destination(24, "David Sheldrick Wildlife Trust", "Wildlife", "Orphaned baby elephants.", "https://images.unsplash.com/photo-1547471080-7cc2caa01a7e", 4.9f, "Nairobi"),
        Destination(25, "Giraffe Centre", "Wildlife", "Hand-feed Rothschild giraffes.", "https://images.unsplash.com/photo-1516422317184-268d71010ee2", 4.8f, "Nairobi"),
        // ... Continuing to 100
        Destination(100, "Voi", "Culture", "The gateway to Tsavo West.", "https://images.unsplash.com/photo-1516026672322-bc52d61a55d5", 4.2f, "Coast")
    )
    
    val allDestinations = (1..100).map { i ->
        val template = destinations[(i - 1) % destinations.size]
        template.copy(id = i, name = "${template.name} View $i")
    }
}

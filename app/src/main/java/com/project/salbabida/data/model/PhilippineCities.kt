package com.project.salbabida.data.model

/**
 * Philippine cities/provinces with their coordinates for map centering
 */
data class PhilippineCity(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

object PhilippineCities {
    val cities = listOf(
        // Bicol Region
        PhilippineCity("Camarines Sur", 13.6252, 123.1826),
        PhilippineCity("Camarines Norte", 14.1389, 122.7632),
        PhilippineCity("Albay", 13.1775, 123.5280),
        PhilippineCity("Sorsogon", 12.9742, 124.0058),
        PhilippineCity("Masbate", 12.3675, 123.6196),
        PhilippineCity("Catanduanes", 13.7089, 124.2422),
        
        // Metro Manila
        PhilippineCity("Metro Manila", 14.5995, 120.9842),
        
        // Luzon
        PhilippineCity("Batangas", 13.7565, 121.0583),
        PhilippineCity("Laguna", 14.2691, 121.4113),
        PhilippineCity("Cavite", 14.2456, 120.8786),
        PhilippineCity("Rizal", 14.6037, 121.3084),
        PhilippineCity("Bulacan", 14.7942, 120.8799),
        PhilippineCity("Pampanga", 15.0794, 120.6200),
        PhilippineCity("Zambales", 15.5082, 119.9698),
        PhilippineCity("Pangasinan", 15.8949, 120.2863),
        PhilippineCity("La Union", 16.6159, 120.3209),
        PhilippineCity("Ilocos Sur", 17.2280, 120.5740),
        PhilippineCity("Ilocos Norte", 18.1647, 120.7116),
        PhilippineCity("Cagayan", 17.6132, 121.7270),
        PhilippineCity("Isabela", 16.9754, 121.8107),
        PhilippineCity("Nueva Vizcaya", 16.3301, 121.1710),
        PhilippineCity("Quirino", 16.2900, 121.5370),
        PhilippineCity("Aurora", 15.9784, 121.6323),
        PhilippineCity("Nueva Ecija", 15.5784, 121.1113),
        PhilippineCity("Tarlac", 15.4755, 120.5963),
        
        // Visayas
        PhilippineCity("Cebu", 10.3157, 123.8854),
        PhilippineCity("Bohol", 9.8500, 124.0000),
        PhilippineCity("Leyte", 10.3725, 124.9815),
        PhilippineCity("Samar", 11.5930, 125.0250),
        PhilippineCity("Iloilo", 10.7202, 122.5621),
        PhilippineCity("Negros Occidental", 10.4113, 123.0438),
        PhilippineCity("Negros Oriental", 9.3092, 123.3051),
        PhilippineCity("Aklan", 11.8166, 122.0942),
        PhilippineCity("Capiz", 11.5530, 122.7405),
        PhilippineCity("Antique", 11.3682, 121.9497),
        PhilippineCity("Guimaras", 10.5894, 122.6277),
        
        // Mindanao
        PhilippineCity("Davao del Sur", 6.7674, 125.3593),
        PhilippineCity("Davao del Norte", 7.5619, 125.6549),
        PhilippineCity("Davao Oriental", 7.3172, 126.5420),
        PhilippineCity("Davao Occidental", 6.1055, 125.6083),
        PhilippineCity("Davao de Oro", 7.3117, 126.1747),
        PhilippineCity("Zamboanga del Sur", 7.8383, 123.2968),
        PhilippineCity("Zamboanga del Norte", 8.1527, 123.2577),
        PhilippineCity("Zamboanga Sibugay", 7.5222, 122.8198),
        PhilippineCity("Bukidnon", 8.0515, 125.0980),
        PhilippineCity("Misamis Oriental", 8.5046, 124.6220),
        PhilippineCity("Misamis Occidental", 8.3375, 123.7071),
        PhilippineCity("Lanao del Norte", 8.0730, 123.8857),
        PhilippineCity("Lanao del Sur", 7.8232, 124.4365),
        PhilippineCity("South Cotabato", 6.2969, 124.8533),
        PhilippineCity("North Cotabato", 7.1436, 124.8511),
        PhilippineCity("Sultan Kudarat", 6.5069, 124.4198),
        PhilippineCity("Sarangani", 5.9263, 125.2880),
        PhilippineCity("General Santos", 6.1164, 125.1716),
        PhilippineCity("Agusan del Norte", 8.9456, 125.5319),
        PhilippineCity("Agusan del Sur", 8.1527, 126.0165),
        PhilippineCity("Surigao del Norte", 9.7877, 125.4960),
        PhilippineCity("Surigao del Sur", 8.7512, 126.1378),
        PhilippineCity("Dinagat Islands", 10.1280, 125.6082),
        
        // Palawan
        PhilippineCity("Palawan", 9.8349, 118.7384),
    ).sortedBy { it.name }
    
    fun findByName(name: String): PhilippineCity? {
        return cities.find { it.name.equals(name, ignoreCase = true) }
    }
    
    fun getDefault(): PhilippineCity {
        return cities.find { it.name == "Camarines Sur" } ?: cities.first()
    }
}

package sv.edu.udb.desafio02.model

data class Destination(
    var id: String = "",
    var name: String = "",
    var country: String = "",
    var price: Double = 0.0,
    var description: String = "",
    var imagePath: String = ""
)
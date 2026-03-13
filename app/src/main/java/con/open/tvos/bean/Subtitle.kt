package con.open.tvos.bean

data class Subtitle(
    var name: String? = null,
    var url: String? = null,
    var isZip: Boolean = false
) {
    override fun toString(): String {
        return "Subtitle{name='$name', url='$url', isZip=$isZip}"
    }
}

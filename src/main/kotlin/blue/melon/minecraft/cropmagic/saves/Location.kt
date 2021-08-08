package blue.melon.minecraft.cropmagic.saves

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Location(
    @Expose @SerializedName("x") val x: Int,
    @Expose @SerializedName("y") val y: Int,
    @Expose @SerializedName("z") val z: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Location

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }
}

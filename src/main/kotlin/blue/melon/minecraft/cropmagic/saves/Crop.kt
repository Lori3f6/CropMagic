package blue.melon.minecraft.cropmagic.saves

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Crop(
    @Expose @SerializedName("location") val location: Location,
    @Expose @SerializedName("ticksToNextStage") val ticksToNextStage: Int


) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Crop

        if (location != other.location) return false
        if (ticksToNextStage != other.ticksToNextStage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = location.hashCode()
        result = 31 * result + ticksToNextStage
        return result
    }
}

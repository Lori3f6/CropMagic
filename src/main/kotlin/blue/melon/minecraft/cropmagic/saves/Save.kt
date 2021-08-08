package blue.melon.minecraft.cropmagic.saves

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Save(
    @Expose @SerializedName("speedFactor") var speedFactor: Double,
    @Expose @SerializedName("cropList") val cropList: ArrayList<Crop>
)

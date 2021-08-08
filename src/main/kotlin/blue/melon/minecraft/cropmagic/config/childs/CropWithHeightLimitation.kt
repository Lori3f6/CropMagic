package blue.melon.minecraft.cropmagic.config.childs

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class CropWithHeightLimitation(
    @Expose
    @SerializedName("ticksToNextGrow")
    val ticksToNextGrow: Int,
    @Expose
    @SerializedName("heightLimitation")
    val heightLimitation: Int
)

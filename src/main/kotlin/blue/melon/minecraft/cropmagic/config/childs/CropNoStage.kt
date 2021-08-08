package blue.melon.minecraft.cropmagic.config.childs

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class CropNoStage(
    @Expose
    @SerializedName("ticksToGrowUp")
    val ticksToGrowUp: Int
)

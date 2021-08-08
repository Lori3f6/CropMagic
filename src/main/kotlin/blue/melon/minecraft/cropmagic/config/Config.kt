package blue.melon.minecraft.cropmagic.config

import blue.melon.minecraft.cropmagic.config.childs.CropNoStage
import blue.melon.minecraft.cropmagic.config.childs.CropWithHeightLimitation
import blue.melon.minecraft.cropmagic.config.childs.CropWithStage
import blue.melon.minecraft.cropmagic.config.childs.StemWithStage
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Config(
    @Expose
    @SerializedName("world") val world: String,
    @Expose
    @SerializedName("DryFarmlandGrowthFactor") val dryFarmlandGrowthFactor: Double,
    @Expose
    @SerializedName("wheat") val wheat: CropWithStage,
    @Expose
    @SerializedName("potatoes") val potatoes: CropWithStage,
    @Expose
    @SerializedName("carrots") val carrots: CropWithStage,
    @Expose
    @SerializedName("beetroots") val beetroots: CropWithStage,
    @Expose
    @SerializedName("sweetBerries") val sweetBerries: CropWithStage,
    @Expose
    @SerializedName("pumpkinStem") val pumpkinStem: StemWithStage,
    @Expose
    @SerializedName("melonStem") val melonStem: StemWithStage,
    @Expose
    @SerializedName("sugarCane") val sugarCane: CropWithHeightLimitation,
    @Expose
    @SerializedName("saplings") val sapling: CropNoStage,
    @Expose
    @SerializedName("pigs") val pigs: CropNoStage,
    @Expose
    @SerializedName("cows") val cows: CropNoStage,
    @Expose
    @SerializedName("sheep") val sheep: CropNoStage,
    @Expose
    @SerializedName("chickens") val chickens: CropNoStage,
    @Expose
    @SerializedName("hoglins") val hoglins: CropNoStage

)
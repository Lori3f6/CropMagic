package blue.melon.minecraft.cropmagic

import blue.melon.minecraft.cropmagic.config.Config
import blue.melon.minecraft.cropmagic.saves.Crop
import blue.melon.minecraft.cropmagic.saves.Location
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Ageable
import org.bukkit.block.data.Directional
import org.bukkit.block.data.type.Farmland
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.random.Random

class TickCore constructor(
    cropList: List<Crop>, var speedFactor: Double, val config: Config, pluginInstance: JavaPlugin
) {
    companion object {
        val GROW_SPEED_UP_COLOUR = ChatColor.of("#e15e43")
        val GROW_SLOW_DOWN_COLOUR = ChatColor.of("#43e1a0")
    }

    private val cropMap: ConcurrentMap<Location, Double>
    private val world: World
    private val tree = arrayOf(
        arrayOf(
            "     ".toCharArray(),
            "     ".toCharArray(),
            "  x  ".toCharArray(),
            "     ".toCharArray(),
            "     ".toCharArray()
        ),
        arrayOf(
            "r000r".toCharArray(),
            "00000".toCharArray(),
            "00x00".toCharArray(),
            "00000".toCharArray(),
            "r000r".toCharArray()
        ),
        arrayOf(
            "r000r".toCharArray(),
            "00000".toCharArray(),
            "00x00".toCharArray(),
            "00000".toCharArray(),
            "r000r".toCharArray()
        ),
        arrayOf(
            "     ".toCharArray(),
            " r0r ".toCharArray(),
            " 0x0 ".toCharArray(),
            " r0r ".toCharArray(),
            "     ".toCharArray()
        ),
        arrayOf(
            "     ".toCharArray(),
            "  0  ".toCharArray(),
            " 000 ".toCharArray(),
            "  0  ".toCharArray(),
            "     ".toCharArray()
        )
    )

    init {
        cropMap = ConcurrentHashMap()
        world = Bukkit.getWorld(config.world)!!
        cropList.forEach { crop -> cropMap[crop.location] = crop.ticksToNextStage.toDouble() }
        Bukkit.getScheduler().runTaskTimer(pluginInstance, Runnable {
            for (location in cropMap.keys) {
                val cropBlock = world.getBlockAt(location.x, location.y, location.z)
                if (cropMap[location]!! <= 0) {
                    when (cropBlock.type) {
                        Material.WHEAT,
                        Material.CARROTS,
                        Material.POTATOES,
                        Material.BEETROOTS,
                        Material.SWEET_BERRY_BUSH -> {
                            val cropData = cropBlock.blockData as Ageable
                            if (cropData.age != cropData.maximumAge) {
                                cropData.age++
                                cropBlock.blockData = cropData
                                cropMap[location] = when (cropBlock.type) {
                                    Material.WHEAT -> config.wheat.ticksPerStage.toDouble()
                                    Material.CARROTS -> config.carrots.ticksPerStage.toDouble()
                                    Material.POTATOES -> config.potatoes.ticksPerStage.toDouble()
                                    Material.BEETROOTS -> config.beetroots.ticksPerStage.toDouble()
                                    /*Material.SWEET_BERRY_BUSH*/ else -> config.sweetBerries.ticksPerStage.toDouble()
                                }
                            }
                        }
                        Material.PUMPKIN_STEM,
                        Material.MELON_STEM -> {
                            val cropData = cropBlock.blockData as Ageable
                            if (cropData.age != cropData.maximumAge) {
                                cropData.age++
                                cropBlock.blockData = cropData
                                if (cropData.age != cropData.maximumAge) {
                                    cropMap[location] =
                                        if (cropBlock.type == Material.PUMPKIN_STEM) config.pumpkinStem.ticksPerStage.toDouble() else config.melonStem.ticksPerStage.toDouble()
                                } else {
                                    cropMap[location] =
                                        if (cropBlock.type == Material.PUMPKIN_STEM) config.pumpkinStem.ticksPerFruit.toDouble() else config.melonStem.ticksPerFruit.toDouble()
                                }
                            } else {
                                val fruitOffsite = findFruitOffsite(world, location)
                                if (fruitOffsite == null) {
                                    cropMap[location] =
                                        (if (cropBlock.type == Material.PUMPKIN_STEM) config.pumpkinStem.ticksPerFruit.toDouble() else config.melonStem.ticksPerFruit.toDouble()) / 3
                                } else {
                                    cropBlock.type =
                                        if (cropBlock.type == Material.PUMPKIN_STEM) Material.ATTACHED_PUMPKIN_STEM else Material.ATTACHED_MELON_STEM
                                    val cropFacingData = cropBlock.blockData as Directional
                                    (cropFacingData).facing = when {
                                        fruitOffsite.x == 1 -> BlockFace.EAST
                                        fruitOffsite.z == 1 -> BlockFace.SOUTH
                                        fruitOffsite.x == -1 -> BlockFace.WEST
                                        /*fruitOffsite.z == 1*/ else -> BlockFace.NORTH
                                    }
                                    cropBlock.blockData = cropFacingData
                                    world.getBlockAt(
                                        cropBlock.x + fruitOffsite.x,
                                        cropBlock.y + fruitOffsite.y,
                                        cropBlock.z + fruitOffsite.z
                                    ).type =
                                        if (cropBlock.type == Material.ATTACHED_PUMPKIN_STEM) Material.PUMPKIN else Material.MELON
                                    cropMap[location] =
                                        if (cropBlock.type == Material.ATTACHED_PUMPKIN_STEM) config.pumpkinStem.ticksPerFruit.toDouble() else config.melonStem.ticksPerFruit.toDouble()
                                }
                            }
                        }
                        Material.SUGAR_CANE -> {
                            val caneOffsite = findSugarCaneOffsite(world, location)
                            if (caneOffsite != null) {
                                world.getBlockAt(location.x, location.y + caneOffsite.y, location.z).type =
                                    Material.SUGAR_CANE
                            }
                            cropMap[location] = config.sugarCane.ticksToNextGrow.toDouble()
                        }
                        Material.OAK_SAPLING -> {
                            generateTreeAt(cropBlock.location)
                            cropMap.remove(location)
                            continue
//                            cropBlock.type = Material.AIR
//                            if (world.generateTree(cropBlock.location, TreeType.TREE)) {
//                                cropMap.remove(location)
//                                continue
//                            } else {
//                                cropBlock.type = Material.OAK_SAPLING
//                                cropMap[location] = config.sapling.ticksToGrowUp.toDouble() / 3
//                            }
                        }
                        else -> {
                            cropMap.remove(location)
//                            pluginInstance.logger.warning("A record of exist crop(located at: ${location}, type: ${cropBlock.type}) has been removed")
                            continue
                        }
                    }
                }

                val tickToNextStage = cropMap[location]!!

                when (cropBlock.type) {
                    Material.WHEAT,
                    Material.BEETROOTS,
                    Material.CARROTS,
                    Material.POTATOES -> {
                        val cropData = cropBlock.blockData as Ageable
                        if (cropData.age != cropData.maximumAge) {
                            val baseBlock = world.getBlockAt(location.x, location.y - 1, location.z)
                            if (baseBlock.type == Material.FARMLAND) {
                                if ((baseBlock.blockData as Farmland).moisture == 7) {
                                    cropMap[location] = tickToNextStage - 1 * speedFactor
                                } else {
                                    cropMap[location] =
                                        tickToNextStage - 1 * speedFactor * config.dryFarmlandGrowthFactor
                                }
                            } else {
                                cropMap.remove(location)
                                continue
                            }
                        }
                    }
                    Material.SWEET_BERRY_BUSH -> {
                        val cropData = cropBlock.blockData as Ageable
                        if (cropData.age != cropData.maximumAge) {
                            cropMap[location] = tickToNextStage - 1 * speedFactor
                        }
                    }
                    Material.PUMPKIN_STEM,
                    Material.MELON_STEM -> {
                        val baseBlock = world.getBlockAt(location.x, location.y - 1, location.z)
                        if (baseBlock.type == Material.FARMLAND) {
                            if ((baseBlock.blockData as Farmland).moisture == 7) {
                                cropMap[location] = tickToNextStage - 1 * speedFactor
                            } else {
                                cropMap[location] =
                                    tickToNextStage - 1 * speedFactor * config.dryFarmlandGrowthFactor
                            }
                        } else {
                            cropMap.remove(location)
                            continue
                        }
                    }
                    Material.SUGAR_CANE -> {
                        val sugarCaneOffsite = findSugarCaneOffsite(world, location)
                        if (sugarCaneOffsite != null && sugarCaneOffsite.y + 1 <= config.sugarCane.heightLimitation) {
                            cropMap[location] = tickToNextStage - 1 * speedFactor
                        }
                    }
                    Material.OAK_SAPLING -> {
                        cropMap[location] = tickToNextStage - 1 * speedFactor
                    }
                    Material.ATTACHED_PUMPKIN_STEM,
                    Material.ATTACHED_MELON_STEM -> {
                        //do nothing
                    }
                    else -> {
                        cropMap.remove(location)
//                        pluginInstance.logger.warning("A record of exist crop(located at: ${location}, type: ${cropBlock.type}) has been removed")
                        continue
                    }
                }
            }
        }, 0L, 1L)
    }

    private fun findFruitOffsite(world: World, centre: Location): Location? {
        return when {
            world.getBlockAt(centre.x + 1, centre.y - 1, centre.z).type.isSolid && world.getBlockAt(
                centre.x + 1,
                centre.y - 1,
                centre.z
            ).type != Material.FARMLAND && world.getBlockAt(
                centre.x + 1,
                centre.y,
                centre.z
            ).isEmpty -> Location(1, 0, 0)
            world.getBlockAt(centre.x, centre.y - 1, centre.z + 1).type.isSolid && world.getBlockAt(
                centre.x,
                centre.y - 1,
                centre.z + 1
            ).type != Material.FARMLAND && world.getBlockAt(
                centre.x,
                centre.y,
                centre.z + 1
            ).isEmpty -> Location(0, 0, 1)
            world.getBlockAt(centre.x - 1, centre.y - 1, centre.z).type.isSolid && world.getBlockAt(
                centre.x - 1,
                centre.y - 1,
                centre.z
            ).type != Material.FARMLAND && world.getBlockAt(
                centre.x - 1,
                centre.y,
                centre.z
            ).isEmpty -> Location(-1, 0, 0)
            world.getBlockAt(centre.x, centre.y - 1, centre.z - 1).type.isSolid && world.getBlockAt(
                centre.x,
                centre.y - 1,
                centre.z - 1
            ).type != Material.FARMLAND && world.getBlockAt(
                centre.x,
                centre.y,
                centre.z - 1
            ).isEmpty -> Location(0, 0, -1)
            else -> null
        }
    }

    private fun generateTreeAt(location: org.bukkit.Location) {
        for (y in 0..4) {
            for (x in 0..4) {
                for (z in 0..4) {
                    location.world!!.getBlockAt(
                        location.blockX + x - 2,
                        location.blockY + y,
                        location.blockZ + z - 2
                    ).type =
                        when (tree[y][z][x]) {
                            'x' -> Material.OAK_LOG
                            '0' -> Material.OAK_LEAVES
                            'r' -> if (Random.nextBoolean()) Material.OAK_LEAVES else Material.AIR
                            else -> continue
                        }
                }
            }
        }
    }

    private fun findSugarCaneOffsite(world: World, centre: Location): Location? {
        var height = centre.y + 1
        while (height < 256) {
            if (world.getBlockAt(centre.x, height, centre.z).type.isAir)
                return Location(centre.x, height - centre.y, centre.z)
            height++
        }
        return null
    }

    fun addCrop(location: Location, tickToNextStage: Double) {
        cropMap[location] = tickToNextStage
    }

    fun getCrop(location: Location): Double? {
        return cropMap[location]
    }

    fun getCorpList(): ArrayList<Crop> {
        val corpList = ArrayList<Crop>()
        cropMap.forEach { (location, ticksToNextStage) ->
            corpList.add(Crop(location, ticksToNextStage.toInt()))
        }
        return corpList
    } //肥肥说：臭汪汪汪汪叫
}

package blue.melon.minecraft.cropmagic

import blue.melon.minecraft.cropmagic.saves.Location
import net.md_5.bungee.api.ChatColor
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.BlockFace
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.player.PlayerHarvestBlockEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.world.StructureGrowEvent
import org.bukkit.plugin.java.JavaPlugin
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.random.Random

class EventListener(private val tickCore: TickCore, private val pluginInstance: JavaPlugin) : Listener {
    private val df = DecimalFormat("0.0")

    init {
        df.roundingMode = RoundingMode.HALF_UP
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (event.player.isOp) {
            pluginInstance.server.scheduler.runTaskLater(pluginInstance, Runnable {
                event.player.sendMessage("")
                event.player.sendMessage("${if (tickCore.speedFactor >= 1.0) TickCore.GROW_SPEED_UP_COLOUR else TickCore.GROW_SLOW_DOWN_COLOUR}管理员${ChatColor.WHITE}提醒：")
                event.player.sendMessage("当前的农作物全局生长系数是 ${if (tickCore.speedFactor >= 1.0) TickCore.GROW_SPEED_UP_COLOUR else TickCore.GROW_SLOW_DOWN_COLOUR}${tickCore.speedFactor}")
                event.player.sendMessage("如果不符合预期，可以使用 ${if (tickCore.speedFactor >= 1.0) TickCore.GROW_SPEED_UP_COLOUR else TickCore.GROW_SLOW_DOWN_COLOUR}/grow <speedFactor:Double> ${ChatColor.WHITE}重新设置哦~")
                event.player.sendMessage("")
            }, 40L)
        }
        if (tickCore.speedFactor != 1.0) {
            event.player.sendTitle(
                when {
                    tickCore.speedFactor > 1.0 -> "农作物生长${TickCore.GROW_SPEED_UP_COLOUR}加速${ChatColor.WHITE}中"
                    /*tickCore.speedFactor < 1.0*/else -> "农作物生长${TickCore.GROW_SLOW_DOWN_COLOUR}偷懒${ChatColor.WHITE}中"
                }, when {
                    tickCore.speedFactor > 1.0 -> "正以 ${TickCore.GROW_SPEED_UP_COLOUR}${tickCore.speedFactor} ${ChatColor.WHITE}倍速生长哦~"
                    tickCore.speedFactor == 0.0 -> "生长${TickCore.GROW_SLOW_DOWN_COLOUR}暂停${ChatColor.WHITE}了哦~"
                    /*tickCore.speedFactor < 1.0*/ else -> "正以 ${TickCore.GROW_SLOW_DOWN_COLOUR}${tickCore.speedFactor} ${ChatColor.WHITE}倍速生长哦~"
                }, 10, 50, 20
            )
        }
    }

    @EventHandler
    fun onCropsGrow(event: BlockGrowEvent) {
        if (event.newState.type == Material.MELON || event.newState.type == Material.PUMPKIN || event.newState.type == Material.SUGAR_CANE) {
            event.isCancelled = true
        }
        tickCore.getCrop(Location(event.block.x, event.block.y, event.block.z)) ?: return
        event.isCancelled = true
    }

    @EventHandler
    fun onTreeGrowUp(event: StructureGrowEvent) {
        tickCore.getCrop(
            Location(
                event.location.blockX,
                event.location.blockY,
                event.location.blockZ
            )
        ) ?: return
        event.isCancelled = true

    }

    @EventHandler
    fun onPlayerGrowPlantOnFarmland(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = event.clickedBlock!!
        if (clickedBlock.type != Material.FARMLAND) return
        if (event.blockFace != BlockFace.UP) return
        val blockToGrowPlant = clickedBlock.world.getBlockAt(clickedBlock.x, clickedBlock.y + 1, clickedBlock.z)
        if (!blockToGrowPlant.type.isAir) return
        when (event.material) {
            Material.WHEAT_SEEDS,
            Material.BEETROOT_SEEDS,
            Material.CARROT,
            Material.POTATO,
            Material.PUMPKIN_SEEDS,
            Material.MELON_SEEDS -> {
//                event.player.sendMessage("你刚才在 $blockToGrowPlant 种了一个 ${event.material}")
                tickCore.addCrop(
                    Location(blockToGrowPlant.x, blockToGrowPlant.y, blockToGrowPlant.z),
                    when (event.material) {
                        Material.WHEAT_SEEDS -> tickCore.config.wheat.ticksPerStage.toDouble()
                        Material.BEETROOT_SEEDS -> tickCore.config.beetroots.ticksPerStage.toDouble()
                        Material.CARROT -> tickCore.config.carrots.ticksPerStage.toDouble()
                        Material.POTATO -> tickCore.config.potatoes.ticksPerStage.toDouble()
                        Material.PUMPKIN_SEEDS -> tickCore.config.pumpkinStem.ticksPerStage.toDouble()
                        /*Material.MELON_SEEDS*/ else -> tickCore.config.melonStem.ticksPerStage.toDouble()
                    }
                )
            }
            else -> return
        }

    }

    @EventHandler
    fun onPlayerUseBoneMeal(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = event.clickedBlock!!
        val item = event.item ?: return
        if (item.type != Material.BONE_MEAL) return
        event.isCancelled = true
        val cropLoc = Location(clickedBlock.x, clickedBlock.y, clickedBlock.z)
        when (clickedBlock.type) {
            Material.WHEAT -> {
                event.isCancelled = true
                val blockData = clickedBlock.blockData as Ageable
                if (blockData.age == blockData.maximumAge) return
                if (event.player.gameMode != GameMode.CREATIVE) {
                    item.amount -= 1
                    event.player.inventory.setItem(event.hand!!, item)
                }
                var age = blockData.age + Random.nextInt(2, 6/*exclusive*/)
                if (age > blockData.maximumAge) age = blockData.maximumAge
                blockData.age = age
                clickedBlock.blockData = blockData
                tickCore.addCrop(
                    cropLoc,
                    tickCore.config.wheat.ticksPerStage.toDouble()
                )
                clickedBlock.world.spawnParticle(
                    Particle.FALLING_DUST,
                    clickedBlock.location.x + 0.5,
                    clickedBlock.location.y + 0.5,
                    clickedBlock.location.z + 0.5, 23, 1.0, 1.0,
                    1.0, Material.LIME_WOOL.createBlockData()
                )
            }
            Material.CARROTS -> {
                event.isCancelled = true
                val blockData = clickedBlock.blockData as Ageable
                if (blockData.age == blockData.maximumAge) return
                if (event.player.gameMode != GameMode.CREATIVE) {
                    item.amount -= 1
                    event.player.inventory.setItem(event.hand!!, item)
                }
                var age = blockData.age + Random.nextInt(2, 6/*exclusive*/)
                if (age > blockData.maximumAge) age = blockData.maximumAge
                blockData.age = age
                clickedBlock.blockData = blockData
                tickCore.addCrop(
                    cropLoc,
                    tickCore.config.carrots.ticksPerStage.toDouble()
                )
                clickedBlock.world.spawnParticle(
                    Particle.FALLING_DUST,
                    clickedBlock.location.x + 0.5,
                    clickedBlock.location.y + 0.5,
                    clickedBlock.location.z + 0.5, 23, 1.0, 1.0,
                    1.0, Material.RED_WOOL.createBlockData()
                )
            }
            Material.POTATOES -> {
                event.isCancelled = true
                val blockData = clickedBlock.blockData as Ageable
                if (blockData.age == blockData.maximumAge) return
                if (event.player.gameMode != GameMode.CREATIVE) {
                    item.amount -= 1
                    event.player.inventory.setItem(event.hand!!, item)
                }
                var age = blockData.age + Random.nextInt(2, 6/*exclusive*/)
                if (age > blockData.maximumAge) age = blockData.maximumAge
                blockData.age = age
                clickedBlock.blockData = blockData
                tickCore.addCrop(
                    cropLoc,
                    tickCore.config.potatoes.ticksPerStage.toDouble()
                )
                clickedBlock.world.spawnParticle(
                    Particle.FALLING_DUST,
                    clickedBlock.location.x + 0.5,
                    clickedBlock.location.y + 0.5,
                    clickedBlock.location.z + 0.5, 23, 1.0, 1.0,
                    1.0, Material.LIME_WOOL.createBlockData()
                )

            }
            Material.SWEET_BERRY_BUSH -> {
                event.isCancelled = true
                val blockData = clickedBlock.blockData as Ageable
                if (blockData.age == blockData.maximumAge) return
                if (event.player.gameMode != GameMode.CREATIVE) {
                    item.amount -= 1
                    event.player.inventory.setItem(event.hand!!, item)
                }
                blockData.age += 1
                clickedBlock.blockData = blockData
                tickCore.addCrop(
                    cropLoc,
                    tickCore.config.sweetBerries.ticksPerStage.toDouble()
                )
                clickedBlock.world.spawnParticle(
                    Particle.FALLING_DUST,
                    clickedBlock.location.x + 0.5,
                    clickedBlock.location.y + 0.5,
                    clickedBlock.location.z + 0.5, 23, 1.0, 1.0,
                    1.0, Material.RED_WOOL.createBlockData()
                )

            }
            Material.BEETROOTS -> {
                event.isCancelled = true
                val blockData = clickedBlock.blockData as Ageable
                if (blockData.age == blockData.maximumAge) return
                if (event.player.gameMode != GameMode.CREATIVE) {
                    item.amount -= 1
                    event.player.inventory.setItem(event.hand!!, item)
                }
                if (Random.nextDouble() > 0.75) return
                blockData.age += 1
                clickedBlock.blockData = blockData
                tickCore.addCrop(
                    cropLoc,
                    tickCore.config.beetroots.ticksPerStage.toDouble()
                )
                clickedBlock.world.spawnParticle(
                    Particle.FALLING_DUST,
                    clickedBlock.location.x + 0.5,
                    clickedBlock.location.y + 0.5,
                    clickedBlock.location.z + 0.5, 23, 1.0, 1.0,
                    1.0, Material.RED_WOOL.createBlockData()
                )

            }
            Material.MELON_STEM -> {
                event.isCancelled = true
                val blockData = clickedBlock.blockData as Ageable
                if (blockData.age == blockData.maximumAge) return
                if (event.player.gameMode != GameMode.CREATIVE) {
                    item.amount -= 1
                    event.player.inventory.setItem(event.hand!!, item)
                }
                var age = blockData.age + Random.nextInt(2, 6/*exclusive*/)
                if (age > blockData.maximumAge) age = blockData.maximumAge
                blockData.age = age
                clickedBlock.blockData = blockData
                tickCore.addCrop(
                    cropLoc,
                    if ((clickedBlock.blockData as Ageable).age != (clickedBlock.blockData as Ageable).maximumAge) tickCore.config.melonStem.ticksPerStage.toDouble() else tickCore.config.melonStem.ticksPerFruit.toDouble()
                )
                clickedBlock.world.spawnParticle(
                    Particle.FALLING_DUST,
                    clickedBlock.location.x + 0.5,
                    clickedBlock.location.y + 0.5,
                    clickedBlock.location.z + 0.5, 23, 1.0, 1.0,
                    1.0, Material.LIME_WOOL.createBlockData()
                )
            }
            Material.PUMPKIN_STEM -> {
                event.isCancelled = true
                val blockData = clickedBlock.blockData as Ageable
                if (blockData.age == blockData.maximumAge) return
                if (event.player.gameMode != GameMode.CREATIVE) {
                    item.amount -= 1
                    event.player.inventory.setItem(event.hand!!, item)
                }
                var age = blockData.age + Random.nextInt(2, 6/*exclusive*/)
                if (age > 7) age = 7
                blockData.age = age
                clickedBlock.blockData = blockData
                tickCore.addCrop(
                    cropLoc,
                    if ((clickedBlock.blockData as Ageable).age != (clickedBlock.blockData as Ageable).maximumAge) tickCore.config.pumpkinStem.ticksPerStage.toDouble() else tickCore.config.pumpkinStem.ticksPerFruit.toDouble()
                )
                clickedBlock.world.spawnParticle(
                    Particle.FALLING_DUST,
                    clickedBlock.location.x + 0.5,
                    clickedBlock.location.y + 0.5,
                    clickedBlock.location.z + 0.5, 23, 1.0, 1.0,
                    1.0, Material.LIME_WOOL.createBlockData()
                )
            }
            Material.OAK_SAPLING -> {
                event.isCancelled = true
                if (event.player.gameMode != GameMode.CREATIVE) {
                    item.amount -= 1
                    event.player.inventory.setItem(event.hand!!, item)
                }
                if (Random.nextDouble() > 0.45) return
                val tickToGrowUp = tickCore.getCrop(cropLoc)
                if (tickToGrowUp == null) {
                    tickCore.addCrop(cropLoc, tickCore.config.sapling.ticksToGrowUp.toDouble() / 2)
                } else {
                    tickCore.addCrop(cropLoc, tickToGrowUp - tickCore.config.sapling.ticksToGrowUp.toDouble() / 2)
                }
                event.player.sendMessage("remain: ${tickCore.getCrop(cropLoc)}")
                clickedBlock.world.spawnParticle(
                    Particle.FALLING_DUST,
                    clickedBlock.location.x + 0.5,
                    clickedBlock.location.y + 0.5,
                    clickedBlock.location.z + 0.5, 23, 1.0, 1.0,
                    1.0, Material.LIME_WOOL.createBlockData()
                )
            }
            else -> {
                return
            }
        }
    }

    @EventHandler
    fun onPlayerGrowSugarCane(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = event.clickedBlock!!
        if (clickedBlock.type != Material.RED_SAND) return
        if (event.blockFace != BlockFace.UP) return
        if (event.material != Material.SUGAR_CANE) return
        val blockToGrowPlant = clickedBlock.world.getBlockAt(clickedBlock.x, clickedBlock.y + 1, clickedBlock.z)
        if (!blockToGrowPlant.type.isAir) return
//        event.player.sendMessage("你刚才在 $blockToGrowPlant 种了一个 ${event.material}")
        tickCore.addCrop(
            Location(blockToGrowPlant.x, blockToGrowPlant.y, blockToGrowPlant.z),
            tickCore.config.sugarCane.ticksToNextGrow.toDouble()
        )
    }

    @EventHandler
    fun onPlayerPlantTree(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val clickedBlock = event.clickedBlock!!
        if (clickedBlock.type != Material.COARSE_DIRT) return
        if (event.blockFace != BlockFace.UP) return
        if (event.material != Material.OAK_SAPLING) return
        val blockToGrowPlant = clickedBlock.world.getBlockAt(clickedBlock.x, clickedBlock.y + 1, clickedBlock.z)
        if (!blockToGrowPlant.type.isAir) return
//        event.player.sendMessage("你刚才在 $blockToGrowPlant 种了一个 ${event.material}")
        tickCore.addCrop(
            Location(blockToGrowPlant.x, blockToGrowPlant.y, blockToGrowPlant.z),
            tickCore.config.sapling.ticksToGrowUp.toDouble()
        )
    }

    @EventHandler
    fun onPlayerTakeSweetBerry(event: PlayerHarvestBlockEvent) {
        if (event.harvestedBlock.type != Material.SWEET_BERRY_BUSH) return
//        event.player.sendMessage("你刚才在 ${event.harvestedBlock.location} 收获了 ${event.itemsHarvested}")
        tickCore.addCrop(
            Location(event.harvestedBlock.x, event.harvestedBlock.y, event.harvestedBlock.z),
            tickCore.config.sweetBerries.ticksPerStage.toDouble()
        )
    }
}
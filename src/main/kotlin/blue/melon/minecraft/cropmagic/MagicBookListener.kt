package blue.melon.minecraft.cropmagic

import blue.melon.minecraft.cropmagic.saves.Location
import net.md_5.bungee.api.ChatColor
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.block.data.type.Farmland
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import java.math.RoundingMode
import java.text.DecimalFormat


class MagicBookListener(private val tickCore: TickCore) : Listener {
    private val df = DecimalFormat("0.0")

    init {
        df.roundingMode = RoundingMode.HALF_UP
    }

    @EventHandler
    fun onPlayerClickAtCrop(event: PlayerInteractEvent) {
        val item = event.item ?: return
        val block = event.clickedBlock ?: return
        if (item.type != Material.BOOK) return
        val dur = item.enchantments[Enchantment.DURABILITY]
        val inf = item.enchantments[Enchantment.ARROW_INFINITE]
        if (!(dur != null && inf != null && dur == 1 && inf == 1)) return
        val tickToNextStage = tickCore.getCrop(Location(block.x, block.y, block.z)) ?: return
        val total: Double
        val toGrow: Double
        val speed: Double
        when (block.type) {
            Material.WHEAT,
            Material.POTATOES,
            Material.CARROTS,
            Material.BEETROOTS,
            Material.SWEET_BERRY_BUSH -> {
                val data = block.blockData as Ageable
                val ticksPerStage = when (block.type) {
                    Material.WHEAT -> tickCore.config.wheat.ticksPerStage.toDouble()
                    Material.POTATOES -> tickCore.config.potatoes.ticksPerStage.toDouble()
                    Material.CARROTS -> tickCore.config.carrots.ticksPerStage.toDouble()
                    Material.BEETROOTS -> tickCore.config.beetroots.ticksPerStage.toDouble()
                    /*Material.SWEET_BERRY_BUSH*/else -> tickCore.config.sweetBerries.ticksPerStage.toDouble()
                }
                val stageDone = data.age
                val stageToGrow = data.maximumAge - stageDone - 1
                if (data.age < data.maximumAge) {
                    total = data.maximumAge * ticksPerStage
                    toGrow = stageToGrow * ticksPerStage + tickToNextStage
                } else {
                    total = ticksPerStage
                    toGrow = 0.0
                }
                val baseBlock = block.world.getBlockAt(block.x, block.y - 1, block.z)
                speed = if (block.type != Material.SWEET_BERRY_BUSH) {
                    if ((baseBlock.blockData as Farmland).moisture == 7) {
                        1 * tickCore.speedFactor
                    } else {
                        tickCore.speedFactor * tickCore.config.dryFarmlandGrowthFactor
                    }
                } else {
                    1 * tickCore.speedFactor
                }
            }
            Material.MELON_STEM,
            Material.PUMPKIN_STEM -> {
                val data = block.blockData as Ageable
                val ticksPerStage = when (block.type) {
                    Material.MELON_STEM -> tickCore.config.melonStem.ticksPerStage.toDouble()
                    /*Material.PUMPKIN_STEM*/else -> tickCore.config.pumpkinStem.ticksPerStage.toDouble()
                }
                if (data.age < data.maximumAge) {
                    val stageDone = data.age
                    val stageToGrow = data.maximumAge - stageDone - 1
                    total = data.maximumAge * ticksPerStage
                    toGrow = stageToGrow * ticksPerStage + tickToNextStage
                } else {
                    total = when (block.type) {
                        Material.MELON_STEM -> tickCore.config.melonStem.ticksPerFruit.toDouble()
                        /*Material.PUMPKIN_STEM*/else -> tickCore.config.pumpkinStem.ticksPerFruit.toDouble()
                    }
                    toGrow = tickToNextStage
                }
                val baseBlock = block.world.getBlockAt(block.x, block.y - 1, block.z)
                speed = if ((baseBlock.blockData as Farmland).moisture == 7) {
                    1 * tickCore.speedFactor
                } else {
                    1 * tickCore.speedFactor * tickCore.config.dryFarmlandGrowthFactor
                }
            }
            Material.ATTACHED_MELON_STEM,
            Material.ATTACHED_PUMPKIN_STEM -> {
                total = 1.0
                toGrow = 0.0
                speed = 1 * tickCore.speedFactor
            } /* constantly 100% */
            Material.OAK_SAPLING -> {
                toGrow = tickToNextStage
                total = tickCore.config.sapling.ticksToGrowUp.toDouble()
                speed = 1 * tickCore.speedFactor
            }
            Material.SUGAR_CANE -> {
                val root = findTheRootOfSugarCane(block)
                val top = findTheTopOfSugarCane(block)
                val height = top.y - root.y + 1
                toGrow =
                    tickToNextStage + (tickCore.config.sugarCane.heightLimitation - height - 1) * tickCore.config.sugarCane.ticksToNextGrow
                total =
                    tickCore.config.sugarCane.heightLimitation * tickCore.config.sugarCane.ticksToNextGrow.toDouble()
                speed = 1 * tickCore.speedFactor
            }
            else -> {
                return
            }
        }
        event.player.sendTitle("", progressBar(total, toGrow, speed), 5, 30, 10)
        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerRightClickAtAnimal(event: PlayerInteractEntityEvent) {
        val item = event.player.inventory.itemInMainHand
        if (item.type != Material.BOOK) return
        val dur = item.enchantments[Enchantment.DURABILITY]
        val inf = item.enchantments[Enchantment.ARROW_INFINITE]
        if (!(dur != null && inf != null && dur == 1 && inf == 1)) return
        if (event.rightClicked !is org.bukkit.entity.Ageable) return
        val ageable = event.rightClicked as org.bukkit.entity.Ageable
        val message = progressBar(
            when (event.rightClicked.type) {
                EntityType.PIG -> tickCore.config.pigs.ticksToGrowUp.toDouble()
                EntityType.COW -> tickCore.config.cows.ticksToGrowUp.toDouble()
                EntityType.SHEEP -> tickCore.config.sheep.ticksToGrowUp.toDouble()
                EntityType.CHICKEN -> tickCore.config.chickens.ticksToGrowUp.toDouble()
                EntityType.HOGLIN -> tickCore.config.hoglins.ticksToGrowUp.toDouble()
                else -> return
            }, -ageable.age.toDouble(), 1.0
        )
        event.player.sendTitle("", message, 5, 30, 10)
        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerLiftClickAtAnimal(event: EntityDamageByEntityEvent) {
        if (event.damager !is Player) return
        val player = event.damager as Player
        val item = player.inventory.itemInMainHand
        if (item.type != Material.BOOK) return
        val dur = item.enchantments[Enchantment.DURABILITY]
        val inf = item.enchantments[Enchantment.ARROW_INFINITE]
        if (!(dur != null && inf != null && dur == 1 && inf == 1)) return
        if (event.entity !is org.bukkit.entity.Ageable) return
        val ageable = event.entity as org.bukkit.entity.Ageable
        val message = progressBar(
            when (event.entity.type) {
                EntityType.PIG -> tickCore.config.pigs.ticksToGrowUp.toDouble()
                EntityType.COW -> tickCore.config.cows.ticksToGrowUp.toDouble()
                EntityType.SHEEP -> tickCore.config.sheep.ticksToGrowUp.toDouble()
                EntityType.CHICKEN -> tickCore.config.chickens.ticksToGrowUp.toDouble()
                EntityType.HOGLIN -> tickCore.config.hoglins.ticksToGrowUp.toDouble()
                else -> return
            }, -ageable.age.toDouble(), 1.0
        )
        player.sendTitle("", message, 5, 30, 10)
        event.isCancelled = true
    }

    private fun progressBar(total: Double, toGrow: Double, amp: Double): String {
        val ratio = 1 - (toGrow / total)
        val redLoc = (ratio * 12).toInt()

        val stringBuilder = StringBuilder()
        stringBuilder.append("░░░░░░░░░░░░ ").append("").append(TickCore.GROW_SPEED_UP_COLOUR)
            .append("${df.format(ratio * 100)}% ")
            .append(ChatColor.WHITE).append("| ").append(TickCore.GROW_SPEED_UP_COLOUR)
            .insert(redLoc, ChatColor.WHITE).insert(0, TickCore.GROW_SPEED_UP_COLOUR)

        if (toGrow != 0.0) {
            if (amp != 0.0) {
                stringBuilder.append("预计需要").append(getTime((toGrow / (20 * amp)).toInt()))
            } else {
                stringBuilder.append("生长暂停中")
            }
        } else {
            stringBuilder.append("已长成")
        }

        return stringBuilder.toString()
    }

    private fun getTime(seconds: Int): String {
        val sec = seconds % 60
        val min = (seconds - sec) % 3600 / 60
        val hour = (seconds - sec - min * 60) / 3600
        return if (hour != 0) {
            "${hour}时${min}分"
        } else {
            if (min != 0) {
                "${min}分${sec}秒"
            } else {
                "${sec}秒"
            }
        }
    }

    private fun findTheRootOfSugarCane(block: Block): Location {
        var y = block.y
        while (y > 1) {
            if (block.world.getBlockAt(block.x, y - 1, block.z).type != Material.SUGAR_CANE)
                break
            y--
        }
        return Location(block.x, y, block.z)
    }

    private fun findTheTopOfSugarCane(block: Block): Location {
        var y = block.y
        while (y < 254) {
            if (block.world.getBlockAt(block.x, y + 1, block.z).type != Material.SUGAR_CANE)
                break
            y++
        }
        return Location(block.x, y, block.z)
    }
}
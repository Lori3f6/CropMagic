package blue.melon.minecraft.cropmagic

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class GrowCommand(private val tickCore: TickCore) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        try {
            val factor = args[0].toDouble()
            if (factor >= 0.0) {
                tickCore.speedFactor = factor
            } else {
                sender.sendMessage("全局生长系数不能为负，无法按您的要求将此值更改为 ${TickCore.GROW_SLOW_DOWN_COLOUR}${factor}")
            }
        } catch (ignore: Exception) {
        }
        sender.sendMessage("当前生效的全局生长系数是 ${if (tickCore.speedFactor >= 1.0) TickCore.GROW_SPEED_UP_COLOUR else TickCore.GROW_SLOW_DOWN_COLOUR}${tickCore.speedFactor}")
        return true
    }
}
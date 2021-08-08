package blue.melon.minecraft.cropmagic

import blue.melon.minecraft.cropmagic.config.Config
import blue.melon.minecraft.cropmagic.config.childs.CropNoStage
import blue.melon.minecraft.cropmagic.config.childs.CropWithHeightLimitation
import blue.melon.minecraft.cropmagic.config.childs.CropWithStage
import blue.melon.minecraft.cropmagic.config.childs.StemWithStage
import blue.melon.minecraft.cropmagic.saves.Save
import com.google.gson.GsonBuilder
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException

class Loader : JavaPlugin() {
    private val gsonInstance = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    private val saveFile = File(dataFolder, "save.json")
    private val configFile = File(dataFolder, "config.json")
    private lateinit var tickCore: TickCore
    override fun onEnable() {
        logger.info("Initializing...")
        if (!dataFolder.mkdir() && !dataFolder.isDirectory)
            throw IOException("${dataFolder.absolutePath} should be a directory, but found a file.")
        val config = loadConfigFile(configFile)
        saveJsonToFile(configFile, config)
        logger.info("Config loaded.")
        var save = loadSave(saveFile)
        tickCore = TickCore(save.cropList, save.speedFactor, config, this)
        logger.info("Tick started.")
        server.pluginManager.registerEvents(EventListener(tickCore, this), this)
        server.pluginManager.registerEvents(MagicBookListener(tickCore), this)
        logger.info("Events registered.")
        server.getPluginCommand("grow")?.setExecutor(GrowCommand(tickCore))
        logger.info("Command registered.")
        server.scheduler.runTaskTimer(this, Runnable {
            saveJsonToFile(saveFile, Save(tickCore.speedFactor, tickCore.getCorpList()))
        }, 20 * 60 * 3L/*after 3 minutes*/, 20 * 60 * 3L/*execute pre 3 minutes*/)
        logger.info("Autosave enabled.")
    }

    override fun onDisable() {
        server.scheduler.cancelTasks(this)
        saveJsonToFile(saveFile, Save(tickCore.speedFactor, tickCore.getCorpList()))
        logger.info("Crops saved.")
    }


    @Throws(IOException::class)
    private fun loadConfigFile(configFile: File): Config {
        val defaultConfig = Config(
            "world",
            0.5,
            CropWithStage(36000),
            CropWithStage(36000),
            CropWithStage(18000),
            CropWithStage(36000),
            CropWithStage(18000),
            StemWithStage(36000, 252000),
            StemWithStage(36000, 108000),
            CropWithHeightLimitation(72000, 3),
            CropNoStage(216000),
            CropNoStage(1440000),
            CropNoStage(1440000),
            CropNoStage(1440000),
            CropNoStage(720000),
            CropNoStage(1440000)
        )
        return when {
            configFile.createNewFile() -> defaultConfig
            configFile.isDirectory -> throw IOException(configFile.absolutePath + " should be a file, but found a directory.")
            else -> {
                var configLoaded: Config
                val configFileReader = FileReader(configFile, Charsets.UTF_8)
                configLoaded = gsonInstance.fromJson(configFileReader, Config::class.java)
                if (configLoaded == null) {
                    configLoaded = defaultConfig
                }
                configLoaded
            }
        }
    }

    private fun loadSave(saveFile: File): Save {
        return when {
            saveFile.createNewFile() -> Save(1.0, ArrayList())
            saveFile.isDirectory -> throw IOException(saveFile.absolutePath + " should be a file, but found a directory.")
            else -> {
                var saveLoaded: Save
                val configFileReader = FileReader(saveFile, Charsets.UTF_8)
                saveLoaded = gsonInstance.fromJson(configFileReader, Save::class.java)
                if (saveLoaded == null) {
                    saveLoaded = Save(1.0, ArrayList())
                }
                saveLoaded
            }
        }
    }

    @Throws(IOException::class)
    private fun <T> saveJsonToFile(file: File, instance: T) {
        val fileOutputStream = FileOutputStream(file, false)
        fileOutputStream.write(gsonInstance.toJson(instance).toByteArray(Charsets.UTF_8))
        fileOutputStream.flush()
        fileOutputStream.close()
    }
}
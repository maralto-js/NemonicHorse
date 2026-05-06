package com.nemonichorse;

import com.nemonichorse.command.HorseCommand;
import com.nemonichorse.config.HorseConfig;
import com.nemonichorse.gui.HorseMenuGUI;
import com.nemonichorse.gui.SkillSelectGUI;
import com.nemonichorse.hook.MMOCoreHook;
import com.nemonichorse.item.HorseItemRegistry;
import com.nemonichorse.listener.*;
import com.nemonichorse.manager.HorseManager;
import com.nemonichorse.manager.NamingSessionManager;
import com.nemonichorse.manager.SkillManager;
import com.nemonichorse.repository.HorseRepository;
import com.nemonichorse.repository.SQLiteHorseRepository;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public final class NemonicHorse extends JavaPlugin {

    private HorseConfig horseConfig;
    private HorseRepository repository;
    private SkillManager skillManager;
    private MMOCoreHook mmoCoreHook;
    private HorseManager horseManager;
    private NamingSessionManager namingSessionManager;
    private HorseItemRegistry itemRegistry;
    private HorseMenuGUI menuGUI;
    private SkillSelectGUI skillSelectGUI;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        horseConfig = new HorseConfig(getConfig());

        // Database
        File dbFile = new File(getDataFolder(), getConfig().getString("database.file", "horses.db"));
        getDataFolder().mkdirs();
        repository = new SQLiteHorseRepository(dbFile, getLogger());

        try {
            repository.initialize();
        } catch (Exception e) {
            getLogger().severe("[NemonicHorse] Failed to initialize database: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Core components
        skillManager         = new SkillManager();
        mmoCoreHook          = new MMOCoreHook(getLogger());
        horseManager         = new HorseManager(this, horseConfig, repository, skillManager, mmoCoreHook);
        namingSessionManager = new NamingSessionManager(horseConfig);
        itemRegistry         = new HorseItemRegistry(this);

        // GUI (skillSelectGUI first — menuGUI depends on it)
        skillSelectGUI = new SkillSelectGUI(horseManager, this);
        menuGUI        = new HorseMenuGUI(horseManager, namingSessionManager, skillSelectGUI);

        // Wire naming callback
        namingSessionManager.setOnNameConfirmed((data, name) ->
                getServer().getWorlds().forEach(world ->
                        world.getEntitiesByClass(org.bukkit.entity.AbstractHorse.class).stream()
                                .filter(h -> h.getUniqueId().equals(data.getHorseId()))
                                .findFirst()
                                .ifPresent(h -> {
                                    horseManager.updateDisplay(h, data);
                                    getServer().getScheduler().runTaskAsynchronously(this, () ->
                                            repository.save(data));
                                })));

        // Start periodic save
        horseManager.startSaveTask();

        // Register listeners
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new HorseLifecycleListener(horseManager, namingSessionManager), this);
        pm.registerEvents(new HorseMoveListener(horseManager), this);
        pm.registerEvents(new HorseCombatListener(horseManager), this);
        pm.registerEvents(new HorseInteractListener(horseManager, itemRegistry, namingSessionManager), this);
        pm.registerEvents(new ChunkListener(horseManager), this);
        pm.registerEvents(new PlayerListener(namingSessionManager, this), this);
        pm.registerEvents(skillSelectGUI, this);
        pm.registerEvents(menuGUI, this);

        // Register command via CommandMap — paper-plugin.yml não usa a seção commands do plugin.yml
        registerCommand(new HorseCommand(horseManager, menuGUI, skillSelectGUI, itemRegistry));

        getLogger().info("[NemonicHorse] Enabled — sistema de cavalaria profissional ativado.");
    }

    @Override
    public void onDisable() {
        if (horseManager != null) {
            horseManager.shutdown();
        }
        if (repository != null) {
            repository.close();
        }
        getLogger().info("[NemonicHorse] Disabled — todos os dados salvos.");
    }

    private void registerCommand(HorseCommand executor) {
        Command cmd = new Command("horse", "Comando de cavalaria", "/horse [info|skill|admin]",
                List.of("cavalo", "hors")) {

            @Override
            public boolean execute(CommandSender sender, String label, String[] args) {
                return executor.onCommand(sender, this, label, args);
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                List<String> result = executor.onTabComplete(sender, this, alias, args);
                return result != null ? result : List.of();
            }
        };
        getServer().getCommandMap().register("nemonichorse", cmd);
    }
}

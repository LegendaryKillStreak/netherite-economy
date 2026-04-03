package com.gmail.sneakdevs.diamondeconomy;

import com.gmail.sneakdevs.diamondeconomy.command.DiamondEconomyCommands;
import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;
import com.gmail.sneakdevs.diamondeconomy.integration.DiamondEconomyProvider;
import com.gmail.sneakdevs.diamondeconomy.integration.DiamondPlaceholders;
import com.gmail.sneakdevs.diamondeconomy.sql.SQLiteDatabaseManager;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.util.ArrayList;
import com.gmail.sneakdevs.diamondeconomy.CurrencyType;

public class DiamondEconomy implements ModInitializer {
    public static final String MODID = "diamondeconomy";
    public static ArrayList<String> tableRegistry = new ArrayList<>();
    public static ArrayList<CurrencyType> currencyList = new ArrayList<>();

    public static void initServer(MinecraftServer server) {
        // Initialize DB manager instance
        DiamondUtils.databaseManager = new SQLiteDatabaseManager();

        // Register tables (money stored as TEXT to support BigInteger)
        DiamondUtils.registerTable("CREATE TABLE IF NOT EXISTS diamonds (uuid TEXT PRIMARY KEY, name TEXT NOT NULL, money TEXT NOT NULL);");
        DiamondUtils.registerTable("CREATE TABLE IF NOT EXISTS currencies (item TEXT PRIMARY KEY, sellvalue INTEGER, buyvalue INTEGER, incurrencylist BIT, canbuy BIT, cansell BIT);");

        // Resolve DB file path
        File dbFile = (DiamondEconomyConfig.getInstance().fileLocation != null)
                ? new File(DiamondEconomyConfig.getInstance().fileLocation)
                : server.getWorldPath(LevelResource.ROOT).resolve(DiamondEconomy.MODID + ".sqlite").toFile();

        // Call instance method on the DatabaseManager
        DiamondUtils.getDatabaseManager().createNewDatabase(dbFile);

        // Ensure main currency exists and populate in-memory list
        DiamondUtils.getDatabaseManager().addCurrency(DiamondEconomyConfig.getInstance().mainCurrency, 1, 1, true, true, true, true);
        DiamondUtils.createCurrencyList();
    }

    @Override
    public void onInitialize() {
        AutoConfig.register(DiamondEconomyConfig.class, JanksonConfigSerializer::new);
        DiamondPlaceholders.registerPlaceholders();
        CommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext, environment) -> DiamondEconomyCommands.register(dispatcher, commandBuildContext));
        ServerLifecycleEvents.SERVER_STARTING.register(DiamondEconomy::initServer);
        DiamondEconomyProvider.init();
    }
}


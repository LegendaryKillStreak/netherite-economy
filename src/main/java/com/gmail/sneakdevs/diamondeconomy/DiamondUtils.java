package com.gmail.sneakdevs.diamondeconomy;

import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;
import com.gmail.sneakdevs.diamondeconomy.sql.DatabaseManager;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility helpers used across the mod.
 * Contains BigInteger-aware formatting and simple DB/table helpers used by DiamondEconomy.
 */
public final class DiamondUtils {
    private DiamondUtils() {}

    /**
     * The active DatabaseManager instance. Set by DiamondEconomy during initialization.
     */
    public static DatabaseManager databaseManager;

    /**
     * Register a table creation SQL to be executed when the DB is created.
     * This simply stores the SQL in DiamondEconomy.tableRegistry for SQLiteDatabaseManager#createNewTable to execute.
     */
    public static void registerTable(String createTableSql) {
        if (createTableSql == null || createTableSql.isEmpty()) return;
        DiamondEconomy.tableRegistry.add(createTableSql);
    }

    /**
     * Return the configured DatabaseManager instance.
     */
    public static DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * Convenience accessor for the currency list.
     */
    public static List<CurrencyType> getCurrencyList() {
        return DiamondEconomy.currencyList;
    }

    /**
     * Populate DiamondEconomy.currencyList from the database.
     * This reads currencies until the DB returns null for an index.
     */
    public static void createCurrencyList() {
        DiamondEconomy.currencyList.clear();
        DatabaseManager dm = getDatabaseManager();
        if (dm == null) return;

        int i = 0;
        while (true) {
            try {
                CurrencyType ct = dm.getCurrency(i);
                if (ct == null) break;
                DiamondEconomy.currencyList.add(ct);
                i++;
            } catch (Exception ex) {
                ex.printStackTrace();
                break;
            }
        }
    }

    // -------------------------
    // BigInteger formatting
    // -------------------------

    /**
     * Format an int amount using the BigInteger formatter.
     * Keeps backward compatibility with older call sites.
     */
    public static String valueString(int amount) {
        return valueString(BigInteger.valueOf(amount));
    }

    /**
     * Format a long amount using the BigInteger formatter.
     */
    public static String valueString(long amount) {
        return valueString(BigInteger.valueOf(amount));
    }

    /**
     * Format a BigInteger amount with comma grouping (e.g., 1,234,567).
     * Handles negative values.
     */
    public static String valueString(BigInteger amount) {
        if (amount == null) return "0";

        String s = amount.toString();
        boolean negative = s.startsWith("-");
        if (negative) s = s.substring(1);

        int len = s.length();
        if (len <= 3) {
            return (negative ? "-" : "") + s;
        }

        StringBuilder sb = new StringBuilder();
        int first = len % 3;
        if (first == 0) first = 3;
        sb.append(s.substring(0, first));
        for (int i = first; i < len; i += 3) {
            sb.append(',');
            sb.append(s.substring(i, i + 3));
        }

        String formatted = sb.toString();
        return negative ? "-" + formatted : formatted;
    }

    /**
     * Short-format helper (optional): returns a compact representation using suffixes.
     * Examples: 1.2K, 3.4M.
     */
    public static String valueStringShort(BigInteger amount) {
        if (amount == null) return "0";
        BigInteger abs = amount.abs();
        BigInteger thousand = BigInteger.valueOf(1000);

        if (abs.compareTo(thousand) < 0) return valueString(amount);

        String[] suffixes = {"K", "M", "B", "T", "P", "E"};
        BigInteger value = abs;
        int idx = -1;
        while (value.compareTo(thousand) >= 0 && idx < suffixes.length - 1) {
            value = value.divide(thousand);
            idx++;
        }

        String base = value.toString();
        if (amount.signum() < 0) base = "-" + base;
        return base + (idx >= 0 ? suffixes[idx] : "");
    }

    // -------------------------
    // Small helpers
    // -------------------------

    /**
     * Safe getter for the configured starting money from config as BigInteger.
     * Returns ZERO if config is missing or invalid.
     */
    public static BigInteger getStartingMoney() {
        try {
            int start = DiamondEconomyConfig.getInstance().startingMoney;
            return BigInteger.valueOf(Math.max(0, start));
        } catch (Exception ex) {
            return BigInteger.ZERO;
        }
    }

    /**
     * Helper to ensure the database manager is set; throws IllegalStateException if not.
     */
    public static DatabaseManager requireDatabaseManager() {
        if (databaseManager == null) {
            throw new IllegalStateException("DatabaseManager not initialized. Call DiamondUtils.databaseManager = new SQLiteDatabaseManager() during startup.");
        }
        return databaseManager;
    }
}

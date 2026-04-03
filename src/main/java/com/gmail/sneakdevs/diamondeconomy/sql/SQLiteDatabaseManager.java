package com.gmail.sneakdevs.diamondeconomy.sql;

import com.gmail.sneakdevs.diamondeconomy.CurrencyType;
import com.gmail.sneakdevs.diamondeconomy.DiamondEconomy;
import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;

import java.io.File;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteDatabaseManager implements DatabaseManager {
    public static String url;

    private static final BigInteger MAX_BALANCE = new BigInteger("340282366920938463463374607431768211455"); // 2^128 - 1
    private static final BigInteger ZERO = BigInteger.ZERO;
    private static final BigInteger NEG_ONE = BigInteger.valueOf(-1);

    @Override
    public void createNewDatabase(File file) {
        url = "jdbc:sqlite:" + file.getPath().replace('\\', '/');

        try (Connection conn = DriverManager.getConnection(url)) {
            // ensure DB file exists
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        createNewTable();
    }

    @Override
    public Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    private static void createNewTable() {
        try (Connection conn = DriverManager.getConnection(url); Statement stmt = conn.createStatement()) {
            for (String query : DiamondEconomy.tableRegistry) {
                stmt.execute(query);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addPlayer(String uuid, String name) {
        String sql = "INSERT INTO diamonds(uuid,name,money) VALUES(?,?,?)";

        try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setString(2, name);

            BigInteger starting = BigInteger.valueOf(DiamondEconomyConfig.getInstance().startingMoney);
            if (starting.compareTo(ZERO) < 0 || starting.compareTo(MAX_BALANCE) > 0) {
                starting = ZERO;
            }

            pstmt.setString(3, starting.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            updateName(uuid, name);
        }
    }

    @Override
    public void updateName(String uuid, String name) {
        String sql = "UPDATE diamonds SET name = ? WHERE uuid = ?";

        try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, uuid);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setName(String uuid, String name) {
        String sql = "UPDATE diamonds SET name = ? WHERE uuid != ? AND name = ?";

        try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "a");
            pstmt.setString(2, uuid);
            pstmt.setString(3, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public BigInteger getBalanceFromUUID(String uuid) {
        String sql = "SELECT money FROM diamonds WHERE uuid = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String moneyStr = rs.getString("money");
                    if (moneyStr == null) return NEG_ONE;
                    try {
                        return new BigInteger(moneyStr);
                    } catch (NumberFormatException ex) {
                        ex.printStackTrace();
                        return NEG_ONE;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return NEG_ONE;
    }

    @Override
    public String getNameFromUUID(String uuid) {
        String sql = "SELECT name FROM diamonds WHERE uuid = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, uuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public BigInteger getBalanceFromName(String name) {
        String sql = "SELECT money FROM diamonds WHERE name = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String moneyStr = rs.getString("money");
                    if (moneyStr == null) return NEG_ONE;
                    try {
                        return new BigInteger(moneyStr);
                    } catch (NumberFormatException ex) {
                        ex.printStackTrace();
                        return NEG_ONE;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return NEG_ONE;
    }

    @Override
    public boolean setBalance(String uuid, BigInteger money) {
        String sql = "UPDATE diamonds SET money = ? WHERE uuid = ?";

        if (money == null) return false;
        if (money.compareTo(ZERO) < 0 || money.compareTo(MAX_BALANCE) > 0) return false;

        try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, money.toString());
            pstmt.setString(2, uuid);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void setAllBalance(BigInteger money) {
        String sql = "UPDATE diamonds SET money = ?";

        if (money == null) return;
        if (money.compareTo(ZERO) < 0 || money.compareTo(MAX_BALANCE) > 0) return;

        try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, money.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean changeBalance(String uuid, BigInteger money) {
        if (money == null) return false;

        BigInteger bal = getBalanceFromUUID(uuid);
        if (bal.equals(NEG_ONE)) return false;

        BigInteger newBal = bal.add(money);
        if (newBal.compareTo(ZERO) >= 0 && newBal.compareTo(MAX_BALANCE) <= 0) {
            String sql = "UPDATE diamonds SET money = ? WHERE uuid = ?";
            try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newBal.toString());
                pstmt.setString(2, uuid);
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public void changeAllBalance(BigInteger money) {
        if (money == null) return;

        String selectSql = "SELECT uuid, money FROM diamonds";
        String updateSql = "UPDATE diamonds SET money = ? WHERE uuid = ?";

        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                String moneyStr = rs.getString("money");
                if (moneyStr == null) continue;

                BigInteger current;
                try {
                    current = new BigInteger(moneyStr);
                } catch (NumberFormatException ex) {
                    continue;
                }

                BigInteger newBal = current.add(money);
                if (newBal.compareTo(ZERO) >= 0 && newBal.compareTo(MAX_BALANCE) <= 0) {
                    try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                        pstmt.setString(1, newBal.toString());
                        pstmt.setString(2, uuid);
                        pstmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String top(String uuid, int page) {
        String sql = "SELECT uuid, name, money FROM diamonds ORDER BY LENGTH(money) DESC, money DESC";
        StringBuilder rankings = new StringBuilder();
        int i = 0;
        int playerRank = 0;
        int repeats = 0;

        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next() && (repeats < 10 || playerRank == 0)) {
                repeats++;
                if (repeats / 10 + 1 == page) {
                    String name = rs.getString("name");
                    String moneyStr = rs.getString("money");
                    BigInteger money = ZERO;
                    if (moneyStr != null) {
                        try {
                            money = new BigInteger(moneyStr);
                        } catch (NumberFormatException ex) {
                            money = ZERO;
                        }
                    }
                    rankings.append(rs.getRow()).append(") ").append(name).append(": $").append(money.toString()).append("\n");
                    i++;
                }
                if (uuid.equals(rs.getString("uuid"))) {
                    playerRank = repeats;
                }
            }
            if (i < 10) {
                rankings.append("---End--- \n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        rankings.append("Your rank is: ").append(playerRank);
        return rankings.toString();
    }

    @Override
    public String rank(int rank) {
        int repeats = 0;
        String sql = "SELECT name FROM diamonds ORDER BY LENGTH(money) DESC, money DESC";
        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                repeats++;
                if (repeats == rank) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "No Player";
    }

    @Override
    public int playerRank(String uuid) {
        String sql = "SELECT uuid FROM diamonds ORDER BY LENGTH(money) DESC, money DESC";
        int repeats = 1;

        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (!rs.next()) return -1;
            while (!rs.getString("uuid").equals(uuid)) {
                if (!rs.next()) return -1;
                repeats++;
            }
            return repeats;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public boolean addCurrency(String item, int sellValue, int buyvalue, boolean incurrencylist, boolean canbuy, boolean cansell, boolean init) {
        if (init || !item.equals(DiamondEconomyConfig.getInstance().mainCurrency)) {
            String sql = "INSERT INTO currencies(item,sellvalue,buyvalue,incurrencylist,canbuy,cansell) VALUES(?,?,?,?,?,?)";
            try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, item);
                pstmt.setInt(2, sellValue);
                pstmt.setInt(3, buyvalue);
                pstmt.setBoolean(4, incurrencylist);
                pstmt.setBoolean(5, canbuy);
                pstmt.setBoolean(6, cansell);
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean setSellValue(String item, int sellValue) {
        if (!item.equals(DiamondEconomyConfig.getInstance().mainCurrency)) {
            String sql = "UPDATE currencies SET sellvalue = ? WHERE item = ?";
            try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, sellValue);
                pstmt.setString(2, item);
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public boolean setBuyValue(String item, int buyValue) {
        if (!item.equals(DiamondEconomyConfig.getInstance().mainCurrency)) {
            String sql = "UPDATE currencies SET buyvalue = ? WHERE item = ?";
            try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, buyValue);
                pstmt.setString(2, item);
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public boolean setInCurrencyList(String item, boolean inCurrencyList) {
        if (!item.equals(DiamondEconomyConfig.getInstance().mainCurrency)) {
            String sql = "UPDATE currencies SET incurrencylist = ? WHERE item = ?";
            try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setBoolean(1, inCurrencyList);
                pstmt.setString(2, item);
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public boolean setCanBuy(String item, boolean canBuy) {
        if (!item.equals(DiamondEconomyConfig.getInstance().mainCurrency)) {
            String sql = "UPDATE currencies SET canbuy = ? WHERE item = ?";
            try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setBoolean(1, canBuy);
                pstmt.setString(2, item);
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public boolean setCanSell(String item, boolean canSell) {
        if (!item.equals(DiamondEconomyConfig.getInstance().mainCurrency)) {
            String sql = "UPDATE currencies SET cansell = ? WHERE item = ?";
            try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setBoolean(1, canSell);
                pstmt.setString(2, item);
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public boolean isCurrency(String item) {
        String sql = "SELECT item FROM currencies WHERE item = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return (rs.getString("item")).equals(item);
                }
            }
        } catch (SQLException e) {
            return false;
        }
        return false;
    }

    @Override
    public int getSellValue(String item) {
        String sql = "SELECT sellvalue FROM currencies WHERE item = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("sellvalue");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public int getBuyValue(String item) {
        String sql = "SELECT buyvalue FROM currencies WHERE item = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("buyvalue");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public boolean getInCurrencyList(String item) {
        String sql = "SELECT incurrencylist FROM currencies WHERE item = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("incurrencylist");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean getCanBuy(String item) {
        String sql = "SELECT canbuy FROM currencies WHERE item = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("canbuy");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean getCanSell(String item) {
        String sql = "SELECT cansell FROM currencies WHERE item = ?";

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("cansell");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean removeCurrency(String item) {
        if (!item.equals(DiamondEconomyConfig.getInstance().mainCurrency)) {
            String sql = "DELETE FROM currencies WHERE item = ?";
            try (Connection conn = this.connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, item);
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public CurrencyType getCurrency(String item) {
        String sql = "SELECT item, sellvalue, buyvalue, incurrencylist, canbuy, cansell FROM currencies WHERE item = ?";
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, item);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int sellValue = rs.getInt("sellvalue");
                    int buyValue = rs.getInt("buyvalue");
                    boolean inCurrencyList = rs.getBoolean("incurrencylist");
                    boolean canBuy = rs.getBoolean("canbuy");
                    boolean canSell = rs.getBoolean("cansell");
                    return new CurrencyType(item, sellValue, buyValue, inCurrencyList, canBuy, canSell);
                }
            }
        } catch (SQLException e) {
            return null;
        }
        return null;
    }

    @Override
    public CurrencyType getCurrency(int i) {
        String sql = "SELECT item, sellvalue, buyvalue, incurrencylist, canbuy, cansell FROM currencies ORDER BY buyvalue DESC";
        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            for (int j = -1; j < i; j++) {
                if (!rs.next()) {
                    return null;
                }
            }
            String item = rs.getString("item");
            int sellValue = rs.getInt("sellvalue");
            int buyValue = rs.getInt("buyvalue");
            boolean inCurrencyList = rs.getBoolean("incurrencylist");
            boolean canBuy = rs.getBoolean("canbuy");
            boolean canSell = rs.getBoolean("cansell");
            return new CurrencyType(item, sellValue, buyValue, inCurrencyList, canBuy, canSell);
        } catch (SQLException e) {
            return null;
        }
    }
}

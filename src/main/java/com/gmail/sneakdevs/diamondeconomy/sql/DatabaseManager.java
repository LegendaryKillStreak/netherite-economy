package com.gmail.sneakdevs.diamondeconomy.sql;

import com.gmail.sneakdevs.diamondeconomy.CurrencyType;

import java.io.File;
import java.math.BigInteger;
import java.sql.Connection;

public interface DatabaseManager {
    void createNewDatabase(File file);
    Connection connect();
    void addPlayer(String uuid, String name);
    void updateName(String uuid, String name);
    void setName(String uuid, String name);

    BigInteger getBalanceFromUUID(String uuid);
    String getNameFromUUID(String uuid);
    BigInteger getBalanceFromName(String name);

    boolean setBalance(String uuid, BigInteger money);
    void setAllBalance(BigInteger money);
    boolean changeBalance(String uuid, BigInteger money);
    void changeAllBalance(BigInteger money);

    String top(String uuid, int page);
    String rank(int rank);
    int playerRank(String uuid);

    boolean addCurrency(String item, int sellValue, int buyvalue, boolean incurrencylist, boolean canbuy, boolean cansell, boolean init);
    boolean setSellValue(String item, int sellValue);
    boolean setBuyValue(String item, int buyValue);
    boolean setInCurrencyList(String item, boolean inCurrencyList);
    boolean setCanBuy(String item, boolean canBuy);
    boolean setCanSell(String item, boolean canSell);
    boolean isCurrency(String item);
    int getSellValue(String item);
    int getBuyValue(String item);
    boolean getInCurrencyList(String item);
    boolean getCanBuy(String item);
    boolean getCanSell(String item);
    boolean removeCurrency(String item);
    CurrencyType getCurrency(String item);
    CurrencyType getCurrency(int i);
}


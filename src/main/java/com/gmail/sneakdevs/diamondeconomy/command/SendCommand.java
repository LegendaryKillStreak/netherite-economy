package com.gmail.sneakdevs.diamondeconomy.command;

import com.gmail.sneakdevs.diamondeconomy.DiamondUtils;
import com.gmail.sneakdevs.diamondeconomy.config.DiamondEconomyConfig;
import com.gmail.sneakdevs.diamondeconomy.sql.DatabaseManager;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigInteger;

public class SendCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> buildCommand(){
        return Commands.literal(DiamondEconomyConfig.getInstance().sendCommandName)
                .then(
                        Commands.argument("player", EntityArgument.player())
                                .then(
                                        Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(e -> {
                                                    ServerPlayer target = EntityArgument.getPlayer(e, "player");
                                                    int amount = IntegerArgumentType.getInteger(e, "amount");
                                                    return sendCommand(e, target, e.getSource().getPlayerOrException(), amount);
                                                })));
    }

    public static int sendCommand(CommandContext<CommandSourceStack> ctx, ServerPlayer recipient, ServerPlayer sender, int amount) throws CommandSyntaxException {
        DatabaseManager dm = DiamondUtils.getDatabaseManager();

        BigInteger amountBI = BigInteger.valueOf(amount);
        BigInteger senderBal = dm.getBalanceFromUUID(sender.getStringUUID());
        if (senderBal == null || senderBal.equals(BigInteger.valueOf(-1))) {
            ctx.getSource().sendFailure(Component.literal("Could not read your balance."));
            return 0;
        }

        if (senderBal.compareTo(amountBI) < 0) {
            ctx.getSource().sendFailure(Component.literal("Insufficient funds."));
            return 0;
        }

        // Attempt to withdraw from sender first
        boolean withdrawn = dm.changeBalance(sender.getStringUUID(), amountBI.negate());
        if (!withdrawn) {
            ctx.getSource().sendFailure(Component.literal("Failed to withdraw funds (cap or DB error)."));
            return 0;
        }

        // Credit recipient
        boolean credited = dm.changeBalance(recipient.getStringUUID(), amountBI);
        if (!credited) {
            // Attempt to rollback withdrawal
            boolean rollback = dm.changeBalance(sender.getStringUUID(), amountBI);
            if (!rollback) {
                ctx.getSource().sendFailure(Component.literal("Critical error: transfer failed and rollback unsuccessful. Contact an admin."));
                return 0;
            }
            ctx.getSource().sendFailure(Component.literal("Transfer failed; your balance was restored."));
            return 0;
        }

        recipient.displayClientMessage(Component.literal("You received " + DiamondUtils.valueString(amountBI) + " from " + sender.getName().getString()), false);
        ctx.getSource().sendSuccess(() -> Component.literal("Sent " + DiamondUtils.valueString(amountBI) + " to " + recipient.getName().getString()), false);

        return 1;
    }
}

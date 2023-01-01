package com.clovercard.pixelshopcurrencies.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class ViewCurrenciesCommand {
    //Create command structure
    public ViewCurrenciesCommand(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("clbal")
                        .then(
                                Commands.argument("currency", StringArgumentType.string())
                                        .executes(cmd -> showCurrency(cmd.getSource(), StringArgumentType.getString(cmd, "currency")))
                        )
        );
    }
    //Provide the user their balance of the currency if it exists.
    public int showCurrency(CommandSource source, String currency) {
        ServerScoreboard scoreboard = ServerLifecycleHooks.getCurrentServer().getScoreboard();
        ScoreObjective obj = scoreboard.getObjective(currency);
        if(obj != null) {
            try {
                source.getPlayerOrException().sendMessage(new StringTextComponent("Your balance: " + scoreboard.getOrCreatePlayerScore(source.getPlayerOrException().getName().getString(), obj).getScore()), ChatType.GAME_INFO, Util.NIL_UUID);
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        else {
            try {
                StringTextComponent errMsg = new StringTextComponent("This currency does not exist within the scoreboard!");
                Style style = errMsg.getStyle();
                style.applyFormats(TextFormatting.DARK_RED, TextFormatting.BOLD);
                errMsg.setStyle(style);
                source.getPlayerOrException().sendMessage(new StringTextComponent("This currency does not exist!"), ChatType.GAME_INFO, Util.NIL_UUID);
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return 0;
    }
}

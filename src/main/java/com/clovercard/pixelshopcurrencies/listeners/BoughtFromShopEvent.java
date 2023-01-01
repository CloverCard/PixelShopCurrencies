package com.clovercard.pixelshopcurrencies.listeners;

import com.pixelmonmod.pixelmon.api.events.ShopkeeperEvent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import java.util.Objects;

public class BoughtFromShopEvent {
    /*
    * This listener will observe all store purchases.
    * If in the config the item has the NBT tags: clovercur and clovercost, it will treat it as special currency.
    * With the addition of the clovercmd tag, it will treat it not as an item, but as a command to be run.
    */
    @SubscribeEvent
    public void onShopPurchase(ShopkeeperEvent.Purchase e){
        CompoundNBT nbt = e.getItem().getTag();
        ServerScoreboard board = ServerLifecycleHooks.getCurrentServer().getScoreboard();
        //Check if NBT data is valid for special currency.
        if(validateNbt(nbt)) {
            ScoreObjective curr = board.getObjective(nbt.getString("clovercur"));
            //Handle invalid currency
            if (Objects.isNull(curr))
                e.getEntityPlayer().sendMessage(new StringTextComponent("The admin(s) of this server have not created a currency called " + nbt.getString("clovercur") + "!"), ChatType.GAME_INFO, Util.NIL_UUID);
            //Handle valid currency
            else {
                Score bal = board.getOrCreatePlayerScore(e.getEntityPlayer().getName().getString(), curr);
                int cost = ((IntNBT) Objects.requireNonNull(nbt.get("clovercost"))).getAsInt();
                //Handle case where player can afford purchase
                if (bal.getScore() >= (cost * e.getItem().getCount())) {
                    //Remove cost from player balance
                    bal.setScore(bal.getScore() - (cost * e.getItem().getCount()));
                    //Handle command purchases
                    if (nbt.contains("clovercmd")) {
                        handleCommandPurchase(nbt, e.getEntityPlayer(), e.getItem().getCount());
                    }
                    //Handle item purchases
                    else {
                        nbt.remove("display");
                        nbt.remove("clovercur");
                        nbt.remove("clovercost");
                        e.getEntityPlayer().inventory.add(e.getItem());
                    }
                    //Inform player of their new balance
                    StringTextComponent balMsg = new StringTextComponent("Your new balance: " + bal.getScore() + " " + curr.getName());
                    balMsg.setStyle(balMsg.getStyle().applyFormat(TextFormatting.BOLD));
                    e.getEntityPlayer().sendMessage(balMsg, ChatType.GAME_INFO, Util.NIL_UUID);
                }
                //Handle case where player can't afford purchase.
                else {
                    StringTextComponent errMsg = new StringTextComponent("You do not have enough to purchase this!");
                    errMsg.setStyle(errMsg.getStyle().applyFormats(TextFormatting.DARK_RED, TextFormatting.BOLD));
                    e.getEntityPlayer().sendMessage(errMsg, ChatType.GAME_INFO, Util.NIL_UUID);
                }
            }
            e.setCanceled(true);
        }
    }
    public boolean validateNbt(CompoundNBT nbt) {
        if(Objects.isNull(nbt)){
            return false;
        } else return nbt.contains("clovercur") && nbt.contains("clovercost");
    }

    public void handleCommandPurchase(CompoundNBT nbt, ServerPlayerEntity player, int count) {
        StringBuilder cmd = new StringBuilder(nbt.getString("clovercmd"));
        String[] split = cmd.toString().split(" ");
        cmd = new StringBuilder();
        //Handle PLAYER placeholder.
        for (int i = 1; i < split.length; i++) {
            if (split[i].contains("PLAYER")) {
                split[i] = split[i].replace("PLAYER", player.getName().getContents());
            }
            cmd.append(split[i]).append(" ");
        }
        MinecraftServer world = player.getServer();
        //Handle who/what runs the command.
        if (Objects.equals(split[0], "console")) {
            assert world != null;
            for (int i = 0; i < count; i++)
                world.getCommands().performCommand(world.createCommandSourceStack(), cmd.toString());
        } else if (Objects.equals(split[0], "self")) {
            assert world != null;
            for (int i = 0; i < count; i++)
                world.getCommands().performCommand(player.createCommandSourceStack(), cmd.toString());
        }
    }
}

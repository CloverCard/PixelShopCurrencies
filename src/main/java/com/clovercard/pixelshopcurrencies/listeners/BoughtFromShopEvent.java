package com.clovercard.pixelshopcurrencies.listeners;

import com.pixelmonmod.pixelmon.api.economy.BankAccount;
import com.pixelmonmod.pixelmon.api.economy.BankAccountProxy;
import com.pixelmonmod.pixelmon.api.events.ShopkeeperEvent;
import com.pixelmonmod.pixelmon.api.util.helpers.ResourceLocationHelper;
import com.pixelmonmod.pixelmon.entities.npcs.registry.BaseShopItem;
import com.pixelmonmod.pixelmon.entities.npcs.registry.ServerNPCRegistry;
import com.pixelmonmod.pixelmon.entities.npcs.registry.ShopItem;
import com.pixelmonmod.pixelmon.entities.npcs.registry.ShopItemWithVariation;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class BoughtFromShopEvent {
    /*
     * This listener will observe all store purchases.
     * If in the config the item has the NBT tags: clovercur, clovercost, it will treat it as special currency.
     * If in the config the item has the NBT tags: cloveritemcur and cloveritemcost, it will treat it as an item currency
     * All four combined will expect costs to require both types.
     * With the addition of the clovercmd tag, it will treat it not as an item, but as a command to be run.
     */
    @SubscribeEvent
    public void onShopPurchase(ShopkeeperEvent.Purchase e) {
        CompoundNBT nbt = e.getItem().getTag();
        ServerScoreboard board = ServerLifecycleHooks.getCurrentServer().getScoreboard();
        BaseShopItem shopItem = ServerNPCRegistry.shopkeepers.getItem(e.getItem());
        if (validateItemCurrencyNbt(nbt) && validateNbt(nbt)) {
            ScoreObjective curr = board.getObjective(nbt.getString("clovercur"));
            //Check if special currency is valid
            if (Objects.isNull(curr)) {
                StringTextComponent errMsg = new StringTextComponent("The admin(s) of this server have not created a currency called " + nbt.getString("clovercur") + "!");
                errMsg.setStyle(errMsg.getStyle().applyFormats(TextFormatting.DARK_RED, TextFormatting.BOLD));
                e.getEntityPlayer().sendMessage(errMsg, ChatType.GAME_INFO, Util.NIL_UUID);
                e.setCanceled(true);
                return;
            }
            //Check if player has the item currency and has enough
            HashMap<String, Integer> itemPrices = handleItemCurrencies(nbt);
            boolean cancelPurchase = false;
            for (Map.Entry<String, Integer> itemCur : itemPrices.entrySet()) {
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocationHelper.of(itemCur.getKey()));
                if (item == null) {
                    StringTextComponent errMsg = new StringTextComponent("Cannot find " + itemCur.getKey() + " within registry!");
                    errMsg.setStyle(errMsg.getStyle().applyFormats(TextFormatting.DARK_RED, TextFormatting.BOLD));
                    e.getEntityPlayer().sendMessage(errMsg, ChatType.GAME_INFO, Util.NIL_UUID);
                    cancelPurchase = true;
                    break;
                }
                int count = 0;
                //Get player's item balance
                for (ItemStack itemstack : e.getEntityPlayer().inventory.items) {
                    if (itemstack.getItem().equals(item)) {
                        count += itemstack.getCount();
                    }
                }
                if (count < itemCur.getValue() * e.getItem().getCount()) {
                    StringTextComponent errMsg = new StringTextComponent("You do not have enough " + itemCur.getKey() + " to purchase this item!");
                    errMsg.setStyle(errMsg.getStyle().applyFormats(TextFormatting.DARK_RED, TextFormatting.BOLD));
                    e.getEntityPlayer().sendMessage(errMsg, ChatType.GAME_INFO, Util.NIL_UUID);
                    cancelPurchase = true;
                    break;
                }
            }
            if (cancelPurchase) {
                e.setCanceled(true);
                return;
            }
            //Get special currency prices
            Score bal = board.getOrCreatePlayerScore(e.getEntityPlayer().getName().getString(), curr);
            int pCost = ((IntNBT) Objects.requireNonNull(nbt.get("clovercost"))).getAsInt();
            //Check if player has enough special currency
            if (bal.getScore() < (pCost * e.getItem().getCount())) {
                StringTextComponent errMsg = new StringTextComponent("You do not have enough to purchase this!");
                errMsg.setStyle(errMsg.getStyle().applyFormats(TextFormatting.DARK_RED, TextFormatting.BOLD));
                e.getEntityPlayer().sendMessage(errMsg, ChatType.GAME_INFO, Util.NIL_UUID);
                return;
            }
            //Remove special currency from player balance
            bal.setScore(bal.getScore() - (pCost * e.getItem().getCount()));
            //Remove items
            for (Map.Entry<String, Integer> itemCur : itemPrices.entrySet()) {
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocationHelper.of(itemCur.getKey()));
                int count = 0;
                //Get player's item balance
                for (ItemStack itemstack : e.getEntityPlayer().inventory.items) {
                    if (itemstack.getItem().equals(item)) {
                        count += itemstack.getCount();
                    }
                }
                int total = itemCur.getValue() * e.getItem().getCount();
                ArrayList<ItemStack> toRemoveItems = new ArrayList<>();
                //Remove item from inventory
                for (ItemStack itemStack : e.getEntityPlayer().inventory.items) {
                    if (itemStack.getItem().equals(item)) {
                        int stackCount = itemStack.getCount();
                        if (stackCount <= total) {
                            total -= stackCount;
                            toRemoveItems.add(itemStack);
                        } else {
                            itemStack.shrink(total);
                            break;
                        }
                    }
                }
                toRemoveItems.forEach(removed -> e.getEntityPlayer().inventory.removeItem(removed));
            }
            BankAccount account = BankAccountProxy.getBankAccount(e.getEntityPlayer()).orElse(null);
            if(account != null) {
                if(shopItem != null) {
                    ShopItemWithVariation shopItemVar = new ShopItemWithVariation(new ShopItem(shopItem, 1 ,1, false));
                    //Get Cost Data
                    float cost = shopItemVar.getBuyCost() * e.getItem().getCount();
                    account.take(cost);
                }
            }
            //Handle command purchases
            if (nbt.contains("clovercmd")) {
                handleCommandPurchase(nbt, e.getEntityPlayer(), e.getItem().getCount());
            }
            //Handle item purchases
            else {
                nbt.remove("display");
                nbt.remove("cloveritemcur");
                nbt.remove("cloveritemcost");
                nbt.remove("clovercost");
                nbt.remove("clovercur");
                e.getEntityPlayer().inventory.add(e.getItem());
            }
            //Inform player of their new balance
            StringTextComponent balMsg = new StringTextComponent("Your new balance: " + bal.getScore() + " " + curr.getName());
            balMsg.setStyle(balMsg.getStyle().applyFormat(TextFormatting.BOLD));
            e.getEntityPlayer().sendMessage(balMsg, ChatType.GAME_INFO, Util.NIL_UUID);
            e.setCanceled(true);
        }
        //Check if NBT data is valid for special currency.
        else if (validateNbt(nbt)) {
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
                    BankAccount account = BankAccountProxy.getBankAccount(e.getEntityPlayer()).orElse(null);
                    if(account != null) {
                        if(shopItem != null) {
                            ShopItemWithVariation shopItemVar = new ShopItemWithVariation(new ShopItem(shopItem, 1 ,1, false));
                            //Get Cost Data
                            float price = shopItemVar.getBuyCost() * e.getItem().getCount();
                            account.take(price);
                        }
                    }
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
        } else if (validateItemCurrencyNbt(nbt)) {
            HashMap<String, Integer> itemPrices = handleItemCurrencies(nbt);
            boolean cancelPurchase = false;
            //Check if player has the item currency and has enough
            for (Map.Entry<String, Integer> itemCur : itemPrices.entrySet()) {
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocationHelper.of(itemCur.getKey()));
                if (item == null) {
                    StringTextComponent errMsg = new StringTextComponent("Cannot find " + itemCur.getKey() + " within registry!");
                    errMsg.setStyle(errMsg.getStyle().applyFormats(TextFormatting.DARK_RED, TextFormatting.BOLD));
                    e.getEntityPlayer().sendMessage(errMsg, ChatType.GAME_INFO, Util.NIL_UUID);
                    cancelPurchase = true;
                    break;
                }
                int count = 0;
                //Get player's item balance
                for (ItemStack itemstack : e.getEntityPlayer().inventory.items) {
                    if (itemstack.getItem().equals(item)) {
                        count += itemstack.getCount();
                    }
                }
                if (count < itemCur.getValue() * e.getItem().getCount()) {
                    StringTextComponent errMsg = new StringTextComponent("You do not have enough " + itemCur.getKey() + " to purchase this item!");
                    errMsg.setStyle(errMsg.getStyle().applyFormats(TextFormatting.DARK_RED, TextFormatting.BOLD));
                    e.getEntityPlayer().sendMessage(errMsg, ChatType.GAME_INFO, Util.NIL_UUID);
                    cancelPurchase = true;
                    break;
                }
            }
            if (cancelPurchase) {
                e.setCanceled(true);
                return;
            }
            //Remove items
            for (Map.Entry<String, Integer> itemCur : itemPrices.entrySet()) {
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocationHelper.of(itemCur.getKey()));
                int count = 0;
                //Get player's item balance
                for (ItemStack itemstack : e.getEntityPlayer().inventory.items) {
                    if (itemstack.getItem().equals(item)) {
                        count += itemstack.getCount();
                    }
                }
                int total = itemCur.getValue() * e.getItem().getCount();
                ArrayList<ItemStack> toRemoveItems = new ArrayList<>();
                //Remove item from inventory
                for (ItemStack itemStack : e.getEntityPlayer().inventory.items) {
                    if (itemStack.getItem().equals(item)) {
                        int stackCount = itemStack.getCount();
                        if (stackCount <= total) {
                            total -= stackCount;
                            toRemoveItems.add(itemStack);
                        } else {
                            itemStack.shrink(total);
                            break;
                        }
                    }
                }
                toRemoveItems.forEach(removed -> e.getEntityPlayer().inventory.removeItem(removed));
            }
            BankAccount account = BankAccountProxy.getBankAccount(e.getEntityPlayer()).orElse(null);
            if(account != null) {
                if(shopItem != null) {
                    ShopItemWithVariation shopItemVar = new ShopItemWithVariation(new ShopItem(shopItem, 1 ,1, false));
                    //Get Cost Data
                    float cost = shopItemVar.getBuyCost() * e.getItem().getCount();
                    System.out.println(cost);
                    account.take(cost);
                }
            }
            //Give reward
            if (nbt.contains("clovercmd")) {
                handleCommandPurchase(nbt, e.getEntityPlayer(), e.getItem().getCount());
            } else {
                nbt.remove("display");
                nbt.remove("cloveritemcur");
                e.getEntityPlayer().inventory.add(e.getItem());
            }
            e.setCanceled(true);
        }
    }

    public boolean validateNbt(CompoundNBT nbt) {
        if (Objects.isNull(nbt)) {
            return false;
        } else return nbt.contains("clovercur") && nbt.contains("clovercost");
    }

    public boolean validateItemCurrencyNbt(CompoundNBT nbt) {
        if (Objects.isNull(nbt)) {
            return false;
        } else return nbt.contains("cloveritemcur");
    }

    public void handleCommandPurchase(CompoundNBT nbt, ServerPlayerEntity player, int count) {
        String cmd = nbt.getString("clovercmd");
        String[] split = cmd.split(" ");
        //Handle PLAYER placeholder.
        cmd = cmd.replaceAll("PLAYER", player.getName().getString());
        MinecraftServer world = player.getServer();
        //Handle who/what runs the command.
        if (Objects.equals(split[0], "console")) {
            assert world != null;
            cmd = cmd.replaceAll("console ", "");
            for (int i = 0; i < count; i++) {
                world.getCommands().performCommand(world.createCommandSourceStack(), cmd);
            }
        } else if (Objects.equals(split[0], "self")) {
            assert world != null;
            cmd = cmd.replaceAll("self ", "");
            for (int i = 0; i < count; i++)
                world.getCommands().performCommand(player.createCommandSourceStack(), cmd);
        }
    }

    public HashMap<String, Integer> handleItemCurrencies(CompoundNBT nbt) {
        HashMap<String, Integer> shopPrices = new HashMap<>();
        ListNBT itemCurs = nbt.getList("cloveritemcur", Constants.NBT.TAG_COMPOUND);
        for (INBT itemCur : itemCurs) {
            if (itemCur instanceof CompoundNBT) {
                if (((CompoundNBT) itemCur).contains("name") || ((CompoundNBT) itemCur).contains("cost")) {
                    String name = ((CompoundNBT) itemCur).getString("name");
                    int cost = ((CompoundNBT) itemCur).getInt("cost");
                    shopPrices.put(name, cost);
                }
            }
        }
        return shopPrices;
    }
}

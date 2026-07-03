package net.villagerzock;

import net.villagerzock.annotations.Action;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DefaultActions {

    @Action("execute_command")
    public static void executeCommand(String command, CommandSender executor) {
        Bukkit.dispatchCommand(executor, command);
    }

    @Action("execute_console_command")
    public static void executeConsoleCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    @Action("send_message")
    public static void sendMessage(Player player, String message) {
        player.sendMessage(message);
    }

    @Action("broadcast")
    public static void broadcast(String message) {
        Bukkit.broadcastMessage(message);
    }

    @Action("teleport")
    public static void teleport(Player player, Location location) {
        player.teleport(location);
    }

    @Action("kick")
    public static void kick(Player player, String reason) {
        player.kickPlayer(reason);
    }

    @Action("play_sound")
    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    @Action("play_sound_at")
    public static void playSound(Location location, Sound sound, float volume, float pitch) {
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    @Action("set_health")
    public static void setHealth(Player player, double health) {
        player.setHealth(Math.min(health, player.getMaxHealth()));
    }

    @Action("heal")
    public static void heal(Player player) {
        player.setHealth(player.getMaxHealth());
    }

    @Action("damage")
    public static void damage(Player player, double amount) {
        player.damage(amount);
    }

    @Action("feed")
    public static void feed(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }

    @Action("set_food")
    public static void setFood(Player player, int food) {
        player.setFoodLevel(food);
    }

    @Action("set_gamemode")
    public static void setGamemode(Player player, org.bukkit.GameMode gamemode) {
        player.setGameMode(gamemode);
    }

    @Action("give_exp")
    public static void giveExp(Player player, int amount) {
        player.giveExp(amount);
    }

    @Action("send_actionbar")
    public static void sendActionbar(Player player, String message) {
        player.sendActionBar(message);
    }

    @Action("send_title")
    public static void sendTitle(Player player, String title, String subtitle) {
        player.sendTitle(title, subtitle);
    }
}
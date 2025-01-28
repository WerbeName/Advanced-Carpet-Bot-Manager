package carpetbotrestriction;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.apache.logging.log4j.core.jmx.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

public class CarpetBotRestriction implements ModInitializer {
	public static final String MOD_ID = "carpetbotrestriction";
	public static final int MAX_BOTS = 2;
	public static HashMap<String, HashSet<String>> PLAYERS = new HashMap<>();
	public static HashMap<String, String> BOTS = new HashMap<>();

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution
	}
	public static void say(ServerPlayerEntity player, String message) {
		if (player == null) return;
		player.sendMessage(Text.literal("[Carpet Bot Restriction] ").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xF07F1D))).append(Text.literal(message).setStyle(Style.EMPTY.withColor(TextColor. fromRgb(0xAAAAAA)))));
	}

	public static ServerPlayerEntity getBot(CommandContext<ServerCommandSource> context) {
		String playerName = StringArgumentType.getString(context, "player");
		MinecraftServer server = (context.getSource()).getServer();
		return server.getPlayerManager().getPlayer(playerName);
	}
}
package me.kteq.hiddenarmor.handler;

import com.google.common.collect.ImmutableMap;
import me.kteq.hiddenarmor.HiddenArmor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageHandler {

    private static MessageHandler instance;

    private final Map<String, Map<String, String>> localeMap = new ConcurrentHashMap<>();

    private HiddenArmor plugin;
    private String defaultLocale;
    private String prefix = "";
    private Map<String, String> defaultMessageMap = Collections.emptyMap();

    public static MessageHandler getInstance() {
        if (instance == null) {
            instance = new MessageHandler();
        }
        return instance;
    }

    public void setup(HiddenArmor plugin, String prefix) {
        this.plugin = plugin;
        this.prefix = prefix;

        try (var inputStream = plugin.getResource("locale/en_us.yml")) {
            if (inputStream != null) {
                try (var inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    defaultMessageMap = readConfiguration(YamlConfiguration.loadConfiguration(inputStreamReader));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        reloadLocales();
    }

    public void reloadLocales() {
        this.defaultLocale = plugin.getHiddenArmorConfig().defaultLocale().replace('-', '_').toLowerCase(Locale.ENGLISH);

        var dir = plugin.getDataFolder().toPath().resolve("locale");

        for (String locale : List.of("en_us", "pt_br")) {
            if (Files.notExists(dir.resolve(locale + ".yml"))) {
                plugin.saveResource("locale/" + locale + ".yml", false);
            }
        }

        localeMap.clear();

        try (var list = Files.list(dir)) {
            list.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".yml"))
                    .forEach(path -> {
                        FileConfiguration localeYaml = YamlConfiguration.loadConfiguration(path.toFile());
                        localeMap.put(path.getFileName().toString().replace(".yml", ""), readConfiguration(localeYaml));
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void message(CommandSender sender, String message) {
        message(sender, message, false);
    }

    public void message(CommandSender sender, String message, boolean prefix) {
        message(sender, message, prefix, new HashMap<>());
    }

    public void message(CommandSender sender, String message, boolean prefix, Map<String, String> placeholderMap) {
        var replaced = replacePlaceholders(sender, message, prefix, placeholderMap);
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(replaced));
    }

    public void actionBar(CommandSender sender, String message, boolean prefix, Map<String, String> placeholderMap) {
        if (sender instanceof Player player) {
            var replaced = replacePlaceholders(player, message, prefix, placeholderMap);
            player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(replaced));
        }
    }

    private String replacePlaceholders(CommandSender sender, String message, boolean prefix, Map<String, String> placeholderMap) {
        message = replaceHoldersFromConfig(sender, message);

        for (String placeholder : placeholderMap.keySet()) {
            String value = placeholderMap.get(placeholder);
            value = replaceHoldersFromConfig(sender, value);

            message = message.replace("%" + placeholder + "%", value);
        }

        if (prefix) {
            message = this.prefix + message;
        }

        return message;
    }

    private String replaceHoldersFromConfig(CommandSender sender, String message) {
        for (String string : message.split("%")) {
            if (string.contains(" ")) continue;
            String localizedMessage = getLocalizedMessage(sender, string);

            if (localizedMessage != null) {
                message = message.replace("%" + string + "%", localizedMessage);
            }
        }

        return message;
    }

    private String getLocalizedMessage(CommandSender sender, String messageKey) {
        String locale;

        if (sender instanceof Player player) {
            locale = player.locale().toString().toLowerCase();
        } else {
            locale = defaultLocale;
        }

        return localeMap.getOrDefault(locale, defaultMessageMap).getOrDefault(messageKey, messageKey);
    }

    private Map<String, String> readConfiguration(ConfigurationSection source) {
        var builder = ImmutableMap.<String, String>builder();

        for (var key : source.getKeys(false)) {
            builder.put(key, source.getString(key, key));
        }

        return builder.build();
    }
}

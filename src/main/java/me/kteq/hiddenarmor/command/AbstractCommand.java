package me.kteq.hiddenarmor.command;

import me.kteq.hiddenarmor.handler.MessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractCommand extends BukkitCommand implements CommandExecutor {
    private final String pluginName;
    private final boolean defaultPermission;
    private String permission;
    private final int minArguments;
    private final int maxArguments;
    private final boolean playerOnly;

    public AbstractCommand(JavaPlugin plugin, String command, int minArguments, int maxArguments, boolean playerOnly, boolean defaultPermission) {
        super(command);

        this.pluginName = plugin.getName().toLowerCase().replace(" ", "");

        this.minArguments = minArguments;
        this.maxArguments = maxArguments;
        this.playerOnly = playerOnly;
        this.defaultPermission = defaultPermission;
        this.setCPermission(command);

        CommandMap commandMap = getCommandMap();
        if (commandMap != null) {
            commandMap.register(this.pluginName, this);
        }
    }

    public CommandMap getCommandMap() {
        return Bukkit.getCommandMap();
    }

    public AbstractCommand setCPermission(String perm) {
        super.setPermission(defaultPermission ? null : this.pluginName + "." + perm);
        this.permission = this.pluginName + "." + perm;
        return this;
    }

    protected boolean canUseArg(CommandSender sender, String arg) {
        return sender.isOp() || sender.hasPermission(permission + "." + arg);
    }

    public boolean execute(CommandSender sender, String alias, String[] arguments) {
        MessageHandler messageHandler = MessageHandler.getInstance();
        String permission = getPermission();
        if (!defaultPermission && permission != null && !sender.hasPermission(permission) && !sender.isOp()) {
            messageHandler.message(sender, "%command-no-permission%");
            return true;
        }

        if (arguments.length < minArguments || arguments.length > maxArguments) {
            sendUsage(sender);
            return true;
        }

        if (playerOnly && !(sender instanceof Player)) {
            messageHandler.message(sender, "%command-player-only%");
            return true;
        }

        if (!onCommand(sender, arguments)) {
            sendUsage(sender);
        }

        return true;
    }

    public boolean onCommand(CommandSender sender, Command command, String alias, String[] arguments) {
        return this.onCommand(sender, arguments);
    }

    public abstract boolean onCommand(CommandSender sender, String[] arguments);

    public abstract void sendUsage(CommandSender sender);
}

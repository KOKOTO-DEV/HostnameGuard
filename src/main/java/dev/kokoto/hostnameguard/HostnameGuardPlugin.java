package dev.kokoto.hostnameguard;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.IDN;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class HostnameGuardPlugin extends JavaPlugin implements Listener {

    private final Set<String> allowedHosts = new HashSet<>();
    private final Set<String> wildcardHosts = new HashSet<>();

    private boolean allowWildcards;
    private boolean allowUnknownHost;
    private boolean enableBypassPermission;
    private boolean logDenied;
    private boolean logAllowed;

    private String bypassPermission;
    private String kickMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("HostnameGuard enabled.");
        getLogger().info("Allowed hosts: " + allowedHosts);
        if (allowWildcards) {
            getLogger().info("Wildcard hosts: " + wildcardHosts);
        }
    }

    private void loadSettings() {
        reloadConfig();

        allowedHosts.clear();
        wildcardHosts.clear();

        for (String host : getConfig().getStringList("allowed-hosts")) {
            String normalized = normalizeHostOnly(host);
            if (!normalized.isEmpty()) {
                allowedHosts.add(normalized);
            }
        }

        for (String host : getConfig().getStringList("wildcard-hosts")) {
            String normalized = normalizeHostOnly(host);
            if (!normalized.isEmpty()) {
                wildcardHosts.add(normalized);
            }
        }

        allowWildcards = getConfig().getBoolean("allow-wildcards", true);
        allowUnknownHost = getConfig().getBoolean("allow-unknown-host", false);
        enableBypassPermission = getConfig().getBoolean("enable-bypass-permission", false);
        logDenied = getConfig().getBoolean("log-denied", true);
        logAllowed = getConfig().getBoolean("log-allowed", false);

        bypassPermission = getConfig().getString("bypass-permission", "hostnameguard.bypass");
        kickMessage = color(getConfig().getString("kick-message", "&c공식 주소로 접속해주세요."));

        if (allowedHosts.isEmpty() && wildcardHosts.isEmpty()) {
            getLogger().warning("No allowed hostnames are configured. Most joins will be denied.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        if (enableBypassPermission
                && bypassPermission != null
                && !bypassPermission.isBlank()
                && player.hasPermission(bypassPermission)) {
            if (logAllowed) {
                getLogger().info("Allowed " + player.getName() + " by bypass permission.");
            }
            return;
        }

        String rawHost = event.getHostname();
        String host = normalizeHostOnly(rawHost);

        if (host.isEmpty()) {
            if (allowUnknownHost) {
                if (logAllowed) {
                    getLogger().info("Allowed " + player.getName() + " because host is unknown and allow-unknown-host=true. rawHost=" + rawHost);
                }
                return;
            }

            deny(event, player.getName(), rawHost, "unknown host");
            return;
        }

        if (isAllowed(host)) {
            if (logAllowed) {
                getLogger().info("Allowed " + player.getName() + " / host=" + host + " / rawHost=" + rawHost);
            }
            return;
        }

        deny(event, player.getName(), rawHost, "not in whitelist");
    }

    private boolean isAllowed(String host) {
        if (allowedHosts.contains(host)) {
            return true;
        }

        if (!allowWildcards) {
            return false;
        }

        for (String pattern : wildcardHosts) {
            if (!pattern.startsWith("*.")) {
                continue;
            }

            String suffix = pattern.substring(1); // ".example.com"

            // *.example.com allows a.example.com, but not example.com itself.
            if (host.endsWith(suffix) && host.length() > suffix.length()) {
                return true;
            }
        }

        return false;
    }

    private void deny(PlayerLoginEvent event, String playerName, String rawHost, String reason) {
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, kickMessage);

        if (logDenied) {
            getLogger().info("Denied " + playerName
                    + " / host=" + rawHost
                    + " / reason=" + reason);
        }
    }

    private String normalizeHostOnly(String input) {
        if (input == null) {
            return "";
        }

        String s = input.trim();

        // Some proxies/modded servers can append extra data after a NUL byte.
        int nullIndex = s.indexOf('\0');
        if (nullIndex >= 0) {
            s = s.substring(0, nullIndex);
        }

        s = s.toLowerCase(Locale.ROOT);

        // IPv6 bracket form: [::1]:25565
        if (s.startsWith("[")) {
            int end = s.indexOf(']');
            if (end > 0) {
                s = s.substring(1, end);
            }
        } else {
            // Remove :port from normal host:port, but do not break raw IPv6 addresses.
            int colon = s.lastIndexOf(':');
            if (colon > -1 && s.indexOf(':') == colon) {
                String afterColon = s.substring(colon + 1);
                if (afterColon.matches("\\d+")) {
                    s = s.substring(0, colon);
                }
            }
        }

        if (s.endsWith(".")) {
            s = s.substring(0, s.length() - 1);
        }

        try {
            s = IDN.toASCII(s);
        } catch (IllegalArgumentException ignored) {
            return "";
        }

        return s;
    }

    private String color(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("hostnameguard.reload")) {
                sender.sendMessage(ChatColor.RED + "권한이 없습니다.");
                return true;
            }

            loadSettings();
            sender.sendMessage(ChatColor.GREEN + "HostnameGuard 설정을 다시 불러왔습니다.");
            sender.sendMessage(ChatColor.GRAY + "Allowed hosts: " + allowedHosts);
            if (allowWildcards) {
                sender.sendMessage(ChatColor.GRAY + "Wildcard hosts: " + wildcardHosts);
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
            if (!sender.hasPermission("hostnameguard.status")) {
                sender.sendMessage(ChatColor.RED + "권한이 없습니다.");
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "HostnameGuard status");
            sender.sendMessage(ChatColor.GRAY + "Allowed hosts: " + allowedHosts);
            sender.sendMessage(ChatColor.GRAY + "Allow wildcards: " + allowWildcards);
            sender.sendMessage(ChatColor.GRAY + "Wildcard hosts: " + wildcardHosts);
            sender.sendMessage(ChatColor.GRAY + "Allow unknown host: " + allowUnknownHost);
            sender.sendMessage(ChatColor.GRAY + "Bypass permission enabled: " + enableBypassPermission);
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " status");
        return true;
    }

    public Set<String> getAllowedHostsView() {
        return Collections.unmodifiableSet(allowedHosts);
    }
}

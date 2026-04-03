package protocolsupport.config;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class PluginConfig {

	private static volatile boolean encapsulationEnabledByDefault = true;
	private static volatile Set<String> noEncapsulationTargets = Collections.emptySet();

	private PluginConfig() {
	}

	public static void load(Plugin plugin) {
		File dataFolder = plugin.getDataFolder();
		if (!dataFolder.exists() && !dataFolder.mkdirs()) {
			plugin.getLogger().log(Level.WARNING, "Failed to create plugin data folder: {0}", dataFolder.getAbsolutePath());
		}
		File configFile = new File(dataFolder, "config.yml");
		createDefaultConfig(configFile, plugin);
		try {
			Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
			encapsulationEnabledByDefault = config.getBoolean("encapsulation.enabled-by-default", true);
			noEncapsulationTargets = parseTargets(config.getStringList("encapsulation.disable-for-targets"));
			plugin.getLogger().log(
				Level.INFO,
				MessageFormat.format(
					"Loaded config: encapsulation.enabled-by-default={0}, disable-for-targets={1}",
					encapsulationEnabledByDefault,
					noEncapsulationTargets.size()
				)
			);
		} catch (IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to load config.yml, using defaults", e);
			encapsulationEnabledByDefault = true;
			noEncapsulationTargets = Collections.emptySet();
		}
	}

	public static boolean shouldSendEncapsulation(SocketAddress targetAddress) {
		if (!(targetAddress instanceof InetSocketAddress)) {
			return encapsulationEnabledByDefault;
		}
		InetSocketAddress target = (InetSocketAddress) targetAddress;
		String host = target.getHostString().toLowerCase(Locale.ROOT);
		int port = target.getPort();
		if (noEncapsulationTargets.contains(normalizeTarget(host, port))) {
			return false;
		}
		if (target.getAddress() != null) {
			String ip = target.getAddress().getHostAddress().toLowerCase(Locale.ROOT);
			if (noEncapsulationTargets.contains(normalizeTarget(ip, port))) {
				return false;
			}
		}
		return encapsulationEnabledByDefault;
	}

	private static Set<String> parseTargets(List<String> rawTargets) {
		if (rawTargets == null || rawTargets.isEmpty()) {
			return Collections.emptySet();
		}
		HashSet<String> normalized = new HashSet<>();
		for (String rawTarget : rawTargets) {
			if (rawTarget == null) {
				continue;
			}
			String value = rawTarget.trim().toLowerCase(Locale.ROOT);
			int idx = value.lastIndexOf(':');
			if (idx <= 0 || idx == value.length() - 1) {
				continue;
			}
			try {
				int port = Integer.parseInt(value.substring(idx + 1));
				normalized.add(normalizeTarget(value.substring(0, idx), port));
			} catch (NumberFormatException ignored) {
			}
		}
		return Collections.unmodifiableSet(normalized);
	}

	private static String normalizeTarget(String host, int port) {
		return host + ":" + port;
	}

	private static void createDefaultConfig(File configFile, Plugin plugin) {
		if (configFile.exists()) {
			return;
		}
		try {
			String defaultConfig = String.join(
				"\n",
				"# ProtocolSupportBungee config",
				"encapsulation:",
				"  # Keep true for normal behavior.",
				"  enabled-by-default: true",
				"  # Backend targets (host:port) that should use the old/direct method without encapsulated handshake.",
				"  disable-for-targets: []",
				""
			);
			Files.write(configFile.toPath(), defaultConfig.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to create default config.yml", e);
		}
	}

}

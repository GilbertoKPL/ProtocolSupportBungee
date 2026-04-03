package protocolsupport.config;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
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
import protocolsupport.api.ProtocolVersion;

public class PluginConfig {

	public static class VersionRoute {
		public final ProtocolVersion min;
		public final ProtocolVersion max;
		public final String serverName;

		public VersionRoute(ProtocolVersion min, ProtocolVersion max, String serverName) {
			this.min = min;
			this.max = max;
			this.serverName = serverName;
		}
	}

	private static volatile boolean encapsulationEnabledByDefault = false;
	private static volatile Set<String> noEncapsulationTargets = Collections.emptySet();
	private static volatile List<VersionRoute> versionRoutes = Collections.emptyList();

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
			encapsulationEnabledByDefault = config.getBoolean("encapsulation.enabled-by-default", false);
			noEncapsulationTargets = parseTargets(config.getStringList("encapsulation.disable-for-targets"));
			List<VersionRoute> routes = new ArrayList<>();
			Configuration routingSection = config.getSection("version-routing");
			if (routingSection != null) {
				for (String serverName : routingSection.getKeys()) {
					Configuration entry = routingSection.getSection(serverName);
					if (entry == null) {
						continue;
					}
					ProtocolVersion min = findVersionByName(entry.getString("min-version"));
					ProtocolVersion max = findVersionByName(entry.getString("max-version"));
					if (min == null || max == null) {
						plugin.getLogger().log(Level.WARNING, "Invalid version-routing entry for server: {0} (check min-version/max-version)", serverName);
						continue;
					}
					routes.add(new VersionRoute(min, max, serverName));
				}
			}
			versionRoutes = Collections.unmodifiableList(routes);
			plugin.getLogger().log(
				Level.INFO,
				MessageFormat.format(
					"Loaded config: encapsulation.enabled-by-default={0}, disable-for-targets={1}, version-routing-entries={2}",
					encapsulationEnabledByDefault,
					noEncapsulationTargets.size(),
					versionRoutes.size()
				)
			);
		} catch (IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to load config.yml, using defaults", e);
			encapsulationEnabledByDefault = false;
			noEncapsulationTargets = Collections.emptySet();
			versionRoutes = Collections.emptyList();
		}
	}

	public static String getServerForVersion(ProtocolVersion version) {
		for (VersionRoute route : versionRoutes) {
			if (version.isBetween(route.min, route.max)) {
				return route.serverName;
			}
		}
		return null;
	}

	private static ProtocolVersion findVersionByName(String name) {
		if (name == null) {
			return null;
		}
		for (ProtocolVersion version : ProtocolVersion.getAllSupported()) {
			if (name.equals(version.getName())) {
				return version;
			}
		}
		return null;
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
				"  # Set to true ONLY if ALL your backend servers have ProtocolSupport plugin installed.",
				"  # If any backend server does NOT have ProtocolSupport, keep this false.",
				"  enabled-by-default: false",
				"  # Backend targets (host:port) that should USE the encapsulated handshake (when enabled-by-default is false).",
				"  # Or targets to SKIP encapsulation for (when enabled-by-default is true).",
				"  disable-for-targets: []",
				"",
				"# Version-based routing: route players to specific BungeeCord servers based on their client version.",
				"# Each entry key must match a server name defined in BungeeCord's config.yml.",
				"# min-version and max-version are inclusive. Valid version names: 1.4.7, 1.5.1, 1.5.2,",
				"# 1.6.1, 1.6.2, 1.6.4, 1.7.5, 1.7.10, 1.8, 1.9, 1.9.1, 1.9.2, 1.9.4, 1.10, 1.11, 1.11.2,",
				"# 1.12, 1.12.1, 1.12.2, 1.13, 1.13.1, 1.13.2, 1.14, 1.14.1, 1.14.2, 1.14.3, 1.14.4,",
				"# 1.15, 1.15.1, 1.15.2, 1.16, 1.16.1, 1.16.2, 1.16.3, 1.16.4",
				"# Example:",
				"# version-routing:",
				"#   server152:",
				"#     min-version: \"1.4.7\"",
				"#     max-version: \"1.5.2\"",
				"#   server1710:",
				"#     min-version: \"1.6.1\"",
				"#     max-version: \"1.7.10\"",
				"#   server1165:",
				"#     min-version: \"1.8\"",
				"#     max-version: \"1.16.4\"",
				"version-routing: {}",
				""
			);
			Files.write(configFile.toPath(), defaultConfig.getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Failed to create default config.yml", e);
		}
	}

}

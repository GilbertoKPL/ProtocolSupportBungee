package protocolsupport;

import java.util.logging.Level;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import protocolsupport.injector.BungeeNettyChannelInjector;

public class ProtocolSupport extends Plugin {

	@Override
	public void onLoad() {
		try {
			getProxy().getPluginManager().registerCommand(this, new CommandHandler());
			BungeeNettyChannelInjector.inject();
		} catch (Throwable t) {
			getLogger().log(Level.SEVERE, "Error occured while initalizing", t);
		}
	}

	@Override
	public void onEnable() {
		// Newer Bungee builds removed `disable_entity_metadata_rewrite`; keep plugin startup compatible.
		if (ProxyServer.getInstance().getConfig() == null) {
			getLogger().log(Level.WARNING, "Proxy config is unavailable during enable");
		}
	}

}

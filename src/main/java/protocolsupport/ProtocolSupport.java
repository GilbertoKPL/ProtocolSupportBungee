package protocolsupport;

import java.lang.reflect.Method;
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

	@SuppressWarnings("deprecation")
	@Override
	public void onEnable() {
		try {
			Method rewriteFlagMethod = ProxyServer.getInstance().getConfig().getClass().getMethod("isDisableEntityMetadataRewrite");
			Object rewriteFlag = rewriteFlagMethod.invoke(ProxyServer.getInstance().getConfig());
			if (Boolean.FALSE.equals(rewriteFlag)) {
				getLogger().log(Level.SEVERE, "Entity metadata rewrite must be disabled in order for plugin to work");
			}
		} catch (ReflectiveOperationException ignored) {
			// Config option was removed in newer BungeeCord builds.
		}
	}

}

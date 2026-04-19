package protocolsupport.protocol.pipeline.common;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.protocol.packet.Handshake;
import net.md_5.bungee.protocol.packet.LoginRequest;
import protocolsupport.api.events.ConnectionCloseEvent;
import protocolsupport.api.events.ConnectionOpenEvent;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.storage.ProtocolStorage;

public class LogicHandler extends ChannelDuplexHandler {

	private static final InternalLogger logger = InternalLoggerFactory.getInstance(LogicHandler.class);
	private static final boolean debugConnection = Boolean.getBoolean("ps.debug.connection");

	protected final ConnectionImpl connection;
	protected final boolean isClientConnection;
	public LogicHandler(ConnectionImpl connection, boolean isClientConnection) {
		this.connection = connection;
		this.isClientConnection = isClientConnection;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		msg = connection.handlePacketReceive(msg, isClientConnection);
		if (msg == null) {
			return;
		}
		super.channelRead(ctx, msg);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		msg = connection.handlePacketSend(msg, isClientConnection);
		if (msg == null) {
			promise.setSuccess();
			return;
		}
		if (debugConnection && !isClientConnection) {
			if (msg instanceof Handshake) {
				Handshake handshake = (Handshake) msg;
				logger.info("[ps-debug] proxy->server handshake protocolVersion={} host={} port={} requestedProtocol={}",
					handshake.getProtocolVersion(), handshake.getHost(), handshake.getPort(), handshake.getRequestedProtocol());
			} else if (msg instanceof LoginRequest) {
				logger.info("[ps-debug] proxy->server login request username={}", ((LoginRequest) msg).getData());
			}
		}
		super.write(ctx, msg, promise);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		if (isClientConnection) {
			if (debugConnection) {
				logger.info("[ps-debug] client channel active address={} version={} profile={}", connection.getRawAddress(), connection.getVersion(), connection.getProfile());
			}
			ProxyServer.getInstance().getPluginManager().callEvent(new ConnectionOpenEvent(connection));
		} else if (debugConnection) {
			logger.info("[ps-debug] server channel active serverAddress={} clientAddress={} version={}", ctx.channel().remoteAddress(), connection.getRawAddress(), connection.getVersion());
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		if (isClientConnection) {
			if (debugConnection) {
				logger.info("[ps-debug] client channel inactive address={} version={} player={} connected={}", connection.getRawAddress(), connection.getVersion(), connection.getPlayer(), connection.isConnected());
			}
			ProxyServer.getInstance().getPluginManager().callEvent(new ConnectionCloseEvent(connection));
			ProtocolStorage.removeConnection(connection.getRawAddress());
		} else if (debugConnection) {
			logger.info("[ps-debug] server channel inactive serverAddress={} clientAddress={} version={} player={}", ctx.channel().remoteAddress(), connection.getRawAddress(), connection.getVersion(), connection.getPlayer());
		}
	}

}

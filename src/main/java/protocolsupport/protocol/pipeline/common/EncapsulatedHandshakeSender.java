package protocolsupport.protocol.pipeline.common;

import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.md_5.bungee.BungeeCord;
import protocolsupport.config.PluginConfig;
import protocolsupport.protocol.utils.EncapsulatedProtocolInfo;
import protocolsupport.protocol.utils.EncapsulatedProtocolUtils;

public class EncapsulatedHandshakeSender extends ChannelInboundHandlerAdapter {

	private final InetSocketAddress remote;
	private final boolean hasCompression;
	private final AtomicBoolean loggedFailure = new AtomicBoolean(false);
	private final AtomicBoolean becameActive = new AtomicBoolean(false);
	public EncapsulatedHandshakeSender(InetSocketAddress remote, boolean hasCompression) {
		this.remote = remote;
		this.hasCompression = hasCompression;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		becameActive.set(true);
		if (!PluginConfig.shouldSendEncapsulation(ctx.channel().remoteAddress())) {
			ctx.pipeline().remove(this);
			super.channelActive(ctx);
			return;
		}
		ByteBuf handshake = ctx.alloc().buffer();
		handshake.writeByte(EncapsulatedProtocolUtils.FIRST_BYTE);
		EncapsulatedProtocolUtils.writeInfo(handshake, new EncapsulatedProtocolInfo(remote, hasCompression));
		ctx.writeAndFlush(handshake).addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
		super.channelActive(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		if (becameActive.get()) {
			super.exceptionCaught(ctx, cause);
			return;
		}
		logFailureIfNeeded(ctx, cause);
		super.exceptionCaught(ctx, cause);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		if (becameActive.get()) {
			super.channelInactive(ctx);
			return;
		}
		logFailureIfNeeded(ctx, null);
		super.channelInactive(ctx);
	}

	private void logFailureIfNeeded(ChannelHandlerContext ctx, Throwable cause) {
		if (!loggedFailure.compareAndSet(false, true)) {
			return;
		}
		Object target = remote != null ? remote : ctx.channel().remoteAddress();
		String message = cause == null ? "channel closed before login completed" : cause.toString();
		BungeeCord.getInstance().getLogger().log(
			Level.WARNING,
			MessageFormat.format(
				"[ProtocolSupportBungee] Backend connection problem while preparing login to {0}: {1}",
				target,
				message
			)
		);
	}

}

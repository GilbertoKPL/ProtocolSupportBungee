package protocolsupport.protocol.packet.middleimpl.readable.play.v_4_5_6;

import java.util.Collection;
import java.util.Collections;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.KeepAlive;
import protocolsupport.protocol.packet.id.LegacyPacketId;
import protocolsupport.protocol.packet.middleimpl.readable.LegacyDefinedReadableMiddlePacket;

public class KeepAlivePacket extends LegacyDefinedReadableMiddlePacket {

	private static final InternalLogger logger = InternalLoggerFactory.getInstance(KeepAlivePacket.class);
	private static final boolean debugConnection = Boolean.getBoolean("ps.debug.connection");

	public KeepAlivePacket() {
		super(LegacyPacketId.Dualbound.PLAY_KEEP_ALIVE);
	}

	protected int keepaliveId;

	@Override
	protected void read0(ByteBuf data) {
		keepaliveId = data.readInt();
		if (debugConnection) {
			logger.info("[ps-debug] legacy->native keepalive id={}", keepaliveId);
		}
	}

	@Override
	public Collection<PacketWrapper> toNative() {
		return Collections.singletonList(new PacketWrapper(new KeepAlive(keepaliveId), Unpooled.wrappedBuffer(readbytes)));
	}

}

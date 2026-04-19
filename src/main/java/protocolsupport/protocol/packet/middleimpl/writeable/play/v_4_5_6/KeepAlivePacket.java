package protocolsupport.protocol.packet.middleimpl.writeable.play.v_4_5_6;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import net.md_5.bungee.protocol.packet.KeepAlive;
import protocolsupport.protocol.packet.id.LegacyPacketId;
import protocolsupport.protocol.packet.middleimpl.writeable.LegacySingleWriteablePacket;

public class KeepAlivePacket extends LegacySingleWriteablePacket<KeepAlive> {

	private static final InternalLogger logger = InternalLoggerFactory.getInstance(KeepAlivePacket.class);
	private static final boolean debugConnection = Boolean.getBoolean("ps.debug.connection");

	public KeepAlivePacket() {
		super(LegacyPacketId.Dualbound.PLAY_KEEP_ALIVE);
	}

	@Override
	protected void write(ByteBuf data, KeepAlive packet) {
		if (debugConnection) {
			logger.info("[ps-debug] native->legacy keepalive id={}", packet.getRandomId());
		}
		data.writeInt((int) packet.getRandomId());
	}

}

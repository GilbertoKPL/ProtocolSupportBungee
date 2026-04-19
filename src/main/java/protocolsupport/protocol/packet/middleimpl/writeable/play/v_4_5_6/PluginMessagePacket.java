package protocolsupport.protocol.packet.middleimpl.writeable.play.v_4_5_6;

import io.netty.buffer.ByteBuf;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import net.md_5.bungee.protocol.packet.PluginMessage;
import protocolsupport.protocol.packet.id.LegacyPacketId;
import protocolsupport.protocol.packet.middleimpl.writeable.LegacySingleWriteablePacket;
import protocolsupport.protocol.serializer.ArraySerializer;
import protocolsupport.protocol.serializer.StringSerializer;

public class PluginMessagePacket extends LegacySingleWriteablePacket<PluginMessage> {

	private static final InternalLogger logger = InternalLoggerFactory.getInstance(PluginMessagePacket.class);
	private static final boolean debugConnection = Boolean.getBoolean("ps.debug.connection");

	public PluginMessagePacket() {
		super(LegacyPacketId.Dualbound.PLAY_PLUGIN_MESSAGE);
	}

	@Override
	protected void write(ByteBuf data, PluginMessage packet) {
		String tag = packet.getTag();
		if (tag.equals("bungeecord:main")) {
			tag = "BungeeCord";
		}
		if (debugConnection) {
			logger.info("[ps-debug] native->legacy plugin message tag={} bytes={}", tag, packet.getData().length);
		}
		StringSerializer.writeShortUTF16BEString(data, tag);
		ArraySerializer.writeShortLengthByteArray(data, packet.getData());
	}

}

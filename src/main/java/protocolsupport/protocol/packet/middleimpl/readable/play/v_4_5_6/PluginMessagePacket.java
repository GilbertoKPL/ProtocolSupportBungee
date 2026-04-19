package protocolsupport.protocol.packet.middleimpl.readable.play.v_4_5_6;

import java.util.Collection;
import java.util.Collections;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.PluginMessage;
import protocolsupport.protocol.packet.id.LegacyPacketId;
import protocolsupport.protocol.packet.middleimpl.readable.LegacyDefinedReadableMiddlePacket;
import protocolsupport.protocol.serializer.ArraySerializer;
import protocolsupport.protocol.serializer.StringSerializer;

public class PluginMessagePacket extends LegacyDefinedReadableMiddlePacket {

	private static final InternalLogger logger = InternalLoggerFactory.getInstance(PluginMessagePacket.class);
	private static final boolean debugConnection = Boolean.getBoolean("ps.debug.connection");

	public PluginMessagePacket() {
		super(LegacyPacketId.Dualbound.PLAY_PLUGIN_MESSAGE);
	}

	protected String tag;
	protected byte[] data;

	@Override
	protected void read0(ByteBuf from) {
		tag = StringSerializer.readShortUTF16BEString(from);
		data = ArraySerializer.readShortLengthByteArray(from);
		if (debugConnection) {
			logger.info("[ps-debug] legacy->native plugin message tag={} bytes={}", tag, data.length);
		}
	}

	@Override
	public Collection<PacketWrapper> toNative() {
		return Collections.singletonList(new PacketWrapper(new PluginMessage(tag, data, true), Unpooled.wrappedBuffer(readbytes)));
	}

}

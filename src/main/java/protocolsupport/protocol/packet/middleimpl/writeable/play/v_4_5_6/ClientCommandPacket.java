package protocolsupport.protocol.packet.middleimpl.writeable.play.v_4_5_6;

import java.util.Collection;
import java.util.Collections;

import io.netty.buffer.ByteBuf;
import net.md_5.bungee.protocol.DefinedPacket;
import protocolsupport.protocol.packet.id.LegacyPacketId;
import protocolsupport.protocol.packet.middle.WriteableMiddlePacket;
import protocolsupport.utils.netty.Allocator;

public class ClientCommandPacket extends WriteableMiddlePacket<DefinedPacket> {

	@Override
	public Collection<ByteBuf> toData(DefinedPacket packet) {
		byte payload;
		try {
			payload = ((Number) packet.getClass().getMethod("getPayload").invoke(packet)).byteValue();
		} catch (ReflectiveOperationException e) {
			return Collections.emptyList();
		}
		if (payload == 1) {
			ByteBuf data = Allocator.allocateBuffer();
			data.writeByte(LegacyPacketId.Serverbound.LOGIN_PLAY_CLIENT_COMMAND);
			data.writeByte(payload);
			return Collections.singletonList(data);
		} else {
			return Collections.emptyList();
		}
	}

}

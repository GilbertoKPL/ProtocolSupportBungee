package protocolsupport.protocol.packet.middleimpl.readable.play.v_4_5_6;

import java.util.Collection;
import java.util.Collections;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import protocolsupport.protocol.packet.id.LegacyPacketId;
import protocolsupport.protocol.packet.middleimpl.readable.LegacyDefinedReadableMiddlePacket;

public class ClientCommandPacket extends LegacyDefinedReadableMiddlePacket {

	public ClientCommandPacket() {
		super(LegacyPacketId.Serverbound.LOGIN_PLAY_CLIENT_COMMAND);
	}

	protected int status;

	@Override
	protected void read0(ByteBuf from) {
		status = from.readByte();
	}

	@Override
	public Collection<PacketWrapper> toNative() {
		if (status == 1) {
			try {
				Class<?> clazz = Class.forName("net.md_5.bungee.protocol.packet.ClientStatus");
				DefinedPacket packet = (DefinedPacket) clazz.getDeclaredConstructor(byte.class).newInstance((byte) status);
				return Collections.singletonList(protocolsupport.protocol.utils.PacketWrapperFactory.create(packet, Unpooled.wrappedBuffer(readbytes)));
			} catch (ReflectiveOperationException ignored) {
				return Collections.emptyList();
			}
		} else {
			return Collections.emptyList();
		}
	}

}

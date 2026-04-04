package protocolsupport.protocol.packet.middleimpl.readable.handshake.v_4_5_6;

import java.util.Arrays;
import java.util.Collection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.packet.Handshake;
import net.md_5.bungee.protocol.packet.LoginRequest;
import protocolsupport.protocol.packet.id.LegacyPacketId;
import protocolsupport.protocol.packet.middleimpl.readable.LegacyDefinedReadableMiddlePacket;
import protocolsupport.protocol.serializer.StringSerializer;

public class LoginHandshakePacket extends LegacyDefinedReadableMiddlePacket {

	public LoginHandshakePacket() {
		super(LegacyPacketId.Serverbound.HANDSHAKE_LOGIN);
	}

	protected String username;
	protected String host;
	protected int port;
	protected int protocolVersion;

	@Override
	protected void read0(ByteBuf from) {
		protocolVersion = from.readUnsignedByte();
		username = StringSerializer.readShortUTF16BEString(from);
		host = StringSerializer.readShortUTF16BEString(from);
		port = from.readInt();
	}

	@Override
	public Collection<PacketWrapper> toNative() {
		return Arrays.asList(
			new PacketWrapper(new Handshake(protocolVersion, host, port, 2), Unpooled.wrappedBuffer(readbytes)),
			new PacketWrapper(new LoginRequest(username), Unpooled.EMPTY_BUFFER)
		);
	}

}

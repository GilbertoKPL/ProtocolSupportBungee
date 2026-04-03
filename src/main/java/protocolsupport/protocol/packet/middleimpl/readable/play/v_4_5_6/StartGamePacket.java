package protocolsupport.protocol.packet.middleimpl.readable.play.v_4_5_6;

import java.util.Arrays;
import java.util.Collection;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.md_5.bungee.protocol.PacketWrapper;
import protocolsupport.protocol.packet.id.LegacyPacketId;
import protocolsupport.protocol.packet.middleimpl.readable.LegacyDefinedReadableMiddlePacket;
import protocolsupport.protocol.serializer.StringSerializer;

public class StartGamePacket extends LegacyDefinedReadableMiddlePacket {

	public StartGamePacket() {
		super(LegacyPacketId.Clientbound.PLAY_START_GAME);
	}

	protected int entityId;
	protected boolean hardcode;
	protected int gamemode;
	protected int dimension;
	protected int difficulty;
	protected int maxPlayers;
	protected String levelType;

	@Override
	protected void read0(ByteBuf from) {
		entityId = from.readInt();
		levelType = StringSerializer.readShortUTF16BEString(from);
		int	sGamemodeHardcore = from.readByte();
		hardcode = (sGamemodeHardcore & 0b1000) != 0;
		gamemode = (sGamemodeHardcore & 0b111);
		dimension = from.readByte();
		difficulty = from.readByte();
		from.readByte();
		maxPlayers = from.readByte();
	}

	@Override
	public Collection<PacketWrapper> toNative() {
		return Arrays.asList(
			protocolsupport.protocol.utils.PacketWrapperFactory.create(protocolsupport.protocol.utils.LegacyPacketFactory.createLoginSuccess(), Unpooled.EMPTY_BUFFER),
			protocolsupport.protocol.utils.PacketWrapperFactory.create(
				protocolsupport.protocol.utils.LegacyPacketFactory.createLoginPacket(entityId, hardcode, gamemode, dimension, difficulty, maxPlayers, levelType),
				Unpooled.wrappedBuffer(readbytes)
			)
		);
	}

}

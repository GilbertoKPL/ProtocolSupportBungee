package protocolsupport.protocol.packet.middleimpl.readable.play.v_4_5_6;

import protocolsupport.protocol.packet.id.LegacyPacketId;
import protocolsupport.protocol.packet.middleimpl.readable.LegacyFixedLengthPassthroughReadableMiddlePacket;

public class PlayerPositionLookPacket extends LegacyFixedLengthPassthroughReadableMiddlePacket {

	public PlayerPositionLookPacket() {
		super(LegacyPacketId.Serverbound.PLAY_POSITION_LOOK, (Double.BYTES * 4) + (Float.BYTES * 2) + Byte.BYTES);
	}

}

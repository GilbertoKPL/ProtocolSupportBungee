package protocolsupport.protocol.packet.middleimpl.readable.play.v_6;

import protocolsupport.protocol.packet.id.LegacyPacketId;
import protocolsupport.protocol.packet.middleimpl.readable.LegacyFixedLengthPassthroughReadableMiddlePacket;

public class PlayerAbilitiesPacket extends LegacyFixedLengthPassthroughReadableMiddlePacket {

	public PlayerAbilitiesPacket() {
		super(LegacyPacketId.Dualbound.PLAY_ABILITIES, Byte.BYTES + (Float.BYTES * 2));
	}

}

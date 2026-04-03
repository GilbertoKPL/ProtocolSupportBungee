package protocolsupport.protocol.packet.middleimpl.writeable.play.v_4_5_6;

import io.netty.buffer.ByteBuf;
import net.md_5.bungee.protocol.DefinedPacket;
import protocolsupport.protocol.packet.id.LegacyPacketId;
import protocolsupport.protocol.packet.middleimpl.writeable.LegacySingleWriteablePacket;

public class EntityEffectRemovePacket extends LegacySingleWriteablePacket<DefinedPacket> {

	public EntityEffectRemovePacket(int packetId) {
		super(LegacyPacketId.Clientbound.PLAY_ENTITY_EFFECT_REMOVE);
	}

	@Override
	protected void write(ByteBuf data, DefinedPacket packet) {
		try {
			int entityId = ((Number) packet.getClass().getMethod("getEntityId").invoke(packet)).intValue();
			int effectId = ((Number) packet.getClass().getMethod("getEffectId").invoke(packet)).intValue();
			data.writeInt(entityId);
			data.writeByte(effectId);
		} catch (ReflectiveOperationException ignored) {
			// Packet format changed on newer BungeeCord versions.
		}
	}

}

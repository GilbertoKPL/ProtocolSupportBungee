package protocolsupport.protocol.packet.middleimpl.writeable.play.v_4_5_6;

import io.netty.buffer.ByteBuf;
import net.md_5.bungee.protocol.DefinedPacket;
import protocolsupport.protocol.packet.id.LegacyPacketId;
import protocolsupport.protocol.packet.middleimpl.writeable.LegacySingleWriteablePacket;

public class EntityEffectAddPacket extends LegacySingleWriteablePacket<DefinedPacket> {

	public EntityEffectAddPacket(int packetId) {
		super(LegacyPacketId.Clientbound.PLAY_ENTITY_EFFECT_ADD);
	}

	@Override
	protected void write(ByteBuf data, DefinedPacket packet) {
		try {
			int entityId = ((Number) packet.getClass().getMethod("getEntityId").invoke(packet)).intValue();
			int effectId = ((Number) packet.getClass().getMethod("getEffectId").invoke(packet)).intValue();
			int amplifier = ((Number) packet.getClass().getMethod("getAmplifier").invoke(packet)).intValue();
			int duration = ((Number) packet.getClass().getMethod("getDuration").invoke(packet)).intValue();
			data.writeInt(entityId);
			data.writeByte(effectId);
			data.writeByte(amplifier);
			data.writeShort(duration);
		} catch (ReflectiveOperationException ignored) {
			// Packet format changed on newer BungeeCord versions.
		}
	}

}

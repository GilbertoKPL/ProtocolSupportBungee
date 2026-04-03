package protocolsupport.protocol.utils;

import java.lang.reflect.Constructor;

import io.netty.buffer.ByteBuf;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.packet.EncryptionRequest;
import net.md_5.bungee.protocol.packet.EncryptionResponse;
import net.md_5.bungee.protocol.packet.Handshake;
import net.md_5.bungee.protocol.packet.KeepAlive;
import net.md_5.bungee.protocol.packet.LoginRequest;
import net.md_5.bungee.protocol.packet.LoginSuccess;
import net.md_5.bungee.protocol.packet.StatusRequest;
import net.md_5.bungee.protocol.packet.StatusResponse;

public final class PacketWrapperFactory {

	private static final Constructor<PacketWrapper> ctor3;
	private static final Constructor<PacketWrapper> ctor2;

	static {
		Constructor<PacketWrapper> found3 = null;
		Constructor<PacketWrapper> found2 = null;
		for (Constructor<?> constructor : PacketWrapper.class.getConstructors()) {
			Class<?>[] parameterTypes = constructor.getParameterTypes();
			if ((parameterTypes.length == 3) && (parameterTypes[0] == DefinedPacket.class) && (parameterTypes[1] == ByteBuf.class) && (parameterTypes[2] == Protocol.class)) {
				@SuppressWarnings("unchecked")
				Constructor<PacketWrapper> casted = (Constructor<PacketWrapper>) constructor;
				found3 = casted;
			}
			if ((parameterTypes.length == 2) && (parameterTypes[0] == DefinedPacket.class) && (parameterTypes[1] == ByteBuf.class)) {
				@SuppressWarnings("unchecked")
				Constructor<PacketWrapper> casted = (Constructor<PacketWrapper>) constructor;
				found2 = casted;
			}
		}
		ctor3 = found3;
		ctor2 = found2;
	}

	private PacketWrapperFactory() {
	}

	public static PacketWrapper create(DefinedPacket packet, ByteBuf buf) {
		try {
			if (ctor3 != null) {
				return ctor3.newInstance(packet, buf, detectProtocol(packet));
			}
			if (ctor2 != null) {
				return ctor2.newInstance(packet, buf);
			}
			throw new IllegalStateException("Unsupported PacketWrapper constructor signature");
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Unable to construct PacketWrapper", ex);
		}
	}

	private static Protocol detectProtocol(DefinedPacket packet) {
		if (packet == null) {
			return Protocol.GAME;
		}
		if ((packet instanceof Handshake)) {
			return Protocol.HANDSHAKE;
		}
		if ((packet instanceof StatusRequest) || (packet instanceof StatusResponse) || (packet instanceof KeepAlive)) {
			return Protocol.STATUS;
		}
		if ((packet instanceof LoginRequest) || (packet instanceof LoginSuccess) || (packet instanceof EncryptionRequest) || (packet instanceof EncryptionResponse)) {
			return Protocol.LOGIN;
		}
		return Protocol.GAME;
	}

}

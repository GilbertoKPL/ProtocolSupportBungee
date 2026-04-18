package protocolsupport.protocol.pipeline.version;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import net.md_5.bungee.protocol.MinecraftDecoder;
import net.md_5.bungee.protocol.Protocol;
import protocolsupport.api.Connection;
import protocolsupport.protocol.packet.middle.ReadableMiddlePacket;
import protocolsupport.protocol.storage.NetworkDataCache;
import protocolsupport.protocol.utils.ProtocolVersionsHelper;
import protocolsupport.protocol.utils.registry.PacketIdMiddleTransformerRegistry;
import protocolsupport.utils.netty.ReplayingDecoderBuffer;
import protocolsupport.utils.netty.ReplayingDecoderBuffer.EOFSignal;

public abstract class LegacyAbstractFromClientPacketDecoder extends MinecraftDecoder {

	private static final InternalLogger logger = InternalLoggerFactory.getInstance(LegacyAbstractFromClientPacketDecoder.class);
	private static final boolean debugLegacyPackets = Boolean.getBoolean("ps.debug.legacy");

	protected final PacketIdMiddleTransformerRegistry<ReadableMiddlePacket> registry = new PacketIdMiddleTransformerRegistry<>();

	protected final Connection connection;
	protected final NetworkDataCache cache;

	protected Protocol protocol = Protocol.HANDSHAKE;

	public LegacyAbstractFromClientPacketDecoder(Connection connection, NetworkDataCache cache) {
		super(Protocol.HANDSHAKE, true, ProtocolVersionsHelper.LATEST_PC.getId());
		this.connection = connection;
		this.cache = cache;
		registry.setCallBack(transformer -> {
			transformer.setConnection(this.connection);
			transformer.setSharedStorage(this.cache);
		});
	}

	@Override
	public void setProtocol(Protocol protocol) {
		super.setProtocol(protocol);
		this.protocol = protocol;
	}

	private final ReplayingDecoderBuffer buffer = new ReplayingDecoderBuffer(Unpooled.buffer());

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> packets) throws Exception {
		if (!buf.isReadable()) {
			return;
		}
		buffer.writeBytes(buf);
		buffer.markReaderIndex();
		try {
			while (buffer.isReadable()) {
				buffer.markReaderIndex();
				int packetId = buffer.readUnsignedByte();
				ReadableMiddlePacket transformer = registry.getTransformer(protocol, packetId, true);
				if (transformer == null) {
					throw new DecoderException("No transformer found for packetId 0x" + Integer.toHexString(packetId) + " in protocol " + protocol);
				}
				if (debugLegacyPackets) {
					logger.info("[ps-debug] decode from legacy client protocol={} packetId=0x{} transformer={}", protocol, Integer.toHexString(packetId), transformer.getClass().getSimpleName());
				}
				transformer.read(buffer);
				packets.addAll(transformer.toNative());
				buffer.discardReadBytes();
			}
		} catch (EOFSignal e) {
			buffer.resetReaderIndex();
		} catch (Throwable t) {
			if (debugLegacyPackets) {
				logger.warn("[ps-debug] failed to decode packet from legacy client in protocol {}", protocol, t);
			}
			throw t;
		}
	}

}

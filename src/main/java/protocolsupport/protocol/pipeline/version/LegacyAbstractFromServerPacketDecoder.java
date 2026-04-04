package protocolsupport.protocol.pipeline.version;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import net.md_5.bungee.protocol.MinecraftDecoder;
import net.md_5.bungee.protocol.PacketWrapper;
import net.md_5.bungee.protocol.Protocol;
import protocolsupport.api.Connection;
import protocolsupport.protocol.packet.middle.ReadableMiddlePacket;
import protocolsupport.protocol.storage.NetworkDataCache;
import protocolsupport.protocol.utils.ProtocolVersionsHelper;
import protocolsupport.protocol.utils.registry.PacketIdMiddleTransformerRegistry;

public abstract class LegacyAbstractFromServerPacketDecoder extends MinecraftDecoder {

	private static final InternalLogger logger = InternalLoggerFactory.getInstance(LegacyAbstractFromServerPacketDecoder.class);
	private static final boolean debugLegacyPackets = Boolean.getBoolean("ps.debug.legacy");

	protected final PacketIdMiddleTransformerRegistry<ReadableMiddlePacket> registry = new PacketIdMiddleTransformerRegistry<>();

	protected final Connection connection;
	protected final NetworkDataCache cache;

	public LegacyAbstractFromServerPacketDecoder(Connection connection, NetworkDataCache cache) {
		super(Protocol.GAME, false, ProtocolVersionsHelper.LATEST_PC.getId());
		this.connection = connection;
		this.cache = cache;
		registry.setCallBack(transformer -> {
			transformer.setConnection(this.connection);
			transformer.setSharedStorage(this.cache);
		});
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> packets) throws Exception {
		if (!buf.isReadable()) {
			return;
		}
		buf.markReaderIndex();
		int packetId = buf.readUnsignedByte();
		ReadableMiddlePacket transformer = registry.getTransformer(Protocol.GAME, packetId, false);
		if (transformer != null) {
			if (debugLegacyPackets) {
				logger.info("[ps-debug] decode from legacy server protocol={} packetId=0x{} transformer={}", Protocol.GAME, Integer.toHexString(packetId), transformer.getClass().getSimpleName());
			}
			transformer.read(buf);
			if (buf.isReadable()) {
				throw new DecoderException("Did not read all data from packet " + transformer.getClass().getName() + ", bytes left: " + buf.readableBytes());
			}
			packets.addAll(transformer.toNative());
		} else {
			if (debugLegacyPackets) {
				logger.info("[ps-debug] passthrough server packet protocol={} packetId=0x{}", Protocol.GAME, Integer.toHexString(packetId));
			}
			buf.resetReaderIndex();
			packets.add(new PacketWrapper(null, buf.copy()));
		}
	}

}

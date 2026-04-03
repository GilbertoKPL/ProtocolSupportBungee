package protocolsupport.injector;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.ServerConnector;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PacketHandler;
import net.md_5.bungee.netty.PipelineUtils;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.packet.handler.PSInitialHandler;
import protocolsupport.protocol.pipeline.ChannelHandlers;
import protocolsupport.protocol.pipeline.IPipeLineBuilder;
import protocolsupport.protocol.pipeline.common.LogicHandler;
import protocolsupport.protocol.pipeline.initial.InitialPacketDecoder;
import protocolsupport.protocol.storage.ProtocolStorage;
import protocolsupport.utils.ReflectionUtils;
import protocolsupport.utils.netty.ChannelInitializer;

//yep, thats our entry point, a single static field
public class BungeeNettyChannelInjector extends MessageToByteEncoder<ByteBuf> {

	private BungeeNettyChannelInjector() {
	}

	public static void inject() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		if (tryInjectViaModernUnsafeFrontendInitializer()) {
			return;
		}
		if (ReflectionUtils.trySetStaticFinalField(
			PipelineUtils.class,
			new BungeeNettyChannelInjector(),
			"framePrepender",
			"FRAME_PREPENDER",
			"frameEncoder",
			"FRAME_ENCODER"
		)) {
			return;
		}
		if (tryInjectViaChildInitializerField("SERVER_CHILD", "CHILD_HANDLER", "SERVER_CHILD_HANDLER")) {
			return;
		}
		for (Field field : PipelineUtils.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers()) && io.netty.channel.ChannelInitializer.class.isAssignableFrom(field.getType())) {
				Object originalChildInitializer = ReflectionUtils.setAccessible(field).get(null);
				if (originalChildInitializer != null) {
					ReflectionUtils.setStaticFinalField(field, new ServerChildInjectingInitializer(originalChildInitializer));
					return;
				}
			}
		}
		throw new RuntimeException("Unable to inject into PipelineUtils: no supported frame prepender or child initializer field found");
	}

	private static boolean tryInjectViaModernUnsafeFrontendInitializer() {
		try {
			Object unsafe = ProxyServer.getInstance().unsafe();
			Method setFrontendInitializerMethod = findMethod(unsafe.getClass(), "setFrontendChannelInitializer", 1);
			if (setFrontendInitializerMethod == null) {
				return false;
			}
			Class<?> initializerType = setFrontendInitializerMethod.getParameterTypes()[0];
			Method getFrontendInitializerMethod = findMethod(unsafe.getClass(), "getFrontendChannelInitializer", 0);
			Object originalInitializer = (getFrontendInitializerMethod != null) ? getFrontendInitializerMethod.invoke(unsafe) : null;
			Object wrappedInitializer = createWrappedFrontendInitializer(initializerType, originalInitializer);
			setFrontendInitializerMethod.invoke(unsafe, wrappedInitializer);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static Object createWrappedFrontendInitializer(Class<?> initializerType, Object originalInitializer) {
		if (io.netty.channel.ChannelInitializer.class.isAssignableFrom(initializerType)) {
			return new ServerChildInjectingInitializer(originalInitializer);
		}
		if (initializerType.isInterface()) {
			InvocationHandler invoker = (proxy, method, args) -> {
				if (method.getDeclaringClass() == Object.class) {
					return method.invoke(proxy, args);
				}
				Channel channel = (args != null && (args.length > 0) && (args[0] instanceof Channel)) ? (Channel) args[0] : null;
				if (channel != null) {
					invokeOriginalInitializer(originalInitializer, method, channel);
					channel.pipeline().addFirst(new ChannelInitializerEntryPoint());
				}
				return getDefaultValue(method.getReturnType());
			};
			return Proxy.newProxyInstance(initializerType.getClassLoader(), new Class<?>[] { initializerType }, invoker);
		}
		throw new RuntimeException("Unsupported frontend initializer type: " + initializerType.getName());
	}

	private static void invokeOriginalInitializer(Object originalInitializer, Method invocationMethod, Channel channel) throws Exception {
		if (originalInitializer == null) {
			return;
		}
		try {
			invocationMethod.invoke(originalInitializer, channel);
			return;
		} catch (IllegalArgumentException ignored) {
		}
		for (Method method : originalInitializer.getClass().getMethods()) {
			if ((method.getParameterCount() == 1) && Channel.class.isAssignableFrom(method.getParameterTypes()[0])) {
				ReflectionUtils.setAccessible(method).invoke(originalInitializer, channel);
				return;
			}
		}
	}

	private static Object getDefaultValue(Class<?> returnType) {
		if ((returnType == void.class) || (returnType == Void.class)) {
			return null;
		}
		if (returnType == boolean.class) {
			return false;
		}
		if (returnType == byte.class) {
			return (byte) 0;
		}
		if (returnType == short.class) {
			return (short) 0;
		}
		if (returnType == int.class) {
			return 0;
		}
		if (returnType == long.class) {
			return 0L;
		}
		if (returnType == float.class) {
			return 0f;
		}
		if (returnType == double.class) {
			return 0d;
		}
		if (returnType == char.class) {
			return (char) 0;
		}
		return null;
	}

	private static Method findMethod(Class<?> sourceClass, String name, int parameterCount) {
		for (Method method : sourceClass.getMethods()) {
			if (method.getName().equals(name) && (method.getParameterCount() == parameterCount)) {
				return method;
			}
		}
		for (Method method : sourceClass.getDeclaredMethods()) {
			if (method.getName().equals(name) && (method.getParameterCount() == parameterCount)) {
				return ReflectionUtils.setAccessible(method);
			}
		}
		return null;
	}

	private static boolean tryInjectViaChildInitializerField(String... fieldNames) throws IllegalArgumentException, IllegalAccessException {
		for (String fieldName : fieldNames) {
			Object originalChildInitializer = ReflectionUtils.getStaticFieldValue(PipelineUtils.class, fieldName);
			if (originalChildInitializer != null && originalChildInitializer instanceof io.netty.channel.ChannelInitializer) {
				ReflectionUtils.setStaticFinalField(PipelineUtils.class, fieldName, new ServerChildInjectingInitializer(originalChildInitializer));
				return true;
			}
		}
		return false;
	}

	private static final class ServerChildInjectingInitializer extends io.netty.channel.ChannelInitializer<Channel> {

		private final Object originalInitializer;

		private ServerChildInjectingInitializer(Object originalInitializer) {
			this.originalInitializer = originalInitializer;
		}

		@Override
		protected void initChannel(Channel channel) throws Exception {
			if (originalInitializer != null) {
				Method originalInitChannel = ReflectionUtils.setAccessible(
					io.netty.channel.ChannelInitializer.class.getDeclaredMethod("initChannel", Channel.class)
				);
				originalInitChannel.invoke(originalInitializer, channel);
			}
			channel.pipeline().addFirst(new ChannelInitializerEntryPoint());
		}

	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		ctx.channel().pipeline().addFirst(new ChannelInitializerEntryPoint());
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception {
		int bodyLen = msg.readableBytes();
		writeVarInt(out, bodyLen);
		out.writeBytes(msg, msg.readerIndex(), bodyLen);
	}

	private static void writeVarInt(ByteBuf out, int value) {
		do {
			byte temp = (byte) (value & 0b0111_1111);
			value >>>= 7;
			if (value != 0) {
				temp |= 0b1000_0000;
			}
			out.writeByte(temp);
		} while (value != 0);
	}

	private static class ChannelInitializerEntryPoint extends ChannelInitializer {

		@Override
		protected void initChannel(Channel channel) throws Exception {
			if (!channel.isOpen()) {
				return;
			}
			ChannelPipeline pipeline = channel.pipeline();
			PacketHandler handler = ReflectionUtils.getFieldValue(pipeline.get(HandlerBoss.class), "handler");
			CustomHandlerBoss boss = new CustomHandlerBoss(handler);
			pipeline.replace(PipelineUtils.BOSS_HANDLER, PipelineUtils.BOSS_HANDLER, boss);
			if (handler instanceof InitialHandler) {//user to bungee connection
				Entry<String, ChannelHandler> firstHandler = pipeline.iterator().next();
				if (firstHandler.getValue() instanceof HAProxyMessageDecoder) {
					pipeline.addAfter(firstHandler.getKey(), ChannelHandlers.INITIAL_DECODER, new InitialPacketDecoder());
				} else {
					pipeline.addFirst(ChannelHandlers.INITIAL_DECODER, new InitialPacketDecoder());
				}
				PSInitialHandler initialhandler = new PSInitialHandler(BungeeCord.getInstance(), channel.attr(PipelineUtils.LISTENER).get());
				ConnectionImpl connection = new ConnectionImpl(boss, initialhandler);
				connection.storeInChannel(channel);
				ProtocolStorage.addConnection(channel.remoteAddress(), connection);
				pipeline.addBefore(PipelineUtils.BOSS_HANDLER, ChannelHandlers.LOGIC, new LogicHandler(connection, true));
				pipeline.remove(PipelineUtils.LEGACY_DECODER);
				pipeline.remove(PipelineUtils.LEGACY_KICKER);
				boss.setHandler(initialhandler);
			} else if (handler instanceof ServerConnector) {//bungee to server connection
				ConnectionImpl connection = ConnectionImpl.getFromChannel(((ChannelWrapper) ReflectionUtils.getFieldValue(ReflectionUtils.getFieldValue(handler, "user"), "ch")).getHandle());
				pipeline.addBefore(PipelineUtils.BOSS_HANDLER, ChannelHandlers.LOGIC, new LogicHandler(connection, false));
				connection.setServerConnectionChannel(channel);
				IPipeLineBuilder builder = InitialPacketDecoder.BUILDERS.get(connection.getVersion());
				if (builder != null) {
					builder.buildBungeeServer(channel, connection);
				}
			}
		}

	}

	public static class CustomHandlerBoss extends HandlerBoss {

		protected PacketHandler handler;
		protected PacketHandlerChangeListener listener = (handler) -> handler;

		public CustomHandlerBoss(PacketHandler handler) {
			setHandler(handler);
		}

		@Override
		public void setHandler(PacketHandler handler) {
			handler = listener.onPacketHandlerChange(handler);
			super.setHandler(handler);
			this.handler = handler;
		}

		public PacketHandler getHandler() {
			return this.handler;
		}

		public void setHandlerChangeListener(PacketHandlerChangeListener listener) {
			this.listener = listener;
		}

		@FunctionalInterface
		public static interface PacketHandlerChangeListener {
			public PacketHandler onPacketHandlerChange(PacketHandler handler);
		}

	}

}

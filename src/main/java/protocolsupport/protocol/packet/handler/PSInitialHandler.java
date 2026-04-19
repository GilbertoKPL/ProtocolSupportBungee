package protocolsupport.protocol.packet.handler;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import com.google.common.base.Preconditions;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.EncryptionUtil;
import net.md_5.bungee.UserConnection;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.connection.UpstreamBridge;
import net.md_5.bungee.http.HttpClient;
import net.md_5.bungee.jni.cipher.BungeeCipher;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.HandlerBoss;
import net.md_5.bungee.netty.PipelineUtils;
import net.md_5.bungee.netty.cipher.CipherDecoder;
import net.md_5.bungee.netty.cipher.CipherEncoder;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.packet.EncryptionRequest;
import net.md_5.bungee.protocol.packet.EncryptionResponse;
import net.md_5.bungee.protocol.packet.LoginRequest;
import net.md_5.bungee.protocol.packet.LoginSuccess;
import protocolsupport.api.ProtocolType;
import protocolsupport.api.ProtocolVersion;
import protocolsupport.api.events.PlayerLoginFinishEvent;
import protocolsupport.api.events.PlayerLoginStartEvent;
import protocolsupport.api.events.PlayerProfileCompleteEvent;
import protocolsupport.api.utils.Profile;
import protocolsupport.api.utils.ProfileProperty;
import protocolsupport.protocol.ConnectionImpl;
import protocolsupport.protocol.utils.GameProfile;

public class PSInitialHandler extends InitialHandler {

	private static final boolean debugConnection = Boolean.getBoolean("ps.debug.connection");

	public PSInitialHandler(BungeeCord bungee, ListenerInfo listener) {
		super(bungee, listener);
	}

	protected ChannelWrapper channel;
	protected ConnectionImpl connection;

	@Override
	public void connected(ChannelWrapper channel) throws Exception {
		super.connected(channel);
		this.channel = channel;
		this.connection = ConnectionImpl.getFromChannel(channel.getHandle());
		debug("connected remote={0} protocol={1}", channel.getRemoteAddress(), connection.getVersion());
	}

	public ChannelWrapper getChannelWrapper() {
		return channel;
	}

	protected LoginRequest loginRequest;

	@Override
	public LoginRequest getLoginRequest() {
		return loginRequest;
	}

	protected LoginResult loginProfile;

	@Override
	public LoginResult getLoginProfile() {
		return loginProfile;
	}

	protected LoginState state = LoginState.HELLO;

	protected String username;

	@Override
	public String getName() {
		return username;
	}

	protected void updateUsername(String newName, boolean original) {
		username = newName;
		if (original) {
			connection.getProfile().setOriginalName(newName);
		} else {
			connection.getProfile().setName(newName);
		}
	}

	protected UUID uuid;

	@Override
	public void setUniqueId(UUID uuid) {
		Preconditions.checkState((state == LoginState.HELLO) || (state == LoginState.ONLINEMODERESOLVE), "Can only set uuid while state is username");
		Preconditions.checkState(!isOnlineMode(), "Can only set uuid when online mode is false");
		updateUUID(uuid, true);
	}

	@Override
	public UUID getUniqueId() {
		return uuid;
	}

	@Override
	public String getUUID() {
		return getUniqueId().toString().replaceAll("-", "");
	}

	protected void updateUUID(UUID newUUID, boolean original) {
		uuid = newUUID;
		if (original) {
			connection.getProfile().setOriginalUUID(newUUID);
		} else {
			connection.getProfile().setUUID(newUUID);
		}
	}

	@Override
	public void setOnlineMode(boolean onlineMode) {
		Preconditions.checkState((state == LoginState.HELLO) || (state == LoginState.ONLINEMODERESOLVE), "Can only set uuid while state is username");
		connection.getProfile().setOnlineMode(onlineMode);
	}

	@Override
	public boolean isOnlineMode() {
		return connection.getProfile().isOnlineMode();
	}

	@Override
	public void handle(LoginRequest lLoginRequest) throws Exception {
		Preconditions.checkState(state == LoginState.HELLO, "Not expecting USERNAME");
		state = LoginState.ONLINEMODERESOLVE;
		String host = getHandshake().getHost();
		boolean hasForgeMarker = (host != null) && host.contains("\0FML\0");
		debug("received login request username={0} host={1} forgeMarker={2} version={3}", lLoginRequest.getData(), sanitizeHostForDebug(host), hasForgeMarker, connection.getVersion());

		loginRequest = lLoginRequest;

		updateUsername(lLoginRequest.getData(), true);

		if (getName().contains(".")) {
			debug("disconnecting username={0} reason=name_invalid", getName());
			disconnect(BungeeCord.getInstance().getTranslation("name_invalid"));
			return;
		}
		if (getName().length() > 16) {
			debug("disconnecting username={0} reason=name_too_long", getName());
			disconnect(BungeeCord.getInstance().getTranslation("name_too_long"));
			return;
		}
		int limit = BungeeCord.getInstance().config.getPlayerLimit();
		if ((limit > 0) && (BungeeCord.getInstance().getOnlineCount() > limit)) {
			debug("disconnecting username={0} reason=proxy_full online={1} limit={2}", getName(), BungeeCord.getInstance().getOnlineCount(), limit);
			disconnect(BungeeCord.getInstance().getTranslation("proxy_full"));
			return;
		}
		if (!isOnlineMode() && (BungeeCord.getInstance().getPlayer(getUniqueId()) != null)) {
			debug("disconnecting username={0} reason=already_connected_proxy uuid={1}", getName(), getUniqueId());
			disconnect(BungeeCord.getInstance().getTranslation("already_connected_proxy"));
			return;
		}

		BungeeCord.getInstance().getPluginManager().callEvent(new PreLoginEvent(this, new Callback<PreLoginEvent>() {
			@Override
			public void done(PreLoginEvent result, Throwable error) {
				if (result.isCancelled()) {
					debug("disconnecting username={0} reason=prelogin_cancelled", getName());
					disconnect(result.getCancelReasonComponents());
					return;
				}
				if (!isConnected()) {
					return;
				}
				processLoginStart();
			}
		}));
	}

	protected EncryptionRequest request;

	protected void processLoginStart() {
		PlayerLoginStartEvent event = new PlayerLoginStartEvent(connection, getHandshake().getHost());
		ProxyServer.getInstance().getPluginManager().callEvent(event);
		if (event.isLoginDenied()) {
			debug("disconnecting username={0} reason=player_login_start_denied", getName());
			disconnect(event.getDenyLoginMessage());
			return;
		}

		connection.getProfile().setOnlineMode(event.isOnlineMode());
		debug("login start resolved username={0} onlineMode={1} protocolType={2} version={3}", getName(), isOnlineMode(), connection.getVersion().getProtocolType(), connection.getVersion());

		switch (connection.getVersion().getProtocolType()) {
			case PC: {
				if (isOnlineMode()) {
					state = LoginState.KEY;
					debug("sending encryption request username={0}", getName());
					unsafe().sendPacket((request = EncryptionUtil.encryptRequest()));
				} else {
					offlineuuid = Profile.generateOfflineModeUUID(getName());
					updateUUID(offlineuuid, true);
					debug("offline mode login username={0} uuid={1}", getName(), offlineuuid);
					finishLogin();
				}
				return;
			}
			default: {
				throw new IllegalArgumentException(MessageFormat.format("Unknown protocol type {0}", connection.getVersion().getProtocolType()));
			}
		}
	}

	@Override
	public void handle(EncryptionResponse encryptResponse) throws Exception {
		Preconditions.checkState(state == LoginState.KEY, "Not expecting ENCRYPT");
		state = LoginState.AUTHENTICATING;
		debug("received encryption response username={0}", getName());
		SecretKey sharedKey = EncryptionUtil.getSecret(encryptResponse, request);
		BungeeCipher decrypt = EncryptionUtil.getCipher(false, sharedKey);
		channel.addBefore(PipelineUtils.FRAME_DECODER, PipelineUtils.DECRYPT_HANDLER, new CipherDecoder(decrypt));
		if (isFullEncryption(connection.getVersion())) {
			BungeeCipher encrypt = EncryptionUtil.getCipher(true, sharedKey);
			channel.addBefore(PipelineUtils.FRAME_PREPENDER, PipelineUtils.ENCRYPT_HANDLER, new CipherEncoder(encrypt));
		}
		String encName = URLEncoder.encode(getName(), "UTF-8");
		MessageDigest sha = MessageDigest.getInstance("SHA-1");
		for (byte[] bit : new byte[][] { request.getServerId().getBytes("ISO_8859_1"), sharedKey.getEncoded(), EncryptionUtil.keys.getPublic().getEncoded() }) {
			sha.update(bit);
		}
		String encodedHash = URLEncoder.encode(new BigInteger(sha.digest()).toString(16), "UTF-8");
		String preventProxy = BungeeCord.getInstance().config.isPreventProxyConnections() ? ("&ip=" + URLEncoder.encode(getAddress().getAddress().getHostAddress(), "UTF-8")) : "";
		String authURL = "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=" + encName + "&serverId=" + encodedHash + preventProxy;
		Callback<String> handler = new Callback<String>() {
			@Override
			public void done(String result, Throwable error) {
				if (error == null) {
					LoginResult obj = BungeeCord.getInstance().gson.fromJson(result, LoginResult.class);
					if ((obj != null) && (obj.getId() != null)) {
						loginProfile = obj;
						updateUsername(obj.getName(), true);
						updateUUID(Util.getUUID(obj.getId()), true);
						Arrays.stream(loginProfile.getProperties())
						.forEach(bProperty -> connection.getProfile().addProperty(
							new ProfileProperty(bProperty.getName(), bProperty.getValue(), bProperty.getSignature())
						));
						debug("mojang auth success username={0} uuid={1}", obj.getName(), Util.getUUID(obj.getId()));
						finishLogin();
						return;
					}
					debug("disconnecting username={0} reason=offline_mode_player", getName());
					disconnect(BungeeCord.getInstance().getTranslation("offline_mode_player"));
				} else {
					debug("disconnecting username={0} reason=mojang_fail error={1}", getName(), error.getMessage());
					disconnect(BungeeCord.getInstance().getTranslation("mojang_fail"));
					BungeeCord.getInstance().getLogger().log(Level.SEVERE, "Error authenticating " + getName() + " with minecraft.net", error);
				}
			}
		};
		HttpClient.get(authURL, channel.getHandle().eventLoop(), handler);
	}

	protected UUID offlineuuid;

	@Override
	public UUID getOfflineId() {
		return offlineuuid;
	}

	@SuppressWarnings("deprecation")
	protected void finishLogin() {
		GameProfile profile = connection.getProfile();
		debug("finishing login username={0} uuid={1} version={2}", getName(), getUniqueId(), connection.getVersion());

		PlayerProfileCompleteEvent profileCompleteEvent = new PlayerProfileCompleteEvent(connection);
		BungeeCord.getInstance().getPluginManager().callEvent(profileCompleteEvent);
		if (profileCompleteEvent.isLoginDenied()) {
			debug("disconnecting username={0} reason=profile_complete_denied", getName());
			disconnect(profileCompleteEvent.getDenyLoginMessage());
			return;
		}

		if (profileCompleteEvent.getForcedName() != null) {
			updateUsername(profileCompleteEvent.getForcedName(), false);
		}
		if (profileCompleteEvent.getForcedUUID() != null) {
			updateUUID(profileCompleteEvent.getForcedUUID(), false);
		}
		profile.setProperties(profileCompleteEvent.getProperties());

		loginProfile = new LoginResult(
			getName(), getUUID(),
			profile.getProperties().values().stream()
			.flatMap(Collection::stream)
			.map(psprop -> new LoginResult.Property(psprop.getName(), psprop.getValue(), psprop.getSignature()))
			.collect(Collectors.toList()).toArray(new LoginResult.Property[0])
		);

		ProxiedPlayer oldName = BungeeCord.getInstance().getPlayer(getName());
		if (oldName != null) {
			debug("disconnecting previous connection by name username={0}", getName());
			oldName.disconnect(BungeeCord.getInstance().getTranslation("already_connected_proxy"));
		}
		ProxiedPlayer oldID = BungeeCord.getInstance().getPlayer(getUniqueId());
		if (oldID != null) {
			debug("disconnecting previous connection by uuid uuid={0}", getUniqueId());
			oldID.disconnect(BungeeCord.getInstance().getTranslation("already_connected_proxy"));
		}

		Callback<LoginEvent> complete = new Callback<LoginEvent>() {
			@Override
			public void done(LoginEvent result, Throwable error) {
				if (result.isCancelled()) {
					debug("disconnecting username={0} reason=login_event_cancelled", getName());
					disconnect(result.getCancelReasonComponents());
					return;
				}
				if (!isConnected()) {
					return;
				}
				channel.getHandle().eventLoop().execute(() -> processLoginFinish());
			}
		};
		BungeeCord.getInstance().getPluginManager().callEvent(new LoginEvent(this, complete));
	}

	@SuppressWarnings("deprecation")
	protected void processLoginFinish() {
		if (!channel.isClosing()) {
			debug("sending login success username={0} uuid={1} compression={2}", getName(), getUniqueId(), hasCompression(connection.getVersion()));
			UserConnection userCon = new UserConnection(BungeeCord.getInstance(), channel, getName(), PSInitialHandler.this);
			if (hasCompression(connection.getVersion())) {
				userCon.setCompressionThreshold(BungeeCord.getInstance().config.getCompressionThreshold());
			}
			userCon.init();
			unsafe().sendPacket(new LoginSuccess(getUniqueId(), getName()));
			channel.setProtocol(Protocol.GAME);

			PlayerLoginFinishEvent loginFinishEvent = new PlayerLoginFinishEvent(connection);
			BungeeCord.getInstance().getPluginManager().callEvent(loginFinishEvent);
			if (loginFinishEvent.isLoginDenied()) {
				debug("disconnecting username={0} reason=player_login_finish_denied", getName());
				disconnect(loginFinishEvent.getDenyLoginMessage());
				return;
			}

			channel.getHandle().pipeline().get(HandlerBoss.class).setHandler(new UpstreamBridge(BungeeCord.getInstance(), userCon));
			BungeeCord.getInstance().getPluginManager().callEvent(new PostLoginEvent(userCon));

			ServerInfo server;
			if (BungeeCord.getInstance().getReconnectHandler() != null) {
				server = BungeeCord.getInstance().getReconnectHandler().getServer(userCon);
			} else {
				server = AbstractReconnectHandler.getForcedHost(PSInitialHandler.this);
			}
			if (server == null) {
				server = BungeeCord.getInstance().getServerInfo(getListener().getDefaultServer());
			}
			debug("connecting player username={0} to server={1}", getName(), server != null ? server.getName() : "null");
			userCon.connect(server, null, true);
		}
	}

	private void debug(String pattern, Object... args) {
		if (!debugConnection) {
			return;
		}
		Logger logger = BungeeCord.getInstance().getLogger();
		if (logger != null) {
			logger.info("[ps-debug] " + MessageFormat.format(pattern, args));
		}
	}

	private static String sanitizeHostForDebug(String host) {
		return host == null ? "null" : host.replace("\0", "\\0");
	}

	public enum LoginState {
		HELLO, ONLINEMODERESOLVE, KEY, AUTHENTICATING;
	}

	protected static boolean hasCompression(ProtocolVersion version) {
		return (version.getProtocolType() == ProtocolType.PC) && version.isAfterOrEq(ProtocolVersion.MINECRAFT_1_8);
	}

	protected static boolean isFullEncryption(ProtocolVersion version) {
		return (version.getProtocolType() == ProtocolType.PC) && version.isAfterOrEq(ProtocolVersion.MINECRAFT_1_7_5);
	}

}

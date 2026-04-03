package protocolsupport.protocol.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import net.md_5.bungee.protocol.packet.ClientSettings;
import net.md_5.bungee.protocol.packet.EncryptionResponse;
import net.md_5.bungee.protocol.packet.LoginSuccess;
import net.md_5.bungee.protocol.packet.LoginRequest;
import net.md_5.bungee.protocol.packet.Login;
import net.md_5.bungee.protocol.packet.Respawn;
import net.md_5.bungee.protocol.packet.ScoreboardObjective;
import net.md_5.bungee.protocol.packet.ScoreboardScore;
import net.md_5.bungee.protocol.packet.Team;

public final class LegacyPacketFactory {

	private LegacyPacketFactory() {
	}

	public static Team createTeam(String name, byte mode, String displayName, String prefix, String suffix, byte friendlyFire, String[] players) {
		for (Constructor<?> constructor : Team.class.getConstructors()) {
			Class<?>[] params = constructor.getParameterTypes();
			if (params.length != 10) {
				continue;
			}
			try {
				Object[] args = new Object[10];
				args[0] = name;
				args[1] = mode;
				args[2] = adaptTextArg(params[2], displayName);
				args[3] = adaptTextArg(params[3], prefix);
				args[4] = adaptTextArg(params[4], suffix);
				args[5] = "always";
				args[6] = "always";
				args[7] = (byte) -1;
				args[8] = friendlyFire;
				args[9] = players;
				return (Team) constructor.newInstance(args);
			} catch (ReflectiveOperationException ignored) {
			}
		}
		throw new IllegalStateException("Unable to instantiate Team packet with current Bungee API");
	}

	public static ScoreboardObjective createScoreboardObjective(String name, String value, byte action) {
		for (Constructor<?> constructor : ScoreboardObjective.class.getConstructors()) {
			Class<?>[] params = constructor.getParameterTypes();
			if (params.length < 4) {
				continue;
			}
			try {
				Object[] args = new Object[params.length];
				args[0] = name;
				args[1] = adaptTextArg(params[1], value);
				args[2] = ScoreboardObjective.HealthDisplay.INTEGER;
				args[3] = action;
				for (int i = 4; i < params.length; i++) {
					args[i] = null;
				}
				return (ScoreboardObjective) constructor.newInstance(args);
			} catch (ReflectiveOperationException ignored) {
			}
		}
		throw new IllegalStateException("Unable to instantiate ScoreboardObjective packet");
	}

	public static ScoreboardScore createScoreboardScore(String itemName, byte action, String scoreName, int value) {
		for (Constructor<?> constructor : ScoreboardScore.class.getConstructors()) {
			Class<?>[] params = constructor.getParameterTypes();
			if (params.length < 4) {
				continue;
			}
			try {
				Object[] args = new Object[params.length];
				args[0] = itemName;
				args[1] = action;
				args[2] = scoreName;
				args[3] = value;
				for (int i = 4; i < params.length; i++) {
					args[i] = null;
				}
				return (ScoreboardScore) constructor.newInstance(args);
			} catch (ReflectiveOperationException ignored) {
			}
		}
		throw new IllegalStateException("Unable to instantiate ScoreboardScore packet");
	}

	public static Respawn createRespawn(int dimension, short difficulty, short gamemode, String levelType) {
		for (Constructor<?> constructor : Respawn.class.getConstructors()) {
			Class<?>[] params = constructor.getParameterTypes();
			if (params.length < 4) {
				continue;
			}
			try {
				Object[] args = new Object[params.length];
				args[0] = dimension;
				if (params.length > 1) args[1] = null;
				if (params.length > 2) args[2] = 0;
				if (params.length > 3) args[3] = difficulty;
				if (params.length > 4) args[4] = gamemode;
				if (params.length > 5) args[5] = gamemode;
				if (params.length > 6) args[6] = levelType;
				for (int i = 7; i < params.length; i++) {
					args[i] = ((params[i] == boolean.class) ? Boolean.FALSE : null);
				}
				return (Respawn) constructor.newInstance(args);
			} catch (ReflectiveOperationException ignored) {
			}
		}
		throw new IllegalStateException("Unable to instantiate Respawn packet");
	}

	public static LoginSuccess createLoginSuccess() {
		try {
			return LoginSuccess.class.getDeclaredConstructor().newInstance();
		} catch (ReflectiveOperationException ignored) {
		}
		for (Constructor<?> constructor : LoginSuccess.class.getConstructors()) {
			Class<?>[] params = constructor.getParameterTypes();
			if ((params.length == 2) && (params[1] == String.class)) {
				try {
					Object id = (params[0] == java.util.UUID.class) ? java.util.UUID.randomUUID() : "00000000000000000000000000000000";
					return (LoginSuccess) constructor.newInstance(id, "legacy-player");
				} catch (ReflectiveOperationException ignored) {
				}
			}
		}
		throw new IllegalStateException("Unable to instantiate LoginSuccess packet");
	}

	public static LoginRequest createLoginRequest(String username) {
		for (Constructor<?> constructor : LoginRequest.class.getConstructors()) {
			Class<?>[] params = constructor.getParameterTypes();
			if (params.length < 1) {
				continue;
			}
			try {
				Object[] args = new Object[params.length];
				args[0] = username;
				for (int i = 1; i < params.length; i++) {
					args[i] = null;
				}
				return (LoginRequest) constructor.newInstance(args);
			} catch (ReflectiveOperationException ignored) {
			}
		}
		throw new IllegalStateException("Unable to instantiate LoginRequest packet");
	}

	public static EncryptionResponse createEncryptionResponse(byte[] sharedSecret, byte[] verifyToken) {
		for (Constructor<?> constructor : EncryptionResponse.class.getConstructors()) {
			Class<?>[] params = constructor.getParameterTypes();
			if (params.length < 2) {
				continue;
			}
			try {
				Object[] args = new Object[params.length];
				args[0] = sharedSecret;
				args[1] = verifyToken;
				for (int i = 2; i < params.length; i++) {
					args[i] = null;
				}
				return (EncryptionResponse) constructor.newInstance(args);
			} catch (ReflectiveOperationException ignored) {
			}
		}
		throw new IllegalStateException("Unable to instantiate EncryptionResponse packet");
	}

	public static ClientSettings createClientSettings(String locale, byte viewDistance, int chatFlags, boolean chatColours) {
		for (Constructor<?> constructor : ClientSettings.class.getConstructors()) {
			Class<?>[] params = constructor.getParameterTypes();
			if (params.length < 4) {
				continue;
			}
			try {
				Object[] args = new Object[params.length];
				args[0] = locale;
				args[1] = viewDistance;
				args[2] = chatFlags;
				args[3] = chatColours;
				for (int i = 4; i < params.length; i++) {
					Class<?> param = params[i];
					if (param == byte.class || param == Byte.class) {
						args[i] = (byte) 0;
					} else if (param == short.class || param == Short.class) {
						args[i] = (short) 0;
					} else if (param == int.class || param == Integer.class) {
						args[i] = 0;
					} else if (param == boolean.class || param == Boolean.class) {
						args[i] = false;
					} else if (param.isEnum()) {
						Object[] constants = param.getEnumConstants();
						args[i] = ((constants != null) && (constants.length > 0)) ? constants[0] : null;
					} else {
						args[i] = null;
					}
				}
				return (ClientSettings) constructor.newInstance(args);
			} catch (ReflectiveOperationException ignored) {
			}
		}
		throw new IllegalStateException("Unable to instantiate ClientSettings packet");
	}

	public static Login createLoginPacket(int entityId, boolean hardcode, int gamemode, int dimension, int difficulty, int maxPlayers, String levelType) {
		for (Constructor<?> constructor : Login.class.getConstructors()) {
			Class<?>[] params = constructor.getParameterTypes();
			if (params.length < 1) {
				continue;
			}
			try {
				Object[] args = new Object[params.length];
				for (int i = 0; i < params.length; i++) {
					Class<?> param = params[i];
					if ((i == 0) && ((param == int.class) || (param == Integer.class))) {
						args[i] = entityId;
					} else if ((i == 1) && ((param == boolean.class) || (param == Boolean.class))) {
						args[i] = hardcode;
					} else if ((i == 2) && ((param == short.class) || (param == Short.class))) {
						args[i] = (short) gamemode;
					} else if ((i == 3) && ((param == short.class) || (param == Short.class))) {
						args[i] = (short) gamemode;
					} else if ((param == String.class) && (args[i] == null)) {
						args[i] = levelType;
					} else if (((param == Integer.class) || (param == int.class)) && (args[i] == null)) {
						args[i] = (i == 8) ? dimension : 0;
					} else if (((param == Short.class) || (param == short.class)) && (args[i] == null)) {
						args[i] = (short) ((i == 9) ? difficulty : ((i == 10) ? maxPlayers : 0));
					} else if ((param == boolean.class) || (param == Boolean.class)) {
						args[i] = false;
					} else if ((param == long.class) || (param == Long.class)) {
						args[i] = 0L;
					} else {
						args[i] = null;
					}
				}
				return (Login) constructor.newInstance(args);
			} catch (ReflectiveOperationException ignored) {
			}
		}
		throw new IllegalStateException("Unable to instantiate Login packet");
	}

	public static String toLegacyText(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof String) {
			return (String) value;
		}
		try {
			Method isLeft = value.getClass().getMethod("isLeft");
			Method left = value.getClass().getMethod("left");
			Boolean leftValue = (Boolean) isLeft.invoke(value);
			if (Boolean.TRUE.equals(leftValue)) {
				Object raw = left.invoke(value);
				return (raw != null) ? raw.toString() : "";
			}
		} catch (ReflectiveOperationException ignored) {
		}
		return value.toString();
	}

	private static Object adaptTextArg(Class<?> targetType, String value) throws ReflectiveOperationException {
		if (targetType == String.class) {
			return value;
		}
		if ("net.md_5.bungee.protocol.Either".equals(targetType.getName())) {
			Method left = targetType.getMethod("left", Object.class);
			return left.invoke(null, value);
		}
		return value;
	}

}

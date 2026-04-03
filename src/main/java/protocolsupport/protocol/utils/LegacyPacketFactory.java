package protocolsupport.protocol.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import net.md_5.bungee.protocol.packet.LoginSuccess;
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

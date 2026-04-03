package protocolsupport.utils;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import sun.misc.Unsafe;

public class ReflectionUtils {

	private static final Unsafe UNSAFE = lookupUnsafe();

	@SuppressWarnings("unchecked")
	public static <T> T getFieldValue(Object target, String name) throws IllegalArgumentException, IllegalAccessException {
		Class<?> clazz = target.getClass();
		do {
			for (Field field : clazz.getDeclaredFields()) {
				if (field.getName().equals(name)) {
					return (T) setAccessible(field).get(target);
				}
			}
		} while ((clazz = clazz.getSuperclass()) != null);
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getStaticFieldValue(Class<?> target, String name) throws IllegalArgumentException, IllegalAccessException {
		Class<?> clazz = target;
		do {
			for (Field field : clazz.getDeclaredFields()) {
				if (field.getName().equals(name)) {
					return (T) setAccessible(field).get(null);
				}
			}
		} while ((clazz = clazz.getSuperclass()) != null);
		return null;
	}

	public static void setFieldValue(Object target, String name, Object value) throws IllegalArgumentException, IllegalAccessException {
		Class<?> clazz = target.getClass();
		do {
			for (Field field : clazz.getDeclaredFields()) {
				if (field.getName().equals(name)) {
					setAccessible(field).set(target, value);
					return;
				}
			}
		} while ((clazz = clazz.getSuperclass()) != null);
	}

	public static <T extends AccessibleObject> T setAccessible(T object) {
		object.setAccessible(true);
		return object;
	}

	public static void setStaticFinalField(Class<?> clazz, String fieldname, Object value) {
		try {
			setStaticFinalField(setAccessible(clazz.getDeclaredField(fieldname)), value);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static void setStaticFinalField(Field field, Object value) {
		try {
			field = setAccessible(field);
			if (trySetViaDirectFieldWrite(field, value)) {
				return;
			}
			setStaticFinalFieldViaUnsafe(field, value);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean trySetViaDirectFieldWrite(Field field, Object value) {
		try {
			((MethodHandles.Lookup) setAccessible(MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP")).get(null))
			.findSetter(Field.class, "modifiers", int.class)
			.invokeExact(field, field.getModifiers() & ~Modifier.FINAL);
			field.set(null, value);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	private static void setStaticFinalFieldViaUnsafe(Field field, Object value) {
		if (UNSAFE == null) {
			throw new RuntimeException("Unable to acquire Unsafe");
		}
		Object base = UNSAFE.staticFieldBase(field);
		long offset = UNSAFE.staticFieldOffset(field);
		Class<?> type = field.getType();
		if (type == boolean.class) {
			UNSAFE.putBoolean(base, offset, ((Boolean) value).booleanValue());
		} else if (type == byte.class) {
			UNSAFE.putByte(base, offset, ((Byte) value).byteValue());
		} else if (type == short.class) {
			UNSAFE.putShort(base, offset, ((Short) value).shortValue());
		} else if (type == int.class) {
			UNSAFE.putInt(base, offset, ((Integer) value).intValue());
		} else if (type == long.class) {
			UNSAFE.putLong(base, offset, ((Long) value).longValue());
		} else if (type == float.class) {
			UNSAFE.putFloat(base, offset, ((Float) value).floatValue());
		} else if (type == double.class) {
			UNSAFE.putDouble(base, offset, ((Double) value).doubleValue());
		} else if (type == char.class) {
			UNSAFE.putChar(base, offset, ((Character) value).charValue());
		} else {
			UNSAFE.putObject(base, offset, value);
		}
	}

	private static Unsafe lookupUnsafe() {
		try {
			return (Unsafe) setAccessible(Unsafe.class.getDeclaredField("theUnsafe")).get(null);
		} catch (Throwable e) {
			return null;
		}
	}

	public static boolean trySetStaticFinalField(Class<?> clazz, Object value, String... fieldnames) {
		for (String fieldname : fieldnames) {
			try {
				setStaticFinalField(clazz, fieldname, value);
				return true;
			} catch (RuntimeException e) {
			}
		}
		return false;
	}

	public static void setStaticFinalField(Class<?> clazz, Object value, String... fieldnames) {
		if (!trySetStaticFinalField(clazz, value, fieldnames)) {
			throw new RuntimeException("Unable to set static field in " + clazz.getName());
		}
	}

}

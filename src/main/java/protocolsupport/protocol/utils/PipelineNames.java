package protocolsupport.protocol.utils;

import java.lang.reflect.Field;

import net.md_5.bungee.netty.PipelineUtils;

public final class PipelineNames {

	public static final String FRAME_PREPENDER = resolve("FRAME_PREPENDER", "FRAME_ENCODER", "PACKET_ENCODER");

	private PipelineNames() {
	}

	private static String resolve(String... fieldNames) {
		for (String fieldName : fieldNames) {
			try {
				Field field = PipelineUtils.class.getField(fieldName);
				Object value = field.get(null);
				if (value instanceof String) {
					return (String) value;
				}
			} catch (ReflectiveOperationException ignored) {
			}
		}
		throw new IllegalStateException("Unable to resolve frame prepender name from PipelineUtils");
	}

}

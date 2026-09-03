package ua.rawfish2d;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLDebugMessageCallback;

import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL43.*;

public class DebugCallback {
	private GLDebugMessageCallback debugCallback;

	/**
	 * Requires OpenGL 4.3
	 */
	private void setDebugCallback(GLDebugMessageCallback callback) {
		if (!GL.getCapabilities().OpenGL43) {
			System.err.print("Debug callback requires OpenGL 4.3 or higher.\n");
			return;
		}
		this.debugCallback = callback;
		glEnable(GL_DEBUG_OUTPUT);
		glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
		glDebugMessageCallback(callback, 0);
	}

	public void setDefaultDebugCallback() {
		setDebugCallback(new GLDebugMessageCallback() {
			@Override
			public void invoke(int a_source, int a_type, int a_id, int a_severity, int a_length, long a_message, long a_userParam) {
				final String message = GLDebugMessageCallback.getMessage(a_length, a_message);
				final String source = getSourceDebug(a_source);
				final String type = getTypeDebug(a_type);
				final String severity = getSeverityDebug(a_severity);
				final String errorMessage = String.format("[OpenGL] [%s %s %s] Reason: %s (id: %d)\n", source, type, severity, message, a_id);
				final String output = String.format("[%s] %s at:", type, message);
//				if (type.equals("OTHER")) {
//					return;
//				}
				final int startIndex = 4;
				printStackTrace(output, startIndex);
				System.err.println(errorMessage);
			}
		});
	}

	private static String getSourceDebug(int source) {
		final String sourceDebug;
		switch (source) {
			case GL_DEBUG_SOURCE_API: {
				sourceDebug = "API";
				break;
			}
			case GL_DEBUG_SOURCE_WINDOW_SYSTEM: {
				sourceDebug = "WINDOW SYSTEM";
				break;
			}
			case GL_DEBUG_SOURCE_SHADER_COMPILER: {
				sourceDebug = "SHADER COMPILER";
				break;
			}
			case GL_DEBUG_SOURCE_THIRD_PARTY: {
				sourceDebug = "THIRD PARTY";
				break;
			}
			case GL_DEBUG_SOURCE_APPLICATION: {
				sourceDebug = "APPLICATION";
				break;
			}
			case GL_DEBUG_SOURCE_OTHER: {
				sourceDebug = "OTHER";
				break;
			}
			default: {
				sourceDebug = "UNKNOWN";
				break;
			}
		}
		return sourceDebug;
	}

	private static String getTypeDebug(int type) {
		final String typeDebug;
		switch (type) {
			case GL_DEBUG_TYPE_ERROR: {
				typeDebug = "ERROR";
				break;
			}
			case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR: {
				typeDebug = "DEPRECATED_BEHAVIOR";
				break;
			}
			case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR: {
				typeDebug = "UNDEFINED_BEHAVIOR";
				break;
			}
			case GL_DEBUG_TYPE_PORTABILITY: {
				typeDebug = "PORTABILITY";
				break;
			}
			case GL_DEBUG_TYPE_PERFORMANCE: {
				typeDebug = "PERFORMANCE";
				break;
			}
			case GL_DEBUG_TYPE_MARKER: {
				typeDebug = "MARKER";
				break;
			}
			case GL_DEBUG_TYPE_OTHER: {
				typeDebug = "OTHER";
				break;
			}
			default: {
				typeDebug = "UNKNOWN";
				break;
			}
		}
		return typeDebug;
	}

	private static String getSeverityDebug(int severity) {
		final String severityDebug;
		switch (severity) {
			case GL_DEBUG_SEVERITY_NOTIFICATION: {
				severityDebug = "NOTIFICATION";
				break;
			}
			case GL_DEBUG_SEVERITY_LOW: {
				severityDebug = "LOW";
				break;
			}
			case GL_DEBUG_SEVERITY_MEDIUM: {
				severityDebug = "MEDIUM";
				break;
			}
			case GL_DEBUG_SEVERITY_HIGH: {
				severityDebug = "HIGH";
				break;
			}
			default: {
				severityDebug = "UNKNOWN";
				break;
			}
		}
		return severityDebug;
	}


	private static void printStackTrace(String message, int startIndex) {
		final String stackTrace = getStackTrace(message, startIndex);
		System.err.printf("%s", stackTrace);
	}

	private static String getStackTrace(String message, int startIndex) {
		final StackTraceElement[] stackTraceList = Thread.currentThread().getStackTrace();
		final StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(message).append("\n");
		int index = 0;
		for (StackTraceElement st : stackTraceList) {
			if (index < startIndex) {
				index++;
				continue;
			}
			final String lineNumber;
			String fileName = st.getFileName();
			final int lineNumberValue = st.getLineNumber();
			final String className = st.getClassName();
			final String methodName = st.getMethodName();

			if (fileName == null) {
				fileName = "";
			}

			if (lineNumberValue < 0) {
				lineNumber = "Unknown Source";
			} else {
				lineNumber = String.valueOf(st.getLineNumber());
			}

			stringBuilder.append(String.format("    at %s.%s(%s:%s)\n", className, methodName, fileName, lineNumber));
			index++;
		}
		return stringBuilder.toString();
	}

	public void destroy() {
		debugCallback.free();
	}
}

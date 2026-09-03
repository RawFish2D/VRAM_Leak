package ua.rawfish2d;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER;
import static org.lwjgl.opengl.GL30.*;

public class VRAMLeak {
	private final int ROWS_PER_SLICE = 1024;
	private final long window;
	private boolean pause = false;
	private boolean enableLeak = true;
	private Mode mode = Mode.STEP;
	private boolean step = false;

	public VRAMLeak() {
		if (!GLFW.glfwInit()) {
			throw new RuntimeException("GLFW initialization failed");
		}

		GLFW.glfwDefaultWindowHints();
		GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);
		GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
		GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);

		// use this or
		// GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
		// GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
		// GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 0);

		// use this, doesn't really matter
		GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_ANY_PROFILE);

		window = GLFW.glfwCreateWindow(800, 600, "VRAM Leak", 0, 0);
		if (window == 0) {
			throw new RuntimeException("GLFW window creation failed");
		}
		GLFW.glfwMakeContextCurrent(window);
		GL.createCapabilities();

		System.out.printf("LWJGL version: %s\n", Version.getVersion());
		System.out.printf("GLFW version: %s\n", GLFW.glfwGetVersionString());
		System.out.printf("Version: %s\n", GL11.glGetString(GL11.GL_VERSION));
		System.out.printf("Renderer: %s\n", GL11.glGetString(GL11.GL_RENDERER));

		DebugCallback debugCallback = new DebugCallback();
		debugCallback.setDefaultDebugCallback();
		GLFW.glfwSetKeyCallback(window, new GLFWKeyCallback() {
			@Override
			public void invoke(long window, int key, int scancode, int action, int mods) {
				if (action == GLFW.GLFW_RELEASE) {
					return;
				}
				if (key == GLFW.GLFW_KEY_ESCAPE) {
					GLFW.glfwSetWindowShouldClose(window, true);
				}
				if (key == GLFW.GLFW_KEY_SPACE) {
					pause = !pause;
					if (pause) {
						System.out.println("Paused");
					} else {
						System.out.println("Unpaused");
					}
				}
				if (key == GLFW.GLFW_KEY_L) {
					enableLeak = !enableLeak;
					if (enableLeak) {
						System.out.println("Leak enabled");
					} else {
						System.out.println("Leak disabled");
					}
				}
				if (key == GLFW.GLFW_KEY_1) {
					mode = Mode.CONTINUOUS;
					System.out.print("Continuous mode\n");
				}
				if (key == GLFW.GLFW_KEY_2) {
					mode = Mode.STEP;
					System.out.print("Step mode\n");
				}
				if (key == GLFW.GLFW_KEY_S) {
					step = true;
					System.out.print("Step\n");
				}
			}
		});

		GLFW.glfwSwapInterval(1);

		mode = Mode.STEP;
		System.out.print("Currently in Step mode\n");
		System.out.print("Press S to leak in steps\n");
		System.out.print("Press L to toggle leak\n");
		System.out.print("Press 1 to toggle continuous mode\n");
		System.out.print("Press 2 to toggle step mode\n");

		while (!GLFW.glfwWindowShouldClose(window)) {
			GLFW.glfwPollEvents();
			render();
			GLFW.glfwSwapBuffers(window);
		}

		debugCallback.destroy();
		GLFW.glfwDestroyWindow(window);
		GLFW.glfwTerminate();
	}

	private void render() {
		glClear(GL_COLOR_BUFFER_BIT);

		if (pause) {
			return;
		}
		if (mode == Mode.STEP && !step) {
			return;
		}
		step = false;

		final int pboID = glGenBuffers();
		final int textureID = glGenTextures();
		final int textureType = GL_TEXTURE_2D;
		final int textureInternalFormat = GL_RGBA8;
		final int textureFormat = GL_RGBA;
		final int textureDataType = GL_UNSIGNED_BYTE;
		final int width = 4096;
		final int height = 4096;

		// setup a dummy texture
		glBindTexture(textureType, textureID);
		glTexParameteri(textureType, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(textureType, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(textureType, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(textureType, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexImage2D(textureType, 0, textureInternalFormat, width, height, 0, textureFormat, textureDataType, (ByteBuffer) null);
		glBindTexture(textureType, 0);

		int currentUploadRow = 0;
		while (currentUploadRow < height) {
			final int sliceHeight = Math.min(ROWS_PER_SLICE, height - currentUploadRow);
			final int sliceSize = width * sliceHeight * 4;

			glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboID);
			glBufferData(GL_PIXEL_UNPACK_BUFFER, sliceSize, GL_STREAM_DRAW);
			final ByteBuffer cachedMappedBuffer;
			if (enableLeak) {
				cachedMappedBuffer = glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, sliceSize, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
			} else {
				cachedMappedBuffer = glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, sliceSize, GL_MAP_WRITE_BIT);
			}
			if (cachedMappedBuffer != null) {
				// upload something (garbage)
				for (int a = 0; a < sliceSize / 4; ++a) {
					cachedMappedBuffer.putInt(ThreadLocalRandom.current().nextInt());
				}
				cachedMappedBuffer.flip();
				glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
			} else {
				System.err.println("Failed to map buffer");
			}

			glBindTexture(textureType, textureID);
			glTexSubImage2D(textureType, 0, 0, currentUploadRow, width, sliceHeight, textureFormat, textureDataType, 0);
			glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
			glBindTexture(textureType, 0);

			currentUploadRow += sliceHeight;
		}
		glDeleteBuffers(pboID);
		glDeleteTextures(textureID);
	}

	private enum Mode {
		STEP,
		CONTINUOUS
	}

	public static void main(String[] args) {
		new VRAMLeak();
	}
}
# VRAM Memory Leaker

Example project that triggers a VRAM memory leak on AMD GPU drivers by just properly using OpenGL functions.

## Requirements
* A working GPU.
* OpenGL 3.0 support.
* Monitoring software (RTSS for per-process VRAM usage tracking, or HWInfo for total system VRAM usage) to observe the leak, as this app does not display VRAM usage by itself.
* Java 8 or newer.

## Build
To build the project run this command:
```
mvn clean compile package
```
To run the compiled project:
```
java -jar VRAM_Leak.jar
```

## How do I use it?
This project has two modes to observe the leak, and they are controlled by keys:
- 1 Key: Continuous mode. It will upload data every frame, leaking VRAM if Leak is enabled (L key)
- 2 Key: Step mode. It will only upload data for each S key press, leaking VRAM if Leak is enabled (L key)

Other keys:
- L Key: toggles the memory leak code, on or off.
- Space: pauses memory leaker in Continuous mode.
- Escape: closes the program.

## How does it work?
It creates an empty texture and a pixel buffer every frame:
```java
pboID = glGenBuffers();
textureID = glGenTexture();
```
Then, inside a loop, it binds and orphans the pixel buffer:
```java
glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboID);
glBufferData(GL_PIXEL_UNPACK_BUFFER, sliceSize, GL_STREAM_DRAW);
```
Random data is being uploaded into the texture using glMapBufferRange through the pixel buffer.
```java
cachedMappedBuffer = glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, sliceSize, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
for (int a = 0; a < sliceSize / 4; ++a) {
	cachedMappedBuffer.putInt(ThreadLocalRandom.current().nextInt());
}
cachedMappedBuffer.flip();
glUnmapBuffer(GL_PIXEL_UNPACK_BUFFER);
```
After which, data is uploaded into the texture:
```java
glBindTexture(textureType, textureID);
glTexSubImage2D(textureType, 0, 0, currentUploadRow, width, sliceHeight, textureFormat, textureDataType, 0);
glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);
glBindTexture(textureType, 0);
```
And after the loop, all data is deleted (well, except for the leaked VRAM):
```java
glDeleteBuffers(pboID);
glDeleteTextures(textureID);
```
## The leak 💧
The most important part is this:
```java
cachedMappedBuffer = glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, sliceSize, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
```
The reason VRAM leak happens is the `GL_MAP_INVALIDATE_BUFFER_BIT` flag.
Other flags like `GL_MAP_UNSYNCHRONIZED_BIT` never cause it.

Well, of course if you do this:
```java
cachedMappedBuffer = glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, sliceSize, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT | GL_MAP_UNSYNCHRONIZED_BIT);
```
Then leak also happens, as presence of `GL_MAP_INVALIDATE_BUFFER_BIT` triggers it, not when only `GL_MAP_INVALIDATE_BUFFER_BIT` and `GL_MAP_WRITE_BIT` are present.

## Are these OpenGL functions even useful?
### Yes.
This is not some archaic method of uploading texture data, it's usually used to upload texture data asynchronously to avoid frame drops.
But in this specific scenario if causes a memory leak, which is probably a bug in AMD drivers. Even though `GL_MAP_INVALIDATE_BUFFER_BIT` is redundant here (it orphans the buffer after it was already orphaned) it still shouldn't cause a memory leak.

## Observations (on AMD Radeon RX 7600 with 26.8.1 driver)
When texture is small enough, leaked VRAM clears by itself when it reached a certain peak usage (unless you run out of RAM).
* for 4096x4096 texture, GPU memory spills to RAM and crashes GPU driver when RAM is fully used
* for 2048x2048 texture it will peak and clear at around >7000 MB VRAM used (by process)
* for 1280x1280 texture it will peak and clear at around 3000 MB VRAM used (by process)
* for 1024x1024 texture it will peak and clear at around 2800 MB VRAM used (by process)
* for 512x1024 texture it will peak and clear at around 2800 MB VRAM used (by process)
* for 1024x512 texture it will peak and clear at around 2800 MB VRAM used (by process)
* for 768x768 texture it will peak and clear at around 1600 MB VRAM used (by process)
* for 512x512 texture it will peak and clear at around 1000 MB VRAM used (by process)
* for 256x256 texture it will peak and clear at around 200 MB VRAM used (by process)

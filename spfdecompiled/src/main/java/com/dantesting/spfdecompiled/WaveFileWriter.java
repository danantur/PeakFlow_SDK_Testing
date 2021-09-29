package com.dantesting.spfdecompiled;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class WaveFileWriter {
    private File file;
    private RandomAccessFile fileWriter;
    private boolean isClosed = false;
    private int totalNumberOfBytes;

    public WaveFileWriter(File wavFile, int rate, short numOfChannels, short samplesBitSize) throws IOException {
        this.file = wavFile;
        RandomAccessFile randomAccessFile = new RandomAccessFile(wavFile, "rw");
        this.fileWriter = randomAccessFile;
        randomAccessFile.setLength(0);
        this.fileWriter.writeBytes("RIFF");
        this.fileWriter.writeInt(0);
        this.fileWriter.writeBytes("WAVE");
        this.fileWriter.writeBytes("fmt ");
        this.fileWriter.writeInt(Integer.reverseBytes(16));
        this.fileWriter.writeShort(Short.reverseBytes((short) 1));
        this.fileWriter.writeShort(Short.reverseBytes(numOfChannels));
        this.fileWriter.writeInt(Integer.reverseBytes(rate));
        this.fileWriter.writeInt(Integer.reverseBytes(rate * samplesBitSize * 8 * numOfChannels));
        this.fileWriter.writeShort(Short.reverseBytes((short) (numOfChannels * samplesBitSize * 8)));
        this.fileWriter.writeShort(Short.reverseBytes(samplesBitSize));
        this.fileWriter.writeBytes("data");
        this.fileWriter.writeInt(0);
    }

    public void write(byte[] buffer) throws IOException {
        if (!this.isClosed) {
            this.fileWriter.write(buffer);
            this.totalNumberOfBytes += buffer.length;
        }
    }

    public void close() throws IOException {
        if (!this.isClosed) {
            this.fileWriter.seek(4);
            this.fileWriter.writeInt(Integer.reverseBytes(this.totalNumberOfBytes + 36));
            this.fileWriter.seek(40);
            this.fileWriter.writeInt(Integer.reverseBytes(this.totalNumberOfBytes));
            this.fileWriter.close();
            this.isClosed = true;
        }
    }

    public boolean isClosed() {
        return this.isClosed;
    }

    public File getFile() {
        return this.file;
    }
}

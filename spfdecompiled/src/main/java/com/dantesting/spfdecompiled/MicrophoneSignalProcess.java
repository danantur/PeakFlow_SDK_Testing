package com.dantesting.spfdecompiled;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import com.synthnet.spf.SignalProcess;

import java.io.File;
import java.io.IOException;

public class MicrophoneSignalProcess extends SignalProcess {
    public static final int RECORDER_SAMPLERATE = 44100;
    public static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    public static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_8BIT;
    public static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    private static MicrophoneSignalProcess instance = null;
    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format
    private BufferCallback bufferCallback = new BufferCallback() {
        /* class com.dantesting.spfdecompiled.MicrophoneSignalProcess.AnonymousClass1 */
        short[] tempBuffer;

        @Override // com.dantesting.spfdecompiled.SignalProcess.BufferCallback
        public double[] getBuffer(int size) {
            short[] sArr = this.tempBuffer;
            if (sArr == null || sArr.length != size) {
                this.tempBuffer = new short[size];
            }
            int read = 0;
            while (read < size && MicrophoneSignalProcess.this.isProcesing) {
                read += MicrophoneSignalProcess.this.recorder.read(this.tempBuffer, read, size - read);
                if (read >= size) {
                    break;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (!MicrophoneSignalProcess.this.isProcesing) {
                return null;
            }
            double[] ret = new double[size];
            byte[] wavData = new byte[(size * 2)];
            for (int i = 0; i < size; i++) {
                ret[i] = (double) (((float) this.tempBuffer[i]) / 32768.0f);
                if (MicrophoneSignalProcess.this.waveFileWriter != null) {
                    short[] sArr2 = this.tempBuffer;
                    wavData[i * 2] = (byte) (sArr2[i] & 255);
                    wavData[(i * 2) + 1] = (byte) (sArr2[i] >> 8);
                }
            }
            if (MicrophoneSignalProcess.this.waveFileWriter != null) {
                try {
                    MicrophoneSignalProcess.this.waveFileWriter.write(wavData);
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
            return ret;
        }
    };
    private boolean isProcesing;
    public AudioRecord recorder;
    private com.dantesting.spfdecompiled.WaveFileWriter waveFileWriter;

    public static MicrophoneSignalProcess getInstance() {
        if (instance == null) {
            instance = new MicrophoneSignalProcess();
        }
        return instance;
    }

    private MicrophoneSignalProcess() {
    }

    public synchronized void startAnalyze(OnPeakFound listener, OnModeChanged onModeChangedListener) {
        Log.d("spfdecompiled-Lib", "Start Analyze");
        if (!this.isProcesing) {
            this.isProcesing = true;
            //int audioSource, int sampleRateInHz, int channelConfig, int audioFormat, int bufferSizeInBytes
            if (this.waveFileWriter != null && this.waveFileWriter.isClosed()) {
                this.waveFileWriter = null;
            }
            this.recorder.startRecording();
            start(this.bufferCallback, listener, onModeChangedListener);
        }
    }

    public synchronized void stopAnalyze() {
        stopAnalyze(true);
    }

    public synchronized void stopAnalyze(boolean closeWaveFileWriter) {
        Log.d("spfdecompiled-Lib", "Stop Analyze");
        if (this.isProcesing) {
            this.recorder.stop();
            if (closeWaveFileWriter && this.waveFileWriter != null) {
                try {
                    this.waveFileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.waveFileWriter = null;
            }
            this.isProcesing = false;
        }
    }

    public synchronized void startCalibration(OnCalibrated listener) {
        Log.d("spfdecompiled-Lib", "Start Calibration");
        if (!this.isProcesing) {
            this.isProcesing = true;
            if (this.waveFileWriter != null && this.waveFileWriter.isClosed()) {
                this.waveFileWriter = null;
            }
            this.recorder.startRecording();
            startCalibration(this.bufferCallback, listener);
        }
    }

    public synchronized void stopCalibration() {
        stopCalibration(false);
    }

    public synchronized void stopCalibration(boolean closeWaveFileWriter) {
        Log.d("spfdecompiled-Lib", "Stop Calibration");
        if (this.isProcesing) {
            this.recorder.stop();
            if (closeWaveFileWriter && this.waveFileWriter != null) {
                try {
                    this.waveFileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.waveFileWriter = null;
            }
            this.isProcesing = false;
        }
    }

    public boolean isProcesing() {
        return this.isProcesing;
    }

    public void setRecordFile(File wavFile) {
        if (this.waveFileWriter != null) {
            try {
                this.waveFileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (wavFile == null) {
            this.waveFileWriter = null;
            return;
        }
        try {
            this.waveFileWriter = new WaveFileWriter(wavFile, RECORDER_SAMPLERATE, (short) 1, (short) 16);
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }

    public com.dantesting.spfdecompiled.WaveFileWriter getWaveFileWriter() {
        return this.waveFileWriter;
    }

    @Override // com.dantesting.spfdecompiled.SignalProcess
    public void logToFile(String fileName) {
        super.logToFile(fileName);
    }

    @Override // com.dantesting.spfdecompiled.SignalProcess
    public synchronized void close() {
        super.close();
        if (this.recorder != null) {
            this.recorder.release();
        }
        instance = null;
    }

    public synchronized void debugStartContinuous(OnPeakFound listener) {
        Log.d("spfdecompiled-Lib", "--DEBUG-- Start Analyze");
        if (!this.isProcesing) {
            this.isProcesing = true;
            if (this.waveFileWriter != null && this.waveFileWriter.isClosed()) {
                this.waveFileWriter = null;
            }
            this.recorder.startRecording();
            debugContinuousStart(this.bufferCallback, listener);
        }
    }
}

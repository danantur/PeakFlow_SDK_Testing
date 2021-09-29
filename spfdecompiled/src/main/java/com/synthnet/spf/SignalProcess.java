package com.synthnet.spf;

import com.dantesting.spfdecompiled.SPFMode;

public class SignalProcess {
    private static SignalProcess instance = null;
    private Thread workerThread;
    private boolean needDestroy = false;

    protected SignalProcess() {
        this.init();
    }

    public void close() {
        if (this.isRunning()) {
            this.needDestroy = true;
        } else {
            this.destroyNative();
        }

    }

    public synchronized void start(String filename, SignalProcess.OnAudioProcess listener) {
        if (!this.isRunning()) {
            this.workerThread = new SignalProcess.FileWorkerThread(filename, listener);
            this.workerThread.start();
        }

    }

    public synchronized void start(SignalProcess.BufferCallback bufferCallback, SignalProcess.OnPeakFound onPeakFoundListener, SignalProcess.OnModeChanged onModeChangedListener) {
        if (!this.isRunning()) {
            this.workerThread = new SignalProcess.StreamWorkerThread(bufferCallback, onPeakFoundListener, onModeChangedListener);
            this.workerThread.start();
        }

    }

    protected synchronized void debugContinuousStart(SignalProcess.BufferCallback bufferCallback, SignalProcess.OnPeakFound onPeakFoundListener) {
        if (!this.isRunning()) {
            this.workerThread = new SignalProcess.DebugWorkerThread(bufferCallback, onPeakFoundListener);
            this.workerThread.start();
        }

    }

    public synchronized void startCalibration(SignalProcess.BufferCallback bufferCallback, SignalProcess.OnCalibrated onCalibratedListener) {
        (new SignalProcess.CalibrationWorkerThread(bufferCallback, onCalibratedListener)).start();
    }

    public boolean isRunning() {
        return this.workerThread != null && this.workerThread.isAlive();
    }

    public native void init();

    private native void processingBuffer(SignalProcess.BufferCallback var1, SignalProcess.OnNativeResult var2, SignalProcess.OnModeChanged var3);

    private native int calibrate(SignalProcess.BufferCallback var1);

    private native void debugContinuousProcessBuffer(SignalProcess.BufferCallback var1, SignalProcess.OnNativeResult var2);

    private native int[] processingFile(String var1);

    private native void destroyNative();

    public native void logToFile(String var1);

    public native String getBuildDate();

    public native String getBranchName();

    public native String getCommitId();

    public native String getTag();

    public native String getRepoDirtyState();

    static {
        System.loadLibrary("spf-lib");
    }

    private class DebugWorkerThread extends Thread {
        SignalProcess.BufferCallback bufferCallback;
        SignalProcess.OnPeakFound onPeakFound;

        public DebugWorkerThread(SignalProcess.BufferCallback bufferCallback, SignalProcess.OnPeakFound onPeakFound) {
            this.bufferCallback = bufferCallback;
            this.onPeakFound = onPeakFound;
        }

        public void run() {
            SignalProcess.this.debugContinuousProcessBuffer(this.bufferCallback, new SignalProcess.OnNativeResult() {
                public void onResult(int frequency) {
                    if (DebugWorkerThread.this.onPeakFound != null) {
                        DebugWorkerThread.this.onPeakFound.onResult(frequency);
                    }

                }
            });
            if (SignalProcess.this.needDestroy) {
                SignalProcess.this.destroyNative();
            }

        }
    }

    private class CalibrationWorkerThread extends Thread {
        SignalProcess.BufferCallback bufferCallback;
        SignalProcess.OnCalibrated onCalibrated;

        public CalibrationWorkerThread(SignalProcess.BufferCallback bufferCallback, SignalProcess.OnCalibrated onCalibrated) {
            this.bufferCallback = bufferCallback;
            this.onCalibrated = onCalibrated;
        }

        public void run() {
            int result = SignalProcess.this.calibrate(this.bufferCallback);
            if (this.onCalibrated != null) {
                this.onCalibrated.onCalibrated(result);
            }

            if (SignalProcess.this.needDestroy) {
                SignalProcess.this.destroyNative();
            }

        }
    }

    private class StreamWorkerThread extends Thread {
        SignalProcess.BufferCallback bufferCallback;
        SignalProcess.OnPeakFound onPeakFound;
        SignalProcess.OnModeChanged onModeChangedListener;

        public StreamWorkerThread(SignalProcess.BufferCallback bufferCallback, SignalProcess.OnPeakFound onPeakFound, SignalProcess.OnModeChanged onModeChangedListener) {
            this.bufferCallback = bufferCallback;
            this.onPeakFound = onPeakFound;
            this.onModeChangedListener = onModeChangedListener;
        }

        public void run() {
            SignalProcess.this.processingBuffer(this.bufferCallback, new SignalProcess.OnNativeResult() {
                public void onResult(int frequency) {
                    if (StreamWorkerThread.this.onPeakFound != null) {
                        StreamWorkerThread.this.onPeakFound.onResult(frequency);
                    }

                }
            }, new SignalProcess.OnModeChanged() {
                public void onModeChanged(int cPreviousMode, int cMode) {
                    if (StreamWorkerThread.this.onModeChangedListener != null) {
                        SPFMode previousMode = SPFMode.values()[cPreviousMode];
                        SPFMode mode = SPFMode.values()[cMode];
                        StreamWorkerThread.this.onModeChangedListener.onModeChanged(previousMode, mode);
                    }

                }

                public void onModeChanged(SPFMode previousMode, SPFMode mode) {
                }
            });
            if (SignalProcess.this.needDestroy) {
                SignalProcess.this.destroyNative();
            }

        }
    }

    private class FileWorkerThread extends Thread {
        private String fileName;
        private SignalProcess.OnAudioProcess listener;

        public FileWorkerThread(String fileName, SignalProcess.OnAudioProcess listener) {
            this.fileName = fileName;
            this.listener = listener;
        }

        public void run() {
            int[] result = SignalProcess.this.processingFile(this.fileName);
            if (result != null) {
                if (this.listener != null) {
                    this.listener.onProcessFinished(result);
                }
            } else if (this.listener != null) {
                this.listener.onProcessFinished(new int[0]);
            }

            if (SignalProcess.this.needDestroy) {
                SignalProcess.this.destroyNative();
            }

        }
    }

    public interface OnModeChanged {
        void onModeChanged(SPFMode var1, SPFMode var2);
    }

    public interface OnCalibrated {
        void onCalibrated(int var1);
    }

    public interface OnPeakFound {
        void onResult(int var1);
    }

    private interface OnNativeResult {
        void onResult(int var1);
    }

    public interface BufferCallback {
        double[] getBuffer(int var1);
    }

    public interface OnAudioProcess {
        void onProcessFinished(int[] var1);
    }
}

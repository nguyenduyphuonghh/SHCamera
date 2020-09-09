package com.vcc.mysohalivestreamsdk.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;

/**
 * Modified by nguyenduyphuonghh on 30/08/2019
 */

public class AudioRecorderThread extends Thread {

    private static final String TAG = AudioRecorderThread.class.getSimpleName();

    private final int mSampleRate;
    private final long startTime;
    private volatile boolean stopThread = false;

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Context context;


    public AudioRecorderThread(int sampleRate, long recordStartTime, Context context) {
        this.context = context;
        this.mSampleRate = sampleRate;
        this.startTime = recordStartTime;
    }


    @Override
    public void run() {
        //Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

        int bufferSize = android.media.AudioRecord
                .getMinBufferSize(mSampleRate,
                        AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT);
        byte[][] audioData;
        int bufferReadResult;

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                mSampleRate, AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        // divide byte buffersize to 2 to make it short buffer
        audioData = new byte[1000][bufferSize];
        try {
            audioRecord.startRecording();
            isRecording = true;
        } catch (RuntimeException ex) {
        }

        int i = 0;
        byte[] data;
        while ((bufferReadResult = audioRecord.read(audioData[i], 0, audioData[i].length)) > 0) {

            data = audioData[i];

            Message msg = Message.obtain(new Handler(), 2, data);
            msg.arg1 = bufferReadResult;
            msg.arg2 = (int)(System.currentTimeMillis() - startTime);

            i++;
            if (i == 1000) {
                i = 0;
            }
            if (stopThread) {
                isRecording = false;
                break;
            }
        }

        {
        }

    }

    public void stopAudioRecording() {

        if (audioRecord != null && audioRecord.getRecordingState() == android.media.AudioRecord.RECORDSTATE_RECORDING) {
            stopThread = true;
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    private boolean isRelease() {
        return isRecording;
    }

}

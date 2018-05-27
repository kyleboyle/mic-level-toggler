package com.kyleboyle;

import javax.sound.sampled.*;
import java.util.function.Consumer;


public class LevelMonitor {

    Consumer<Integer> levelCallback;

    private volatile boolean doStop = false;

    public LevelMonitor(Consumer<Integer> levelCallback) {
        this.levelCallback = levelCallback;
    }

    private int calculateRMSLevel(byte[] audioData, int length) {
        long lSum = 0;
        for (int i = 0; i < length; i++)
            lSum = lSum + audioData[i];

        double dAvg = lSum / length;
        double sumMeanSquare = 0d;

        for (int j = 0; j <length; j++)
            sumMeanSquare += Math.pow(audioData[j] - dAvg, 2d);

        double averageMeanSquare = sumMeanSquare / length;

        return (int) (Math.pow(averageMeanSquare, 0.5d) + 0.5);
    }

    public void stop() {
        this.doStop = true;
    }

    public void start() throws LineUnavailableException {

        AudioFormat format = new AudioFormat(1000, 16, 1, true, true);

        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format);

        TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(targetInfo);
        microphone.open(format);

        new Thread(() -> {
            microphone.start();
            this.doStop = false;
            int level = 0;
            int numBytesRead = 0;
            byte[] targetData = new byte[microphone.getBufferSize() / 5];
            try {
                while (!doStop) {
                    numBytesRead = microphone.read(targetData, 0, targetData.length);
                    if (numBytesRead > 0) {
                        level = calculateRMSLevel(targetData, numBytesRead);
                        this.levelCallback.accept(level);
                    } else {
                        Thread.sleep(100);
                    }
                }
                System.out.println("Stopping microphone read");
                microphone.stop();
                microphone.close();
            } catch (Exception e) {
                System.out.println(e);
                microphone.stop();
                microphone.close();
                System.exit(1);
            }
        }).start();

    }
}

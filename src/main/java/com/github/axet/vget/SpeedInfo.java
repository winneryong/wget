package com.github.axet.vget;

import java.util.ArrayList;

public class SpeedInfo {

    public class Sample {
        // bytes downloaded
        public long current;
        // current time
        public long now;

        public Sample() {
            current = 0;
            now = System.currentTimeMillis();
        }

        public Sample(long current) {
            this.current = current;
            now = System.currentTimeMillis();
        }
    }

    ArrayList<Sample> samples = new ArrayList<SpeedInfo.Sample>();
    long lastUpdate = 0;

    static final int SAMPLE_LENGTH = 2000;
    static final int SAMPLE_MAX = 20;

    public SpeedInfo() {
    }

    public void start(long current) {
        samples.add(new Sample(current));

        long now = System.currentTimeMillis();
        lastUpdate = now;
    }

    public void step(long current) {
        long now = System.currentTimeMillis();

        if (lastUpdate + SAMPLE_LENGTH < now) {
            lastUpdate = now;
            samples.add(new Sample(current));
        }
    }

    /**
     * bytes per second
     * 
     * @return
     */
    public int getCurrentSpeed() {
        if (samples.size() < 2)
            return 0;

        // start block
        Sample s = samples.get(0);

        // [s1] [s2] [end]
        Sample s1 = samples.get(samples.size() - 2);
        Sample s2 = samples.get(samples.size() - 1);

        long current = s2.current - s1.current - s.current;
        long time = s2.now - s1.now;

        return (int) (current * 1000 / time);
    }

    public int getAverageSpeed() {
        return 0;
    }

    public int getSamples() {
        return samples.size();
    }

    public Sample getSample(int index) {
        return samples.get(index);
    }
}

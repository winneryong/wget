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

    Sample start;

    public static final int SAMPLE_LENGTH = 2000;
    public static final int SAMPLE_MAX = 20;

    public SpeedInfo() {
    }

    void add(Sample s) {
        samples.add(s);

        while (samples.size() > SAMPLE_MAX)
            samples.remove(0);
    }

    public void start(long current) {
        add(start = new Sample(current));

        long now = System.currentTimeMillis();
        lastUpdate = now;
    }

    public void step(long current) {
        long now = System.currentTimeMillis();

        if (lastUpdate + SAMPLE_LENGTH < now) {
            lastUpdate = now;
            add(new Sample(current));
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
        Sample s = start;

        // [s1] [s2] [end]
        Sample s1 = samples.get(samples.size() - 2);
        Sample s2 = samples.get(samples.size() - 1);

        long current = s2.current - s1.current - s.current;
        long time = s2.now - s1.now;

        return (int) (current * 1000 / time);
    }

    public int getAverageSpeed() {
        Sample s1 = start;
        Sample s2 = samples.get(samples.size() - 1);

        long current = s2.current - s1.current;
        long time = s2.now - s1.now;

        return (int) (current * 1000 / time);
    }

    public int getSamples() {
        return samples.size();
    }

    public Sample getSample(int index) {
        return samples.get(index);
    }
}

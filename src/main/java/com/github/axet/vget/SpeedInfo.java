package com.github.axet.vget;

import java.util.ArrayList;

public class SpeedInfo {

    public class Sample {
        // bytes downloaded
        public long current;
        // current time
        public long now;
        // start block? used to mark block after download has been altered /
        // restarted
        public boolean start;

        public Sample() {
            current = 0;
            now = System.currentTimeMillis();
            start = false;
        }

        public Sample(long current) {
            this.current = current;
            now = System.currentTimeMillis();
            start = false;
        }

        public Sample(long current, long now) {
            this.current = current;
            this.now = now;
            start = false;
        }
    }

    ArrayList<Sample> samples = new ArrayList<SpeedInfo.Sample>();

    public static final int SAMPLE_LENGTH = 2000;
    public static final int SAMPLE_MAX = 20;

    // start sample use to calculate average speed
    Sample start = null;

    public SpeedInfo() {
    }

    /**
     * Start calculate speed from 'current' bytes downloaded
     * 
     * @param current
     */
    public void start(long current) {
        Sample s = new Sample(current);
        s.start = true;
        start = s;
        add(s);
    }

    /**
     * step download process with 'current' bytes downloaded
     * 
     * @param current
     */
    public void step(long current) {
        long now = System.currentTimeMillis();

        long lastUpdate = getLastUpdate();
        if (lastUpdate + SAMPLE_LENGTH < now) {
            add(new Sample(current, now));
        }
    }

    /**
     * Current download speed
     * 
     * @return bytes per second
     */
    public int getCurrentSpeed() {
        if (samples.size() < 2)
            return 0;

        // [s1] [s2] [EOF]
        Sample s1 = samples.get(samples.size() - 2);
        Sample s2 = samples.get(samples.size() - 1);

        long current = s2.current - s1.current - start.current;
        long time = s2.now - s1.now;

        if (time == 0)
            return 0;

        return (int) (current * 1000 / time);
    }

    /**
     * Average speed from start download
     * 
     * @return bytes per second
     */
    public int getAverageSpeed() {
        Sample s2 = samples.get(samples.size() - 1);

        long current = s2.current - start.current;
        long time = s2.now - start.now;

        return (int) (current * 1000 / time);
    }

    public int getSamples() {
        return samples.size();
    }

    public Sample getSample(int index) {
        return samples.get(index);
    }

    //
    // protected
    //

    Sample getStart() {
        for (int i = samples.size() - 1; i >= 0; i--) {
            Sample s = samples.get(i);
            if (s.start)
                return s;
        }

        return null;
    }

    void add(Sample s) {
        // check if we have broken / restarted download. check if here some
        // samples
        if (samples.size() > 1) {
            Sample s1 = samples.get(samples.size() - 1);
            // check if last download 'current' stands before current 'current'
            // download
            if (s1.current > s.current) {
                s.start = true;
                start = s;
            }
        }

        samples.add(s);

        while (samples.size() > SAMPLE_MAX)
            samples.remove(0);
    }

    long getLastUpdate() {
        if (samples.size() == 0)
            return 0;

        Sample s = samples.get(samples.size() - 1);
        return s.now;
    }
}

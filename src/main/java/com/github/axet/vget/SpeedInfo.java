package com.github.axet.vget;

import java.util.ArrayList;

public class SpeedInfo {

    public class Sample {
        // bytes downloaded
        public long current;
        // current time
        public long now;
        // start block? used to mark block after download has been altered / restarted
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

    public SpeedInfo() {
    }

    Sample getStart() {
        for (int i = samples.size() - 1; i >= 0; i--) {
            Sample s = samples.get(i);
            if (s.start)
                return s;
        }

        throw new RuntimeException("start sample not found");
    }

    void add(Sample s) {
        Sample start = getStart();
        if (start.current > s.current) {
            s.start = true;
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

    public void start(long current) {
        Sample s = new Sample(current);
        s.start = true;
        add(s);
    }

    public void step(long current) {
        long now = System.currentTimeMillis();

        long lastUpdate = getLastUpdate();
        if (lastUpdate + SAMPLE_LENGTH < now) {
            add(new Sample(current, now));
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

        Sample start = getStart();

        // start block
        Sample s = start;

        // [s1] [s2] [EOF]
        Sample s1 = samples.get(samples.size() - 2);
        Sample s2 = samples.get(samples.size() - 1);

        long current = s2.current - s1.current - s.current;
        long time = s2.now - s1.now;

        if (time == 0)
            return 0;

        return (int) (current * 1000 / time);
    }

    public int getAverageSpeed() {
        Sample start = getStart();

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

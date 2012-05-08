package com.github.axet.vget.info;

import java.util.Map;

public interface VGetInfo {

    public enum VideoQuality {
        p2304, p1080, p720, p480, p360, p270, p224
    }

    public static class VideoURL {
        public VideoQuality vq;
        public String ext;
        public String url;

        public VideoURL(VideoQuality vq, String url, String ext) {
            this.vq = vq;
            this.url = url;
            this.ext = ext;
        }
    }

    public void extract();

    public String getSource();

    public String getTitle();

    public Map<VideoQuality, VideoURL> getVideos();
}

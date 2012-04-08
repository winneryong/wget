package com.github.axet.vget.info;

public interface VGetInfo {

    public enum VideoQuality {
        p1080, p720, p480, p360, p240, p120
    }

    public static class VideoURL {
        public VideoQuality vq;
        public String url;

        public VideoURL(VideoQuality vq, String url) {
            this.vq = vq;
            this.url = url;
        }
    }

    public static class DownloadError extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public DownloadError(String msg) {
            super(msg);
        }
    }

    public void extract();

    public String getSource();

    public String getTitle();

    public VideoURL getVideo();
}

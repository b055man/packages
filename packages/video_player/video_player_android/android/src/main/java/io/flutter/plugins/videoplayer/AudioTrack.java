package io.flutter.plugins.videoplayer;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

class AudioTrack implements Serializable {
    public int groupId;
    public int trackId;
    public String language;
    public String label;
    public boolean isCurrent;

    public AudioTrack(int groupId, int trackId) {
        this.trackId = trackId;
        this.groupId = groupId;
        this.language = null;
        this.label = null;
        this.isCurrent = false;
    }

    public AudioTrack(int groupId, int trackId, String language, String label, boolean isCurrent) {
        this.trackId = trackId;
        this.groupId = groupId;
        this.language = language;
        this.label = label;
        this.isCurrent = isCurrent;
    }

    @Override
    public String toString() {
        return "AudioTrack{" +
                "groupId='" + groupId + '\'' +
                ", trackId=" + trackId +
                ", language=" + language +
                ", label=" + label +
                ", isCurrent=" + isCurrent +
                '}';
    }

    public Map<String, Object> asMap() {
        return Map.of(
                "groupId", groupId,
                "trackId", trackId,
                "language", Objects.requireNonNullElse(language, ""),
                "label", Objects.requireNonNullElse(label, ""),
                "isCurrent", isCurrent
        );
    }
}

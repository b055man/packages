// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import static androidx.media3.common.Player.REPEAT_MODE_ALL;
import static androidx.media3.common.Player.REPEAT_MODE_OFF;

import android.content.Context;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.VisibleForTesting;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;

import java.util.List;

import io.flutter.view.TextureRegistry;

final class VideoPlayer {
  private ExoPlayer exoPlayer;
  private Surface surface;
  private final TextureRegistry.SurfaceTextureEntry textureEntry;
  private final VideoPlayerCallbacks videoPlayerEvents;
  private final VideoPlayerOptions options;

  /**
   * Creates a video player.
   *
   * @param context      application context.
   * @param events       event callbacks.
   * @param textureEntry texture to render to.
   * @param asset        asset to play.
   * @param options      options for playback.
   * @return a video player instance.
   */
  @NonNull
  static VideoPlayer create(
          Context context,
          VideoPlayerCallbacks events,
          TextureRegistry.SurfaceTextureEntry textureEntry,
          VideoAsset asset,
          VideoPlayerOptions options) {
    ExoPlayer.Builder builder =
            new ExoPlayer.Builder(context).setMediaSourceFactory(asset.getMediaSourceFactory(context));
    return new VideoPlayer(builder, events, textureEntry, asset.getMediaItem(), options);
  }

  @VisibleForTesting
  VideoPlayer(
          ExoPlayer.Builder builder,
          VideoPlayerCallbacks events,
          TextureRegistry.SurfaceTextureEntry textureEntry,
          MediaItem mediaItem,
          VideoPlayerOptions options) {
    this.videoPlayerEvents = events;
    this.textureEntry = textureEntry;
    this.options = options;

    ExoPlayer exoPlayer = builder.build();
    exoPlayer.setMediaItem(mediaItem);
    exoPlayer.prepare();

    setUpVideoPlayer(exoPlayer);
  }

  private void setUpVideoPlayer(ExoPlayer exoPlayer) {
    this.exoPlayer = exoPlayer;

    surface = new Surface(textureEntry.surfaceTexture());
    exoPlayer.setVideoSurface(surface);
    setAudioAttributes(exoPlayer, options.mixWithOthers);
    exoPlayer.addListener(new ExoPlayerEventListener(exoPlayer, videoPlayerEvents));
  }

  void sendBufferingUpdate() {
    videoPlayerEvents.onBufferingUpdate(exoPlayer.getBufferedPosition());
  }

  private static void setAudioAttributes(ExoPlayer exoPlayer, boolean isMixMode) {
    exoPlayer.setAudioAttributes(
            new AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            !isMixMode);
  }

  void play() {
    exoPlayer.setPlayWhenReady(true);
  }

  void pause() {
    exoPlayer.setPlayWhenReady(false);
  }

  void setLooping(boolean value) {
    exoPlayer.setRepeatMode(value ? REPEAT_MODE_ALL : REPEAT_MODE_OFF);
  }

  void setVolume(double value) {
    float bracketedValue = (float) Math.max(0.0, Math.min(1.0, value));
    exoPlayer.setVolume(bracketedValue);
  }

  void setPlaybackSpeed(double value) {
    // We do not need to consider pitch and skipSilence for now as we do not handle them and
    // therefore never diverge from the default values.
    final PlaybackParameters playbackParameters = new PlaybackParameters(((float) value));

    exoPlayer.setPlaybackParameters(playbackParameters);
  }

  void seekTo(int location) {
    exoPlayer.seekTo(location);
  }

  long getPosition() {
    return exoPlayer.getCurrentPosition();
  }

  @OptIn(markerClass = UnstableApi.class)
  void changeAudioTrack(AudioTrack audioTrack) {
    if (audioTrack.groupId < 0) {
      Log.w("VideoPlayer", "GroupId smaller than zero: " + audioTrack.groupId);
      return;
    }

    Tracks currentTracks = exoPlayer.getCurrentTracks();
    List<Tracks.Group> groups = currentTracks.getGroups();
    if (audioTrack.groupId >= groups.size()) {
      Log.w("VideoPlayer", "GroupId larger or equal than allowed size - groupId " + audioTrack.groupId + ", size:" + groups.size());
      return;
    }

    Tracks.Group group = groups.get(audioTrack.groupId);
    if (group.getType() != C.TRACK_TYPE_AUDIO) {
      Log.w("VideoPlayer", "The track with id: " + audioTrack.groupId + " is not an audio track");
      return;
    }

    if (!group.isTrackSupported(audioTrack.trackId, false)) {
      Log.w("VideoPlayer", "The track with id: " + audioTrack.trackId + " is not supported");
      return;
    }

    TrackSelectionOverride override = new TrackSelectionOverride(group.getMediaTrackGroup(), audioTrack.trackId);
    exoPlayer.setTrackSelectionParameters(
            exoPlayer.getTrackSelectionParameters().buildUpon()
                    .setOverrideForType(override)
                    .build()
    );
  }

  void dispose() {
    textureEntry.release();
    if (surface != null) {
      surface.release();
    }
    if (exoPlayer != null) {
      exoPlayer.release();
    }
  }
}

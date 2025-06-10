// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.videoplayer;

import static androidx.media3.common.Player.REPEAT_MODE_ALL;
import static androidx.media3.common.Player.REPEAT_MODE_OFF;

import android.content.Context;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.ExoPlayer;
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
   * @param context application context.
   * @param events event callbacks.
   * @param textureEntry texture to render to.
   * @param asset asset to play.
   * @param options options for playback.
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

  void setAudioTrack(int groupId, int trackId/*, boolean enabled*/) {
      Tracks currentTracks = exoPlayer.getCurrentTracks();
      // Check if groupId is within bounds and the group exists
      if (groupId >= 0 && groupId < currentTracks.getGroups().size()) {
        Tracks.Group group = currentTracks.getGroups().get(groupId);

        // In Kotlin, 'group.type' is an int. 'trackTypes.contains(group.type)' checks if the type is allowed.
        // 'group.isTrackSupported(trackId, false)' checks if the track is supported.
        if ((group.getType() == C.TRACK_TYPE_AUDIO) && group.isTrackSupported(trackId, false)) {
          //if (enabled) {
            // Create a new TrackSelectionOverride and set it
            TrackSelectionOverride override = new TrackSelectionOverride(group.getMediaTrackGroup(), trackId);
            exoPlayer.setTrackSelectionParameters(
                    exoPlayer.getTrackSelectionParameters().buildUpon()
                            .setOverrideForType(override)
                            .build()
            );
//          }
//        else {
//            // Check if an override for this mediaTrackGroup exists
//            // and if it contains the specific trackId
//            if (exoPlayer.getTrackSelectionParameters().overrides.containsKey(group.getMediaTrackGroup()) &&
//                    exoPlayer.getTrackSelectionParameters().overrides.get(group.getMediaTrackGroup()).trackIndices.contains(trackId)) {
//              // Clear the override for this mediaTrackGroup
//              exoPlayer.setTrackSelectionParameters(
//                      exoPlayer.getTrackSelectionParameters().buildUpon()
//                              .clearOverride(group.getMediaTrackGroup())
//                              .build()
//              );
//            }
//          }
        }
      }
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

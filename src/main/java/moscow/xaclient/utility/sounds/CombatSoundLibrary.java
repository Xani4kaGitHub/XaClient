package moscow.xaclient.utility.sounds;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import moscow.xaclient.XaClient;
import moscow.xaclient.systems.setting.settings.ModeSetting;
import net.minecraft.resource.Resource;

public final class CombatSoundLibrary {
   public static final SoundEntry[] HIT_SOUNDS = new SoundEntry[]{
      new SoundEntry("Bell", "combat/hit/bell.wav"),
      new SoundEntry("Bonk", "combat/hit/bonk.wav"),
      new SoundEntry("Bubble", "combat/hit/bubble.wav"),
      new SoundEntry("Hit 1", "combat/hit/hit1.wav"),
      new SoundEntry("Hit 2", "combat/hit/hit2.wav"),
      new SoundEntry("Hit 3", "combat/hit/hit3.wav"),
      new SoundEntry("Pop", "combat/hit/pop.wav"),
      new SoundEntry("Voice 1", "combat/hit/moan1.wav"),
      new SoundEntry("Voice 2", "combat/hit/moan2.wav"),
      new SoundEntry("Voice 3", "combat/hit/moan3.wav"),
      new SoundEntry("Voice 4", "combat/hit/moan4.wav"),
      new SoundEntry("Uwu", "combat/hit/uwu.wav"),
      new SoundEntry("Anime Short", "combat/hit/anime_female_short.mp3"),
      new SoundEntry("COD Hitmarker", "combat/hit/cod_hitmarker.mp3"),
      new SoundEntry("Headshot Spotted", "combat/hit/headshot_spotted.mp3"),
      new SoundEntry("Job Done", "combat/hit/job_done.mp3"),
      new SoundEntry("Knob Click", "combat/hit/knob.mp3"),
      new SoundEntry("Light", "combat/hit/light.mp3"),
      new SoundEntry("Oreo Chime", "combat/hit/oreo_chime.mp3"),
      new SoundEntry("Chock Pro", "combat/hit/chock_pro.mp3"),
      new SoundEntry("Chock Pro Advanced", "combat/hit/chock_pro_advanced.mp3"),
      new SoundEntry("Yoshi Tongue", "combat/hit/yoshi_tongue.mp3"),
      new SoundEntry("Camera", "combat/hit/camera.mp3")
   };
   public static final SoundEntry[] KILL_SOUNDS = new SoundEntry[]{
      new SoundEntry("Bell", "combat/hit/bell.wav"),
      new SoundEntry("Bonk", "combat/hit/bonk.wav"),
      new SoundEntry("Pop", "combat/hit/pop.wav"),
      new SoundEntry("Anime Short", "combat/kill/anime_female_short.mp3"),
      new SoundEntry("Mario Coin", "combat/kill/mario_coin.mp3"),
      new SoundEntry("Microwave Bell", "combat/kill/microwave_bell.mp3"),
      new SoundEntry("Steam Message", "combat/kill/steam_message.mp3"),
      new SoundEntry("Count Dot", "combat/kill/count_dot.mp3"),
      new SoundEntry("Money", "combat/kill/money.mp3"),
      new SoundEntry("Yamete", "combat/kill/yamete.mp3"),
      new SoundEntry("Sosat", "combat/kill/sosat.mp3")
   };
   private static final Random RANDOM = new Random();
   private static final Set<String> WARNED = new HashSet<>();
   private static final ExecutorService SOUND_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
      Thread thread = new Thread(runnable, "XaClient-CombatSound");
      thread.setDaemon(true);
      return thread;
   });

   public static ModeSetting.Value[] createValues(ModeSetting setting, SoundEntry[] entries, int defaultIndex) {
      ModeSetting.Value[] values = new ModeSetting.Value[entries.length];

      for (int i = 0; i < entries.length; i++) {
         values[i] = new ModeSetting.Value(setting, entries[i].displayName());
         if (i == defaultIndex) {
            values[i].select();
         }
      }

      return values;
   }

   public static SoundEntry selected(SoundEntry[] entries, ModeSetting.Value[] values) {
      for (int i = 0; i < entries.length && i < values.length; i++) {
         if (values[i].isSelected()) {
            return entries[i];
         }
      }

      return entries.length == 0 ? null : entries[0];
   }

   public static SoundEntry randomPlayable(SoundEntry[] entries) {
      SoundEntry[] playable = java.util.Arrays.stream(entries).filter(CombatSoundLibrary::isPlayable).toArray(SoundEntry[]::new);
      if (playable.length == 0) {
         return entries.length == 0 ? null : entries[RANDOM.nextInt(entries.length)];
      }

      return playable[RANDOM.nextInt(playable.length)];
   }

   public static void play(SoundEntry entry, float volume) {
      if (entry == null || volume <= 0.0F) {
         return;
      }

      byte[] soundData = readAsset(entry.relativePath());
      if (soundData == null || soundData.length == 0) {
         return;
      }

      float safeVolume = Math.max(0.0F, Math.min(3.0F, volume));
      SOUND_EXECUTOR.execute(() -> playOnSoundThread(soundData, entry.relativePath(), safeVolume));
   }

   private static void playOnSoundThread(byte[] soundData, String relativePath, float volume) {
      if (relativePath.toLowerCase().endsWith(".mp3")) {
         playMp3(soundData, relativePath, volume);
      } else {
         playSampled(soundData, relativePath, volume);
      }
   }

   private static void playSampled(byte[] soundData, String relativePath, float volume) {
      try (AudioInputStream stream = AudioSystem.getAudioInputStream(new BufferedInputStream(new ByteArrayInputStream(soundData)))) {
         playStream(stream, volume);
      } catch (Exception exception) {
         warnOnce("unsupported:" + relativePath, "Unsupported combat sound format: " + relativePath);
      }
   }

   private static void playMp3(byte[] soundData, String relativePath, float volume) {
      try (BufferedInputStream input = new BufferedInputStream(new ByteArrayInputStream(soundData))) {
         Bitstream bitstream = new Bitstream(input);
         Decoder decoder = new Decoder();
         ByteArrayOutputStream output = new ByteArrayOutputStream();
         int sampleRate = 44100;
         int channels = 2;

         try {
            Header header;
            while ((header = bitstream.readFrame()) != null) {
               SampleBuffer buffer = (SampleBuffer)decoder.decodeFrame(header, bitstream);
               sampleRate = buffer.getSampleFrequency();
               channels = buffer.getChannelCount();
               short[] samples = buffer.getBuffer();
               int length = buffer.getBufferLength();

               for (int i = 0; i < length; i++) {
                  short sample = samples[i];
                  output.write(sample & 0xFF);
                  output.write(sample >> 8 & 0xFF);
               }

               bitstream.closeFrame();
            }
         } finally {
            bitstream.close();
         }

         byte[] pcm = output.toByteArray();
         AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
         try (AudioInputStream stream = new AudioInputStream(new ByteArrayInputStream(pcm), format, pcm.length / format.getFrameSize())) {
            playStream(stream, volume);
         }
      } catch (Exception exception) {
         warnOnce("unsupported:" + relativePath, "Unsupported combat sound format: " + relativePath);
      }
   }

   private static void playStream(AudioInputStream stream, float volume) throws Exception {
      Clip clip = AudioSystem.getClip();
      clip.open(stream);
      applyVolume(clip, volume);
      clip.addLineListener(event -> {
         if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
            clip.close();
         }
      });
      clip.start();
   }

   private static void applyVolume(Clip clip, float volume) {
      if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
         FloatControl gain = (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
         float db = gainDb(volume);
         gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
      } else if (clip.isControlSupported(BooleanControl.Type.MUTE)) {
         ((BooleanControl)clip.getControl(BooleanControl.Type.MUTE)).setValue(volume <= 0.0F);
      }
   }

   private static float gainDb(float volume) {
      return volume <= 0.0F ? -80.0F : (float)(20.0 * Math.log10(volume));
   }

   private static boolean isPlayable(SoundEntry entry) {
      String path = entry.relativePath().toLowerCase();
      return path.endsWith(".wav") || path.endsWith(".mp3");
   }

   private static byte[] readAsset(String relativePath) {
      try {
         Optional<Resource> resource = XaClient.mc.getResourceManager().getResource(XaClient.id("sounds/" + relativePath));
         if (resource.isEmpty()) {
            warnOnce("missing:" + relativePath, "Combat sound asset not found: " + relativePath);
            return null;
         }

         try (InputStream input = resource.get().getInputStream()) {
            return input.readAllBytes();
         }
      } catch (IOException exception) {
         warnOnce("read:" + relativePath, "Failed to read combat sound asset: " + relativePath);
         return null;
      }
   }

   private static void warnOnce(String key, String message) {
      synchronized (WARNED) {
         if (WARNED.add(key)) {
            XaClient.LOGGER.warn(message);
         }
      }
   }

   private CombatSoundLibrary() {
   }

   public record SoundEntry(String displayName, String relativePath) {
   }
}

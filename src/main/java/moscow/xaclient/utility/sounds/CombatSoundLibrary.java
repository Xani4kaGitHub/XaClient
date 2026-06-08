package moscow.xaclient.utility.sounds;

import java.io.File;
import java.util.HashSet;
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
import moscow.xaclient.systems.modules.impl.BaseModule;
import moscow.xaclient.systems.setting.settings.ModeSetting;

public final class CombatSoundLibrary {
   public static final SoundEntry[] HIT_SOUNDS = new SoundEntry[]{
      new SoundEntry("Bell", "bell.wav"),
      new SoundEntry("Bonk", "bonk.wav"),
      new SoundEntry("Bubble", "bubble.wav"),
      new SoundEntry("Hit 1", "hit1.wav"),
      new SoundEntry("Hit 2", "hit2.wav"),
      new SoundEntry("Hit 3", "hit3.wav"),
      new SoundEntry("Pop", "pop.wav"),
      new SoundEntry("Voice 1", "moan1.wav"),
      new SoundEntry("Voice 2", "moan2.wav"),
      new SoundEntry("Voice 3", "moan3.wav"),
      new SoundEntry("Voice 4", "moan4.wav"),
      new SoundEntry("Uwu", "uwu.wav"),
      new SoundEntry("Anime Short", "AnimeFemaleShort(Hitsound).mp3"),
      new SoundEntry("COD Hitmarker", "COD_Hitmarker(Hitsound).mp3"),
      new SoundEntry("Headshot Spotted", "HeadshotSpotted1(Hitsound).mp3"),
      new SoundEntry("Job Done", "JobDone(Hitsound).mp3"),
      new SoundEntry("Knob Click", "Knob(Hitsound).mp3"),
      new SoundEntry("Light", "Light(Hitsound).mp3"),
      new SoundEntry("Oreo Chime", "OreoChime(Hitsound).mp3"),
      new SoundEntry("Chock Pro", "TheRevez5ChockPro(Hitsound).mp3"),
      new SoundEntry("Chock Pro Advanced", "TheRevez6ChockProAdvanced(Hitsound).mp3"),
      new SoundEntry("Yoshi Tongue", "YoshiTongueShort(Hitsound).mp3")
   };
   public static final SoundEntry[] KILL_SOUNDS = new SoundEntry[]{
      new SoundEntry("Bell", "bell.wav"),
      new SoundEntry("Bonk", "bonk.wav"),
      new SoundEntry("Pop", "pop.wav"),
      new SoundEntry("Anime Short", "killsound/AnimeFemaleShort(Hitsound).mp3"),
      new SoundEntry("Mario Coin", "killsound/MarioCoin(Hitsound).mp3"),
      new SoundEntry("Microwave Bell", "killsound/MicrowaveShortBell1(Hitsound).mp3"),
      new SoundEntry("Steam Message", "killsound/SteamMessageEarrape(Hitsound).mp3"),
      new SoundEntry("Count Dot", "killsound/TheRevez1CountDotShort(Hitsound).mp3"),
      new SoundEntry("Money", "killsound/TheRevez2MoneY(Hitsound).mp3"),
      new SoundEntry("Yamete", "killsound/YameteAnimeFemale(Hitsound).mp3")
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

      File file = resolve(entry.relativePath());
      if (file == null || !file.exists()) {
         warnOnce("missing:" + entry.relativePath(), "Combat sound file not found: " + entry.relativePath());
         return;
      }

      float safeVolume = Math.max(0.0F, Math.min(3.0F, volume));
      SOUND_EXECUTOR.execute(() -> playOnSoundThread(file, entry.relativePath(), safeVolume));
   }

   private static void playOnSoundThread(File file, String relativePath, float volume) {
      if (relativePath.toLowerCase().endsWith(".mp3")) {
         playMp3(file, volume);
      } else {
         playSampled(file, volume);
      }
   }

   private static void playSampled(File file, float volume) {
      try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
         playStream(stream, volume);
      } catch (Exception exception) {
         warnOnce(file.getAbsolutePath(), "Unsupported combat sound format: " + file.getName());
      }
   }

   private static void playMp3(File file, float volume) {
      try (java.io.BufferedInputStream input = new java.io.BufferedInputStream(new java.io.FileInputStream(file))) {
         Bitstream bitstream = new Bitstream(input);
         Decoder decoder = new Decoder();
         java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
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
         try (AudioInputStream stream = new AudioInputStream(new java.io.ByteArrayInputStream(pcm), format, pcm.length / format.getFrameSize())) {
            playStream(stream, volume);
         }
      } catch (Exception exception) {
         warnOnce(file.getAbsolutePath(), "Unsupported combat sound format: " + file.getName());
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

   private static File resolve(String relativePath) {
      File[] candidates = new File[]{
         new File("hitsound and killsounds", relativePath),
         new File(XaClient.mc.runDirectory, "hitsound and killsounds/" + relativePath),
         new File(XaClient.mc.runDirectory.getParentFile() == null ? XaClient.mc.runDirectory : XaClient.mc.runDirectory.getParentFile(), "hitsound and killsounds/" + relativePath)
      };

      for (File candidate : candidates) {
         if (candidate.exists()) {
            return candidate;
         }
      }

      return candidates[0];
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

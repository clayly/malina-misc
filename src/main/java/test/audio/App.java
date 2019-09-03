package test.audio;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"ConstantConditions", "UnnecessaryLocalVariable", "SpellCheckingInspection", "WeakerAccess"})
public class App {

    static public final boolean LOG = true;
    static public final boolean LOOPBACK_TEST = true;
    static public final boolean FORMAT_TEST = false;

    static private final long CHECK_PERIOD = 5000;
    static private final int READ_FACTOR = 4000;
    static private final int READ_SLICE = 30;
    static private final int BITS_PER_BYTE = 8;

    static private final AudioFormat.Encoding[] ENCODING = new AudioFormat.Encoding[]{
            AudioFormat.Encoding.ALAW,
            AudioFormat.Encoding.PCM_FLOAT,
            AudioFormat.Encoding.PCM_SIGNED,
            AudioFormat.Encoding.PCM_UNSIGNED,
            AudioFormat.Encoding.ULAW};
    static private final float[] SAMPLE_RATE = new float[]{8000, 16000, 32000};
    static private final int[] SAMPLE_SIZE = new int[]{8, 16, 32};
    static private final int[] CHANNELS = new int[]{1, 2};
    static private final boolean[] BIG_ENDIAN = new boolean[]{true, false};

    static private void log(String format, Object... args) {
        if (LOG) System.out.println(String.format(Locale.US, format, args));
    }

    static private void print(String format, Object... args) {
        System.out.println(String.format(Locale.US, format, args));
    }

    static private void print(int num, Mixer.Info info) {
        System.out.println(String.format(Locale.US,
                "====== MIXER INFO num: %02d ====== \ndescr: <%s> \nname: <%s> \nvend: <%s> \nver: <%s>",
                num,
                info.getDescription(),
                info.getName(),
                info.getVendor(),
                info.getVersion()
        ));
    }

    static public void main(String[] args) {
        AudioFormat systemFormat = systemFormat();
        AudioFormat mediumFormat = mediumFormat();
        AudioFormat linphoneFormat = linphoneFormat();
        log("systemFormat: " + systemFormat);
        log("mediumFormat: " + mediumFormat);
        log("linphoneFormat: " + linphoneFormat);
        log("isConversionSupported system to medium: " +
                AudioSystem.isConversionSupported(systemFormat, mediumFormat));
        log("isConversionSupported medium to linphone: " +
                AudioSystem.isConversionSupported(mediumFormat, linphoneFormat));
        if (FORMAT_TEST) {
            formatTest();
        }
        if (LOOPBACK_TEST) {
            loopbackTest();
        }
    }

    private static void loopBack(TargetDataLine dst, AudioFormat dstFormat, SourceDataLine src, AudioFormat srcFormat) {
        if (dst == null || src == null)
            return;
        int frameSize = dstFormat.getFrameSize();
        int toRead = frameSize * READ_FACTOR;
        dst.addLineListener(event -> log("dst: " + event.toString()));
        src.addLineListener(event -> log("src: " + event.toString()));
        try {
//            AudioInputStream dstStr = new AudioInputStream(dst);
//            AudioInputStream srcStr = AudioSystem.getAudioInputStream(AudioFormat.Encoding.ULAW, dstStr);
//            System.out.println("converted frameLength: " + srcStr.getFrameLength() + " format: " + srcStr.getFormat());
            dst.open(dstFormat);
            src.open(srcFormat);
            dst.start();
            src.start();
            long startTs = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTs < CHECK_PERIOD) {
                byte[] readData = new byte[toRead];
                boolean isRead = false;
                int readCnt = dst.read(readData, 0, toRead);
                if (readCnt == 0)
                    continue;
                for (byte datum : readData) {
                    if (datum != 0) {
                        isRead = true;
                        break;
                    }
                }
                if (!isRead)
                    continue;
                src.write(readData, 0, toRead);
                byte[] readSlice = new byte[READ_SLICE];
                System.arraycopy(readData, 0, readSlice, 0, readSlice.length);
                log("read and written: " + readData.length + " slice: " + Arrays.toString(readSlice));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dst.close();
            src.close();
        }
    }

    static private void loopbackTest() {
        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        for (int i = 0; i < mixerInfo.length; ++i) {
            Mixer.Info info = mixerInfo[i];
            log("");
            print(i, info);
            Mixer mixer = AudioSystem.getMixer(info);
            Line.Info[] srcLinesInfo = mixer.getSourceLineInfo();
            log("srcLinesInfo cnt: " + srcLinesInfo.length);
            for (int i1 = 0; i1 < srcLinesInfo.length; i1++)
                log("srcLineInfo num: %02d srcLineInfo: <%s>", i1, srcLinesInfo[i1]);
            Line.Info[] dstLinesInfo = mixer.getTargetLineInfo();
            log("dstLinesInfo cnt: " + dstLinesInfo.length);
            for (int i1 = 0; i1 < dstLinesInfo.length; i1++)
                log("dstLineInfo num: %02d dstLineInfo: <%s>", i1, dstLinesInfo[i1]);
            TargetDataLine dst = null;
            SourceDataLine src = null;
            AudioFormat dstFormat = linphoneFormat();
            AudioFormat srcFormat = linphoneFormat();
            try {
                dst = AudioSystem.getTargetDataLine(dstFormat, info);
                log("dstDataLine OK " + dst.getLineInfo());
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                src = AudioSystem.getSourceDataLine(srcFormat, info);
                log("srcDataLine OK " + src.getLineInfo());
            } catch (Exception e) {
                e.printStackTrace();
            }
            loopBack(dst, dstFormat, src, srcFormat);
            log("");
        }
    }

    static private void formatTest() {
        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        for (int i = 0; i < mixerInfo.length; ++i) {
            Mixer.Info info = mixerInfo[i];
            print(i, info);
            for (AudioFormat format : allFormats()) {
                log("check format: " + format);
                boolean isDstOk = false;
                try {
                    TargetDataLine dst = AudioSystem.getTargetDataLine(format, info);
                    log("OK dstDataLine getTargetDataLine lineInfo: " + dst.getLineInfo());
                    dst.addLineListener(event -> log("dstDataLine event: " + event));
                    dst.open(format);
                    log("OK dstDataLine open");
                    isDstOk = true;
                    dst.close();
                } catch (Exception e) {
                    log("FAIL dstDataLine exception: " + e.getMessage());
                }
                boolean isSrcOk = false;
                try {
                    SourceDataLine src = AudioSystem.getSourceDataLine(format, info);
                    log("OK srcDataLine getSourceDataLine lineInfo: " + src.getLineInfo());
                    src.addLineListener(event -> log("srcDataLine event: " + event));
                    src.open(format);
                    log("OK srcDataLine open");
                    isSrcOk = true;
                    src.close();
                } catch (Exception e) {
                    log("FAIL srcDataLine exception: " + e.getMessage());
                }
                if (!isDstOk && !isSrcOk)
                    continue;
                String dstStatus = isDstOk ? "OK" : "BAD";
                String srcStatus = isSrcOk ? "OK" : "BAD";
                print("dst " + dstStatus + " src " + srcStatus + " format: " + format);
            }
        }
    }

    static private List<AudioFormat> allFormats() {
        List<AudioFormat> allFormats = new ArrayList<>();
        for (AudioFormat.Encoding en : ENCODING) {
            for (float sr : SAMPLE_RATE) {
                for (int ss : SAMPLE_SIZE) {
                    for (int c : CHANNELS) {
                        for (boolean b : BIG_ENDIAN) {
                            try {
                                allFormats.add(constructFormat(en, sr, ss, c, b));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return allFormats;
    }

    static private AudioFormat constructFormat(AudioFormat.Encoding encoding, float sampleRate, int sampleSize, int channels, boolean isBigEndian) {
        float frameRate = sampleRate;
        int frameSize = (sampleSize * channels) / BITS_PER_BYTE;
        return new AudioFormat(
                encoding,
                sampleRate,
                sampleSize,
                channels,
                frameSize,
                frameRate,
                isBigEndian);
    }

    static private AudioFormat systemFormat() {
        float sampleRate = 32000.0F;
        float frameRate = sampleRate;
        int sampleSize = 16;
        int channels = 2;
        int frameSize = (sampleSize * channels) / BITS_PER_BYTE;
        boolean isBigEndian = false;
        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                sampleSize,
                channels,
                frameSize,
                frameRate,
                isBigEndian);
    }

    static private AudioFormat mediumFormat() {
        float sampleRate = 32000.0F;
        float frameRate = sampleRate;
        int sampleSize = 8;
        int channels = 2;
        int frameSize = (sampleSize * channels) / BITS_PER_BYTE;
        boolean isBigEndian = false;
        return new AudioFormat(
                AudioFormat.Encoding.ULAW,
                sampleRate,
                sampleSize,
                channels,
                frameSize,
                frameRate,
                isBigEndian);
    }

    static private AudioFormat linphoneFormat() {
        float sampleRate = 8000.0F;
        float frameRate = sampleRate;
        int sampleSize = 8;
        int channels = 1;
        int frameSize = (sampleSize * channels) / BITS_PER_BYTE;
        boolean isBigEndian = false;
        return new AudioFormat(
                AudioFormat.Encoding.ULAW,
                sampleRate,
                sampleSize,
                channels,
                frameSize,
                frameRate,
                isBigEndian);
    }

}

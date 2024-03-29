package test.audio;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"UnnecessaryLocalVariable", "SpellCheckingInspection", "SameParameterValue", "ManualMinMaxCalculation"})
public final class App {

    static private final String PROPERTY_LOG             = "LOG";
    static private final String PROPERTY_LOOPBACK_TEST   = "LOOPBACK_TEST";
    static private final String PROPERTY_LOOPBACK_PERIOD = "LOOPBACK_PERIOD";
    static private final String PROPERTY_FORMAT_TEST     = "FORMAT_TEST";

    static private boolean LOG             = false;
    static private boolean LOOPBACK_TEST   = false;
    static private boolean FORMAT_TEST     = false;
    static private long    LOOPBACK_PERIOD = 5000;

    static private final int READ_FACTOR   = 4000;
    static private final int READ_SLICE    = 30;
    static private final int BITS_PER_BYTE = 8;

    static private final AudioFormat.Encoding[] ENCODING = new AudioFormat.Encoding[]{
            AudioFormat.Encoding.ALAW,
            AudioFormat.Encoding.PCM_FLOAT,
            AudioFormat.Encoding.PCM_SIGNED,
            AudioFormat.Encoding.PCM_UNSIGNED,
            AudioFormat.Encoding.ULAW};

    static private final float  [] SAMPLE_RATE = new float  []{8000, 16000, 32000};
    static private final int    [] SAMPLE_SIZE = new int    []{8, 16, 32};
    static private final int    [] CHANNELS    = new int    []{1, 2};
    static private final boolean[] BIG_ENDIAN  = new boolean[]{true, false};

    static public void main(String[] args) {
        setup();
        trash();
        if (FORMAT_TEST)
            formatTest();
        if (LOOPBACK_TEST)
            loopbackTest();
    }

    static private void setup() {
        print("====== SETUP ======");
        LOG             = parseProperty(PROPERTY_LOG            , false);
        FORMAT_TEST     = parseProperty(PROPERTY_FORMAT_TEST    , false);
        LOOPBACK_TEST   = parseProperty(PROPERTY_LOOPBACK_TEST  , false);
        LOOPBACK_PERIOD = parseProperty(PROPERTY_LOOPBACK_PERIOD, 5000, 500, 50000);
        print("%s:%b\n%s:%b\n%s:%b\n%s:%d\n",
                PROPERTY_LOG, LOG,
                PROPERTY_FORMAT_TEST, FORMAT_TEST,
                PROPERTY_LOOPBACK_TEST, LOOPBACK_TEST,
                PROPERTY_LOOPBACK_PERIOD, LOOPBACK_PERIOD
        );
    }

    static private void trash() {
        AudioFormat system = systemFormat();
        AudioFormat medium = mediumFormat();
        AudioFormat linphone = linphoneFormat();
        log("system: " + system);
        log("medium: " + medium);
        log("linphone: " + linphone);
        log("isConversable system to medium: " + AudioSystem.isConversionSupported(system, medium));
        log("isConversable medium to linphone: " + AudioSystem.isConversionSupported(medium, linphone));
    }

    static private void loopBack(TargetDataLine dst, AudioFormat dstFormat, SourceDataLine src, AudioFormat srcFormat) {
        if (dst == null || src == null)
            return;
        int frameSize = dstFormat.getFrameSize();
        int toRead = frameSize * READ_FACTOR;
        dst.addLineListener(event -> print("dst: " + event));
        src.addLineListener(event -> print("src: " + event));
        try {
            dst.open(dstFormat);
            src.open(srcFormat);
            dst.start();
            src.start();
            long startTs = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTs < LOOPBACK_PERIOD) {
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
                print("read and written: " + readData.length + " slice: " + Arrays.toString(readSlice));
            }
        } catch (Exception e) {
            print(e.getMessage());
        } finally {
            dst.close();
            src.close();
        }
    }

    static private void loopbackTest() {
        printChapter("LOOPBACK TEST");
        print("format: " + linphoneFormat() + "\n");
        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        for (int i = 0; i < mixerInfo.length; ++i) {
            Mixer.Info info = mixerInfo[i];
            print(i, info);
            Mixer mixer = AudioSystem.getMixer(info);
            Line.Info[] srcLinesInfo = mixer.getSourceLineInfo();
            print("srcLinesInfo cnt: " + srcLinesInfo.length);
            for (int i1 = 0; i1 < srcLinesInfo.length; i1++)
                print("srcLineInfo num: %02d srcLineInfo: <%s>", i1, srcLinesInfo[i1]);
            Line.Info[] dstLinesInfo = mixer.getTargetLineInfo();
            print("dstLinesInfo cnt: " + dstLinesInfo.length);
            for (int i1 = 0; i1 < dstLinesInfo.length; i1++)
                print("dstLineInfo num: %02d dstLineInfo: <%s>", i1, dstLinesInfo[i1]);
            TargetDataLine dst = null;
            SourceDataLine src = null;
            AudioFormat dstFormat = linphoneFormat();
            AudioFormat srcFormat = linphoneFormat();
            try {
                dst = AudioSystem.getTargetDataLine(dstFormat, info);
                print("dstDataLine OK " + dst.getLineInfo());
            } catch (Exception e) {
                print(e.getMessage());
            }
            try {
                src = AudioSystem.getSourceDataLine(srcFormat, info);
                print("srcDataLine OK " + src.getLineInfo());
            } catch (Exception e) {
                print(e.getMessage());
            }
            loopBack(dst, dstFormat, src, srcFormat);
            print("");
        }
    }

    static private void formatTest() {
        printChapter("FORMAT TEST");
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
            print("");
        }
    }

    static private List<AudioFormat> allFormats() {
        List<AudioFormat> allFormats = new ArrayList<>();
        for (AudioFormat.Encoding en : ENCODING)
            for (float sr : SAMPLE_RATE)
                for (int ss : SAMPLE_SIZE)
                    for (int c : CHANNELS)
                        for (boolean b : BIG_ENDIAN)
                            try {
                                allFormats.add(constructFormat(en, sr, ss, c, b));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
        return allFormats;
    }

    static private AudioFormat systemFormat() {
        return constructFormat(AudioFormat.Encoding.PCM_SIGNED, 32000.0F, 16, 2, false);
    }

    static private AudioFormat mediumFormat() {
        return constructFormat(AudioFormat.Encoding.ULAW, 32000.0F, 8, 2, false);
    }

    static private AudioFormat linphoneFormat() {
        return constructFormat(AudioFormat.Encoding.ULAW, 8000.0F, 8, 1, false);
    }

    static private AudioFormat constructFormat(
            AudioFormat.Encoding encoding,
            float sampleRate,
            int sampleSize,
            int channels,
            boolean isBigEndian) {
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

    static private void conversion(TargetDataLine dst) {
        AudioInputStream dstStr = new AudioInputStream(dst);
        AudioInputStream srcStr = AudioSystem.getAudioInputStream(AudioFormat.Encoding.ULAW, dstStr);
        System.out.println("converted frameLength: " + srcStr.getFrameLength() + " format: " + srcStr.getFormat());
    }

    static private boolean parseProperty(String name, boolean defaultValue) {
        try {
            if (System.getProperty(name) == null)
                return defaultValue;
            else
                return true;
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    static private long parseProperty(String name, long defaultValue, long minValue, long maxValue) {
        try {
            String property = System.getProperty(name);
            if (property == null) {
                return defaultValue;
            } else {
                long value = Long.parseLong(property);
                if (value < minValue)
                    return minValue;
                else if (value > maxValue)
                    return maxValue;
                else
                    return value;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    static private void print(String msg, Object... args) {
        System.out.println(String.format(Locale.US, msg == null ? "<null>" : msg, args));
    }

    static private void log(String msg, Object... args) {
        if (LOG) print(msg, args);
    }

    static private void printChapter(String name) {
        print("");
        print("=======================================");
        print("============ " + name);
        print("=======================================");
        print("");
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

}

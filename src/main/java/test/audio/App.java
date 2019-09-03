package test.audio;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"ConstantConditions", "UnnecessaryLocalVariable", "SpellCheckingInspection"})
public class App {

    public static void main(String[] args) {
        testFormats();
//        AudioFormat systemFormat = systemFormat();
//        AudioFormat mediumFormat = mediumFormat();
//        AudioFormat linphoneFormat = linphoneFormat();
//        System.out.println("systemFormat: " + systemFormat);
//        System.out.println("mediumFormat: " + mediumFormat);
//        System.out.println("linphoneFormat: " + linphoneFormat);
//        System.out.println("isConversionSupported system to medium: " +
//                AudioSystem.isConversionSupported(systemFormat, mediumFormat));
//        System.out.println("isConversionSupported medium to linphone: " +
//                AudioSystem.isConversionSupported(mediumFormat, linphoneFormat));
//        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
//        for (int i = 0; i < mixerInfo.length; ++i) {
//            Mixer.Info info = mixerInfo[i];
//            System.out.println();
//            System.out.println(String.format(Locale.US,
//                    "====== MIXER INFO num: %02d ====== \ndescr: <%s> \nname: <%s> \nvend: <%s> \nver: <%s>",
//                    i,
//                    info.getDescription(),
//                    info.getName(),
//                    info.getVendor(),
//                    info.getVersion()
//            ));
//            Mixer mixer = AudioSystem.getMixer(info);
//            Line.Info[] srcLinesInfo = mixer.getSourceLineInfo();
//            System.out.println("srcLinesInfo cnt: " + srcLinesInfo.length);
//            for (int i1 = 0; i1 < srcLinesInfo.length; i1++) {
//                System.out.println(String.format(Locale.US,
//                        "srcLineInfo num: %02d srcLineInfo: <%s>",
//                        i1, srcLinesInfo[i1].toString()
//                ));
//            }
//            Line.Info[] dstLinesInfo = mixer.getTargetLineInfo();
//            System.out.println("dstLinesInfo cnt: " + dstLinesInfo.length);
//            for (int i1 = 0; i1 < dstLinesInfo.length; i1++) {
//                System.out.println(String.format(Locale.US,
//                        "dstLineInfo num: %02d dstLineInfo: <%s>",
//                        i1, dstLinesInfo[i1].toString()
//                ));
//            }
//            TargetDataLine dst = null;
//            SourceDataLine src = null;
//            AudioFormat dstFormat = linphoneFormat();
//            AudioFormat srcFormat = linphoneFormat();
//            try {
//                dst = AudioSystem.getTargetDataLine(dstFormat, info);
//                System.out.println("dstDataLine OK " + dst.getLineInfo());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            try {
//                src = AudioSystem.getSourceDataLine(srcFormat, info);
//                System.out.println("srcDataLine OK " + src.getLineInfo());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            loopBack(dst, dstFormat, src, srcFormat);
//            System.out.println("\n\n");
//        }
    }

    static private final long CHECK_PERIOD = 5000;
    static private final int READ_FACTOR = 4000;
    static private final int READ_SLICE = 30;
    static private final int BITS_PER_BYTE = 8;

    private static void loopBack(TargetDataLine dst, AudioFormat dstFormat, SourceDataLine src, AudioFormat srcFormat) {
        if (dst == null || src == null)
            return;
        int frameSize = dstFormat.getFrameSize();
        int toRead = frameSize * READ_FACTOR;
        dst.addLineListener(event -> System.out.println("dst: " + event.toString()));
        src.addLineListener(event -> System.out.println("src: " + event.toString()));
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
                System.out.println("read and written: " + readData.length + " slice: " + Arrays.toString(readSlice));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dst.close();
            src.close();
        }
    }

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

    static private void testFormats() {
        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        for (int i = 0; i < mixerInfo.length; ++i) {
            Mixer.Info info = mixerInfo[i];
            System.out.println(String.format(Locale.US,
                    "====== MIXER INFO num: %02d ====== \ndescr: <%s> \nname: <%s> \nvend: <%s> \nver: <%s>",
                    i,
                    info.getDescription(),
                    info.getName(),
                    info.getVendor(),
                    info.getVersion()
            ));
            for (AudioFormat format : allFormats()) {
//                System.out.println("check format: " + format);
                boolean isDstOk = false;
                try {
                    TargetDataLine dst = AudioSystem.getTargetDataLine(format, info);
//                    System.out.println("OK dstDataLine getTargetDataLine lineInfo: " + dst.getLineInfo());
//                    dst.addLineListener(event -> System.out.println("dstDataLine event: " + event));
                    dst.open(format);
//                    System.out.println("OK dstDataLine open");
                    isDstOk = true;
                    dst.close();
                } catch (Exception e) {
//                    System.out.println("FAIL dstDataLine exception: " + e.getMessage());
                }
                boolean isSrcOk = false;
                try {
                    SourceDataLine src = AudioSystem.getSourceDataLine(format, info);
//                    System.out.println("OK srcDataLine getSourceDataLine lineInfo: " + src.getLineInfo());
//                    src.addLineListener(event -> System.out.println("srcDataLine event: " + event));
                    src.open(format);
//                    System.out.println("OK srcDataLine open");
                    isSrcOk = true;
                    src.close();
                } catch (Exception e) {
//                    System.out.println("FAIL srcDataLine exception: " + e.getMessage());
                }
                if (!isDstOk && !isSrcOk)
                    continue;
                System.out.println("format: " + format);
                if (isDstOk)
                    System.out.println("OK dst");
                if (isSrcOk)
                    System.out.println("OK src");
                System.out.println();
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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.AsyncTask;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;

public class Main {
    private static final String DEFAULT_SAVE_PATH = "/sdcard/image.jpg";
    private static final int SCALE = 2;
    private static final Object obj = new Object();
    private static CountDownLatch latch;
    private static Paint paint = new Paint();
    private static int[] x;
    private static int[] y;
    private static int size;

    public static void main(String... args) {
        try {
            long startTime = System.currentTimeMillis();
            execute(args);
            System.out.println("exec time:" + (System.currentTimeMillis() - startTime));
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        System.exit(0);
    }

    private static void execute(String... args) {
        long start = System.currentTimeMillis();
        Options options = new Options();

        Option sourceOption = new Option("source", "sourceImage", true, "source image");
        sourceOption.setRequired(true);
        options.addOption(sourceOption);

        Option sourceStartPointOption = new Option("ssp", "sourceStartPoint", true, "source image start point");
        sourceStartPointOption.setRequired(false);
        sourceStartPointOption.setArgs(2);
        options.addOption(sourceStartPointOption);

        Option destOption = new Option("dest", "destImage", true, "destination image");
        destOption.setRequired(true);
        options.addOption(destOption);

        Option destStartPointOption = new Option("dsp", "destStartPoint", true, "destination image start point");
        destStartPointOption.setRequired(false);
        destStartPointOption.setArgs(2);
        options.addOption(destStartPointOption);

        Option sizeOption = new Option("size", "size", true, "image size");
        sizeOption.setRequired(false);
        sizeOption.setArgs(2);
        options.addOption(sizeOption);

        Option thresholdOption = new Option("t", "threshold", true, "pixel threshold");
        thresholdOption.setRequired(false);
        options.addOption(thresholdOption);

        Option minPercentOption = new Option("minp", "minPercent", true, "min percent");
        minPercentOption.setRequired(false);
        options.addOption(minPercentOption);

        Option maxPercentOption = new Option("maxp", "maxPercent", true, "max percent");
        maxPercentOption.setRequired(false);
        options.addOption(maxPercentOption);

        Option resultPathOption = new Option("r", "resultImage", true, "final result image path");
        resultPathOption.setRequired(false);
        options.addOption(resultPathOption);

        Option invertOption = new Option("invert", "invert", false, "invert");
        invertOption.setRequired(false);
        options.addOption(invertOption);

        CommandLineParser parser = new DefaultParser();

        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("ImageDiff", options);
            System.exit(1);
            return;
        }

        String sourcePath = cmd.getOptionValue("sourceImage");
        String[] sourceStartPoints = cmd.getOptionValues("sourceStartPoint");
        int sourceStartX = 0;
        int sourceStartY = 0;
        if (sourceStartPoints != null && sourceStartPoints.length > 0) {
            sourceStartX = Integer.parseInt(sourceStartPoints[0]);
            sourceStartY = Integer.parseInt(sourceStartPoints[1]);
        }
        String destPath = cmd.getOptionValue("destImage");
        String[] destStartPoints = cmd.getOptionValues("destStartPoint");
        int destStartX = 0;
        int destStartY = 0;
        if (destStartPoints != null && destStartPoints.length > 0) {
            destStartX = Integer.parseInt(destStartPoints[0]);
            destStartY = Integer.parseInt(destStartPoints[1]);
            if (sourceStartX == 0 && sourceStartY == 0) {
                sourceStartY = destStartY;
                sourceStartX = destStartX;
            }
        } else if (sourceStartX != 0 && sourceStartY != 0) {
            destStartX = sourceStartX;
            destStartY = sourceStartY;
        }
        String resultPath = cmd.getOptionValue("resultImage");
        if (resultPath == null || resultPath.isEmpty()) {
            resultPath = DEFAULT_SAVE_PATH;
        }
        String[] sizeArray = cmd.getOptionValues("size");
        int resultWidth = 0;
        int resultHeight = 0;
        if (sizeArray != null && sizeArray.length > 0) {
            resultWidth = Integer.parseInt(sizeArray[0]) / SCALE;
            resultHeight = Integer.parseInt(sizeArray[1]) / SCALE;
        }
        int threshold = Integer.parseInt(cmd.getOptionValue("threshold"));
        int minPercentValue = Integer.parseInt(cmd.getOptionValue("minPercent"));
        int maxPercentValue = Integer.parseInt(cmd.getOptionValue("maxPercent"));
        boolean invert = cmd.hasOption("invert");

        System.out.println("parsed cli arguments:" + String.valueOf(System.currentTimeMillis() - start));

        long bitmapLoadTime = System.currentTimeMillis();

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = SCALE;
        bitmapOptions.inMutable = false;
        Bitmap sourceBitmap = BitmapFactory.decodeFile(sourcePath, bitmapOptions);
        if (SCALE != 1) {
            sourceStartX /= SCALE;
            sourceStartY /= SCALE;
            destStartX /= SCALE;
            destStartY /= SCALE;
        }

        if (resultWidth == 0 || resultHeight == 0) {
            resultWidth = sourceBitmap.getWidth() - sourceStartX;
            resultHeight = sourceBitmap.getHeight() - sourceStartY;
        }

        if (sourceStartX != 0 || sourceStartY != 0 || resultWidth != 0 || resultHeight != 0) {
            Bitmap tempBitmap = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(tempBitmap);
            Matrix matrix = new Matrix();
            matrix.setTranslate(-sourceStartX, -sourceStartY);
            canvas.drawBitmap(sourceBitmap, matrix, paint);
            sourceBitmap.recycle();
            sourceBitmap = tempBitmap;
        }
        Bitmap destBitmap = BitmapFactory.decodeFile(destPath, bitmapOptions);
        if (destStartX != 0 || destStartY != 0 || resultWidth != 0 || resultHeight != 0) {
            Bitmap tempBitmap = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(tempBitmap);
            Matrix matrix = new Matrix();
            matrix.setTranslate(-destStartX, -destStartY);
            canvas.drawBitmap(destBitmap, matrix, paint);
            destBitmap.recycle();
            destBitmap = tempBitmap;
        }
        Bitmap diffBitmap = destBitmap.copy(Bitmap.Config.ARGB_8888, true);

        System.out.println("bitmap load:" + String.valueOf(System.currentTimeMillis() - bitmapLoadTime));

        long firstFor = System.currentTimeMillis();

        int bitmapSize = sourceBitmap.getWidth() * sourceBitmap.getHeight();
        x = new int[bitmapSize];
        y = new int[bitmapSize];
        size = 0;
        findDifference(sourceBitmap, destBitmap, threshold);

        System.out.println("first for" + String.valueOf(System.currentTimeMillis() - firstFor));

        double percent = ((double) size / x.length) * 100;
        System.out.println("percent:" + percent);
        if ((!invert && (percent < minPercentValue || percent > maxPercentValue)) ||
                invert && (minPercentValue < percent && percent < maxPercentValue)) {
            long pixelShift = System.currentTimeMillis();
            int rawBytes = diffBitmap.getRowBytes();
            ByteBuffer diffBuffer = ByteBuffer.allocateDirect(rawBytes * diffBitmap.getHeight()).order(ByteOrder.nativeOrder());
            diffBitmap.copyPixelsToBuffer(diffBuffer);
            for (int i = 0; i < x.length; ++i) {
                if (x[i] > 1) {
                    x[i] -= 2;
                }
                diffBuffer.put(rawBytes * y[i] + x[i] * 4, (byte) 0);
                diffBuffer.put(rawBytes * y[i] + x[i] * 4 + 1, (byte) 255);
                diffBuffer.put(rawBytes * y[i] + x[i] * 4 + 2, (byte) 0);
                diffBuffer.put(rawBytes * y[i] + x[i] * 4 + 3, (byte) 255);
            }
            diffBuffer.rewind();
            diffBitmap.copyPixelsFromBuffer(diffBuffer);
            System.out.println("pixel shift and draw" + String.valueOf(System.currentTimeMillis() - pixelShift));

            long save = System.currentTimeMillis();

            Bitmap finalImage = Bitmap.createBitmap(sourceBitmap.getWidth() * 3, sourceBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(finalImage);
            Paint paint = new Paint();
            paint.setFilterBitmap(true);
            paint.setAntiAlias(true);
            canvas.drawBitmap(sourceBitmap, 0, 0, paint);
            canvas.drawBitmap(diffBitmap, sourceBitmap.getWidth(), 0, paint);
            canvas.drawBitmap(destBitmap, sourceBitmap.getWidth() * 2, 0, paint);
            File file = new File(resultPath);
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(file);
                finalImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("save time" + String.valueOf(System.currentTimeMillis() - save));
        }
        System.out.println("finish");
    }

    private static void findDifference(Bitmap firstImage, Bitmap secondImage, int threshold) {
        int rawBytes = firstImage.getRowBytes();
        ByteBuffer byteBuffer1 = ByteBuffer.allocateDirect(rawBytes * firstImage.getHeight()).order(ByteOrder.nativeOrder());
        ByteBuffer byteBuffer2 = ByteBuffer.allocateDirect(rawBytes * secondImage.getHeight()).order(ByteOrder.nativeOrder());
        firstImage.copyPixelsToBuffer(byteBuffer1);
        secondImage.copyPixelsToBuffer(byteBuffer2);

        int width = firstImage.getWidth();
        int count = Math.max(2, Math.min(Runtime.getRuntime().availableProcessors() - 1, 4));
        int size = width / count;
        if (width % count != 0) {
            count += 1;
        }
        latch = new CountDownLatch(count);
        Runnable runnable;
        int chunk = 0;
        int[] iterations = new int[count + 1];
        for (int i = 0; i < count + 1; i++) {
            iterations[i] = chunk;
            chunk += size;
            if (chunk > width) {
                chunk = width;
            }
        }
        for (int i = 0; i < count; i++) {
            int finalI = i;
            runnable = new Runnable() {
                @Override
                public void run() {
                    runDiff(iterations[finalI], iterations[finalI + 1], firstImage.getHeight(), byteBuffer1, byteBuffer2, rawBytes, threshold);
                    latch.countDown();
                }
            };
            AsyncTask.THREAD_POOL_EXECUTOR.execute(runnable);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void runDiff(int stat, int end, int height, ByteBuffer byteBuffer1, ByteBuffer byteBuffer2, int rawBytes, int threshold) {
        for (int i = stat; i < end; i++) {
            for (int j = 0; j < height; j++) {

                int A = byteBuffer1.get(rawBytes * j + i * 4);
                int R = byteBuffer1.get(rawBytes * j + i * 4 + 1);
                int G = byteBuffer1.get(rawBytes * j + i * 4 + 2);
                int B = byteBuffer1.get(rawBytes * j + i * 4 + 3);

                int A1 = byteBuffer2.get(rawBytes * j + i * 4);
                int R1 = byteBuffer2.get(rawBytes * j + i * 4 + 1);
                int G1 = byteBuffer2.get(rawBytes * j + i * 4 + 2);
                int B1 = byteBuffer2.get(rawBytes * j + i * 4 + 3);

                if ((A > A1 ? A - A1 : A1 - A + R > R1 ? R - R1 : R1 - R + G > G1 ? G - G1 : G1 - G + B > B1 ? B - B1 : B1 - B) > threshold) {
                    synchronized (obj) {
                        x[size] = i;
                        y[size] = j;
                        ++size;
                    }
                }
            }
        }
    }
}

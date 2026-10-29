package com.liskovsoft.smartyoutubetv2.tv.ui.main;

import android.content.Context;
import android.os.Build;

import com.liskovsoft.smartyoutubetv2.tv.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

/** Small, offline crash recorder. It deliberately does not collect system-wide logcat data. */
public final class PiTubeCrashReporter {
    private static final String REPORT = "pitube-last-crash.txt";
    private static final String PENDING = "pitube-crash-pending";
    private static final String SAFE_MODE = "pitube-safe-start";
    private static final int MAX_REPORT_CHARACTERS = 64 * 1024;
    private static final ArrayDeque<String> BREADCRUMBS = new ArrayDeque<>();
    private static boolean sInstalled;

    private PiTubeCrashReporter() {}

    public static synchronized void install(Context context) {
        if (sInstalled) {
            return;
        }
        final Context appContext = context.getApplicationContext() != null
                ? context.getApplicationContext() : context;
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                saveCrash(appContext, thread, throwable);
            } catch (Throwable ignored) {
                // Never let the diagnostic writer prevent normal Android crash handling.
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            } else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        });
        sInstalled = true;
        note("Crash recorder installed");
    }

    public static synchronized void note(String event) {
        if (BREADCRUMBS.size() >= 32) {
            BREADCRUMBS.removeFirst();
        }
        BREADCRUMBS.addLast(System.currentTimeMillis() + "  " + event);
    }

    private static void saveCrash(Context context, Thread thread, Throwable error) throws Exception {
        StringWriter trace = new StringWriter(8192);
        error.printStackTrace(new PrintWriter(trace));

        StringBuilder report = new StringBuilder(10000);
        report.append("PiTube crash report\n")
                .append("Time: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
                        .format(new Date())).append('\n')
                .append("Version: ").append(BuildConfig.VERSION_NAME).append(" (")
                .append(BuildConfig.VERSION_CODE).append(")\n")
                .append("Package: ").append(BuildConfig.APPLICATION_ID).append('\n')
                .append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
                .append("Android: ").append(Build.VERSION.RELEASE).append(" / API ")
                .append(Build.VERSION.SDK_INT).append('\n')
                .append("Thread: ").append(thread.getName()).append("\n\n")
                .append("Last app events:\n");
        synchronized (PiTubeCrashReporter.class) {
            for (String event : BREADCRUMBS) {
                report.append(event).append('\n');
            }
        }
        report.append("\nFatal exception:\n").append(trace);

        String text = report.length() > MAX_REPORT_CHARACTERS
                ? report.substring(0, MAX_REPORT_CHARACTERS) + "\n[report truncated]"
                : report.toString();
        File reportFile = new File(context.getFilesDir(), REPORT);
        File tempFile = new File(context.getFilesDir(), REPORT + ".tmp");
        try (FileOutputStream output = new FileOutputStream(tempFile)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
        if (!tempFile.renameTo(reportFile)) {
            try (FileOutputStream output = new FileOutputStream(reportFile)) {
                output.write(text.getBytes(StandardCharsets.UTF_8));
            }
        }
        // Mark the report for display on the next launch only after writing it.
        new File(context.getFilesDir(), PENDING).createNewFile();
    }

    public static boolean shouldShowRecovery(Context context) {
        return hasPendingReport(context) || isSafeMode(context);
    }

    public static boolean hasPendingReport(Context context) {
        File directory = context.getFilesDir();
        return new File(directory, PENDING).exists() && new File(directory, REPORT).exists();
    }

    public static boolean isSafeMode(Context context) {
        return new File(context.getFilesDir(), SAFE_MODE).exists();
    }

    public static void setSafeMode(Context context, boolean enabled) {
        File marker = new File(context.getFilesDir(), SAFE_MODE);
        if (enabled) {
            try {
                marker.createNewFile();
            } catch (Exception ignored) {
                // The app can still attempt a normal launch.
            }
        } else {
            marker.delete();
        }
    }

    public static void acknowledgeReport(Context context) {
        new File(context.getFilesDir(), PENDING).delete();
    }

    public static String readReport(Context context) {
        File report = new File(context.getFilesDir(), REPORT);
        if (!report.isFile()) {
            return "No previous crash report is available.";
        }
        try (java.io.FileInputStream input = new java.io.FileInputStream(report)) {
            byte[] bytes = new byte[(int) Math.min(report.length(), MAX_REPORT_CHARACTERS + 256)];
            int offset = 0;
            while (offset < bytes.length) {
                int count = input.read(bytes, offset, bytes.length - offset);
                if (count < 0) break;
                offset += count;
            }
            return new String(bytes, 0, offset, StandardCharsets.UTF_8);
        } catch (Exception error) {
            return "Unable to read the saved crash report: " + error;
        }
    }
}

package io.github.datromtool.cli.progressbar;

import io.github.datromtool.ByteUnit;
import io.github.datromtool.io.FileScanner;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.Ansi;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import static io.github.datromtool.cli.util.TerminalUtils.*;
import static java.lang.Math.toIntExact;
import static java.lang.String.format;
import static org.fusesource.jansi.Ansi.ansi;

@Slf4j
public final class CommandLineScannerProgressBar implements FileScanner.Listener {

    private static final String THREAD_FORMAT = "Thread %3d |%3d%%|%6.2f %2s/s|: %s";
    private static final long FRAME_TIME_MILLIS = Math.round(1_000L / 60.0d);
    private static final String NEW_LINE = System.getProperty("line.separator");

    private int numThreads;
    private int totalItems;
    private LongAdder totalBytesRead;
    private Terminal terminal;
    private PrintWriter writer;
    private AtomicInteger current;
    private String mainBarPrint;

    private LineData[] threadLineData;

    @Data
    private final static class LineData {
        private final Path path;
        private final long totalBytes;
        private final long startInstant = System.nanoTime();
        private volatile String itemName;
        private volatile long totalBytesRead;
        private volatile long lastUpdateBytesRead;
        private volatile long lastUpdateTime = startInstant;
        private volatile int lastUpdatedPercentage;
        private volatile int lastUpdatedColumns;
        private volatile long endInstant;

        public String getItemName() {
            String s = itemName;
            if (s == null) {
                s = path.toString();
                itemName = s;
            }
            return s;
        }
    }

    private double getAverage(LineData lineData) {
        return lineData.getTotalBytesRead() / secondsBetween(lineData.getStartInstant(), lineData.getEndInstant());
    }

    private synchronized void printMainBar(int curr) {
        int size = Math.max(0, availableColumns(mainBarPrint, terminal));
        int x = size * curr / totalItems;
        int lineDiff = numThreads + 1;
        writer.print(ansi()
                .reset()
                .cursorDownLine(lineDiff)
                .a(format(mainBarPrint,
                        repeat('=', x == size ? x : x - 1),
                        repeat('>', x == size ? 0 : 1),
                        repeat('.', size - x),
                        curr))
                .eraseLine()
                .cursorUpLine(lineDiff));
    }

    private synchronized void printThread(int thread, String label, @Nullable String override, int percentage, double speed) {
        ByteUnit unit = ByteUnit.getUnit(speed);
        String forPrint = format(
                THREAD_FORMAT,
                thread,
                percentage,
                unit.convert(speed),
                unit.getSymbol(),
                label);
        writer.print(ansi()
                .reset()
                .cursorDownLine(thread)
                .a(forPrint)
                .a(trimAndCache(thread, forPrint, override))
                .eraseLine()
                .cursorUpLine(thread));
    }

    private String trimAndCache(int thread, String forPrint, @Nullable String override) {
        LineData lineData = threadLineData[thread - 1];
        int availableColumns = availableColumns(forPrint, terminal);
        if (override != null) {
            String newItemName = trimTo(override, availableColumns);
            lineData.setItemName(newItemName);
            lineData.setLastUpdatedColumns(availableColumns);
            return newItemName;
        } else if (lineData.getLastUpdatedColumns() != availableColumns) {
            String newItemName = trimTo(lineData.getPath().toString(), availableColumns);
            lineData.setItemName(newItemName);
            lineData.setLastUpdatedColumns(availableColumns);
            return newItemName;
        } else {
            return lineData.getItemName();
        }
    }

    private boolean shouldPrint(LineData lineData, double percentage) {
        return lineData.getLastUpdatedPercentage() != percentage
                && (System.nanoTime() - lineData.getLastUpdateTime()) / 1_000_000L > FRAME_TIME_MILLIS;
    }

    @Override
    public synchronized void init(int numThreads) {
        try {
            this.terminal = TerminalBuilder.terminal();
            this.writer = terminal.writer();
        } catch (IOException e) {
            log.error("Error while creating terminal", e);
            this.terminal = null;
            this.writer = new PrintWriter(System.err);
        }
        writer.print(ansi()
                .a("Scanning input directories...")
                .eraseLine()
                .a(NEW_LINE)
                .eraseLine());
        this.numThreads = numThreads;
        this.threadLineData = new LineData[numThreads];
        this.totalBytesRead = new LongAdder();
        this.current = new AtomicInteger();
        Ansi ansi = ansi();
        ansi.reset();
        ansi.a(NEW_LINE);
        for (int i = 1; i <= numThreads; i++) {
            ansi.a(format(THREAD_FORMAT, i, 0, 0.0d, ByteUnit.BYTE.getSymbol(), "INITIALIZED"));
            ansi.eraseLine();
            ansi.a(NEW_LINE);
        }
        ansi.cursorUpLine(numThreads + 1);
        writer.print(ansi);
    }

    @Override
    public void reportTotalItems(int totalItems) {
        this.totalItems = totalItems;
        int totalItemsLength = (int) Math.floor(Math.log10(totalItems)) + 1;
        this.mainBarPrint = "Scanning [%s%s%s] %" + totalItemsLength + "d/" + totalItems;
        printMainBar(0);
    }

    @Override
    public synchronized void reportStart(int thread, Path path, long bytes) {
        threadLineData[thread - 1] = new LineData(path, bytes);
        printThread(thread, "", path.toString(), 0, 0);
        printMainBar(current.incrementAndGet());
    }

    @Override
    public void reportBytesRead(int thread, long bytes) {
        totalBytesRead.add(bytes);
        LineData lineData = threadLineData[thread - 1];
        long totalBytesRead = lineData.getTotalBytesRead() + bytes;
        lineData.setTotalBytesRead(totalBytesRead);
        int percentage = toIntExact((long) (((double) totalBytesRead / lineData.getTotalBytes()) * 100.0d));
        if (shouldPrint(lineData, percentage)) {
            long currentTime = System.nanoTime();
            double speed = (totalBytesRead - lineData.getLastUpdateBytesRead())
                    / secondsBetween(lineData.getLastUpdateTime(), currentTime);
            printThread(thread, "", null, percentage, speed);
            lineData.setLastUpdateBytesRead(totalBytesRead);
            lineData.setLastUpdatedPercentage(percentage);
            lineData.setLastUpdateTime(currentTime);
        }
    }

    @Override
    public void reportSkip(int thread, Path path, String message) {
        LineData lineData = threadLineData[thread - 1];
        lineData.setEndInstant(System.nanoTime());
        printThread(thread, "SKIPPED: ", path.toString(), 0, getAverage(lineData));
    }

    @Override
    public void reportFailure(int thread, Path path, String message, Throwable cause) {
        LineData lineData = threadLineData[thread - 1];
        lineData.setEndInstant(System.nanoTime());
        printThread(thread, "FAILED: ", path.toString(), 0, getAverage(lineData));
    }

    @Override
    public void reportFinish(int thread, Path path) {
        LineData lineData = threadLineData[thread - 1];
        lineData.setEndInstant(System.nanoTime());
        printThread(thread, "FINISHED: ", path.toString(), 100, getAverage(lineData));
    }

    @Override
    public synchronized void reportAllFinished() {
        for (int i = 1; i <= numThreads; i++) {
            printThread(i, "FINISHED", "", 100, getAverage(threadLineData[i - 1]));
        }
        printMainBar(totalItems);
        writer.print(ansi()
                .reset()
                .cursorDownLine(numThreads + 1)
                .a(NEW_LINE)
                .a(NEW_LINE));
        try {
            if (terminal != null) {
                terminal.close();
            }
        } catch (IOException e) {
            log.error("Error while closing terminal", e);
        }
    }

    private double secondsBetween(long start, long finish) {
        return (finish - start) / 1E9d;
    }
}

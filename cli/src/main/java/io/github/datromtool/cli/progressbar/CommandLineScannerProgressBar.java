package io.github.datromtool.cli.progressbar;

import io.github.datromtool.ByteUnit;
import io.github.datromtool.io.FileScanner;
import lombok.extern.slf4j.Slf4j;
import org.fusesource.jansi.Ansi;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

import static io.github.datromtool.cli.util.TerminalUtils.*;
import static org.fusesource.jansi.Ansi.ansi;

// FIXME: this is very slow
@Slf4j
public final class CommandLineScannerProgressBar implements FileScanner.Listener {

    private int numThreads;
    private int totalItems;
    private int totalItemsLength;
    private AtomicLongArray averageReportedSpeed;
    private Terminal terminal;
    private PrintWriter writer;
    private final AtomicInteger current = new AtomicInteger();

    private long getAverage(int thread) {
        return averageReportedSpeed.get(thread - 1);
    }

    private void printMainBar(int curr) {
        String forPrint = "Scanning [%s%s%s] "
                + String.format("%" + totalItemsLength + "d", curr)
                + "/"
                + totalItems;
        int size = Math.max(0, availableColumns(forPrint, terminal));
        int x = size * curr / totalItems;
        int lineDiff = numThreads + 1;
        writer.print(ansi().reset()
                .cursorDownLine(lineDiff)
                .a(String.format(
                        forPrint,
                        repeat('=', x == size ? x : x - 1),
                        repeat('>', x == size ? 0 : 1),
                        repeat('.', size - x)))
                .eraseLine()
                .cursorUpLine(lineDiff));
    }

    private void printThread(int thread, String label, String item, int percentage, long speed) {
        ByteUnit unit = ByteUnit.getUnit(speed);
        String forPrint = String.format(
                "Thread %d (%3d%% %6.2f %2s/s): %s",
                thread,
                percentage,
                unit.convert(speed),
                unit.getSymbol(),
                label);
        writer.print(ansi().reset()
                .cursorDownLine(thread)
                .a(forPrint)
                .a(trimTo(item, availableColumns(forPrint, terminal)))
                .eraseLine()
                .cursorUpLine(thread));
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
        this.numThreads = numThreads;
        this.averageReportedSpeed = new AtomicLongArray(numThreads);
        Ansi ansi = ansi().reset().newline();
        for (int i = 1; i <= numThreads; i++) {
            ansi.a("Thread " + i + " (  0%   0.00  B/s): INITIALIZED").newline();
        }
        writer.print(ansi.cursorUpLine(numThreads + 1));
    }

    @Override
    public synchronized void reportTotalItems(int totalItems) {
        this.totalItems = totalItems;
        this.totalItemsLength = (int) Math.floor(Math.log10(totalItems)) + 1;
        printMainBar(0);
    }

    @Override
    public synchronized void reportStart(Path path, int thread) {
        printThread(thread, "", path.toString(), 0, 0);
        printMainBar(current.incrementAndGet());
    }

    @Override
    public synchronized void reportProgress(Path path, int thread, int percentage, long speed) {
        averageReportedSpeed.updateAndGet(thread - 1, v -> Math.round((v + speed) / 2.0));
        printThread(thread, "", path.toString(), percentage, speed);
    }

    @Override
    public synchronized void reportSkip(Path path, int thread, String message) {
        printThread(thread, "SKIPPED: ", path.toString(), 0, getAverage(thread));
    }

    @Override
    public synchronized void reportFailure(Path path, int thread, String message, Throwable cause) {
        printThread(thread, "FAILED: ", path.toString(), 0, getAverage(thread));
    }

    @Override
    public synchronized void reportFinish(Path path, int thread) {
        printThread(thread, "FINISHED: ", path.toString(), 100, getAverage(thread));
    }

    @Override
    public synchronized void reportAllFinished() {
        for (int i = 1; i <= numThreads; i++) {
            printThread(i, "FINISHED", "", 100, getAverage(i));
        }
        printMainBar(totalItems);
        writer.print(ansi().reset()
                .cursorDownLine(numThreads + 1)
                .newline()
                .newline());
        try {
            if (terminal != null) {
                terminal.close();
            }
        } catch (IOException e) {
            log.error("Error while closing terminal", e);
        }
    }
}

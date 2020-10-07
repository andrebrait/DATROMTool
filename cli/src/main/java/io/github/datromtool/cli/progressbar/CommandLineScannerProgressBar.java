package io.github.datromtool.cli.progressbar;

import com.google.common.base.Strings;
import io.github.datromtool.ByteUnit;
import io.github.datromtool.io.FileScanner;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.datromtool.cli.util.TerminalUtils.availableColumns;
import static io.github.datromtool.cli.util.TerminalUtils.repeat;
import static io.github.datromtool.cli.util.TerminalUtils.trimTo;

public final class CommandLineScannerProgressBar implements FileScanner.Listener {

    private int numThreads;
    private int totalItems;
    private int totalItemsLength;
    private final AtomicInteger current = new AtomicInteger();
    private final ThreadLocal<Long> lastReportedSpeed = ThreadLocal.withInitial(() -> 0L);

    private void printMainBar(int curr) {
        String forPrint = "Scanning [%s%s] "
                + Strings.padStart(Integer.toString(curr), totalItemsLength, ' ')
                + "/"
                + totalItems
                + "\u001b[K"
                + "\r";
        int size = Math.max(0, Math.min(totalItems, availableColumns(forPrint, null) + 4));
        int x = size * curr / totalItems;
        System.err.printf(forPrint, repeat('#', x), repeat('.', (size - x)));
    }

    private void printThread(int thread, String label, String item, int percentage, long speed) {
        ByteUnit unit = ByteUnit.getUnit(speed);
        String speedStr = String.format("%.2f", unit.convert(speed)) + unit.getSymbol() + "/s";
        String forPrint = "Thread "
                + thread
                + " ("
                + percentage
                + "%|"
                + speedStr
                + "): "
                + label;
        int diff = numThreads - thread + 1;
        String s = "\r"
                + "\u001b[" + diff + "A"
                + forPrint
                + trimTo(item, availableColumns(forPrint, null))
                + "\u001b[K"
                + "\r"
                + "\u001b[" + diff + "B"
                + "\r";
        System.err.print(s);
    }

    @Override
    public synchronized void init(int numThreads) {
        this.numThreads = numThreads;
        for (int i = 1; i <= numThreads; i++) {
            System.err.println("Threads " + i + " (0%|0.00B/s): INITIALIZED");
        }
    }

    @Override
    public synchronized void reportTotalItems(int totalItems) {
        this.totalItems = totalItems;
        this.totalItemsLength = (int) Math.floor(Math.log10(totalItems)) + 1;
        printMainBar(0);
    }

    @Override
    public synchronized void reportStart(Path path, int thread) {
        printMainBar(current.incrementAndGet());
    }

    @Override
    public synchronized void reportProgress(Path path, int thread, int percentage, long speed) {
        lastReportedSpeed.set(speed);
        printThread(thread, "", path.toString(), percentage, speed);
    }

    @Override
    public synchronized void reportSkip(Path path, int thread, String message) {
        printThread(thread, "SKIPPED", path.toString(), 0, lastReportedSpeed.get());
    }

    @Override
    public synchronized void reportFailure(Path path, int thread, String message, Throwable cause) {
        printThread(thread, "FAILED", path.toString(), 0, lastReportedSpeed.get());
    }

    @Override
    public synchronized void reportFinish(Path path, int thread) {
        printThread(thread, "FINISHED", "", 100, lastReportedSpeed.get());
    }

    @Override
    public synchronized void reportAllFinished() {
        printMainBar(totalItems);
        System.err.println();
        System.err.println();
    }
}

package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.datromtool.cli.converter.CopyBufferSizeConverter;
import io.github.datromtool.cli.converter.CopyThreadsConverter;
import io.github.datromtool.cli.converter.ScanBufferSizeConverter;
import io.github.datromtool.cli.converter.ScanMaxBufferSizeConverter;
import io.github.datromtool.cli.converter.ScanThreadsConverter;
import io.github.datromtool.config.AppConfig;
import io.github.datromtool.config.CopyBufferSize;
import io.github.datromtool.config.CopyThreads;
import io.github.datromtool.config.ScanBufferSize;
import io.github.datromtool.config.ScanMaxBufferSize;
import io.github.datromtool.config.ScanThreads;
import lombok.Data;
import lombok.NoArgsConstructor;
import picocli.CommandLine;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;

@Data
@NoArgsConstructor
@JsonInclude(NON_DEFAULT)
public class PerformanceOptions {

    private ScanThreads scanThreads;
    private ScanBufferSize scanBufferSize;
    private ScanMaxBufferSize scanBufferMaxSize;

    private CopyThreads copyThreads;
    private CopyBufferSize copyBufferSize;
    private Boolean allowRawZipCopy;

    @CommandLine.Option(
            names = "--scan-threads",
            paramLabel = "THREADS",
            converter = ScanThreadsConverter.class,
            description = "Number of threads to use for scanning files. Defaults to half the number of CPUs.")
    public void setScanThreads(ScanThreads scanThreads) {
        this.scanThreads = scanThreads;
    }

    @CommandLine.Option(
            names = "--scan-buffer",
            paramLabel = "BYTES",
            converter = ScanBufferSizeConverter.class,
            description = "Default size for the dynamic I/O buffer used for scanning files (per thread). Defaults to 32KB.")
    public void setScanBufferSize(ScanBufferSize scanBufferSize) {
        this.scanBufferSize = scanBufferSize;
    }

    @CommandLine.Option(
            names = "--scan-max-buffer",
            paramLabel = "BYTES",
            converter = ScanMaxBufferSizeConverter.class,
            description = "Maximum size for the dynamic I/O buffer used for scanning files (per thread). Defaults to 256MB.")
    public void setScanBufferMaxSize(ScanMaxBufferSize scanBufferMaxSize) {
        this.scanBufferMaxSize = scanBufferMaxSize;
    }

    @CommandLine.Option(
            names = "--copy-threads",
            paramLabel = "THREADS",
            converter = CopyThreadsConverter.class,
            description = "Number of threads to use for copying files. Defaults to half the number of CPUs.")
    public void setCopyThreads(CopyThreads copyThreads) {
        this.copyThreads = copyThreads;
    }

    @CommandLine.Option(
            names = "--copy-buffer",
            paramLabel = "BYTES",
            converter = CopyBufferSizeConverter.class,
            description = "Size for the I/O buffer used for copying files (per thread). Defaults to 32KB.")
    public void setCopyBufferSize(CopyBufferSize copyBufferSize) {
        this.copyBufferSize = copyBufferSize;
    }

    @CommandLine.Option(
            names = "--copy-raw-zip",
            description = "Allow raw copies when copying from/to ZIP file. \n" +
                    "Improves performance, but it disables the renaming of files inside the generated ZIP files.")
    public void setAllowRawZipCopy(Boolean allowRawZipCopy) {
        this.allowRawZipCopy = allowRawZipCopy;
    }

    public AppConfig.FileScannerConfig merge(AppConfig.FileScannerConfig original) {
        if (scanThreads != null
                || scanBufferSize != null
                || scanBufferMaxSize != null) {
            AppConfig.FileScannerConfig.FileScannerConfigBuilder builder = original.toBuilder();
            if (scanThreads != null) {
                builder.threads(scanThreads);
            }
            if (scanBufferSize != null) {
                builder.defaultBufferSize(scanBufferSize);
            }
            if (scanBufferMaxSize != null) {
                builder.maxBufferSize(scanBufferMaxSize);
            }
            return builder.build();
        }
        return original;
    }

    public AppConfig.FileCopierConfig merge(AppConfig.FileCopierConfig original) {
        if (copyThreads != null
                || copyBufferSize != null
                || allowRawZipCopy != null) {
            AppConfig.FileCopierConfig.FileCopierConfigBuilder builder = original.toBuilder();
            if (copyThreads != null) {
                builder.threads(copyThreads);
            }
            if (copyBufferSize != null) {
                builder.bufferSize(copyBufferSize);
            }
            if (allowRawZipCopy != null) {
                builder.allowRawZipCopy(allowRawZipCopy);
            }
            return builder.build();
        }
        return original;
    }
}

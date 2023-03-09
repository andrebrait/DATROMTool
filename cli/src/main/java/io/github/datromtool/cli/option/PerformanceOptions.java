package io.github.datromtool.cli.option;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.datromtool.ByteSize;
import io.github.datromtool.cli.converter.ExecutableFileConverter;
import io.github.datromtool.config.AppConfig;
import lombok.*;
import picocli.CommandLine;

import java.nio.file.Path;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.lang.Math.toIntExact;
import static java.lang.String.format;
import static lombok.AccessLevel.NONE;

@Data
@NoArgsConstructor
@JsonInclude(NON_DEFAULT)
public class PerformanceOptions {

    private static final ByteSize MAX_BUFFER_SIZE = ByteSize.fromBytes(Integer.MAX_VALUE);

    @CommandLine.Spec
    @ToString.Exclude
    @JsonIgnore
    @Getter(NONE)
    @Setter(NONE)
    private CommandLine.Model.CommandSpec spec;

    private Integer scanThreads;
    private ByteSize scanBufferSize;
    private ByteSize scanBufferMaxSize;

    private Integer copyThreads;
    private ByteSize copyBufferSize;
    private boolean allowRawZipCopy;
    private Path customUnrarPath;
    private Path customSevenZipPath;

    @CommandLine.Option(
            names = "--scan-threads",
            paramLabel = "THREADS",
            description = "Number of threads to use for scanning files. Defaults to half the number of CPUs.")
    public void setScanThreads(Integer scanThreads) {
        validateThreads(scanThreads);
        this.scanThreads = scanThreads;
    }

    @CommandLine.Option(
            names = "--scan-buffer",
            paramLabel = "BYTES",
            description = "Default size for the dynamic I/O buffer used for scanning files (per thread). Defaults to 32KB.")
    public void setScanBufferSize(ByteSize scanBufferSize) {
        validateBufferSize(scanBufferSize);
        this.scanBufferSize = scanBufferSize;
    }

    @CommandLine.Option(
            names = "--scan-max-buffer",
            paramLabel = "BYTES",
            description = "Maximum size for the dynamic I/O buffer used for scanning files (per thread). Defaults to 256MB.")
    public void setScanBufferMaxSize(ByteSize scanBufferMaxSize) {
        validateBufferSize(scanBufferMaxSize);
        this.scanBufferMaxSize = scanBufferMaxSize;
    }

    @CommandLine.Option(
            names = "--copy-threads",
            paramLabel = "THREADS",
            description = "Number of threads to use for copying files. Defaults to half the number of CPUs.")
    public void setCopyThreads(Integer copyThreads) {
        validateThreads(copyThreads);
        this.copyThreads = copyThreads;
    }

    @CommandLine.Option(
            names = "--copy-buffer",
            paramLabel = "BYTES",
            description = "Size for the I/O buffer used for copying files (per thread). Defaults to 32KB.")
    public void setCopyBufferSize(ByteSize copyBufferSize) {
        validateBufferSize(copyBufferSize);
        this.copyBufferSize = copyBufferSize;
    }

    @CommandLine.Option(
            names = "--copy-raw-zip",
            description = "Allow raw copies when copying from/to ZIP file. \n" +
                    "Improves performance, but it disables the renaming of files inside the generated ZIP files.")
    public void setAllowRawZipCopy(boolean allowRawZipCopy) {
        this.allowRawZipCopy = allowRawZipCopy;
    }


    @CommandLine.Option(
            names = "--unrar-exec",
            paramLabel = "PATH",
            description = "Custom path to the UnRAR executable for RAR5 extraction",
            converter = ExecutableFileConverter.class)
    public void setCustomUnrarPath(Path customUnrarPath) {
        this.customUnrarPath = customUnrarPath;
    }

    @CommandLine.Option(
            names = "--7z-exec",
            paramLabel = "PATH",
            description = "Custom path to the 7-Zip CLI executable for RAR5 extraction",
            converter = ExecutableFileConverter.class)
    public void setCustomSevenZipPath(Path customSevenZipPath) {
        this.customSevenZipPath = customSevenZipPath;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(NON_NULL)
    public static final class ForceOptions {

        @CommandLine.Option(
                names = "--force-unrar",
                description = "Force RAR5 extraction to use UnRAR")
        private boolean forceUnrar;

        @CommandLine.Option(
                names = "--force-7z",
                description = "Force RAR5 extraction to use 7-Zip")
        private boolean forceSevenZip;
    }

    @CommandLine.ArgGroup
    private ForceOptions forceOptions;

    private void validateThreads(Integer threads) {
        if (threads <= 0) {
            throw new CommandLine.ParameterException(spec.commandLine(), "Number of threads should be a positive number");
        }
    }

    private void validateBufferSize(ByteSize scanBufferMaxSize) {
        if (scanBufferMaxSize.compareTo(MAX_BUFFER_SIZE) > 0) {
            throw new CommandLine.ParameterException(spec.commandLine(), format("Maximum byte size is %d bytes", Integer.MAX_VALUE));
        }
    }

    public AppConfig.FileScannerConfig merge(AppConfig.FileScannerConfig original) {
        if (scanThreads != null
                || scanBufferSize != null
                || scanBufferMaxSize != null
                || customUnrarPath != null
                || customSevenZipPath != null
                || forceOptions != null) {
            AppConfig.FileScannerConfig.FileScannerConfigBuilder builder = original.toBuilder();
            if (scanThreads != null) {
                builder.threads(scanThreads);
            }
            if (scanBufferSize != null) {
                builder.defaultBufferSize(toIntExact(scanBufferSize.getSizeInBytes()));
            }
            if (scanBufferMaxSize != null) {
                builder.maxBufferSize(toIntExact(scanBufferMaxSize.getSizeInBytes()));
            }
            if (customUnrarPath != null) {
                builder.customUnrarPath(customUnrarPath);
            }
            if (customSevenZipPath != null) {
                builder.customSevenZipPath(customSevenZipPath);
            }
            if (forceOptions != null) {
                builder.forceUnrar(forceOptions.isForceUnrar());
                builder.forceSevenZip(forceOptions.isForceSevenZip());
            }
            return builder.build();
        }
        return original;
    }

    public AppConfig.FileCopierConfig merge(AppConfig.FileCopierConfig original) {
        if (copyThreads != null
                || copyBufferSize != null
                || customUnrarPath != null
                || customSevenZipPath != null
                || allowRawZipCopy
                || forceOptions != null) {
            AppConfig.FileCopierConfig.FileCopierConfigBuilder builder = original.toBuilder();
            if (copyThreads != null) {
                builder.threads(scanThreads);
            }
            if (copyBufferSize != null) {
                builder.bufferSize(toIntExact(copyBufferSize.getSizeInBytes()));
            }
            if (customUnrarPath != null) {
                builder.customUnrarPath(customUnrarPath);
            }
            if (customSevenZipPath != null) {
                builder.customSevenZipPath(customSevenZipPath);
            }
            builder.allowRawZipCopy(allowRawZipCopy);
            builder.forceUnrar(forceOptions.isForceUnrar());
            builder.forceSevenZip(forceOptions.isForceSevenZip());
            return builder.build();
        }
        return original;
    }
}

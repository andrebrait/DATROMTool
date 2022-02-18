package io.github.datromtool;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

import static lombok.AccessLevel.PRIVATE;

/**
 * Inspired by https://github.com/trustin/os-maven-plugin/blob/master/src/main/java/kr/motd/maven/os/Detector.java
 */
@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class SystemUtils {

    public static final OperatingSystem OPERATING_SYSTEM = detectOs();
    public static final Architecture ARCHITECTURE = detectArchitecture();

    public enum OperatingSystem {
        UNKNOWN,
        LINUX,
        OSX,
        BSD,
        WINDOWS;

        public static OperatingSystem parse(String value) {
            String normalized = normalize(value);
            if (normalized.startsWith("linux")) {
                return OperatingSystem.LINUX;
            }
            if (normalized.startsWith("macosx") || normalized.startsWith("osx")) {
                return OperatingSystem.OSX;
            }
            if (normalized.startsWith("freebsd") || normalized.startsWith("openbsd") || normalized.startsWith("netbsd")) {
                return OperatingSystem.BSD;
            }
            if (normalized.startsWith("windows")) {
                return OperatingSystem.WINDOWS;
            }
            log.warn("Unknown operating system: '{}'", value);
            return OperatingSystem.UNKNOWN;
        }
    }

    public enum Architecture {
        UNKNOWN,
        X86_32,
        X86_64,
        ARM_32,
        ARM_64,
        POWER_PC_32,
        POWER_PC_64;

        public static Architecture parse(String value) {
            String normalized = normalize(value);
            if (normalized.matches("^(x8664|amd64|ia32e|em64t|x64)$")) {
                return Architecture.X86_64;
            }
            if (normalized.matches("^(x8632|x86|i[3-6]86|ia32|x32)$")) {
                return Architecture.X86_32;
            }
            if (normalized.matches("^(arm|arm32|armhf|armv7l|armv7)$")) {
                return Architecture.ARM_32;
            }
            if (normalized.matches("^(arm64|aarch64|armv8l|armv8|armv9l|armv9)$")) {
                return Architecture.ARM_64;
            }
            if (normalized.matches("^(ppc|ppc32)$")) {
                return Architecture.POWER_PC_32;
            }
            if ("ppc64".equals(normalized)) {
                return Architecture.POWER_PC_64;
            }
            log.warn("Unknown architecture: '{}'", value);
            return Architecture.UNKNOWN;
        }
    }

    private static OperatingSystem detectOs() {
        OperatingSystem parse = OperatingSystem.parse(System.getProperty("os.name"));
        log.info("Detected OS: {}", parse);
        return parse;
    }

    private static Architecture detectArchitecture() {
        Architecture parse = Architecture.parse(System.getProperty("os.arch"));
        log.info("Detected Architecture: {}", parse);
        return parse;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "");
    }
}

package io.github.datromtool.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusManager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #30: a deprecated {@code converterClass} attribute on a {@code <conversionRule>} makes
 * logback emit WARN statuses at bootstrap; with no status listener registered, logback then
 * auto-dumps its entire status history to {@link System#out}, corrupting any command whose
 * output is redirected (e.g. {@code 1g1r --dump-profile > file}). Booting the packaged
 * {@code defaults.xml} into a fresh, isolated {@link LoggerContext} must produce zero
 * WARN-or-above statuses.
 */
class LogbackDefaultsStatusTest {

    // A minimal wrapper reproducing exactly how cli/src/main/resources/logback.xml consumes this
    // file in production: a real classpath <include>, not a hand-parsed fragment.
    private static final String WRAPPER_CONFIG =
            "<configuration>"
                    + "<include resource=\"io/github/datromtool/logging/defaults.xml\"/>"
                    + "<root level=\"INFO\"/>"
                    + "</configuration>";

    @Test
    void bootingPackagedDefaultsProducesNoWarnOrAboveStatus() throws Exception {
        LoggerContext context = new LoggerContext();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        try (InputStream input = new ByteArrayInputStream(WRAPPER_CONFIG.getBytes(StandardCharsets.UTF_8))) {
            configurator.doConfigure(input);
        }

        StatusManager statusManager = context.getStatusManager();
        List<Status> offending = statusManager.getCopyOfStatusList().stream()
                .filter(status -> status.getLevel() >= Status.WARN)
                .toList();

        assertTrue(
                offending.isEmpty(),
                "booting defaults.xml must produce zero WARN/ERROR statuses (any WARN/ERROR status "
                        + "makes logback auto-dump its entire status history to stdout with no listener "
                        + "registered, corrupting redirected output such as --dump-profile > file), got:\n"
                        + offending.stream().map(Status::toString).collect(Collectors.joining("\n")));
    }
}

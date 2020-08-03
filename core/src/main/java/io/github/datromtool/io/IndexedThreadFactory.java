package io.github.datromtool.io;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class IndexedThreadFactory implements ThreadFactory {

    private final Logger logger;
    private final String namePrefix;
    private final AtomicInteger indexCounter = new AtomicInteger(1);

    @Override
    public Thread newThread(@Nonnull Runnable r) {
        IndexedThread thread = new IndexedThread(indexCounter.getAndIncrement(), r);
        thread.setDaemon(true);
        thread.setName(namePrefix + "-" + thread.getIndex());
        thread.setUncaughtExceptionHandler((t, e) ->
                logger.error("Unexpected exception thrown", e));
        return thread;
    }
}

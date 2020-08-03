package io.github.datromtool.io;

import lombok.Getter;

@Getter
final class IndexedThread extends Thread {

    private final int index;

    IndexedThread(int index, Runnable target) {
        super(target);
        this.index = index;
    }

}

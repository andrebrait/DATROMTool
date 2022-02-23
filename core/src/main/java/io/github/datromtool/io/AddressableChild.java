package io.github.datromtool.io;

public interface AddressableChild {

    Addressable getParent();

    String getRelativeName();
}

package io.github.datromtool.logging.ansi;

/**
 * An ANSI encodable element.
 *
 * @author Phillip Webb
 */
public interface AnsiElement {

    /**
     * @return the ANSI escape code
     */
    @Override
    String toString();

}
package io.siggi.http.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class PageOutputStream extends OutputStream {

    private final OutputStream out;
    private final boolean closeInnerStreamOnClose;
    private final Writer headerWriter;
    private final Writer footerWriter;
    private boolean wroteHeader = false;
    private boolean closed = false;

    public PageOutputStream(OutputStream out, boolean closeInnerStreamOnClose, Writer headerWriter, Writer footerWriter) {
        this.out = out;
        this.closeInnerStreamOnClose = closeInnerStreamOnClose;
        this.headerWriter = headerWriter;
        this.footerWriter = footerWriter;
    }

    private void check() throws IOException {
        if (closed) throw new IOException("Stream already closed.");
        if (!wroteHeader) {
            wroteHeader = true;
            if (headerWriter != null) {
                headerWriter.writeTo(this);
            }
        }
    }

    @Override
    public void write(int b) throws IOException {
        check();
        out.write(b);
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        check();
        out.write(buffer);
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        check();
        out.write(buffer, offset, length);
    }

    public void write(String text) throws IOException {
        write(text.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void flush() throws IOException {
        check();
        out.flush();
    }

    @Override
    public void close() throws IOException {
        if (closed) return;
        check();
        closed = true;
        if (footerWriter != null) {
            footerWriter.writeTo(this);
        }
        if (closeInnerStreamOnClose) out.close();
    }

    @FunctionalInterface
    public interface Writer {
        void writeTo(PageOutputStream out) throws IOException;
    }
}

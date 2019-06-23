package com.example.mobilemic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.PushSourceStream;
import javax.media.protocol.SourceTransferHandler;

public class ByteSourceStream implements PushSourceStream {
    private SourceTransferHandler transferHandler;
    private ContentDescriptor contentDescriptor = new ContentDescriptor(ContentDescriptor.RAW_RTP);
    protected InputStream stream;
    private boolean eosReached = false;
    public ByteSourceStream(InputStream stream, ContentDescriptor contentDescriptor)
    {
        this.stream = stream;
        this.contentDescriptor = contentDescriptor;
    }
    @Override
    public int read(byte[] bytes, int i, int i1) throws IOException {
        int bytesRead = stream.read(bytes, i, i1);
        System.out.println("read bytes");
        if (bytesRead == -1)
            eosReached = true;
        return bytesRead;
    }

    @Override
    public int getMinimumTransferSize() {
        return 0;
    }

    @Override
    public void setTransferHandler(SourceTransferHandler sourceTransferHandler) {
        this.transferHandler = sourceTransferHandler;
    }

    @Override
    public ContentDescriptor getContentDescriptor() {
        return contentDescriptor;
    }

    @Override
    public long getContentLength() {
        return 0;
    }

    @Override
    public boolean endOfStream() {
        if (stream == null)
            return true;
        return eosReached;
    }

    @Override
    public Object[] getControls() {
        return new Object[0];
    }

    @Override
    public Object getControl(String s) {
        return null;
    }
}

package io.teamcode.common;

import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * Created by chiang on 2017. 5. 9..
 */
public class ByteBufferTest {

    @Test
    public void get() {
        int off = 6;
        int len = 6;
        int size = 6;
        System.out.println("--> " + (off | len | (off + len) | (size - (off + len))));

        System.out.println("--> " + (off | len));
        System.out.println("--> " + (6 | 6));
        System.out.println("--> " + (off | len | (off + len)));
        System.out.println("--> " + (size - (off + len)));

        //  (  off | len | (off + len) | (size - (off + len))   )
    }

    @Test
    public void get2() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(50);
        byteBuffer.put("hello\n".getBytes());
        byteBuffer.put("world".getBytes());
        byteBuffer.flip();

        int size = 6;
        int offset = 1;
        byte[] data = new byte[size];
        byteBuffer.get(data, 0, size);
        System.out.print(new String(data));

        byteBuffer.get(data, 0, byteBuffer.remaining());
        System.out.print(new String(data));
        System.out.println(byteBuffer);


        System.out.println("-------------------------");
        byteBuffer.limit(50);
        byteBuffer.put("mad".getBytes());
        byteBuffer.flip();
        byte[] data2 = new byte[byteBuffer.remaining()];
        byteBuffer.get(data2, 0, data2.length);
        System.out.println("put then get: " + new String(data2));

        System.out.println("-------------------------");
        byteBuffer.limit(50);
        byteBuffer.put("max".getBytes());
        byteBuffer.flip();
        byte[] data3 = new byte[byteBuffer.remaining()];
        byteBuffer.get(data3, 0, data3.length);
        System.out.println("put then get: " + new String(data3));
        byte[] slice = new byte[byteBuffer.limit() - 5];
        for (int i = 5, j = 0; i < byteBuffer.limit(); i++, j++) {
            slice[j] = byteBuffer.get(i);
        }
        System.out.println("slice data: " + new String(slice));



        //byteBuffer.get(data);
        //byteBuffer.get(data, offset, size + 1);
        //System.out.println("--> " + new String(data));
        /*System.out.println("buffer: " + byteBuffer + ", remaining: " + byteBuffer.remaining());
        int remaining = byteBuffer.remaining();
        for (int i = 0; i < remaining; i++) {
            byte[] b = new byte[1];
            b[0] = byteBuffer.get();
            //System.out.print("[" + i + "]" + new String(b));
            System.out.print(new String(b));
        }*/
    }
}

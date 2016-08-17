package cn.ymex.cute.socket;

import org.junit.Test;

import java.lang.annotation.Retention;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class UnitTest {
    @Test
    public void test_byteString_append() throws Exception {

        ByteString byteString = ByteString.of("12345".getBytes(ByteString.UTF_8));
        System.out.println(byteString.utf8());
        byteString = byteString.append("6789".getBytes(ByteString.UTF_8));
        System.out.println(byteString.utf8());
    }

    @Test
    public void test_split() {

    }

    public boolean arrayRangeEquals(
            byte[] a, int aOffset, byte[] b, int bOffset, int byteCount) {
        for (int i = 0; i < byteCount; i++) {
            if (a[i + aOffset] != b[i + bOffset]) return false;

        }
        return true;
    }

    private byte[] tobt(String text) {
        return text.getBytes(Charset.forName("utf-8"));
    }

    @Test
    public void print() {
        byte[] rawbyte = tobt("780121238123121111");
        byte[] spit = tobt("78");
        split(rawbyte,spit);

    }


    public List<byte[]> split(byte[] rawbyte, byte[] spit) {
        List<byte[]> byteDatas = new ArrayList<>();
        int currentIndex = 0;
        for (int i = 0; i < rawbyte.length; i++) {
            byte[] sclie = new byte[spit.length];
            if (i > rawbyte.length - spit.length) {
                continue;
            }
            System.arraycopy(rawbyte, i, sclie, 0, sclie.length);
            if (arrayRangeEquals(rawbyte, i, spit, 0, sclie.length)) {
                byte[] temp = new byte[i + sclie.length - currentIndex];
                System.arraycopy(rawbyte, currentIndex, temp, 0, temp.length);
                byteDatas.add(temp);
//                System.out.println(new String(temp));

                i += sclie.length - 1;
                currentIndex = i + 1;
            }
        }
        if (currentIndex < rawbyte.length) {
            byte[] last = new byte[rawbyte.length - currentIndex];

            System.arraycopy(rawbyte, currentIndex, last, 0, last.length);
            byteDatas.add(last);
//            System.out.println(new String(last));
        }
        return byteDatas;
    }
}
package cn.ymex.cute.socket;

import org.junit.Test;

import java.nio.charset.Charset;

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
        byte[] rawbyte = tobt("123886687122883");
        byte[] spit = tobt("88");

        for (int i = 0; i < rawbyte.length; i++) {
            byte[] sclie = new byte[spit.length];
            if (i > rawbyte.length - spit.length) {
                return;
            }
            System.arraycopy(rawbyte, i, sclie, 0, sclie.length);
            if (arrayRangeEquals(rawbyte, i, spit, 0, sclie.length)) {
                System.out.println("----------");
            }
        }

    }

    private void showArray(int[] datas) {
        for (int num : datas) {
            System.out.println(num);
        }
    }
}
package cn.ymex.cute.socket;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Created by ymexc on 2016/8/10.
 */
public class ByteString implements Serializable, Comparable<ByteString> {

    public static final Charset UTF_8 = Charset.forName("UTF-8");
    final byte[] data;
    transient String utf8;
    transient int hashCode; // Lazily computed; 0 if unknown.

    ByteString(byte[] data) {
        this.data = data;
    }


    public static ByteString of(byte... data) {
        if (data == null) throw new IllegalArgumentException("data == null");
        return new ByteString(data.clone());
    }


    public static ByteString of(byte[] data, int offset, int byteCount) {
        if (data == null) throw new IllegalArgumentException("data == null");
        checkOffsetAndCount(data.length, offset, byteCount);

        byte[] copy = new byte[byteCount];
        System.arraycopy(data, offset, copy, 0, byteCount);
        return new ByteString(copy);
    }


    public static ByteString encodeUtf8(String s) {
        if (s == null) throw new IllegalArgumentException("s == null");
        ByteString byteString = new ByteString(s.getBytes(UTF_8));
        byteString.utf8 = s;
        return byteString;
    }

    public String utf8() {
        String result = utf8;
        return result != null ? result : (utf8 = new String(data, UTF_8));
    }

    /**
     * 字串转 utf-8 字节码
     * @param text
     * @return
     */
    public static byte[] utf8(String text) {
        return text !=null? text.getBytes(UTF_8) :null;
    }

    public ByteString substring(int beginIndex) {
        return substring(beginIndex, data.length);
    }

    public ByteString substring(int beginIndex, int endIndex) {
        if (beginIndex < 0) throw new IllegalArgumentException("beginIndex < 0");
        if (endIndex > data.length) {
            throw new IllegalArgumentException("endIndex > length(" + data.length + ")");
        }

        int subLen = endIndex - beginIndex;
        if (subLen < 0) throw new IllegalArgumentException("endIndex < beginIndex");

        if ((beginIndex == 0) && (endIndex == data.length)) {
            return this;
        }

        byte[] copy = new byte[subLen];
        System.arraycopy(data, beginIndex, copy, 0, subLen);
        return new ByteString(copy);
    }


    public final boolean startsWith(ByteString prefix) {
        return rangeEquals(0, prefix, 0, prefix.size());
    }

    public final boolean startsWith(byte[] prefix) {
        return rangeEquals(0, prefix, 0, prefix.length);
    }

    public final boolean endsWith(ByteString prefix) {
        return rangeEquals(size() - prefix.size(), prefix, 0, prefix.size());
    }

    public final boolean endsWith(byte[] prefix) {
        return rangeEquals(size() - prefix.length, prefix, 0, prefix.length);
    }

    public final int indexOf(ByteString other) {
        return indexOf(other.internalArray(), 0);
    }

    public final int indexOf(ByteString other, int fromIndex) {
        return indexOf(other.internalArray(), fromIndex);
    }


    public final int indexOf(byte[] other) {
        return indexOf(other, 0);
    }

    public int indexOf(byte[] other, int fromIndex) {
        fromIndex = Math.max(fromIndex, 0);
        for (int i = fromIndex, limit = data.length - other.length; i <= limit; i++) {
            if (arrayRangeEquals(data, i, other, 0, other.length)) {
                return i;
            }
        }
        return -1;
    }

    public final int lastIndexOf(ByteString other) {
        return lastIndexOf(other.internalArray(), size());
    }

    public final int lastIndexOf(ByteString other, int fromIndex) {
        return lastIndexOf(other.internalArray(), fromIndex);
    }

    public final int lastIndexOf(byte[] other) {
        return lastIndexOf(other, size());
    }

    public int lastIndexOf(byte[] other, int fromIndex) {
        fromIndex = Math.min(fromIndex, data.length - other.length);
        for (int i = fromIndex; i >= 0; i--) {
            if (arrayRangeEquals(data, i, other, 0, other.length)) {
                return i;
            }
        }
        return -1;
    }

    public byte[] toByteArray() {
        return data.clone();
    }

    byte[] internalArray() {
        return data;
    }


    @Override
    public int hashCode() {
        int result = hashCode;
        return result != 0 ? result : (hashCode = Arrays.hashCode(data));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        return o instanceof ByteString
                && ((ByteString) o).size() == data.length
                && ((ByteString) o).rangeEquals(0, data, 0, data.length);
    }


    public boolean rangeEquals(int offset, ByteString other, int otherOffset, int byteCount) {
        return other.rangeEquals(otherOffset, this.data, offset, byteCount);
    }


    public boolean rangeEquals(int offset, byte[] other, int otherOffset, int byteCount) {
        return offset >= 0 && offset <= data.length - byteCount
                && otherOffset >= 0 && otherOffset <= other.length - byteCount
                && arrayRangeEquals(data, offset, other, otherOffset, byteCount);
    }

    public int size() {
        return data.length;
    }

    public byte getByte(int pos) {
        return data[pos];
    }

    /**
     * 追加数据
     *
     * @param other
     * @return
     */
    public ByteString append(byte[] other) {
        return ByteString.of(concat(internalArray(), other));
    }

    /**
     * @param the
     * @param other
     * @return
     */
    public byte[] concat(byte[] the, byte[] other) {
        byte[] newone = new byte[the.length + other.length];
        System.arraycopy(the, 0, newone, 0, the.length);
        System.arraycopy(other, 0, newone, the.length, other.length);
        return newone;
    }

    /**
     * 判断数据是否相等
     *
     * @param a
     * @param aOffset
     * @param b
     * @param bOffset
     * @param byteCount
     * @return
     */
    public boolean arrayRangeEquals(
            byte[] a, int aOffset, byte[] b, int bOffset, int byteCount) {
        for (int i = 0; i < byteCount; i++) {
            if (a[i + aOffset] != b[i + bOffset]) return false;
        }
        return true;
    }

    private ByteString[] split(byte[] split) {
        if (split == null) {
            ByteString[] bses = new ByteString[]{new ByteString(toByteArray())};
            return bses;
        }
        int len = split.length;
        ByteString[] byteStrings = new ByteString[size() % len];
        int ri = 0;//上一次截取的位置
        for (int i = 0; i < byteStrings.length; i++) {
            byte[] slice = new byte[len];
            System.arraycopy(slice, 0, internalArray(), i * len, len);
            if (arrayRangeEquals(slice, 0, split, 0, len)) {
                byteStrings[i] = substring(ri,i*len);
                ri += i*len;
            }
        }

        if (ri == 0) {
           return new ByteString[]{new ByteString(toByteArray())};
        }
        return byteStrings;
    }


    /**
     * 转16进制字符串
     *
     * @param src
     * @return
     */
    public String toHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv + " ");
        }
        return stringBuilder.toString();
    }

    /**
     * is 相等
     *
     * @param byteString
     * @return
     */
    @Override
    public int compareTo(ByteString byteString) {
        int sizeA = size();
        int sizeB = byteString.size();
        for (int i = 0, size = Math.min(sizeA, sizeB); i < size; i++) {
            int byteA = getByte(i) & 0xff;
            int byteB = byteString.getByte(i) & 0xff;
            if (byteA == byteB) continue;
            return byteA < byteB ? -1 : 1;
        }
        if (sizeA == sizeB) return 0;
        return sizeA < sizeB ? -1 : 1;
    }

    public static void checkOffsetAndCount(long size, long offset, long byteCount) {
        if ((offset | byteCount) < 0 || offset > size || size - offset < byteCount) {
            throw new ArrayIndexOutOfBoundsException(
                    String.format("size=%s offset=%s byteCount=%s", size, offset, byteCount));
        }
    }

    public static short reverseBytesShort(short s) {
        int i = s & 0xffff;
        int reversed = (i & 0xff00) >>> 8
                | (i & 0x00ff) << 8;
        return (short) reversed;
    }

    public static int reverseBytesInt(int i) {
        return (i & 0xff000000) >>> 24
                | (i & 0x00ff0000) >>> 8
                | (i & 0x0000ff00) << 8
                | (i & 0x000000ff) << 24;
    }

    public static long reverseBytesLong(long v) {
        return (v & 0xff00000000000000L) >>> 56
                | (v & 0x00ff000000000000L) >>> 40
                | (v & 0x0000ff0000000000L) >>> 24
                | (v & 0x000000ff00000000L) >>> 8
                | (v & 0x00000000ff000000L) << 8
                | (v & 0x0000000000ff0000L) << 24
                | (v & 0x000000000000ff00L) << 40
                | (v & 0x00000000000000ffL) << 56;
    }

    public static void sneakyRethrow(Throwable t) {
        ByteString.<Error>sneakyThrow2(t);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow2(Throwable t) throws T {
        throw (T) t;
    }


}

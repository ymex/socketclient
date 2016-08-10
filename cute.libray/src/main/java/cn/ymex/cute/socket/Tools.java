package cn.ymex.cute.socket;

/**
 * Created by ymexc on 2016/8/10.
 */
public class Tools {

    /**
     * 检查对象是否为空，为空抛出 NullPointerException
     *
     * @param notice
     * @param t
     * @param <T>
     * @return
     */
    public static   <T> T checkNull(T t, String notice) {
        if (null == t) {
            throw new NullPointerException(notice == null ? "" : notice);
        }
        return t;
    }

    /**
     * 检查对象是否为空，为空抛出 NullPointerException
     *
     * @param t
     * @param <T>
     * @return
     */
    public static  <T> T checkNull(T t) {
        return checkNull(t, null);
    }

    /**
     * 判断对象是否为空
     *
     * @param t
     * @param <T>
     * @return
     */
    public static <T> boolean isNull(T t) {
        if (null == t) {
            return true;
        }
        return false;
    }
}

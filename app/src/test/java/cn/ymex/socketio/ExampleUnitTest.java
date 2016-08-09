package cn.ymex.socketio;

import org.junit.Test;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {

    @Test
    public void concat() throws InterruptedException {
        int[] the = {1,2,3,4,6};
        int[] other = {7,8,9};
        int[] newone = new int[the.length+other.length];
        System.arraycopy(the,0,newone,0,the.length);
        System.arraycopy(other,0,newone,the.length,other.length);
        showarray(newone);
    }

    private void showarray(int[] arr) {
        StringBuilder builder = new StringBuilder(arr.length);
        for (int i : arr) {
            builder.append(i).append(",");
        }
        System.out.println(builder.toString());
    }
}
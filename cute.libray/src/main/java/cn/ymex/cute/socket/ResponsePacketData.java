package cn.ymex.cute.socket;

/**
 * Created by ymexc on 2016/8/10.
 */
public class ResponsePacketData {
    private int dataLength = 1024;
    private byte[] resultData;

    public ResponsePacketData() {
    }

    public ResponsePacketData(byte[] resultData) {
        this.resultData = resultData;
    }


    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    public byte[] untieData() {
        if (onUntiePacketDate == null) {
            return this.resultData;
        }
        return onUntiePacketDate.untieData(resultData);
    }

    private Listener.OnUntiePacketDate onUntiePacketDate;

    public void setOnUntiePacketDate(Listener.OnUntiePacketDate onUntiePacketDate) {
        this.onUntiePacketDate = onUntiePacketDate;
    }
}

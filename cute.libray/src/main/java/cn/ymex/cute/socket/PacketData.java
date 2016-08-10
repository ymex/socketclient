package cn.ymex.cute.socket;

/**
 * Created by ymexc on 2016/8/10.
 */
public class PacketData {
    private byte[] rawData;

    public PacketData(byte[] rawData) {
        this.rawData = rawData;
    }

    public byte[] warpData() {
        if (onWarpPacketData == null) {
            return this.rawData;
        }
        return onWarpPacketData.warpData(this.rawData);
    }

    private Listener.OnWarpPacketData onWarpPacketData;

    public void setOnWarpPacketData(Listener.OnWarpPacketData onWarpPacketData) {
        this.onWarpPacketData = onWarpPacketData;
    }
}
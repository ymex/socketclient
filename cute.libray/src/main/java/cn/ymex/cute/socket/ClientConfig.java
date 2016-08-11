package cn.ymex.cute.socket;

/**
 * Created by ymexc on 2016/8/10.
 */
/**
 * socket 配置
 */
public class ClientConfig {
    private String host;
    private int port;
    private PacketData heartbeatPacketData;
    private byte[] heartHead; //心跳包头部
    private byte[] heartTail; //心跳包尾部
    private byte[] packetHead; // 数据包头部
    private byte[] packetTail; // 数据包尾部
    private long heartBeatInterval = 30 * 1000;
    private int soTimeout = 1 * 1000;
    private int allocateBuffer = 512;

    private boolean autoConnectWhenBreak = false;//断开重连服务器
    private boolean autoConnectWhenFailed = false;//连接失败重连服务器
    private long autoConnectdelayMillis =3*1000;//重连延时多久


    public ClientConfig() {

    }

    public ClientConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public int getAllocateBuffer() {
        return allocateBuffer;
    }

    public void setAllocateBuffer(int allocateBuffer) {
        this.allocateBuffer = allocateBuffer;
    }

    public int getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public long getHeartBeatInterval() {
        return heartBeatInterval;
    }

    public void setHeartBeatInterval(long heartBeatInterval) {
        this.heartBeatInterval = heartBeatInterval;
    }

    public PacketData getHeartbeatPacketData() {
        return heartbeatPacketData;
    }

    public void setHeartbeatPacketData(PacketData heartbeatPacketData) {
        this.heartbeatPacketData = heartbeatPacketData;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isAutoConnectWhenBreak() {
        return autoConnectWhenBreak;
    }

    public void setAutoConnectWhenBreak(boolean autoConnectWhenBreak) {
        this.autoConnectWhenBreak = autoConnectWhenBreak;
    }

    public boolean isAutoConnectWhenFailed() {
        return autoConnectWhenFailed;
    }

    public void setAutoConnectWhenFailed(boolean autoConnectWhenFailed) {
        this.autoConnectWhenFailed = autoConnectWhenFailed;
    }

    public long getAutoConnectdelayMillis() {
        return autoConnectdelayMillis;
    }

    public void setAutoConnectdelayMillis(long autoConnectdelayMillis) {
        this.autoConnectdelayMillis = autoConnectdelayMillis;
    }
}
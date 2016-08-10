package cn.ymex.cute.socket;

/**
 * Created by ymexc on 2016/8/10.
 */
public interface Listener {
    /**
     * 状态回调
     */
    interface OnConnectListener {
        void connectPrepare();

        void connectWaiting();

        void connectSuccess();

        void connectBreak();

        void connectFailed();
    }

    /**
     * 拆包回调
     */
    interface OnUntiePacketDate {
        byte[] untieData(byte[] resultData);
    }

    /**
     * 封包回调
     */
    interface OnWarpPacketData {
        byte[] warpData(byte[] rawData);
    }

    /**
     * 数据接收回调
     */
    interface OnReceiveListener {
        /**
         * 接收服务器socket 返回数据，每有数据返回时都会调用。
         *
         * @param result
         */
        void receive(ResponsePacketData result);
    }
}

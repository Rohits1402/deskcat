package com.deskcat.net;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * UDP-multicast transport. Everything — presence, chat, DMs — rides the one
 * multicast group; DMs are filtered by target id on the receiving side.
 * Received messages land in a queue the render thread drains once per frame.
 */
public class LanClient {

    public static final String GROUP = "239.42.10.7";
    public static final int PORT = 42107;
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final String selfId;
    private final Queue<LanMsg> inbox = new ConcurrentLinkedQueue<LanMsg>();
    private MulticastSocket socket;
    private InetAddress group;
    private volatile boolean running;

    public LanClient(String selfId) {
        this.selfId = selfId;
    }

    /** Returns false when the socket can't be opened (no network, port taken). */
    public boolean start() {
        try {
            group = InetAddress.getByName(GROUP);
            socket = new MulticastSocket(PORT);
            socket.setLoopbackMode(false);   // deliver our own datagrams too:
                                             // lets two instances on one PC see each other
            socket.joinGroup(group);
            running = true;
            Thread rx = new Thread(this::recvLoop, "deskcat-lan-rx");
            rx.setDaemon(true);
            rx.start();
            return true;
        } catch (Throwable t) {
            close();
            return false;
        }
    }

    private void recvLoop() {
        byte[] buf = new byte[LanProtocol.MAX_PACKET];
        while (running) {
            try {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                socket.receive(pkt);
                LanMsg m = LanProtocol.decode(
                        new String(pkt.getData(), pkt.getOffset(), pkt.getLength(), UTF8));
                if (m != null && !selfId.equals(m.id)) {
                    inbox.add(m);
                }
            } catch (Throwable t) {
                if (!running) {
                    return;   // socket closed during shutdown
                }
                // transient receive error: keep listening
            }
        }
    }

    /** Thread-safe; called from the render thread and the Swing chat popup. */
    public void send(String encoded) {
        MulticastSocket s = socket;
        if (s == null) {
            return;
        }
        try {
            byte[] data = encoded.getBytes(UTF8);
            s.send(new DatagramPacket(data, data.length, group, PORT));
        } catch (Throwable t) {
            // dropped datagram; the next state tick resyncs peers
        }
    }

    /** Drain into the caller's hands; returns null when empty. */
    public LanMsg poll() {
        return inbox.poll();
    }

    public void close() {
        running = false;
        MulticastSocket s = socket;
        socket = null;
        if (s != null) {
            try {
                byte[] bye = LanProtocol.encodeBye(selfId).getBytes(UTF8);
                s.send(new DatagramPacket(bye, bye.length, group, PORT));
            } catch (Throwable ignored) {
            }
            try {
                s.leaveGroup(group);
            } catch (Throwable ignored) {
            }
            s.close();
        }
    }
}

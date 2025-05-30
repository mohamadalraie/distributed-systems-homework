package Nodes.NodesSync;

public class StartNodes {

    public static void main(String[] args) {
        // العقدة 1 - مجلد node1_files
        new Thread(() -> {
            new NodeServer(9091, "E:\\node1\\").start();
        }).start();
        // العقدة 2 - مجلد node2_files
            new Thread(() -> {
            new NodeServer(9092, "E:\\node2\\").start();
            }).start();

        // العقدة 3 - مجلد node3_files
                new Thread(() -> {
            new NodeServer(9093, "E:\\node3\\").start();
                }).start();
    }
}
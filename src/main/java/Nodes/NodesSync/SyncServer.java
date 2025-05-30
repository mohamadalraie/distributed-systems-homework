package Nodes.NodesSync;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.util.zip.*;

public class SyncServer {
    private static final int PORT = 9090;
    private static final List<String> nodeAddresses = Arrays.asList(
            "localhost:9091",
            "localhost:9092",
            "localhost:9093"
    );
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void main(String[] args) {
        startServer();
        scheduleDailySync();
        System.out.println("✅ Coordinator started successfully");
    }

    private static void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("🛡️ Coordinator listening on port " + PORT);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                System.err.println("❌ Failed to start coordinator: " + e.getMessage());
            }
        }).start();
    }

    private static void scheduleDailySync() {
        long initialDelay = getInitialDelayToMidnight();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                syncAllNodes();
            } catch (Exception e) {
                System.err.println("⚠ Error during scheduled sync: " + e.getMessage());
            }
        }, initialDelay, 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);
        System.out.println("⏰ Scheduled daily sync at midnight");
    }

    private static long getInitialDelayToMidnight() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTimeInMillis() - System.currentTimeMillis();
    }

    public static void syncAllNodes() {
        System.out.println("\n🔁 Starting synchronization process...");
        ExecutorService executor = Executors.newFixedThreadPool(nodeAddresses.size());

        List<Future<?>> futures = nodeAddresses.stream()
                .map(address -> executor.submit(() -> syncNode(address)))
                .collect(Collectors.toList());

        futures.forEach(future -> {
            try {
                future.get(1, TimeUnit.MINUTES);
            } catch (Exception e) {
                System.err.println("⚠ Error in sync task: " + e.getMessage());
            }
        });

        executor.shutdown();
        System.out.println("✅ Synchronization completed");
    }

    private static void syncNode(String nodeAddress) {
        System.out.println("\n🔄 Syncing node: " + nodeAddress);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(
                    nodeAddress.split(":")[0],
                    Integer.parseInt(nodeAddress.split(":")[1])), 10000);

            try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                out.writeObject("NODE_READY");
                out.flush();

                out.writeObject("GET_ALL_FILES");
                Map<String, FileData> allFiles = (Map<String, FileData>) in.readObject();

                Map<String, byte[]> filesToSync = prepareFilesToSync(allFiles);
                long checksum = calculateChecksum(filesToSync.values());

                out.writeObject("SYNC_FILES");
                out.writeLong(checksum);
                out.writeObject(filesToSync);
                out.flush();

                distributeUpdates(nodeAddress, filesToSync);

                boolean success = (boolean) in.readObject();
                System.out.println("🔹 " + nodeAddress + " sync " +
                        (success ? "succeeded" : "failed"));
            }
        } catch (Exception e) {
            System.err.println("⚠ Error syncing node " + nodeAddress + ": " + e.getMessage());
        }
    }

    private static Map<String, byte[]> prepareFilesToSync(Map<String, FileData> allFiles) {
        Map<String, byte[]> filesToSync = new HashMap<>();
        long now = System.currentTimeMillis();

        allFiles.forEach((relativePath, fileData) -> {
            try {
                if (now - fileData.getTimestamp() < 86400000) {
                    byte[] content = fileData.getContent();
                    if (content.length > 1024 * 1024) {
                        content = compressContent(content);
                    }
                    filesToSync.put(relativePath, content);
                }
            } catch (Exception e) {
                System.err.println("⚠ Error preparing " + relativePath + ": " + e.getMessage());
            }
        });

        System.out.println("📦 Prepared " + filesToSync.size() + " files for sync");
        return filesToSync;
    }

    private static void distributeUpdates(String sourceNode, Map<String, byte[]> files) {
        nodeAddresses.parallelStream().forEach(targetNode -> {
            if (!targetNode.equals(sourceNode)) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(
                            targetNode.split(":")[0],
                            Integer.parseInt(targetNode.split(":")[1])), 10000);

                    try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                         ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                        out.writeObject("NODE_READY");
                        out.flush();

                        out.writeObject("SYNC_FILES");
                        out.writeLong(calculateChecksum(files.values()));
                        out.writeObject(files);
                        out.flush();

                        boolean success = (boolean) in.readObject();
                        System.out.println("📤 Distributed " + files.size() +
                                " files to " + targetNode + " - " +
                                (success ? "Success" : "Failed"));
                    }
                } catch (Exception e) {
                    System.err.println("⚠ Error distributing to " + targetNode + ": " + e.getMessage());
                }
            }
        });
    }

    private static byte[] compressContent(byte[] original) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(original);
        }
        return baos.toByteArray();
    }

    private static long calculateChecksum(Collection<byte[]> dataCollection) {
        CRC32 crc = new CRC32();
        for (byte[] data : dataCollection) {
            crc.update(data);
        }
        return crc.getValue();
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

                String nodeId = (String) in.readObject();
                System.out.println("📡 Registered node: " + nodeId);

            } catch (Exception e) {
                System.err.println("⚠ Node registration error: " + e.getMessage());
            }
        }
    }

    public static class FileData implements Serializable {
        private static final long serialVersionUID = 1L;
        private final byte[] content;
        private final long timestamp;

        public FileData(byte[] content, long timestamp) {
            this.content = content;
            this.timestamp = timestamp;
        }

        public byte[] getContent() { return content; }
        public long getTimestamp() { return timestamp; }
    }
}
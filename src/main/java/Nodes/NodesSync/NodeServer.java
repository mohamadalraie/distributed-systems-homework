package Nodes.NodesSync;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
import java.util.zip.*;

public class NodeServer {
    private final int port;
    private final String folderPath;
    private final String nodeId;

    public NodeServer(int port, String folderPath) {
        this.port = port;
        this.folderPath = folderPath;
        this.nodeId = "localhost:" + port;
        ensureFolderExists();
    }

    private void ensureFolderExists() {
        try {
            Files.createDirectories(Paths.get(folderPath));
            System.out.println("📁 Node folder ready: " + folderPath);
        } catch (IOException e) {
            System.err.println("❌ Failed to create node folder: " + e.getMessage());
        }
    }

    public void start() {
        registerWithCoordinator();
        startServer();
    }

    private void registerWithCoordinator() {
        try (Socket coordSocket = new Socket("localhost", 9090);
             ObjectOutputStream out = new ObjectOutputStream(coordSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(coordSocket.getInputStream())) {

            out.writeObject(nodeId);
            System.out.println("📝 Registered with coordinator");

        } catch (Exception e) {
            System.err.println("⚠ Registration failed: " + e.getMessage());
        }
    }

    private void startServer() {
        ExecutorService executor = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("🟢 Node " + nodeId + " ready on port " + port);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executor.execute(new RequestHandler(clientSocket));
                } catch (IOException e) {
                    System.err.println("⚠ Connection error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Failed to start node: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    private List<String> listFilesInFolder() throws IOException {
        Path rootPath = Paths.get(folderPath);
        try (Stream<Path> paths = Files.walk(rootPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(rootPath::relativize)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
    }

    private class RequestHandler implements Runnable {
        private final Socket clientSocket;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        public RequestHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                this.clientSocket.setSoTimeout(30000);
                this.out = new ObjectOutputStream(clientSocket.getOutputStream());
                this.in = new ObjectInputStream(clientSocket.getInputStream());
            } catch (IOException e) {
                System.err.println("⚠ Error initializing streams: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                in.readObject(); // Handshake

                String command = (String) in.readObject();
                System.out.println("📩 Received command: " + command);

                switch (command) {
                    case "GET_FILE_LIST":
                        handleGetFileList();
                        break;

                    case "GET_ALL_FILES":
                        handleGetAllFiles();
                        handleSyncCommand();
                        break;

                    case "SYNC_FILES":
                        handleSyncFiles();
                        break;

                    default:
                        out.writeObject("ERROR:UNKNOWN_COMMAND");
                        out.flush();
                }
            } catch (Exception e) {
                handleException(e);
            } finally {
                closeResources();
            }
        }

        private void handleGetFileList() throws IOException {
            List<String> files = listFilesInFolder();
            out.writeObject(files);
            out.flush();
            System.out.println("📤 Sent file list (" + files.size() + " files)");
        }

        private void handleGetAllFiles() throws IOException {
            Map<String, SyncServer.FileData> allFiles = new HashMap<>();
            Path rootPath = Paths.get(folderPath);

            try (Stream<Path> paths = Files.walk(rootPath)) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    try (InputStream is = new FileInputStream(path.toFile())) {
                        String relativePath = rootPath.relativize(path).toString();
                        long timestamp = Files.getLastModifiedTime(path).toMillis();
                        byte[] content = is.readAllBytes();
                        allFiles.put(relativePath, new SyncServer.FileData(content, timestamp));
                    } catch (IOException e) {
                        System.err.println("❌ Failed to read file: " + e.getMessage());
                    }
                });
            }

            out.writeObject(allFiles);
            out.flush();
            System.out.println("📤 Sent " + allFiles.size() + " files with metadata");
        }

        private void handleSyncCommand() throws IOException, ClassNotFoundException {
            String command = (String) in.readObject();
            if ("SYNC_FILES".equals(command)) {
                handleSyncFiles();
            }
        }

        private void handleSyncFiles() throws IOException, ClassNotFoundException {
            long expectedChecksum = in.readLong();
            Map<String, byte[]> receivedFiles = (Map<String, byte[]>) in.readObject();
            int successCount = 0;

            long actualChecksum = calculateChecksum(receivedFiles.values());
            if (actualChecksum != expectedChecksum) {
                System.err.println("⚠ Checksum mismatch! Data may be corrupted");
                return;
            }

            for (Map.Entry<String, byte[]> entry : receivedFiles.entrySet()) {
                if (!isValidPath(entry.getKey())) {
                    System.err.println("❌ Invalid path: " + entry.getKey());
                    continue;
                }

                try {
                    Path fullPath = Paths.get(folderPath, entry.getKey());
                    Files.createDirectories(fullPath.getParent());

                    byte[] content = entry.getValue();
                    if (isCompressed(content)) {
                        try {
                            content = decompressContent(content);
                        } catch (IOException e) {
                            System.err.println("⚠ Using compressed data as-is");
                        }
                    }

                    Files.write(fullPath, content,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE);

                    successCount++;
                } catch (Exception e) {
                    System.err.println("❌ Failed to save " + entry.getKey() + ": " + e.getMessage());
                }
            }

            boolean success = successCount == receivedFiles.size();
            out.writeObject(success);
            out.flush();
            System.out.println("🔄 Updated " + successCount + "/" + receivedFiles.size() + " files");
        }

        private boolean isValidPath(String relativePath) {
            try {
                Path path = Paths.get(relativePath).normalize();
                return !path.startsWith("../") && !path.isAbsolute();
            } catch (InvalidPathException e) {
                return false;
            }
        }

        private boolean isCompressed(byte[] content) {
            return content.length >= 2 &&
                    content[0] == (byte) GZIPInputStream.GZIP_MAGIC &&
                    content[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8);
        }

        private byte[] decompressContent(byte[] compressed) throws IOException {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
                 GZIPInputStream gzip = new GZIPInputStream(bais);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[8192];
                int len;
                while ((len = gzip.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                return baos.toByteArray();
            }
        }

        private long calculateChecksum(Collection<byte[]> dataCollection) {
            CRC32 crc = new CRC32();
            for (byte[] data : dataCollection) {
                crc.update(data);
            }
            return crc.getValue();
        }

        private void handleException(Exception e) {
            if (e instanceof SocketTimeoutException) {
                System.err.println("⌛ Connection timeout");
            } else if (e instanceof EOFException) {
                System.err.println("🔌 Connection closed by client");
            } else {
                System.err.println("⚠ Error in request handler: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        private void closeResources() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (!clientSocket.isClosed()) clientSocket.close();
                System.out.println("🚪 Connection closed properly");
            } catch (IOException e) {
                System.err.println("⚠ Error closing resources: " + e.getMessage());
            }
        }
    }
}
package Nodes;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class HealthChecker implements Runnable {
    private final List<Node> nodes;
    private final Map<String, Boolean> nodeStatus;

    public HealthChecker(List<Node> nodes) {
        this.nodes = nodes;
        this.nodeStatus = new ConcurrentHashMap<>();
        nodes.forEach(node -> nodeStatus.put(node.getStoragePath(), true));
    }

    @Override
    public void run() {
        while (true) {
            for (Node node : nodes) {
                boolean isAlive = checkNodeHealth(node.getStoragePath());
                nodeStatus.put(node.getStoragePath(), isAlive);
            }
            try {
                Thread.sleep(5000); // فحص كل 5 ثواني
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private boolean checkNodeHealth(String nodePath) {
        try {
            Path path = Paths.get(nodePath);
            // محاولة إنشاء ملف اختبار
            Path testFile = path.resolve("healthcheck.tmp");
            Files.write(testFile, "test".getBytes(), StandardOpenOption.CREATE);
            Files.delete(testFile);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isNodeAlive(String node) {
        return nodeStatus.getOrDefault(node, false);
    }
    public List<Node> getActiveNodes() {
        return nodes.stream()
                .filter(node -> nodeStatus.getOrDefault(node.getStoragePath(), false))
                .collect(Collectors.toList());
    }
}
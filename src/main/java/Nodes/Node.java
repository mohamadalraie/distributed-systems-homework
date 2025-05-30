package Nodes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class Node {
    private String nodeId;
    private String storagePath;
    private List<String> otherNodes;



    public Node(String nodeId, String storagePath) throws RemoteException {

        this.nodeId = nodeId;
        this.storagePath = storagePath;
        createDepartmentFolders();

    }


    private void createDepartmentFolders() {
        String[] departments = {"dev", "design", "QA"};
        for (String dept : departments) {
            Path path = Paths.get(storagePath, dept);
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                System.err.println("Error creating folder for " + dept + ": " + e.getMessage());
            }
        }
    }
    public List<String> getFileList(String department) throws IOException {
        Path deptPath = Paths.get(storagePath, department);
        List<String> files = new ArrayList<>();
        Files.list(deptPath).forEach(path -> files.add(path.getFileName().toString()));
        return files;
    }

    public byte[] getFile(String department, String fileName) throws IOException {
        Path filePath = Paths.get(storagePath, department, fileName);
        return Files.readAllBytes(filePath);
    }

    public void saveFile(String department, String fileName, byte[] fileData) throws IOException {
        Path filePath = Paths.get(storagePath, department, fileName);
        Files.write(filePath, fileData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public List<String> getOtherNodes() {
        return otherNodes;
    }


    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }
    public Node() throws RemoteException {
    }

}

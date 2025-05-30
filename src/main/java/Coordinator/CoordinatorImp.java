package Coordinator;

import Nodes.HealthChecker;
import Nodes.Node;
import Employee.Employee;
import Files.FileData;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CoordinatorImp extends UnicastRemoteObject implements Coordinator {
    private List<Node> activeNodes;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private List<Node> nodes;

    private List<Employee> employees;

    // to verify all employees using the system
    private Map<Integer, String> tokens;

    // it's a pointer to the next node
    // we use it in load balance
    private final AtomicInteger currentIndex;

    // instance from this class to help us use singleton pattern
    public static CoordinatorImp instance;

    static {
        try {
            instance = new CoordinatorImp();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

//    Constructor
    public CoordinatorImp() throws RemoteException {
        employees = new ArrayList<>();
        tokens = new HashMap<>();
        nodes = new ArrayList<>();
        nodes.add(new Node("node1", "E:\\nodes\\node1"));
        nodes.add(new Node("node2", "E:\\nodes\\node2"));
        nodes.add(new Node("node3", "E:\\nodes\\node3"));

        HealthChecker h=new HealthChecker(nodes);
      activeNodes= h.getActiveNodes();
        // بدء المؤشر من الصفر
        this.currentIndex = new AtomicInteger(0);

    }


    // Main functions
    @Override
    public String login(String username, String password) throws RemoteException {

        for (Employee emp : employees) {
            System.out.println(emp.getUsername());
            if (emp.getUsername().equals(username) && emp.getPassword().equals(password)) {
                String token = generateToken(emp.getId()); // إنشاء Token
                tokens.put(emp.getId(), token);
                System.out.println("token: " + token);
                return token;
            }

        }
        return null;
    }

    @Override
    public void addEmployee(Employee employee) {
        employee.setId(employees.size());
        this.employees.add(employee);
        System.out.println("a new employee added and the total employees is: " + employees.size());

        Employee emp = employees.getLast();


        System.out.println("Employee ID: " + emp.getId());
        System.out.println("Employee name: " + emp.getUsername());
        System.out.println("Employee department: " + emp.getDepartment());
        System.out.println("Employee permissions: " + emp.getPermissions());
        System.out.println("-------------------");


    }

    @Override
    public boolean uploadFile(String token, String nodeId, FileData file) throws RemoteException, IOException {
        Employee employee = validateToken(token); // التحقق من صحة Token
        if (employee.hasPermission("W")) {
            Node selectedNode = new Node();
            for (Node node : nodes
            ) {
                if (node.getNodeId().equals(nodeId)) {
                    selectedNode = node;
                    break;
                }
            }

            // handling unActive node
            boolean isActive=false;
            for (Node node : activeNodes
            ) {
                if (node.getNodeId().equals(nodeId)) {
                    isActive=true;
                    break;
                }

            }
            if (!isActive) {

                System.out.println("Sorry, Node is not Active");
                return false;
            }

            String folderPath=selectedNode.getStoragePath() + "\\" + employee.getDepartment();
            System.out.println(folderPath);
            Path targetPath = Paths.get(folderPath);

            Path filePath = targetPath.resolve(file.getFilename());
            Files.write(filePath, file.getData());
            System.out.println(
                    "uploaded successfully"
            );
        } else System.out.println("You don't have the upload permission");
        return false;

        // توزيع الطلب على العقد باستخدام Load Balancer
//            String nodeAddress = loadBalancer.selectNode();
//            byte[] fileData = fetchFromNode(nodeAddress, fileName); // جلب الملف من العقدة
//            return fileData;
//        }
//        throw new RemoteException("Permission denied!");
//        return true;
    }


    @Override
    public boolean deleteFile(String token, String nodeId, String fileName) throws RemoteException {
        Employee employee = validateToken(token); // التحقق من صحة Token
        if (employee.hasPermission("D")) {
            Node selectedNode = new Node();
            for (Node node : nodes
            ) {
                if (node.getNodeId().equals(nodeId)) {
                    selectedNode = node;
                    break;
                }
            }
            // handling un active nodes
            boolean isActive=false;
            for (Node node : activeNodes
            ) {
                if (node.getNodeId().equals(nodeId)) {
                    isActive=true;
                    break;
                }

            }
            if (!isActive) {

                System.out.println("Sorry, Node is not Active");
                return false;
            }

            Path folderPath = Paths.get(selectedNode.getStoragePath() + "\\" + employee.getDepartment());

            Path filePath = Paths.get(String.valueOf(folderPath), fileName);

            try {
                // التحقق من وجود الملف أولاً

                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    System.out.println("The file is deleted: " + filePath);
                    return true;
                } else {
                    System.out.println("the file doesn't exist: " + filePath);
                    return false;
                }
            } catch (IOException e) {
                System.err.println("failed in deleting the file: " + e.getMessage());
                return false;
            }
        } else System.out.println("You don't have the delete permission");
        return false;

    }

    @Override
    public synchronized boolean addOrModifyFile(String fileName, String nodeId, String newContent, String token) throws RemoteException {
        lock.writeLock().lock();
        Employee employee = validateToken(token); // التحقق من صحة Token
        if (employee.hasPermission("W")) {
            Node selectedNode = new Node();
            for (Node node : nodes
            ) {
                if (node.getNodeId().equals(nodeId)) {
                    selectedNode = node;
                    break;
                }
            }
            //handling active nodes
            boolean isActive=false;
            for (Node node : activeNodes
            ) {
                if (node.getNodeId().equals(nodeId)) {
                    isActive=true;
                    break;
                }
            }
            if (!isActive) {

                System.out.println("Sorry, Node is not Active");
                return false;
            }

            String folderPath=selectedNode.getStoragePath() + "\\" + employee.getDepartment()+"\\";
            Path filePath = Paths.get(folderPath + fileName);
            try {
                Files.write(filePath, newContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                System.out.println("تم تعديل الملف: " + fileName);
                return true;
            } catch (IOException e) {
                System.err.println("خطأ في تعديل الملف: " + e.getMessage());
                return false;
            }      finally {
            lock.writeLock().unlock();
        }
        }else {
            System.out.println("employee doesn't have write permission");
            return false;
        }
    }

    @Override
    public synchronized FileData findFile(String fileName , String token) throws IOException {
        Employee employee = validateToken(token); // التحقق من صحة Token
        if (employee.hasPermission("R")) {
        lock.readLock().lock();
        Path startDir = Paths.get("E:\\nodes");
        try {
            // البحث باستخدام walkFileTree
            FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.getFileName().toString().equals(fileName)) {
                        throw new RuntimeException(file.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }
            };

            Files.walkFileTree(startDir, visitor);
        } catch (RuntimeException e) {
            Path filePath=Paths.get(e.getMessage());


            byte[] fileData = Files.readAllBytes(filePath);
            String filename = filePath.getFileName().toString();
            FileData file = new FileData(filename, fileData);
            return file;
        } catch (IOException e) {
            System.err.println("حدث خطأ أثناء البحث: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }}else
            System.out.println("Employee doesn't have permission");
        return null;
    }

    @Override
    public synchronized FileData findFileWithLoadBalancer(String fileName) throws IOException {
        // نبحث في جميع العقد بالتناوب
        for (int attempt = 0; attempt < nodes.size(); attempt++) {
            // الحصول على العقدة التالية باستخدام Round Robin
            String selectedNode = getNextNode();

            System.out.println("البحث في العقدة: " + selectedNode + " (المحاولة: " + (attempt+1) + ")");

            // البحث في العقدة المحددة
            FileData result = searchInNode(selectedNode, fileName);

            // إذا وجدنا الملف نرجعه مباشرة
            if (result != null) {
                return result;
            }
        }

        // إذا لم يتم العثور على الملف في أي عقدة
        return null;
    }






    // Helping functions

            //handling tokens
    public String generateToken(int id) {
        return id + "_" + UUID.randomUUID().toString();
    }

    public Employee validateToken(String token) {
        Integer EmployeeId = findKeyByValue(tokens, token);
        if (EmployeeId == null) {
            System.out.println("invalid token");
            return null;
        } else {
            Employee e = employees.get(EmployeeId);
            return e;
        }
    }


            // helping in load balancing and Round Robin
    private String getNextNode() {
        // الحصول على القيمة الحالية وزيادتها
        int currentValue = currentIndex.getAndIncrement();

        // حساب المؤشر الجديد مع ضمان التكرار الدوري
        int nextIndex = currentValue % nodes.size();

        // إذا كانت القيمة سالبة (في حال overflow) نصححها
        if (nextIndex < 0) {
            nextIndex += nodes.size();
            currentIndex.set(0); // إعادة التعيين لتجنب Overflow
        }

        // إرجاع العقدة المختارة
        return nodes.get(nextIndex).getStoragePath();
    }

    private FileData searchInNode(String nodePath, String fileName) throws IOException {
        Path startDir = Paths.get(nodePath);

        try {
            FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.getFileName().toString().equals(fileName)) {
                        // إذا وجدنا الملف نرمي استثناء لنوقف البحث
                        throw new RuntimeException(file.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }
            };

            // بدء البحث في المجلد
            Files.walkFileTree(startDir, visitor);

        } catch (RuntimeException e) {
            // إذا وجدنا الملف نقرأ محتواه
            Path filePath = Paths.get(e.getMessage());
            byte[] fileData = Files.readAllBytes(filePath);
            return new FileData(filePath.getFileName().toString(), fileData);
        }

        return null;
    }



    public Integer findKeyByValue(Map<Integer, String> map, String value) {
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null; // أو يمكنك رمي استثناء إذا لم يتم العثور على القيمة
    }




    // Setters and Getters
    public static CoordinatorImp getInstance() {
        return instance;
    }

    public List<Employee> getEmployees() throws RemoteException {
        return employees;
    }

}

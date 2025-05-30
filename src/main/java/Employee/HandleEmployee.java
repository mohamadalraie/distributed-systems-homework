package Employee;

import Coordinator.Coordinator;
import Files.FileData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Objects;
import java.util.Scanner;

public class HandleEmployee {


    public static String login(Coordinator obj) throws RemoteException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("enter your username:");
        String username = scanner.nextLine();
        System.out.println("enter your password:");
        String password = scanner.nextLine();
        String token = obj.login(username, password);
        if (token != null) {
            System.out.println("Welcome " + username);
            return token;
        } else {
            System.out.println("Failed to login");
            return null;
        }
    }
    public static void addOrModifyFile(String token,Coordinator obj) throws RemoteException {
        if (Objects.equals(token, "")) {
            System.out.println("login first");
            return;
        }
        Scanner scanner =new Scanner(System.in);
        System.out.println("enter the node of the file:\nnode1, node2, node3");
        String nodeId= scanner.nextLine();

        System.out.println("enter the name of the file:");
        String fileName= scanner.nextLine();
        System.out.println("enter the content of the file:");
        String fileContent= scanner.nextLine();
        boolean modified = obj.addOrModifyFile(fileName+".txt",nodeId, fileContent, token);
        System.out.println("نتيجة التعديل: " + (modified ? "نجاح" : "فشل"));
    }

    public static void uploadFile(String token, Coordinator obj) throws IOException {
        if (Objects.equals(token, "")) {
            System.out.println("login first");
            return;
        }
        Scanner s = new Scanner(System.in);
        System.out.println("enter the node you want to upload to: \nnode1, node2, node3");
        String nodeId = s.nextLine();
        System.out.println("enter the path of source file:");
        Path sourcePath = Paths.get(s.nextLine());
        byte[] fileData = Files.readAllBytes(sourcePath);
        String filename = sourcePath.getFileName().toString();
        FileData file = new FileData(filename, fileData);

        obj.uploadFile(token, nodeId, file);

    }

    public static void deleteFile(String token, Coordinator obj) throws IOException {
        if (Objects.equals(token, "")) {
            System.out.println("login first");
            return;
        }
        Scanner s = new Scanner(System.in);
        System.out.println("enter the node you want to delete the file from : \nnode1, node2, node3");
        String nodeId = s.nextLine();
        System.out.println("enter the name of the file:");
        String fileName = s.nextLine();
        boolean deleted =obj.deleteFile(token, nodeId, fileName);
        if (deleted)
            System.out.println("file deleted successfully");
        else
            System.out.println("file doesn't exist or you don't have the delete permission");
    }

    public static void readTextFile(String token, Coordinator obj) throws IOException {
        Scanner scanner=new Scanner(System.in);
        System.out.println("enter the file name that you want to read:");
        String fileName=scanner.nextLine();
        FileData file=obj.findFile(fileName,token);

        if(file!=null){
            System.out.println(file.getFilename());
            System.out.println("محتويات الملف:");
            System.out.println("-------------------------");
            String content = new String(file.getData(), java.nio.charset.StandardCharsets.UTF_8);
            System.out.println(content);
            System.out.println("-------------------------");

        }
        else {
            System.out.println("file doesn't exist");
        }
    }

    public static void readTextFileWithLoadBalancer( Coordinator obj) throws IOException {
        Scanner scanner=new Scanner(System.in);
        System.out.println("enter the file name that you want to read:");
        String fileName=scanner.nextLine();
        FileData file=obj.findFileWithLoadBalancer(fileName);

        if(file!=null){
            System.out.println(file.getFilename());
            System.out.println("محتويات الملف:");
            System.out.println("-------------------------");
            String content = new String(file.getData(), java.nio.charset.StandardCharsets.UTF_8);
            System.out.println(content);
            System.out.println("-------------------------");

        }
        else {
            System.out.println("file doesn't exist");
        }
    }

    public static void main(String[] args) throws IOException, NotBoundException {
        Coordinator obj = (Coordinator) Naming.lookup("rmi://localhost:5000/coordinator");
        String token = "";
        Scanner scanner = new Scanner(System.in);

        boolean in = true;
        while (in) {
            System.out.println("press 1 to Login");
            System.out.println("press 2 to add or modify text file");
            System.out.println("press 3 to upload file");
            System.out.println("press 4 to delete file");
            System.out.println("press 5 to read file");
            System.out.println("press 6 to read file using a load balancer");
            System.out.println("press 7 to logout");


            switch (scanner.nextLine()) {
                case "1":
                    token = login(obj);
                    break;
                case "2":
                   addOrModifyFile(token,obj);
                    break;
                case "3":
                    uploadFile(token, obj);
                    break;
                case "4":
                    deleteFile(token, obj);
                    break;
                case "5":
                    readTextFile(token, obj);
                    break;
                case "6":
                    readTextFileWithLoadBalancer(obj);
                    break;
                case "7":
                    token="";
                    break;
                default:
                    break;

            }
        }

    }
}

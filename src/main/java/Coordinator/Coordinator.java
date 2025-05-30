package Coordinator;

import Files.FileData;
import Employee.Employee;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Coordinator extends Remote {
    // تسجيل الدخول وإصدار Token
    String login(String username, String password) throws RemoteException;

    void addEmployee(Employee employee)throws RemoteException;

    // رفع ملف إلى العقدة المناسبة
    boolean uploadFile(String token, String nodeId, FileData file) throws RemoteException, IOException;


    boolean deleteFile(String token, String nodeId,String fileName)throws RemoteException;

    boolean addOrModifyFile(String fileName, String nodeId, String newContent, String token) throws RemoteException;

    FileData findFile(String fileName,String token) throws IOException;

    FileData findFileWithLoadBalancer(String fileName) throws IOException;

     List<Employee> getEmployees()throws RemoteException;

}

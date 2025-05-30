package Manager;

import Coordinator.Coordinator;
import Employee.Employee;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;

public class HandleManager {
    public static void addEmployee(Coordinator obj) throws RemoteException {

        Employee employee = new Employee();
        Scanner scanner = new Scanner(System.in);
        System.out.println("enter the name of the User:");
        employee.setUsername(scanner.nextLine());
        System.out.println("enter the password of the User:");
        employee.setPassword(scanner.nextLine());

        String[] validDepartments = {"DEV", "DESIGN", "QA"};
        String department;
        while (true) {
            System.out.println("enter the department of the User:  (DEV, DESIGN, QA)");
            department = scanner.nextLine().trim().toUpperCase();

            if (Arrays.asList(validDepartments).contains(department)) {
                break;
            }
            System.out.println("Invalid input! Please enter one of: dev, design, QA");
        }

        employee.setDepartment(department);



        Set<String> permissions = new java.util.HashSet<String>(Set.of());
        System.out.println("enter the permissions of the User: ( R, W, D)");
        while (true) {
            System.out.print("enter the permission:\n" +
                    "R: read\nW: write\nD: delete\ndone: to save \n");
            String permission = scanner.nextLine().trim().toUpperCase();

            if (permission.equals("DONE")) {
                break;
            }

            if (permission.matches("[RWD]")) { // التحقق من أن الإدخال R أو W أو D
                permissions.add(permission);
                System.out.println("permission added successfully: " + permission);
            } else {
                System.out.println("invalid permission!! please enter W or D or U or done to save");
            }
        }

        employee.setPermissions(permissions);

        obj.addEmployee(employee);

    }

    public static void viewEmployees(Coordinator obj) throws RemoteException {
        List<Employee> employees= obj.getEmployees();

        employees.forEach(employee -> {
            System.out.println("Employee ID: " + employee.getId());
            System.out.println("Employee name: " + employee.getUsername());
            System.out.println("Employee department: " + employee.getDepartment());
            System.out.println("Employee permissions: " + employee.getPermissions());
            System.out.println("-------------------");
        });

    }


    public static void main(String[] args) throws MalformedURLException, NotBoundException, RemoteException {

        Coordinator obj = (Coordinator) Naming.lookup("rmi://localhost:5000/coordinator");


        Scanner scanner = new Scanner(System.in);

        boolean in = true;
        while (in) {
            System.out.println("press 1 to add new user");
            System.out.println("press 2 to add view the users");

            switch (scanner.nextInt()) {
                case 1:
                    addEmployee(obj);
                    break;
                case 2:
                    viewEmployees(obj);
                    break;

            }
        }


    }

}

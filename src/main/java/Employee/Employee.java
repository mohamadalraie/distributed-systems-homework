package Employee;

import java.util.Set;
import java.io.Serializable;
public class Employee  implements Serializable{


    private int id;
    private String username;
    private String password;
    private String department; // "DEV", "DESIGN", "QA"
    private Set<String> permissions; // "R", "W", "D"

    public Employee(int id, String username, String password, String department, Set<String> permissions) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.department = department;
        this.permissions = permissions;
    }

    public Employee() {
    }


    public boolean hasPermission(String requiredPermission){
      return   getPermissions().contains(requiredPermission);
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }



}

import Coordinator.CoordinatorImp;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;


public class Server  {

    public static void main(String[] args)
    {
        try {


            CoordinatorImp coordinator =  CoordinatorImp.getInstance();

            // Create registry on port 5000
            LocateRegistry.createRegistry(5000);

            // Bind the object to registry
            Naming.rebind("rmi://localhost:5000/coordinator", coordinator);

            System.out.println("Server ready. Coordinator bound in registry.");

        } catch (RemoteException ex) {
            System.out.println(ex.getMessage());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }


    }

}

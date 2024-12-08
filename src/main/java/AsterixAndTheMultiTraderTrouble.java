import peer.IPeer;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AsterixAndTheMultiTraderTrouble {

    public static int REGISTRY_PORT = 1099;
    public static String CLASS_PATH = "build/classes/java/main";
    public static String BUYER_CLASS = "peer.Buyer";
    public static String SELLER_CLASS = "peer.Seller";
    public static String WAREHOUSE_CLASS = "warehouse.Warehouse";

    public static boolean SEND_HEARTBEATS = true;
    public static int TRADER_CRASH_TIME = 20000; // set higher than zero to simulate crash.

    public static void main(String[] args) throws IOException, InterruptedException, NotBoundException {

        int b = Integer.parseInt(args[0]); // Number of buyers
        int s = Integer.parseInt(args[1]); // Number of sellers
        int t = Integer.parseInt(args[2]); // Number of traders
        int n = b + s; // number of peers

        Registry registry = LocateRegistry.createRegistry(REGISTRY_PORT);
        runProcess("java", "-cp", CLASS_PATH, WAREHOUSE_CLASS);

        Thread.sleep(1000); // ensure that warehouse is bound

        Process[] processes = new Process[n];

        // initialize all peers
        for (int i = 0; i < n; i++) {

            String className;

            if (b == 0) {
                className = SELLER_CLASS;
            } else if (s == 0) {
                className = BUYER_CLASS;
            } else if (i % 2 == 0) {
                b--;
                className = BUYER_CLASS;
            } else {
                className = SELLER_CLASS;
                s--;
            }

            Process process = runProcess(
                    "java",
                    "-cp",
                    CLASS_PATH,
                    className,
                    "" + i,
                    "" + n);

            processes[i] = process;
        }

        Thread.sleep(1000); // ensure that all peers are bound

        // retrieve proxies
        IPeer[] peers = new IPeer[n];
        for (int i = 0; i < n; i++) {
            System.out.println("Registering peer " + i);
            peers[i] = (IPeer) registry.lookup("" + i);
        }

        for (int i = 0; i < n; i++) {
            peers[i].start();
        }

        // do initial election
        peers[0].election(new int[] {}, t);

        // only starts heartbeat for Traders
        if (SEND_HEARTBEATS) {
            for (int i = 0; i < n; i++){
                peers[i].startHeartbeat();
            }
        }

        // crash behavior
        if (TRADER_CRASH_TIME > 0) {
            ScheduledExecutorService crashExecutor = Executors.newScheduledThreadPool(1);
            crashExecutor.schedule(() -> {
                try {
                    peers[peers.length - 1].crash();
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }, TRADER_CRASH_TIME, TimeUnit.MILLISECONDS);
        }

        // don't exit program
        for (int i = 0; i < n; i++) {
            processes[i].waitFor();
        }
    }

    private static Process runProcess(String... cmd) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        Process process = processBuilder.start();
        var inputStream = process.getInputStream();
        var outputStream = System.out;

        // redirect output stream of peer process to main process
        new Thread(() -> {
            try (inputStream; outputStream) {
                int byteData;
                while ((byteData = inputStream.read()) != -1) {
                    outputStream.write(byteData);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // destroy process when stopping program
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (process.isAlive()) {
                process.destroy();
            }
        }));

        return process;
    }
}

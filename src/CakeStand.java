import java.nio.Buffer;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class CakeStand{
    private int cake;
    private boolean availability;
    public CakeStand(int slices){
        cake=slices;
        availability=true;
    }
    // one monster bites at a time. cake-level lock. visitor monsters wait for this.
    private final Semaphore sliceSemaphore = new Semaphore(0);
    // stand-level lock. to mutate member variables
    private final Lock protectStand = new ReentrantLock();

    /* ------------------------------- methods below -------------------------------
     */
    public void inavailable(){availability=false;}
    public void available(){availability=true;}
    public boolean askIFAvailable(){return availability;}
    public int askSlices(){return cake;}
    public void eaten(){cake--;}
    public void insertcake(int slices){cake=slices; inavailable();}
    public void lock_stand(){
       protectStand.lock();
    }
    public void unlock_stand(){
        protectStand.unlock();
    }
    public void wait_sliceSemaphore(){
        try{
            sliceSemaphore.acquire();
        }
        catch(Exception e){
            System.out.println("wait_sliceSemaphore throws: " + e);
        }
    }
    public void notify_slicesSemaphore(int slices) {
        for (int newSlices = slices; newSlices > 0; newSlices--) {
            try {
                sliceSemaphore.release();
            } catch (Exception e) {
                System.out.println("notify_sliceSemaphore throws: " + e);
            }
        }
    }
    // when stand brought :
    // lock ground_suppliers
    // place stand
    // assign id number to stand
    // **let cakebakers know (only one enters -> notify) :
        // lock cake stand storage
        // place cake slices with id;s (buffer)
        // **let waiting monsters if any on that buffer
        // unlock cake stand storage
    // **let monsters  know (only one enters -> notify) :
        // wait for cake slice buffer
        // lock cake buffer
        // eat one slice
        // unlock cake buffer
    // unlock ground_suppliers
    public static void supplyStand() {
        String callerName = Thread.currentThread().getName();
        System.out.println(callerName + " brought a new stand.");
        // one supplier thread can run below at once
        Ground.lockSuppliersGround();
        try {
            CakeStand newStand = new CakeStand(0);
            // its ok to both push to stand and print count as suppliers are only 1 and
            // safe to have its cake's slices value set by another baker thread, after line 36
            Ground.addStand(newStand);
            System.out.println(callerName + " added Stand#" + Ground.getStandsCount() + ".");
            Ground.notify_bakerssuppliedStand();
            //Ground.signalAll_monsters_suppliedStand();
        }
        finally {
            Ground.unlockSuppliersGround();
        }
    }


    public static void putCake(int slices) {
        String callerName = Thread.currentThread().getName();
        System.out.println(callerName+ " baked a cake with " + slices + " slices.");
        Ground.wait_bakerssuppliedStand();
        // lockBakersGround so that more than 1 cake bakers cannot enter at the same time but other stand providers can enter, after this line
        Ground.lockBakersGround();
        try {
            // stand guaranteed to be set. as semaphore just notified of its existence.
            CakeStand stand = null;
            int id = 1;
            // Fail-safe iterator. only modifies elements instead of writing, so memory consumption controlled.
            for (Iterator it = Ground.getStands().iterator(); it.hasNext(); id++) {
                stand = (CakeStand) it.next();
                stand.lock_stand();
                try {
                    if (stand.askIFAvailable()) {
                        System.out.println(callerName + " put the cake with " + slices + " slices on Stand#" + id + ".");
                        // insert cake into stand. this also makes the stand inavailable.
                        stand.insertcake(slices);
                    }
                } finally {
                    stand.unlock_stand();
                }
            }
            if (stand != null) {
                // we should notify monsters of all the new slices
                stand.notify_slicesSemaphore(slices);
            }
        }
        finally {
            Ground.unlockBakersGround();
        }
    }
    public static CakeStand randomStand() {
        String callerName = Thread.currentThread().getName();
        int id = 1;
        CakeStand stand = null;
        // only the early arrivals need to be signalled. others can find a stand anyway.
        // so, if no stands exist, wait for a signal not to employ busy wait.
        // first put stand notifies all, so all waiters can eventually find one.
        Ground.lockprotectStands();
        try {
            if (Ground.getStands().size() == 0) Ground.wait_monsters_suppliedStand();
            for (Iterator it = Ground.getStands().iterator(); it.hasNext(); id++) {
                stand = (CakeStand) it.next();
                System.out.println(callerName + " came to Stand#" + id + " for a slice.");
                break;
            }
            if (stand == null) System.out.println(callerName + " smth went seriously wrong in randomStand#");
        }
        finally {
            Ground.unlockprotectStands();
        }
        return stand;
    }
    public void getSlice() {


    }

}


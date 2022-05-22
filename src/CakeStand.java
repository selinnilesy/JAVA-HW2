import java.nio.Buffer;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class CakeStand{
    private int cake;
    private int id;
    private boolean availability;
    public CakeStand(int slices, int no){
        cake=slices;
        availability=true;
        id = no;
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
    public void eaten(){cake--; if(this.askSlices() == 0) available();}
    public void insertcake(int slices){cake=slices; inavailable();}
    public int getID(){return id;}
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
            CakeStand newStand = new CakeStand(0, Ground.getStandsCount() + 1);
            // its ok to concurrently push new stand to stands and have a cake put by baker,
            // and select any stands by monsters who uses fail-safe.
            Ground.addStand(newStand);
            System.out.println(callerName + " added Stand#" + newStand.getID() + ".");
            Ground.notify_bakerssuppliedStand();
            // for early bird monsters only. safe as mentioned, but i have to lock due to CV strictly bound to a lock object
            if(Ground.getStandsCount() > 1){
                Ground.lockstartWorld();
                try {
                    Ground.signalAll_monsters_suppliedStand();
                }
                finally {
                    Ground.unlockstartWorld();
                }
            }
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
            // Fail-safe iterator. only modifies elements instead of writing, so memory consumption controlled.
            for (Iterator it = Ground.getStands().iterator(); it.hasNext(); ) {
                stand = (CakeStand) it.next();
                // askIFAvailable has to be locked due to potential active eating monsters
                stand.lock_stand();
                try {
                    if (stand.askIFAvailable()) {
                        System.out.println(callerName + " put the cake with " + slices + " slices on Stand#" + stand.getID() + ".");
                        // insert cake into stand. this also makes the stand inavailable.
                        stand.insertcake(slices);
                        // we should notify monsters of all the new slices so if there are monsters they start eating
                        // has to be done while locking slice number. dangerous modification.
                        stand.notify_slicesSemaphore(slices);
                        return;
                    }
                } finally {
                    stand.unlock_stand();
                }
            }
        }
        finally {
            Ground.unlockBakersGround();
        }
    }
    public static CakeStand randomStand() {
        String callerName = Thread.currentThread().getName();
        CakeStand stand = null;
        // only the early arrivals need to be signalled. others can find a stand anyway.
        // so, if no stands exist, wait for a signal not to employ busy wait.
        // first put stand notifies all, so all waiters can eventually find one.
        Ground.lockstartWorld();
        try {
            while (Ground.getStands().size() == 0) Ground.wait_monsters_suppliedStand();
        }
        finally {
            Ground.unlockstartWorld();
        }
        // unlock immediately to allow concurrent run of different monsters
        int chosenID = (int) ((Math.random() * (Ground.getStands().size() - 1)) + 1);
        System.out.println(callerName + " came to Stand#" + chosenID + " for a slice.");
        stand = Ground.getSpecificStand(chosenID-1);
        return stand;
    }
    public void getSlice() {
        String callerName = Thread.currentThread().getName();
        // wait until an available cake.
        // diff monsters cannot eat at the same time due to lock_stand, even if the only signalled monster was this
        // the cake is protected as well for one slice at a time
        wait_sliceSemaphore();
        // to eat it, protect the stand and related cake info
        lock_stand();
        try {
            eaten(); // makes it also available if no cake left anymore
            System.out.println(callerName + " got a slice from Stand#" + this.getID() +", so " + this.askSlices() + " left.");
        }
        finally {
            unlock_stand();
        }
    }

}


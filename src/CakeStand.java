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
    public void eaten(){
        cake--;
        if(this.askSlices() == 0) {
            available();
            Ground.notify_bakerssuppliedStand();
        }
    }
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
            // if the new stand was pushed unlocked, before we notify monsters of it being put on the ground,
            // monsters can prematurely come to the stand, at the entrance. fixed by forum post.
            newStand.lock_stand();
            try{
                Ground.addStand(newStand);
                System.out.println(callerName + " added Stand#" + newStand.getID() + ".");
                Ground.notify_bakerssuppliedStand();
                if(Ground.getStandsCount() > 1){
                    // lock for below CV, for monsters
                    Ground.lockstartWorld();
                    try {
                        // CV for all early bird monsters.
                        Ground.signalAll_monsters_suppliedStand();
                    }
                    finally {
                        Ground.unlockstartWorld();
                    }
                }
            }
            finally {
                newStand.unlock_stand();
            }
        }
        finally {
            Ground.unlockSuppliersGround();
        }
    }


    public static void putCake(int slices) {
        String callerName = Thread.currentThread().getName();
        System.out.println(callerName+ " baked a cake with " + slices + " slices.");
        // wait until a fresh stand is supplied
        Ground.wait_bakerssuppliedStand();
        // lockBakersGround so that more than 1 cake bakers cannot enter at the same time but other stand providers can enter, after this line
        Ground.lockBakersGround();
        try {
            // at least 1 stand guaranteed to be available. as semaphore just notified of its existence and 1 baker works at a time.
            CakeStand stand = null;
            // Fail-safe iterator. here it only modifies elements instead of copy-on-write, therefore memory consumption controlled.
            for (Iterator it = Ground.getStands().iterator(); it.hasNext(); ) {
                stand = (CakeStand) it.next();
                // stand has to be locked as we will modify its fields
                stand.lock_stand();
                try {
                    if (stand.askIFAvailable()) {
                        System.out.println(callerName + " put the cake with " + slices + " slices on Stand#" + stand.getID() + ".");
                        // insert cake into stand: this also makes the stand inavailable.
                        stand.insertcake(slices);
                        // we should notify monsters of all the new slices, so if there are monsters on this stand, they can start eating
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

        // wait the beginning of a world (for monsters), until below CV comes true (kinda boolean)
        Ground.lockstartWorld();
        try {
            // only the early arrivals (all) need to be signalled. others can find a stand anyway.
            // in other words, if no stands exist, wait for a signal not to employ busy wait.
            while (Ground.getStands().size() == 0) Ground.wait_monsters_suppliedStand();
        }
        finally {
            Ground.unlockstartWorld();
        }
        // casting works like a floor function, therefore biased stand selection. (no order specified)
        int chosenID = (int) ((Math.random() * (Ground.getStands().size() - 1)) + 1);
        stand = Ground.getSpecificStand(chosenID-1);
        // wait until its put on the ground (in case it has not been put yet, due to context switches)
        stand.lock_stand();
        try{
            System.out.println(callerName + " came to Stand#" + chosenID + " for a slice.");
        }
        finally {
            stand.unlock_stand();
        }
        return stand;
    }
    public void getSlice() {
        String callerName = Thread.currentThread().getName();
        // wait until an available cake.
        // diff. monsters cannot eat at the same time due to lock_stand
        // the cake is protected for one slice at a time
        wait_sliceSemaphore();
        // to eat it, protect the stand and related cake info
        lock_stand();
        try {
            // slices are consumed one by one for each monster via eaten().
            // marks the stand also available if no slices left anymore, and signals bakers of tis availability.
            eaten();
            System.out.println(callerName + " got a slice from Stand#" + this.getID() +", so " + this.askSlices() + " left.");
        }
        finally {
            unlock_stand();
        }
    }

}


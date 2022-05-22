import java.nio.Buffer;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class CakeStand extends Ground{
    // cake stands here. to supply stand


    // when created :
    // **let cakebakers know (only one -> notify) :
        // lock cake stand storage
        // place cake slices with id;s (buffer)
        // **let waiting monsters if any on that buffer
        // unlock cake stand storage

    // **let monsters  know :
        // wait for cake slice buffer
        // lock cake buffer
        // eat one slice
        // unlock cake buffer
    // lock ground_suppliers
    // place stand
    // assign id number to stand
    // System.out.println("<Supplier name> added Stand#<id no>.");
    // semaphore : stand count, to let cakebakers know (only one cakebaker-> notify)
    // unlock ground_suppliers
    public static void supplyStand() {
        String callerName = Thread.currentThread().getName();
        System.out.println(callerName + " brought a new stand.");
        // one supplier thread can run below at once
        Ground.lockSuppliersGround();
        Stand newStand = new Stand(0);
        // its ok to both push to stand and print count as suppliers are only 1 and
        // safe to have its cake's slices value set by another baker thread, after line 36
        Ground.addStand(newStand);
        System.out.println(callerName + " added Stand#"+ Ground.getStandsCount() + ".");
        Ground.notify_suppliedStand();
        Ground.unlockSuppliersGround();
    }


    public static void putCake(int slices) {
        String callerName = Thread.currentThread().getName();
        System.out.println(callerName+ " baked a cake with " + slices + " slices.");
        Ground.wait_suppliedStand();
        // lockBakersGround so that more than 1 cake bakers cannot enter at the same time but other stand providers can enter, after this line
        Ground.lockBakersGround();
        // did not prefer using Fail-safe iterator. to find the first available stand, as semaphore notified of its existence.
        // this code is safe but used synchronized only to bypass concurrent modif exception
        int id = 1;
        Stand stand = null;
        for (Iterator it = Ground.getStands().iterator(); it.hasNext(); id++) {
            stand = (Stand) it.next();
            stand.lock_stand();
            if(stand.askIFAvailable()){
                System.out.println(callerName + " put the cake with " + slices + " slices on Stand#" + id + ".");
                // insert cake into stand. this also makes the stand inavailable.
                stand.putCake(slices);
            }
            stand.unlock_stand();
        }
        // we should notify monsters of all the new slices
        if(stand != null){
            stand.notify_slicesSemaphore(slices);
        }
        Ground.unlockBakersGround();
    }
    public static CakeStand randomStand() {
        // Create a thread pool with two threads
        CakeStand cs = new CakeStand();
        return cs;

    }
    public void getSlice() {
        // Create a thread pool with two threads

    }

}


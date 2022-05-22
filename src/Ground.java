import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Ground {
    // to protect stands from concurrent ArrayList addition
    // ground-level lock.
    static private Lock ground_suppliers = new ReentrantLock();
    // to protect a stand from multiple concurrent cake inputs (or ground from multiple bakers).
    // ground-level lock.
    static private Lock ground_bakers = new ReentrantLock();
    // id of any stand = its index since they are not destroyed but kept after cake finishes
    static private CopyOnWriteArrayList<Stand> stands= new CopyOnWriteArrayList<Stand>();
    // to wait for any supplied stands, used by cake bakers.
    static private final Semaphore suppliedStand = new Semaphore(0);
    public static class Stand{
        private int cake;
        private boolean availability;
        // one monster bites at a time. cake-level lock. new monsters wait this.
        private final Semaphore sliceSemaphore = new Semaphore(0);
        // to mutate variables before and after semaphore activity
        private final Lock protectStand = new ReentrantLock();
        public Stand(int slices){
            cake=slices;
            availability=true;
        }
        public void inavailable(){availability=false;}
        public void available(){availability=true;}
        public boolean askIFAvailable(){return availability;}
        public int askSlices(){return cake;}
        public void eaten(){cake--;}
        public void putCake(int slices){cake=slices; inavailable();}
        public void lock_stand(){
            try{
                protectStand.lock();
            }
            catch(Exception e){
                System.out.println("lock_cake throws: " + e);
            }
        }
        public void unlock_stand(){
            try{
                protectStand.unlock();
            }
            catch(Exception e){
                System.out.println("unlock_cake throws: " + e);
            }
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
    }

    static protected CopyOnWriteArrayList<Stand> getStands(){
        return stands;
    }
    static protected void addStand(Stand stand){
        stands.add(stand);
    }
    static protected int getStandsCount(){
        return stands.size();
    }
    static protected Stand getSpecificStand(int index){
        return stands.get(index);
    }
    static protected void wait_suppliedStand(){
        try{
            suppliedStand.acquire();
        }
        catch(Exception e){
            System.out.println("wait_suppliedStand throws: " + e);
        }
    }
    static protected void notify_suppliedStand(){
        try{
            suppliedStand.release();
        }
        catch(Exception e){
            System.out.println("notify_suppliedStand throws: " + e);
        }
    }
    static protected void lockBakersGround(){
        try{
            ground_bakers.lock();
        }
        catch(Exception e){
            System.out.println("lockBakersGround throws: " + e);
        }
    }
    static protected void lockSuppliersGround(){
        try{
            ground_suppliers.lock();
        }
        catch(Exception e){
            System.out.println("lockSuppliersGround throws: " + e);
        }
    }
    static protected void unlockBakersGround(){
        try{
            ground_bakers.unlock();
        }
        catch(Exception e){
            System.out.println("unlockBakersGround throws: " + e);
        }
    }
    static protected void unlockSuppliersGround(){
        try{
            ground_suppliers.unlock();
        }
        catch(Exception e){
            System.out.println("unlockSuppliersGround throws: " + e);
        }
    }
}

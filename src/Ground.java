import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Ground {
    /* -------------------------------  static ground members -------------------------------------
     *
     */
    // to protect stands from concurrent ArrayList addition
    // ground-level lock.
    static private Lock ground_suppliers = new ReentrantLock();
    // to protect a stand from multiple concurrent cake inputs (or ground from multiple bakers).
    // ground-level lock.
    static private Lock ground_bakers = new ReentrantLock();
    // id of any stand = its index since they are not destroyed but kept after cake finishes
    // there is no lock on this due to fail safe iterator, and not to block potential concurrency
    static private CopyOnWriteArrayList<CakeStand> stands= new CopyOnWriteArrayList<CakeStand>();
    // to wait for any supplied stands, used by cake bakers.
    static private final Semaphore bakers_suppliedStand = new Semaphore(0);
    // to wait for random stands, used by monsters.
    static private Lock protectStands = new ReentrantLock();
    static private Condition monsters_suppliedStand = protectStands.newCondition();

    /* -------------------------------  methods below -------------------------------------
    *
    */
    static protected CopyOnWriteArrayList<CakeStand> getStands(){
        return stands;
    }
    static protected void addStand(CakeStand stand){
        stands.add(stand);
    }
    static protected int getStandsCount(){
        return stands.size();
    }
    static protected CakeStand getSpecificStand(int index){
        return stands.get(index);
    }
    // no checked exception in lock/unlock, but semaphores and CVs need to be tried.
    static protected void lockprotectStands(){
        protectStands.lock();
    }
    static protected void unlockprotectStands(){
        protectStands.unlock();
    }
    static protected void wait_monsters_suppliedStand() {
        try {
            monsters_suppliedStand.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("wait_monsters_suppliedStand throws: " + e);
        }
    }
    static protected void signalAll_monsters_suppliedStand(){
        try{
            monsters_suppliedStand.signalAll();
        }
        catch(Exception e){
            System.out.println("signalAll_monsters_suppliedStand throws: " + e);
        }
    }
    static protected void wait_bakerssuppliedStand(){
        try{
            bakers_suppliedStand.acquire();
        }
        catch(Exception e){
            System.out.println("wait_bakerssuppliedStand throws: " + e);
        }
    }
    static protected void notify_bakerssuppliedStand(){
        try{
            bakers_suppliedStand.release();
        }
        catch(Exception e){
            System.out.println("notify_bakerssuppliedStand throws: " + e);
        }
    }
    static protected void lockBakersGround(){
        ground_bakers.lock();
    }
    static protected void unlockBakersGround(){
        ground_bakers.unlock();
    }
    static protected void lockSuppliersGround(){
        ground_suppliers.lock();
    }
    static protected void unlockSuppliersGround(){
        ground_suppliers.unlock();
    }
}

package ch.epfl.biop.bdv.bioformats.bioformatssource;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 * https://www.dbtsai.com/blog/2013/java-concurrent-dynamic-object-pool-for-non-thread-safe-objects-using-blocking-queue/
 *
 * Created with IntelliJ IDEA.
 * User: dtsai
 * Date: 2/18/13
 * Time: 3:42 PM
 */

public abstract class ResourcePool<Resource> {
    private final BlockingQueue<Resource> pool;
    private final ReentrantLock lock = new ReentrantLock();
    private int createdObjects = 0;
    private int size;

    protected ResourcePool(int size) {
        this(size, false);
    }

    protected ResourcePool(int size, Boolean dynamicCreation) {
        // Enable the fairness; otherwise, some threads may wait forever.
        pool = new ArrayBlockingQueue<>(size, true);
        this.size = size;
        if (!dynamicCreation) {
            lock.lock();
        }
    }

    public Resource acquire() throws Exception {
        if (!lock.isLocked()) {
            if (lock.tryLock()) {
                try {
                    ++createdObjects;
                    return createObject();
                } finally {
                    if (createdObjects < size) lock.unlock();
                }
            }
        }
        return pool.take();
    }

    public void recycle(Resource resource) throws Exception {
        // Will throws Exception when the queue is full,
        // but it should never happen.
        pool.add(resource);
    }

    public void createPool() {
        if (lock.isLocked()) {
            for (int i = 0; i < size; ++i) {
                pool.add(createObject());
                createdObjects++;
            }
        }
    }

    protected abstract Resource createObject();
}
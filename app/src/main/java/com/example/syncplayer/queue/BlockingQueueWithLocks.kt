import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

class BlockingQueueWithLocks<T>(private val capacity: Int, val debug: Boolean = false) {
    private val queue = ConcurrentLinkedQueue<T>()
    private val lock = ReentrantLock()
    private val notEmpty: Condition = lock.newCondition()
    private val notFull: Condition = lock.newCondition()

    @Throws(InterruptedException::class)
    fun produce(item: T) {
        lock.lock()
        try {
            while (queue.size >= capacity) {
                notFull.await()
            }
            queue.add(item)
            notEmpty.signal()
        } finally {
            lock.unlock()
        }
    }

    @Throws(NoSuchElementException::class)
    fun consume(): T {
        lock.lock()
        try {
            while (queue.isEmpty()) {
                notEmpty.await()
            }
            val item = queue.poll() ?: throw NoSuchElementException("队列为空")
            notFull.signal()
            return item
        } finally {
            lock.unlock()
        }
    }
}

package service;

import model.Command;

/**
 * A small fixed-size queue shared by a producer thread and a consumer thread.
 * put() waits while the buffer is full; take() waits while it is empty. Each
 * side calls notifyAll() after changing the buffer to wake the other side.
 */
public class TransactionBuffer {
    private final Command[] items;
    private final Object lock = new Object();
    private int head;      // next slot to take from
    private int tail;      // next slot to put into
    private int count;

    public TransactionBuffer(int capacity) {
        items = new Command[capacity];
    }

    public void put(Command command) throws InterruptedException {
        synchronized (lock) {
            while (count == items.length) {
                lock.wait();                  // full: release the lock and wait for a take()
            }
            items[tail] = command;
            tail = (tail + 1) % items.length;
            count++;
            lock.notifyAll();
        }
    }

    public Command take() throws InterruptedException {
        synchronized (lock) {
            while (count == 0) {
                lock.wait();                  // empty: release the lock and wait for a put()
            }
            Command command = items[head];
            items[head] = null;
            head = (head + 1) % items.length;
            count--;
            lock.notifyAll();
            return command;
        }
    }

    public int capacity() {
        return items.length;
    }
}

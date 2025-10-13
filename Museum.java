// Compile: javac Museum.java
// Run:     java Museum

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

// ----------------- DOOR -----------------
class Door {
    private boolean isOpen = false;
    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();
    private String name;

    public Door(String name) {
        this.name = name;
    }

    public void open() {
        lock.lock();
        try {
            isOpen = true;
            System.out.println(name + " opened.");
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void close() {
        lock.lock();
        try {
            isOpen = false;
            System.out.println(name + " closed.");
        } finally {
            lock.unlock();
        }
    }

    public void cross(String action) throws InterruptedException {
        lock.lock();
        try {
            while (!isOpen) {
                condition.await();
            }
            System.out.println(action + " through " + name + ".");
        } finally {
            lock.unlock();
        }
    }
}

// ----------------- DIRECTOR -----------------
class Director implements Runnable {
    private Door eastDoor;
    private Door westDoor;
    private int openDuration;

    public Director(Door eastDoor, Door westDoor, int openDuration) {
        this.eastDoor = eastDoor;
        this.westDoor = westDoor;
        this.openDuration = openDuration;
    }

    public void run() {
        try {
            System.out.println("Director: Museum is opening.");
            eastDoor.open(); // allow entries
            westDoor.open(); // allow exits
            Thread.sleep(openDuration); // museum open for some time
            System.out.println("Director: Museum is closing to new entries.");
            eastDoor.close(); // close entry only
            // West door stays open until museum is empty
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// ----------------- CONTROL -----------------
class Control {
    private int peopleCount = 0;
    private Lock lock = new ReentrantLock();
    private Condition emptyCondition = lock.newCondition();

    public void enter() {
        lock.lock();
        try {
            peopleCount++;
            System.out.println("Person entered. People in museum: " + peopleCount);
        } finally {
            lock.unlock();
        }
    }

    public void exit() {
        lock.lock();
        try {
            peopleCount--;
            System.out.println("Person left. People in museum: " + peopleCount);
            if (peopleCount == 0) {
                System.out.println("Museum is empty.");
                emptyCondition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    public void waitUntilEmpty() throws InterruptedException {
        lock.lock();
        try {
            while (peopleCount > 0) {
                emptyCondition.await();
            }
        } finally {
            lock.unlock();
        }
    }
}

// ----------------- MAIN -----------------
public class Museum {
    public static void main(String[] args) {
        Door eastDoor = new Door("East door");
        Door westDoor = new Door("West door");
        Control control = new Control();

        // Director opens and closes museum
        Thread directorThread = new Thread(new Director(eastDoor, westDoor, 5000));
        directorThread.start();

        // Simulate visitors
        for (int i = 0; i < 10; i++) {
            int id = i + 1;
            new Thread(() -> {
                try {
                    eastDoor.cross("Visitor " + id + " entered");
                    control.enter();

                    Thread.sleep((long) (Math.random() * 1000)); // time inside museum

                    westDoor.cross("Visitor " + id + " exited");
                    control.exit();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}

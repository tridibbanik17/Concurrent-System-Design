// Compile: javac Servant.java
// Run:     java Servant

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Philosopher implements Runnable {
    private int id;
    private Lock lock;
    private Condition cookieCondition;
    private Condition colaCondition;
    private int[] cookies;
    private int[] cola;

    public Philosopher(int id, Lock lock, Condition cookieCondition, Condition colaCondition, int[] cookies, int[] cola) {
        this.id = id;
        this.lock = lock;
        this.cookieCondition = cookieCondition;
        this.colaCondition = colaCondition;
        this.cookies = cookies;
        this.cola = cola;
    }

    @Override
    public void run() {
        try {
            while (true) {
                think();
                getCookie();
                eatCookie();
                getCola();
                drinkCola();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Philosopher " + id + " was interrupted.");
        }
    }

    private void think() throws InterruptedException {
        System.out.println("Philosopher " + id + " is thinking...");
        Thread.sleep((long) (Math.random() * 1000));
    }

    private void getCookie() throws InterruptedException {
        lock.lock();
        try {
            while (cookies[0] == 0) {
                System.out.println("Philosopher " + id + " waits for cookies...");
                cookieCondition.await();
            }
            cookies[0]--;
            System.out.println("Philosopher " + id + " got a cookie. Cookies left: " + cookies[0]);
        } finally {
            lock.unlock();
        }
    }

    private void eatCookie() throws InterruptedException {
        System.out.println("Philosopher " + id + " is eating a cookie...");
        Thread.sleep((long) (Math.random() * 1000));
    }

    private void getCola() throws InterruptedException {
        lock.lock();
        try {
            while (cola[0] == 0) {
                System.out.println("Philosopher " + id + " waits for cola...");
                colaCondition.await();
            }
            cola[0]--;
            System.out.println("Philosopher " + id + " got a cola. Cola left: " + cola[0]);
        } finally {
            lock.unlock();
        }
    }

    private void drinkCola() throws InterruptedException {
        System.out.println("Philosopher " + id + " is drinking a cola...");
        Thread.sleep((long) (Math.random() * 1000));
    }
}

class Servant implements Runnable {
    private Lock lock;
    private Condition cookieCondition;
    private Condition colaCondition;
    private int[] cookies;
    private int[] cola;
    private static final int MAX_COOKIES = 3;
    private static final int MAX_COLA = 2;

    public Servant(Lock lock, Condition cookieCondition, Condition colaCondition, int[] cookies, int[] cola) {
        this.lock = lock;
        this.cookieCondition = cookieCondition;
        this.colaCondition = colaCondition;
        this.cookies = cookies;
        this.cola = cola;
    }

    @Override
    public void run() {
        try {
            while (true) {
                refillCookies();
                refillCola();
                Thread.sleep(500); // small delay between refills
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Servant was interrupted.");
        }
    }

    private void refillCookies() throws InterruptedException {
        lock.lock();
        try {
            if (cookies[0] == 0) {
                cookies[0] = MAX_COOKIES;
                System.out.println("Servant refilled cookies. Cookies now: " + cookies[0]);
                cookieCondition.signalAll();
            }
        } finally {
            lock.unlock();
        }
        Thread.sleep((long) (Math.random() * 800));
    }

    private void refillCola() throws InterruptedException {
        lock.lock();
        try {
            if (cola[0] == 0) {
                cola[0] = MAX_COLA;
                System.out.println("Servant refilled cola. Cola now: " + cola[0]);
                colaCondition.signalAll();
            }
        } finally {
            lock.unlock();
        }
        Thread.sleep((long) (Math.random() * 800));
    }

    public static void main(String[] args) {
        Lock lock = new ReentrantLock();
        Condition cookieCondition = lock.newCondition();
        Condition colaCondition = lock.newCondition();
        int[] cookies = {3};
        int[] cola = {2};

        // Start philosophers
        Thread[] philosophers = new Thread[3];
        for (int i = 0; i < philosophers.length; i++) {
            philosophers[i] = new Thread(new Philosopher(i + 1, lock, cookieCondition, colaCondition, cookies, cola));
            philosophers[i].start();
        }

        // Start servant
        Thread servant = new Thread(new Servant(lock, cookieCondition, colaCondition, cookies, cola));
        servant.start();
    }
}

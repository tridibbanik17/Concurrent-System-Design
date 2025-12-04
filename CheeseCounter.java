// CheeseCounter.java
// Compile: javac CheeseCounter.java
// Run:     java CheeseCounter

// ==========================================================
//   Java implementation of Question 4 (Ticket Machine Model)
//   Fully consistent with the FSP model from the assignment
//   Each customer is a thread that:
//      1. Obtains a ticket
//      2. Waits until its ticket is served
//      3. Gets cheese
//      4. Terminates
// ===========================================================

class TicketMachine {
    private static final int MT = 4;     // max ticket number
    private int next = 0;                // next ticket to issue

    // Issue next ticket modulo MT
    public synchronized int getTicket() {
        next = next % MT + 1;
        System.out.println(Thread.currentThread().getName() +
                " received ticket " + next);
        return next;
    }
}

class Counter {
    private static final int MT = 4;     // max ticket number
    private int current = 1;             // ticket currently being served

    // Customer waits until their ticket number is shown
    public synchronized void getCheese(int myTicket) throws InterruptedException {
        while (myTicket != current) {
            wait(); // wait for your turn
        }

        // Serve the customer
        System.out.println("Serving ticket " + myTicket +
                " (" + Thread.currentThread().getName() + ")");

        // Move to next ticket
        current = current % MT + 1;

        // Let waiting customers re-check
        notifyAll();
    }
}

class Customer extends Thread {
    private final TicketMachine ticketMachine;
    private final Counter counter;

    public Customer(String name, TicketMachine tm, Counter c) {
        super(name);
        this.ticketMachine = tm;
        this.counter = c;
    }

    public void run() {
        try {
            // Step 1: take a ticket
            int myTicket = ticketMachine.getTicket();

            // Step 2: wait until the counter displays your ticket
            counter.getCheese(myTicket);

            // Step 3: Finished
            System.out.println(getName() + " received cheese and left.");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

public class CheeseCounter {
    public static void main(String[] args) throws InterruptedException {

        TicketMachine tm = new TicketMachine();
        Counter counter = new Counter();

        // FSP model has 2 Bold + 2 Meek customers (example)
        Thread c1 = new Customer("Bold1", tm, counter);
        Thread c2 = new Customer("Bold2", tm, counter);
        Thread c3 = new Customer("Meek1", tm, counter);
        Thread c4 = new Customer("Meek2", tm, counter);

        // Start all customers
        c1.start();
        c2.start();
        c3.start();
        c4.start();

        // Wait for all customers to finish
        c1.join();
        c2.join();
        c3.join();
        c4.join();

        System.out.println("\nAll customers have been served.");
    }
}

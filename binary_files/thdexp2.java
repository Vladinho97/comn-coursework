import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

public class thdexp2 {
    public static int count = 0;
    public static int interval = 40;

    public static Timer timer;
    public static boolean bTimeout = true;

    public static Object lock = new Object();
    
    static MyThread mythd;

    public static class Timeout extends TimerTask {
        public void run() {
            synchronized (lock) {
                bTimeout = true;
                System.out.println ("Timeout");
                if (count < 3) {
                    count = 0;
                }
                else {
                    System.out.println ("Too slow... Let's wake up MyThread.");
                    lock.notify();
                }
            }
        }
    }

    private static class MyThread implements Runnable {
        public void run() {
            while (true) {
                synchronized (lock) {
                    if (count > 10) break;

                        System.out.println ("MyThread: " + count++);
                
                    while (count < 3) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {}
                    }
                }
                try {
                    Thread.sleep (100);
                } catch (InterruptedException e) {
                    System.out.println("Exception in thread");
                }
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println ("Starting Main Thread...");

        timer = new Timer();
            
        mythd = new MyThread();
        Thread thd = new Thread (mythd);
        thd.start();

        while (true) {
            synchronized (lock) {
                if (count > 10) break;

                System.out.println ("MainThread: " + count++);

                if (bTimeout) {
                    timer.schedule (new Timeout(), interval);
                    interval *= 2;
                    bTimeout = false;
                }
            }
            try {
                Thread.sleep (100);
            } catch (InterruptedException e) {
                System.out.println("Exception in main thread");
            }
        }
        if (bTimeout == false)
            timer.cancel();
    }
}

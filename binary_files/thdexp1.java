import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

public class thdexp1 {
    public static int count = 0;
    
    private static class MyThread implements Runnable {
        public void run() {
            while (count <= 10) {
                System.out.println ("MyThread: " + count++);
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
        MyThread mythd = new MyThread();
        Thread t = new Thread (mythd);
        t.start();
        while (count <= 10) {
            System.out.println ("MainThread: " + count++);
            try {
                Thread.sleep (100);
            } catch (InterruptedException e) {
                System.out.println("Exception in main thread");
            }
        }
    }
}

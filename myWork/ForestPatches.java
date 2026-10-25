import java.util.Deque;
import java.util.ArrayDeque;

public class PeakMonitor {


    public static int[] brute(int[] readings, int k) {
        int n = readings.length;
        int[] result = new int[n - k + 1];

        for (int i = 0; i <= n - k; i++) {
            int max = readings[i];
            for (int j = i + 1; j < i + k; j++) {
                if (readings[j] > max) {
                    max = readings[j];
                }
            }
            result[i] = max;
        }

        return result;
    }


    public static int[] optimized(int[] readings, int k) {
        int n = readings.length;
        int[] result = new int[n - k + 1];
        

        Deque<Integer> deque = new ArrayDeque<>();

        for (int i = 0; i < n; i++) {
            while (!deque.isEmpty() && deque.peekFirst() <= i - k) {
                deque.pollFirst();
            }

            while (!deque.isEmpty() && readings[deque.peekLast()] <= readings[i]) {
                deque.pollLast();
            }


            deque.offerLast(i);


            if (i >= k - 1) {
                result[i - k + 1] = readings[deque.peekFirst()];
            }
        }

        return result;
    }
}
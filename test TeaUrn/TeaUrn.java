import java.util.Arrays;

public class TeaUrn {

    public static int brute(int[] orders) {
        int a = orders[0];
        int b = orders[1];
        int c = orders[2];

        int minutes = 0;

        while (a + b + c > 0) {
            int[] x = {a, b, c};
            Arrays.sort(x); // 

            if (x[1] > 0) {

                x[2]--;
                x[1]--;
            } else {
                x[2]--;
            }

            a = x[0];
            b = x[1];
            c = x[2];

            minutes++;
        }

        return minutes;
    }

    public static int optimized(int[] orders) {
        int sum = orders[0] + orders[1] + orders[2];
                int max = Math.max(orders[0], Math.max(orders[1], orders[2]));

                return Math.max(max, (sum + 1) / 2);
    }
}
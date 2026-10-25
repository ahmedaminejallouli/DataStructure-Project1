import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CS341 Tiered Array Problem Set: test runner.
 *
 * Put t    his file, the tests/ folder, and your solution files in one folder, then:
 *
 *   javac *.java
 *   java TestRunner              (tests every problem it can find)
 *   java TestRunner 1B 3C        (tests only the problems you list)
 *
 * Run "java TestRunner --help" for every option. See README.md for details.
 * Works with Java 8 and newer. No libraries needed.
 */
public class TestRunner {

    // ------------------------------------------------------------------
    // The nine problems: id, title, class name, parameter types.
    // ------------------------------------------------------------------
    static final String[][] PROBLEMS = {
        {"1A", "Tea Urn Rush",          "TeaUrn"},
        {"1B", "Two Floodwalls",        "TwoFloodwalls"},
        {"1C", "Rooftop Puddles",       "RooftopPuddles"},
        {"2A", "Fencing the Pasture",   "FencingPasture"},
        {"2B", "Forest Patches",        "ForestPatches"},
        {"2C", "Reclaim One Plot",      "ReclaimPlot"},
        {"3A", "Warmest Stretch",       "WarmestStretch"},
        {"3B", "Food Truck's Best Run", "BestRun"},
        {"3C", "Rolling Peak Monitor",  "PeakMonitor"},
    };

    static Class<?>[] paramTypes(String id) {
        switch (id) {
            case "2A": case "2B": case "2C": return new Class<?>[] {int[][].class};
            case "3A": case "3C":            return new Class<?>[] {int[].class, int.class};
            default:                         return new Class<?>[] {int[].class};
        }
    }

    static String signature(String id) {
        switch (id) {
            case "1A": return "public static int %s(int[] orders)";
            case "1B": return "public static int %s(int[] walls)";
            case "1C": return "public static int %s(int[] heights)";
            case "2A": return "public static int %s(int[][] land)";
            case "2B": return "public static int %s(int[][] map)";
            case "2C": return "public static int %s(int[][] city)";
            case "3A": return "public static double %s(int[] temps, int k)";
            case "3B": return "public static int %s(int[] profit)";
            default:   return "public static int[] %s(int[] readings, int k)";
        }
    }

    // ------------------------------------------------------------------
    // Options
    // ------------------------------------------------------------------
    static String testDir = "tests";
    static long timeoutMs = 3000;
    static int stackMb = 512;
    static boolean verbose = false;
    static boolean bruteLarge = false;
    static boolean runBrute = true, runOptimized = true;
    static String csvPath = null;
    static String label = "";
    static String pkg = "";
    static int maxFailuresShown = 5;
    static long graceMs = 30000;   // how long to let a timed-out call finish before moving on

    static class Case {
        String name, tag, file;
        int[] arr;       // 1D input
        int[][] grid;    // 2D input
        int k;           // window size, 3A and 3C
        String expect;   // raw expected value
        boolean large() { return "large".equals(tag); }
    }

    static class Outcome {
        String status;   // PASS, FAIL, ERROR, TIMEOUT
        String got = "";
        long millis;
        Thread thread;
    }

    public static void main(String[] args) throws Exception {
        List<String> wanted = new ArrayList<String>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("--help") || a.equals("-h")) { usage(); return; }
            else if (a.equals("--verbose") || a.equals("-v")) verbose = true;
            else if (a.equals("--brute")) { runBrute = true; runOptimized = false; }
            else if (a.equals("--optimized")) { runBrute = false; runOptimized = true; }
            else if (a.equals("--brute-large")) bruteLarge = true;
            else if (a.equals("--tests")) testDir = args[++i];
            else if (a.equals("--timeout")) timeoutMs = Long.parseLong(args[++i]);
            else if (a.equals("--stack")) stackMb = Integer.parseInt(args[++i]);
            else if (a.equals("--csv")) csvPath = args[++i];
            else if (a.equals("--label")) label = args[++i];
            else if (a.equals("--package")) pkg = args[++i] + ".";
            else if (a.equals("--show")) maxFailuresShown = Integer.parseInt(args[++i]);
            else if (a.equals("--grace")) graceMs = Long.parseLong(args[++i]) * 1000;
            else if (a.matches("[1-3][A-Ca-c]")) wanted.add(a.toUpperCase());
            else { System.out.println("Unknown option: " + a); usage(); System.exit(2); }
        }
        if (!new File(testDir).isDirectory()) {
            System.out.println("Cannot find the test folder '" + testDir + "'. Run from the folder that contains it, or pass --tests <folder>.");
            System.exit(2);
        }

        List<String[]> summary = new ArrayList<String[]>();
        boolean allPassed = true;
        int found = 0;

        for (String[] p : PROBLEMS) {
            String id = p[0], title = p[1], cls = p[2];
            if (!wanted.isEmpty() && !wanted.contains(id)) continue;

            Class<?> c;
            try {
                c = Class.forName(pkg + cls);
            } catch (ClassNotFoundException e) {
                if (!wanted.isEmpty()) {
                    System.out.println("\n== " + id + " " + title + " ==");
                    System.out.println("  " + cls + ".java was not found (or not compiled). Skipping.");
                    summary.add(new String[] {id, "not found", "not found"});
                    allPassed = false;
                }
                continue;
            }
            found++;
            System.out.println("\n== " + id + " " + title + " (" + cls + ".java) ==");
            List<Case> cases = loadCases(id);
            String[] row = {id, "-", "-"};
            String[] variants = {"brute", "optimized"};
            for (int v = 0; v < 2; v++) {
                String variant = variants[v];
                if (variant.equals("brute") && !runBrute) continue;
                if (variant.equals("optimized") && !runOptimized) continue;
                Method m;
                try {
                    m = c.getMethod(variant, paramTypes(id));
                } catch (NoSuchMethodException e) {
                    System.out.printf("  %-10s MISSING  expected: %s%n", variant, String.format(signature(id), variant));
                    row[v + 1] = "missing";
                    allPassed = false;
                    continue;
                }
                String res = runVariant(id, variant, m, cases);
                row[v + 1] = res;
                if (!res.startsWith("ok")) allPassed = false;
            }
            summary.add(row);
        }

        if (found == 0 && wanted.isEmpty()) {
            System.out.println("No solution classes found. Expected one or more of:");
            for (String[] p : PROBLEMS) System.out.println("  " + p[2] + ".java  (" + p[0] + " " + p[1] + ")");
            System.out.println("Did you run 'javac *.java' in this folder first?");
            System.exit(2);
        }

        System.out.println("\n================ SUMMARY ================");
        System.out.printf("%-8s %-16s %-16s%n", "problem", "brute", "optimized");
        for (String[] r : summary)
            System.out.printf("%-8s %-16s %-16s%n", r[0], r[1].replace("ok ", ""), r[2].replace("ok ", ""));
        System.out.println(allPassed ? "\nAll tests passed." : "\nSome tests did not pass. Scroll up for details.");
        System.exit(allPassed ? 0 : 1);
    }

    static void usage() {
        System.out.println("Usage: java TestRunner [problems] [options]");
        System.out.println("  problems          any of 1A 1B 1C 2A 2B 2C 3A 3B 3C (default: every class found)");
        System.out.println("  --brute           test only the brute() method");
        System.out.println("  --optimized       test only the optimized() method");
        System.out.println("  --verbose, -v     print every case, not just failures");
        System.out.println("  --show N          print at most N failure details per method (default 5)");
        System.out.println("  --timeout MS      time limit per case in milliseconds (default 3000)");
        System.out.println("  --grace SEC       after a timeout, wait this long for the call to finish (default 30)");
        System.out.println("  --brute-large     also run brute() on the large inputs (skipped by default)");
        System.out.println("  --stack MB        stack size for each call (default 512; use 1 to mimic a default JVM)");
        System.out.println("  --tests DIR       folder with the test files (default: tests)");
        System.out.println("  --package NAME    if your classes are inside a package");
        System.out.println("  --csv FILE        append result rows to FILE (for batch grading)");
        System.out.println("  --label NAME      name written in the CSV rows (e.g. a student id)");
    }

    // ------------------------------------------------------------------
    // Running one method over all cases
    // ------------------------------------------------------------------
    static String runVariant(String id, String variant, Method m, List<Case> cases) throws IOException {
        int passed = 0, total = 0, fails = 0, errors = 0, timeouts = 0, skipped = 0, shown = 0;
        long slowestLarge = -1;
        boolean timedOut = false;
        for (Case tc : cases) {
            if (tc.large() && variant.equals("brute") && !bruteLarge) { skipped++; continue; }
            total++;
            if (tc.large() && timedOut) {
                // An earlier case is still running in the background; timing more large cases would be unfair.
                timeouts++;
                System.out.printf("    [SKIP]    %s (an earlier call is still running, counted as timed out)%n", tc.name);
                continue;
            }
            Outcome o = runCase(id, m, tc);
            if (tc.large() && !o.status.equals("TIMEOUT")) slowestLarge = Math.max(slowestLarge, o.millis);
            if (o.status.equals("PASS")) {
                passed++;
                if (verbose) System.out.printf("    [PASS]    %s (%d ms)%n", tc.name, o.millis);
                continue;
            }
            if (o.status.equals("TIMEOUT")) {
                timeouts++;
                // Java cannot safely kill the call, so give it time to finish; otherwise it would
                // slow down every case after it. If it is still running, later large cases are skipped.
                System.out.println("    (a call timed out; waiting up to " + graceMs / 1000 + " s for it to finish before continuing)");
                try { o.thread.join(graceMs); } catch (InterruptedException e) { /* ignore */ }
                if (o.thread.isAlive()) timedOut = true;
            }
            else if (o.status.equals("ERROR")) errors++;
            else fails++;
            if (verbose || shown < maxFailuresShown) {
                shown++;
                System.out.printf("    [%s] %s: \"%s\" (%s, %s)%n", o.status, variant, tc.name, tc.tag, tc.file);
                System.out.println("        input:    " + describeInput(id, tc));
                if (!o.status.equals("TIMEOUT") && !o.status.equals("ERROR"))
                    System.out.println("        expected: " + shorten(tc.expect));
                System.out.println("        got:      " + shorten(o.got));
            }
        }
        int notShown = fails + errors + timeouts - shown;
        if (notShown > 0) System.out.println("    ... " + notShown + " more problem case(s) not shown (use --show N or --verbose)");

        StringBuilder line = new StringBuilder();
        line.append(String.format("  %-10s %d/%d passed", variant, passed, total));
        if (fails > 0) line.append(", ").append(fails).append(" wrong");
        if (errors > 0) line.append(", ").append(errors).append(" crashed");
        if (timeouts > 0) line.append(", ").append(timeouts).append(" timed out");
        if (skipped > 0) line.append("  (").append(skipped).append(" large case(s) skipped for brute force)");
        if (slowestLarge >= 0) line.append("  slowest large case: ").append(slowestLarge).append(" ms");
        System.out.println(line);

        if (csvPath != null) {
            PrintWriter w = new PrintWriter(new FileWriter(csvPath, true));
            w.printf("%s,%s,%s,%d,%d,%d,%d,%d%n", csv(label), id, variant, passed, total, fails, errors, timeouts);
            w.close();
        }
        return (passed == total ? "ok " : "") + passed + "/" + total;
    }

    static Outcome runCase(final String id, final Method m, final Case tc) {
        final Outcome o = new Outcome();
        final Object[] result = new Object[1];
        final Throwable[] thrown = new Throwable[1];
        // Each call gets a fresh copy of the input, so a solution that modifies
        // its input cannot affect the next call.
        final Object[] callArgs = buildArgs(id, tc);
        Thread t = new Thread(null, new Runnable() {
            public void run() {
                try {
                    result[0] = m.invoke(null, callArgs);
                } catch (InvocationTargetException e) {
                    thrown[0] = e.getCause();
                } catch (Throwable e) {
                    thrown[0] = e;
                }
            }
        }, "case", (long) stackMb * 1024 * 1024);
        t.setDaemon(true);
        o.thread = t;
        long start = System.nanoTime();
        t.start();
        try { t.join(timeoutMs); } catch (InterruptedException e) { /* ignore */ }
        o.millis = (System.nanoTime() - start) / 1000000;
        if (t.isAlive()) {
            o.status = "TIMEOUT";
            o.got = "no answer within " + timeoutMs + " ms";
        } else if (thrown[0] != null) {
            o.status = "ERROR";
            o.got = thrown[0].getClass().getSimpleName()
                    + (thrown[0].getMessage() != null ? ": " + thrown[0].getMessage() : "")
                    + where(thrown[0]);
        } else {
            o.got = format(result[0]);
            o.status = matches(id, result[0], tc.expect) ? "PASS" : "FAIL";
        }
        return o;
    }

    static Object[] buildArgs(String id, Case tc) {
        if (tc.grid != null) {
            int[][] g = new int[tc.grid.length][];
            for (int i = 0; i < g.length; i++) g[i] = tc.grid[i].clone();
            return new Object[] {g};
        }
        if (id.equals("3A") || id.equals("3C")) return new Object[] {tc.arr.clone(), tc.k};
        return new Object[] {tc.arr.clone()};
    }

    static boolean matches(String id, Object got, String expect) {
        if (got == null) return false;
        if (id.equals("3A")) {
            double g = (Double) got, e = Double.parseDouble(expect);
            return Math.abs(g - e) <= 1e-6 * Math.max(1.0, Math.abs(e));
        }
        if (id.equals("3C")) return Arrays.equals((int[]) got, parseInts(expect));
        return ((Integer) got).intValue() == Integer.parseInt(expect.trim());
    }

    static String format(Object o) {
        if (o == null) return "null";
        if (o instanceof int[]) {
            int[] a = (int[]) o;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < a.length; i++) { if (i > 0) sb.append(' '); sb.append(a[i]); }
            return sb.toString() + "   (length " + a.length + ")";
        }
        return String.valueOf(o);
    }

    static String where(Throwable t) {
        for (StackTraceElement e : t.getStackTrace()) {
            String c = e.getClassName();
            if (!c.startsWith("java.") && !c.startsWith("jdk.") && !c.startsWith("sun.") && !c.equals("TestRunner") && !c.startsWith("TestRunner$"))
                return "  at " + e.getFileName() + ":" + e.getLineNumber();
        }
        return "";
    }

    static String describeInput(String id, Case tc) {
        if (tc.grid != null) {
            int r = tc.grid.length, c = tc.grid[0].length;
            if (r * c > 64) return r + "x" + c + " grid (see the test file)";
            StringBuilder sb = new StringBuilder();
            for (int[] row : tc.grid) {
                if (sb.length() > 0) sb.append(" / ");
                for (int x : row) sb.append(x);
            }
            return r + "x" + c + " grid, rows: " + sb;
        }
        String s = "[" + shorten(joinInts(tc.arr)) + "] (n = " + tc.arr.length + ")";
        if (id.equals("3A") || id.equals("3C")) s += ", k = " + tc.k;
        return s;
    }

    static String joinInts(int[] a) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length && sb.length() < 200; i++) { if (i > 0) sb.append(", "); sb.append(a[i]); }
        return sb.toString();
    }

    static String shorten(String s) {
        return s.length() <= 100 ? s : s.substring(0, 100) + " ...";
    }

    static String csv(String s) {
        return s.contains(",") || s.contains("\"") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    // ------------------------------------------------------------------
    // Reading the test files
    // ------------------------------------------------------------------
    static List<Case> loadCases(String id) throws IOException {
        // Every file whose name starts with the problem id, e.g. 3B.txt, 3B_large.txt, 3B_extra.txt
        List<Case> all = new ArrayList<Case>();
        File[] files = new File(testDir).listFiles();
        if (files == null) files = new File[0];
        Arrays.sort(files);
        for (File f : files) {
            String n = f.getName();
            if (f.isFile() && n.endsWith(".txt") && n.toUpperCase().startsWith(id)) all.addAll(parseFile(id, f));
        }
        if (all.isEmpty()) System.out.println("  (no test files found for " + id + " in " + testDir + ")");
        return all;
    }

    static List<Case> parseFile(String id, File f) throws IOException {
        List<Case> cases = new ArrayList<Case>();
        BufferedReader in = new BufferedReader(new FileReader(f));
        Case cur = null;
        String line;
        int lineNo = 0;
        while ((line = in.readLine()) != null) {
            lineNo++;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int colon = line.indexOf(':');
            if (colon < 0) throw new IOException(f + ":" + lineNo + ": expected 'key: value'");
            String key = line.substring(0, colon).trim(), val = line.substring(colon + 1).trim();
            if (key.equals("case")) {
                cur = new Case();
                cur.name = val;
                cur.tag = "basic";
                cur.file = f.getName();
                cases.add(cur);
            } else if (cur == null) {
                throw new IOException(f + ":" + lineNo + ": 'case:' must come first");
            } else if (key.equals("tags")) {
                cur.tag = val;
            } else if (key.equals("k")) {
                cur.k = Integer.parseInt(val);
            } else if (key.equals("expect")) {
                cur.expect = val;
            } else if (key.equals("grid")) {
                int[] rc = parseInts(val);
                cur.grid = new int[rc[0]][];
                for (int r = 0; r < rc[0]; r++) {
                    String row = in.readLine();
                    lineNo++;
                    cur.grid[r] = parseInts(row);
                    if (cur.grid[r].length != rc[1]) throw new IOException(f + ":" + lineNo + ": grid row has the wrong length");
                }
            } else {
                cur.arr = parseInts(val);   // orders, walls, heights, temps, profit, readings
            }
        }
        in.close();
        return cases;
    }

    static int[] parseInts(String s) {
        s = s.trim();
        if (s.isEmpty()) return new int[0];
        String[] parts = s.split("\\s+");
        int[] a = new int[parts.length];
        for (int i = 0; i < parts.length; i++) a[i] = Integer.parseInt(parts[i]);
        return a;
    }
}

# CS341 Tiered Array Problem Set: How to Test Your Solutions

This kit lets you check your Java solutions against the course test cases. It needs only a JDK (Java 8 or newer).

### Step 1: set up one folder

From `starter/`, copy the files for **your three
problems** into the same folder as `TestRunner.java` and `tests/`:

```
my-project/
  TestRunner.java
  tests/
  TeaUrn.java        <- your problems only
  BestRun.java
  PeakMonitor.java
```

### Step 2: write both methods in each file

Each problem is one class with two methods, `brute` and `optimized`. The runner
finds them by name, so **do not rename the class, the methods, or change the
parameter types**. You can add as many helper methods as you like.

| Problem | File | Both methods look like |
|---|---|---|
| 1A Tea Urn Rush | `TeaUrn.java` | `public static int brute(int[] orders)` |
| 1B Two Floodwalls | `TwoFloodwalls.java` | `public static int brute(int[] walls)` |
| 1C Rooftop Puddles | `RooftopPuddles.java` | `public static int brute(int[] heights)` |
| 2A Fencing the Pasture | `FencingPasture.java` | `public static int brute(int[][] land)` |
| 2B Forest Patches | `ForestPatches.java` | `public static int brute(int[][] map)` |
| 2C Reclaim One Plot | `ReclaimPlot.java` | `public static int brute(int[][] city)` |
| 3A Warmest Stretch | `WarmestStretch.java` | `public static double brute(int[] temps, int k)` |
| 3B Food Truck's Best Run | `BestRun.java` | `public static int brute(int[] profit)` |
| 3C Rolling Peak Monitor | `PeakMonitor.java` | `public static int[] brute(int[] readings, int k)` |

`optimized` has the same signature with the name `optimized`.

### Step 3: compile and run

Open a terminal in the folder and run:

```
javac *.java
java TestRunner
```

That tests every problem it finds. To test only some problems or one method:

```
java TestRunner 3C                 only problem 3C
java TestRunner 1A 3B --optimized  only the optimized methods of 1A and 3B
java TestRunner 2B --brute -v      only brute for 2B, printing every case
```

**Using an IDE (IntelliJ, Eclipse, VS Code):** put the files in the default
package (no `package` line), run `TestRunner`'s `main` method, and set the
working directory to the folder that contains `tests/`. If your files must stay
in a package, run `java TestRunner --package yourpackage`.

### Reading the output

```
== 3B Food Truck's Best Run (BestRun.java) ==
    [FAIL] optimized: "all negative, answer is the least bad day" (edge, 3B.txt)
        input:    [-3, -1, -7] (n = 3)
        expected: -1
        got:      0
  brute      45/45 passed  (3 large case(s) skipped for brute force)
  optimized  47/48 passed, 1 wrong  slowest large case: 2 ms
```

Each failure shows the case name, which says what edge case it tests, followed by
the input, the expected answer and your answer. You can find the full case in the
named test file by searching for its name. A result can be one of:

- **FAIL**: your method returned the wrong answer.
- **ERROR**: your method threw an exception. The runner shows the exception and
  the line in your file where it happened.
- **TIMEOUT**: your method took longer than 3 seconds. On the large inputs this
  almost always means the "optimized" solution is not actually optimized.

### How the runner treats your code

- **Brute force is not run on the large inputs** (`large` tag), since a correct
  O(n²) solution would take minutes. Add `--brute-large` to try it anyway.
- **Every call gets a fresh copy of the input**, so modifying the array or grid
  you are given is safe and won't affect the next case.
- **Recursion depth:** each call runs with a large stack (512 MB), so a
  recursive flood fill on a 300×300 grid will not crash. To see how your code
  behaves on a normal JVM stack, add `--stack 1`.
- **3A** answers are compared to within 0.000001, so any correct way of
  computing the average passes. Integer division, such as `sum / k` with two
  `int`s, does not pass.
- **3C** must return an array of exactly `n - k + 1` values.

### All options

| Option | Meaning |
|---|---|
| `1A 3C ...` | test only these problems (default: every class found) |
| `--brute` / `--optimized` | test only one of the two methods |
| `-v`, `--verbose` | print every case, including passes |
| `--show N` | show at most N failure details per method (default 5) |
| `--timeout MS` | time limit per case in milliseconds (default 3000) |
| `--grace SEC` | after a timeout, how long to let that call finish before moving on (default 30) |
| `--brute-large` | also run `brute` on the large inputs |
| `--stack MB` | stack size per call in MB (default 512) |
| `--tests DIR` | where the test files are (default `tests`) |
| `--package NAME` | your classes are in package NAME |
| `--csv FILE`, `--label NAME` | append results to a CSV file (used by the grading script) |

The runner exits with status 0 when everything passes and 1 otherwise, so it
also works in scripts and autograders.

### Test file format

The test files are plain text. Blank lines and lines starting with `#` are
ignored. Each case looks like this:

```
case: all negative, answer is the least bad day
tags: edge
profit: -3 -1 -7
expect: -1
```

Grids give the size, then one row per line:

```
case: flip joins four separate cells
tags: edge
grid: 3 3
0 1 0
1 0 1
0 1 0
expect: 5
```

| Problem | Input lines | `expect` |
|---|---|---|
| 1A | `orders: a b c` | an integer |
| 1B, 1C, 3B | `walls:` / `heights:` / `profit:` followed by the values | an integer |
| 2A, 2B, 2C | `grid: rows cols` and then the rows | an integer |
| 3A | `temps: ...` and `k: ...` | a decimal number |
| 3C | `readings: ...` and `k: ...` | the output values, separated by spaces |

You can add your own cases to a new file whose name starts with the problem id, for example `tests/3B_mine.txt`.

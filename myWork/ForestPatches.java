
public class ForestPatches {


    public static int brute(int[][] map) {
        if (map == null || map.length == 0) return 0;
        
        int rows = map.length;
        int cols = map[0].length;
        boolean[][] visited = new boolean[rows][cols];
        int patchCount = 0;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (map[r][c] == 1 && !visited[r][c]) {
                    patchCount++;
                    dfs(map, visited, r, c);
                }
            }
        }
        return patchCount;
    }
// apparently this is a fundamental algorithm used to walk through grids or graphs or trees, walking through all 4 directionally connected tree cells and marking them 
    private static void dfs(int[][] map, boolean[][] visited, int r, int c) {
        int rows = map.length;
        int cols = map[0].length;

        if (r < 0 || r >= rows || c < 0 || c >= cols || map[r][c] == 0 || visited[r][c]) {
            return;
        }

        visited[r][c] = true;

        dfs(map, visited, r - 1, c); 
        dfs(map, visited, r + 1, c); 
        dfs(map, visited, r, c - 1); 
        dfs(map, visited, r, c + 1); 
    }


    public static int optimized(int[][] map) {
        if (map == null || map.length == 0) return 0;
        
        int rows = map.length;
        int cols = map[0].length;
        
        int[] parent = new int[rows * cols];
        int[] rank = new int[rows * cols];
        int patchCount = 0;


        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (map[r][c] == 1) {
                    int id = r * cols + c;
                    parent[id] = id; 
                    rank[id] = 0;
                    patchCount++;    
                }
            }
        }

        int[] dr = {-1, 0}; 
        int[] dc = {0, -1}; 

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (map[r][c] == 1) {
                    int currentId = r * cols + c;

                    // Check Up and Left
                    for (int i = 0; i < 2; i++) {
                        int nextR = r + dr[i];
                        int nextC = c + dc[i];

                        if (nextR >= 0 && nextR < rows && nextC >= 0 && nextC < cols && map[nextR][nextC] == 1) {
                            int neighborId = nextR * cols + nextC;
                            
                            if (union(parent, rank, currentId, neighborId)) {
                                patchCount--;
                            }
                        }
                    }
                }
            }
        }
        return patchCount;
    }


    private static int find(int[] parent, int i) {
        if (parent[i] != i) {
            parent[i] = find(parent, parent[i]); // Path compression
        }
        return parent[i];
    }

    private static boolean union(int[] parent, int[] rank, int i, int j) {
        int rootI = find(parent, i);
        int rootJ = find(parent, j);
        
        if (rootI == rootJ) {
            return false; 
        }
        
        if (rank[rootI] < rank[rootJ]) {
            parent[rootI] = rootJ;
        } else if (rank[rootI] > rank[rootJ]) {
            parent[rootJ] = rootI;
        } else {
            parent[rootJ] = rootI;
            rank[rootI]++;
        }
        
        return true; 
    }
}
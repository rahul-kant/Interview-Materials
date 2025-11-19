# Data Structures & Algorithms - Complete Guide

## Overview
This guide covers all DSA topics required for FAANG interviews with problem patterns, templates, and practice recommendations.

---

## 1. Arrays & Strings

### Key Concepts
- **Time Complexity**: O(1) access, O(n) search
- **Java**: Arrays, ArrayList, StringBuilder
- **Common Operations**: Iteration, reversal, rotation

### Important Patterns

#### 1.1 Two Pointers
**Use Cases**: Remove duplicates, pair sum, palindrome check

**Template:**
```java
// Opposite direction
int left = 0, right = arr.length - 1;
while (left < right) {
    // Process arr[left] and arr[right]
    if (condition) {
        left++;
    } else {
        right--;
    }
}

// Same direction (fast & slow)
int slow = 0;
for (int fast = 0; fast < arr.length; fast++) {
    if (condition) {
        arr[slow++] = arr[fast];
    }
}
```

**Practice Problems:**
- Two Sum (LeetCode 1)
- Container With Most Water (11)
- 3Sum (15)
- Remove Duplicates from Sorted Array (26)
- Valid Palindrome (125)
- Move Zeroes (283)

---

#### 1.2 Sliding Window
**Use Cases**: Subarray/substring problems with contiguous elements

**Template:**
```java
// Fixed size window
int windowSum = 0;
for (int i = 0; i < k; i++) {
    windowSum += arr[i];
}
int maxSum = windowSum;

for (int i = k; i < arr.length; i++) {
    windowSum += arr[i] - arr[i - k];
    maxSum = Math.max(maxSum, windowSum);
}

// Variable size window
int left = 0, maxLength = 0;
Map<Character, Integer> map = new HashMap<>();

for (int right = 0; right < s.length(); right++) {
    // Expand window
    map.put(s.charAt(right), map.getOrDefault(s.charAt(right), 0) + 1);
    
    // Shrink window if needed
    while (condition_violated) {
        map.put(s.charAt(left), map.get(s.charAt(left)) - 1);
        left++;
    }
    
    maxLength = Math.max(maxLength, right - left + 1);
}
```

**Practice Problems:**
- Maximum Sum Subarray of Size K
- Longest Substring Without Repeating Characters (3)
- Minimum Window Substring (76)
- Longest Repeating Character Replacement (424)
- Permutation in String (567)
- Sliding Window Maximum (239)

---

#### 1.3 Prefix Sum
**Use Cases**: Range sum queries, subarray sum problems

**Template:**
```java
// Build prefix sum
int[] prefix = new int[arr.length + 1];
for (int i = 0; i < arr.length; i++) {
    prefix[i + 1] = prefix[i] + arr[i];
}

// Range sum [left, right]
int rangeSum = prefix[right + 1] - prefix[left];
```

**Practice Problems:**
- Range Sum Query (303)
- Subarray Sum Equals K (560)
- Contiguous Array (525)
- Product of Array Except Self (238)

---

## 2. HashMap & HashSet

### Key Concepts
- **Time Complexity**: O(1) average for insert, delete, search
- **Java**: HashMap, HashSet, LinkedHashMap, TreeMap
- **Collision Handling**: Chaining, Open Addressing

### Important Patterns

#### 2.1 Frequency Counter
**Template:**
```java
Map<Character, Integer> freq = new HashMap<>();
for (char c : s.toCharArray()) {
    freq.put(c, freq.getOrDefault(c, 0) + 1);
}
```

**Practice Problems:**
- Valid Anagram (242)
- Group Anagrams (49)
- Top K Frequent Elements (347)
- First Unique Character in a String (387)

#### 2.2 Index Mapping
**Template:**
```java
Map<Integer, Integer> indexMap = new HashMap<>();
for (int i = 0; i < arr.length; i++) {
    int complement = target - arr[i];
    if (indexMap.containsKey(complement)) {
        return new int[]{indexMap.get(complement), i};
    }
    indexMap.put(arr[i], i);
}
```

**Practice Problems:**
- Two Sum (1)
- 4Sum II (454)
- Longest Consecutive Sequence (128)
- Subarray Sum Equals K (560)

---

## 3. Linked Lists

### Key Concepts
- **Time Complexity**: O(1) insert/delete at known position, O(n) search
- **Java**: No built-in, implement Node class
- **Types**: Singly, Doubly, Circular

### Node Definition
```java
class ListNode {
    int val;
    ListNode next;
    ListNode(int val) { this.val = val; }
}
```

### Important Patterns

#### 3.1 Fast & Slow Pointers (Floyd's Cycle)
**Template:**
```java
ListNode slow = head, fast = head;
while (fast != null && fast.next != null) {
    slow = slow.next;
    fast = fast.next.next;
    
    // Detect cycle
    if (slow == fast) {
        return true;
    }
}
```

**Practice Problems:**
- Linked List Cycle (141)
- Linked List Cycle II (142)
- Middle of the Linked List (876)
- Happy Number (202)
- Find the Duplicate Number (287)

#### 3.2 Reversal
**Template:**
```java
ListNode reverse(ListNode head) {
    ListNode prev = null, curr = head;
    while (curr != null) {
        ListNode next = curr.next;
        curr.next = prev;
        prev = curr;
        curr = next;
    }
    return prev;
}
```

**Practice Problems:**
- Reverse Linked List (206)
- Reverse Linked List II (92)
- Reverse Nodes in k-Group (25)
- Palindrome Linked List (234)

#### 3.3 Merge & Split
**Practice Problems:**
- Merge Two Sorted Lists (21)
- Merge k Sorted Lists (23)
- Sort List (148)
- Remove Nth Node From End (19)

---

## 4. Stacks & Queues

### Key Concepts
- **Stack**: LIFO, Java Stack/ArrayDeque
- **Queue**: FIFO, Java Queue/LinkedList/ArrayDeque
- **Deque**: Double-ended queue

### Important Patterns

#### 4.1 Monotonic Stack
**Use Cases**: Next greater/smaller element

**Template:**
```java
int[] result = new int[arr.length];
Stack<Integer> stack = new Stack<>();

for (int i = arr.length - 1; i >= 0; i--) {
    // Pop smaller elements
    while (!stack.isEmpty() && stack.peek() <= arr[i]) {
        stack.pop();
    }
    result[i] = stack.isEmpty() ? -1 : stack.peek();
    stack.push(arr[i]);
}
```

**Practice Problems:**
- Valid Parentheses (20)
- Next Greater Element I (496)
- Daily Temperatures (739)
- Largest Rectangle in Histogram (84)
- Trapping Rain Water (42)
- Asteroid Collision (735)

#### 4.2 Queue-based BFS
**Template:**
```java
Queue<TreeNode> queue = new LinkedList<>();
queue.offer(root);

while (!queue.isEmpty()) {
    int size = queue.size();
    for (int i = 0; i < size; i++) {
        TreeNode node = queue.poll();
        // Process node
        if (node.left != null) queue.offer(node.left);
        if (node.right != null) queue.offer(node.right);
    }
}
```

---

## 5. Trees & Binary Trees

### Key Concepts
- **Time Complexity**: O(log n) for balanced, O(n) for skewed
- **Java**: Implement TreeNode class
- **Traversals**: Inorder, Preorder, Postorder, Level-order

### Node Definition
```java
class TreeNode {
    int val;
    TreeNode left, right;
    TreeNode(int val) { this.val = val; }
}
```

### Important Patterns

#### 5.1 DFS Traversals
**Inorder (Left-Root-Right):**
```java
void inorder(TreeNode root) {
    if (root == null) return;
    inorder(root.left);
    System.out.print(root.val + " ");
    inorder(root.right);
}
```

**Preorder (Root-Left-Right):**
```java
void preorder(TreeNode root) {
    if (root == null) return;
    System.out.print(root.val + " ");
    preorder(root.left);
    preorder(root.right);
}
```

**Postorder (Left-Right-Root):**
```java
void postorder(TreeNode root) {
    if (root == null) return;
    postorder(root.left);
    postorder(root.right);
    System.out.print(root.val + " ");
}
```

#### 5.2 Level Order (BFS)
```java
List<List<Integer>> levelOrder(TreeNode root) {
    List<List<Integer>> result = new ArrayList<>();
    if (root == null) return result;
    
    Queue<TreeNode> queue = new LinkedList<>();
    queue.offer(root);
    
    while (!queue.isEmpty()) {
        int levelSize = queue.size();
        List<Integer> level = new ArrayList<>();
        
        for (int i = 0; i < levelSize; i++) {
            TreeNode node = queue.poll();
            level.add(node.val);
            if (node.left != null) queue.offer(node.left);
            if (node.right != null) queue.offer(node.right);
        }
        result.add(level);
    }
    return result;
}
```

#### 5.3 Tree Patterns
**Practice Problems:**
- Maximum Depth of Binary Tree (104)
- Invert Binary Tree (226)
- Diameter of Binary Tree (543)
- Lowest Common Ancestor (236)
- Binary Tree Level Order Traversal (102)
- Validate Binary Search Tree (98)
- Serialize and Deserialize Binary Tree (297)
- Binary Tree Maximum Path Sum (124)

---

## 6. Binary Search Trees (BST)

### Key Properties
- **Left subtree** < Root < **Right subtree**
- **Inorder traversal** gives sorted order
- **Time Complexity**: O(log n) average

### Important Operations

#### 6.1 BST Search
```java
TreeNode searchBST(TreeNode root, int val) {
    if (root == null || root.val == val) return root;
    return val < root.val ? searchBST(root.left, val) : searchBST(root.right, val);
}
```

#### 6.2 BST Insert
```java
TreeNode insertIntoBST(TreeNode root, int val) {
    if (root == null) return new TreeNode(val);
    if (val < root.val) {
        root.left = insertIntoBST(root.left, val);
    } else {
        root.right = insertIntoBST(root.right, val);
    }
    return root;
}
```

**Practice Problems:**
- Search in BST (700)
- Insert into BST (701)
- Delete Node in BST (450)
- Kth Smallest Element in BST (230)
- Convert Sorted Array to BST (108)

---

## 7. Heaps & Priority Queues

### Key Concepts
- **Min Heap**: Parent ≤ Children
- **Max Heap**: Parent ≥ Children
- **Java**: PriorityQueue (min heap by default)
- **Time Complexity**: O(log n) insert/delete, O(1) peek

### Templates

#### 7.1 Min Heap
```java
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
```

#### 7.2 Max Heap
```java
PriorityQueue<Integer> maxHeap = new PriorityQueue<>((a, b) -> b - a);
// Or
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
```

#### 7.3 Custom Comparator
```java
PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> a[0] - b[0]);
```

### Important Patterns

#### 7.4 Top K Elements
**Template:**
```java
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
for (int num : nums) {
    minHeap.offer(num);
    if (minHeap.size() > k) {
        minHeap.poll();
    }
}
```

**Practice Problems:**
- Kth Largest Element in Array (215)
- Top K Frequent Elements (347)
- K Closest Points to Origin (973)
- Meeting Rooms II (253)
- Merge K Sorted Lists (23)
- Find Median from Data Stream (295)

---

## 8. Graphs

### Key Concepts
- **Representation**: Adjacency List, Adjacency Matrix
- **Traversal**: DFS, BFS
- **Types**: Directed, Undirected, Weighted

### Graph Representation
```java
// Adjacency List
Map<Integer, List<Integer>> graph = new HashMap<>();

// Or for simple cases
List<Integer>[] graph = new ArrayList[n];
for (int i = 0; i < n; i++) {
    graph[i] = new ArrayList<>();
}
```

### Important Patterns

#### 8.1 DFS Template
```java
void dfs(int node, boolean[] visited, List<Integer>[] graph) {
    visited[node] = true;
    // Process node
    
    for (int neighbor : graph[node]) {
        if (!visited[neighbor]) {
            dfs(neighbor, visited, graph);
        }
    }
}
```

#### 8.2 BFS Template
```java
void bfs(int start, List<Integer>[] graph) {
    Queue<Integer> queue = new LinkedList<>();
    boolean[] visited = new boolean[graph.length];
    
    queue.offer(start);
    visited[start] = true;
    
    while (!queue.isEmpty()) {
        int node = queue.poll();
        // Process node
        
        for (int neighbor : graph[node]) {
            if (!visited[neighbor]) {
                visited[neighbor] = true;
                queue.offer(neighbor);
            }
        }
    }
}
```

#### 8.3 Topological Sort (Kahn's Algorithm)
```java
List<Integer> topologicalSort(int n, int[][] edges) {
    List<Integer>[] graph = new ArrayList[n];
    int[] indegree = new int[n];
    
    for (int i = 0; i < n; i++) graph[i] = new ArrayList<>();
    
    for (int[] edge : edges) {
        graph[edge[0]].add(edge[1]);
        indegree[edge[1]]++;
    }
    
    Queue<Integer> queue = new LinkedList<>();
    for (int i = 0; i < n; i++) {
        if (indegree[i] == 0) queue.offer(i);
    }
    
    List<Integer> result = new ArrayList<>();
    while (!queue.isEmpty()) {
        int node = queue.poll();
        result.add(node);
        
        for (int neighbor : graph[node]) {
            if (--indegree[neighbor] == 0) {
                queue.offer(neighbor);
            }
        }
    }
    
    return result.size() == n ? result : new ArrayList<>();
}
```

#### 8.4 Union Find (Disjoint Set)
```java
class UnionFind {
    private int[] parent;
    private int[] rank;
    
    public UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
    }
    
    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]); // Path compression
        }
        return parent[x];
    }
    
    public boolean union(int x, int y) {
        int px = find(x), py = find(y);
        if (px == py) return false;
        
        // Union by rank
        if (rank[px] < rank[py]) {
            parent[px] = py;
        } else if (rank[px] > rank[py]) {
            parent[py] = px;
        } else {
            parent[py] = px;
            rank[px]++;
        }
        return true;
    }
}
```

**Practice Problems:**
- Number of Islands (200)
- Clone Graph (133)
- Course Schedule (207)
- Course Schedule II (210)
- Pacific Atlantic Water Flow (417)
- Graph Valid Tree (261)
- Number of Connected Components (323)
- Redundant Connection (684)

---

## 9. Binary Search

### Key Concepts
- **Time Complexity**: O(log n)
- **Use Cases**: Sorted array search, optimization problems

### Templates

#### 9.1 Classic Binary Search
```java
int binarySearch(int[] arr, int target) {
    int left = 0, right = arr.length - 1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        
        if (arr[mid] == target) {
            return mid;
        } else if (arr[mid] < target) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }
    return -1;
}
```

#### 9.2 Find First/Last Occurrence
```java
int findFirst(int[] arr, int target) {
    int left = 0, right = arr.length - 1, result = -1;
    
    while (left <= right) {
        int mid = left + (right - left) / 2;
        
        if (arr[mid] == target) {
            result = mid;
            right = mid - 1; // Continue searching left
        } else if (arr[mid] < target) {
            left = mid + 1;
        } else {
            right = mid - 1;
        }
    }
    return result;
}
```

**Practice Problems:**
- Binary Search (704)
- Search Insert Position (35)
- First Bad Version (278)
- Search in Rotated Sorted Array (33)
- Find Minimum in Rotated Sorted Array (153)
- Find Peak Element (162)
- Koko Eating Bananas (875)
- Median of Two Sorted Arrays (4)

---

## 10. Recursion & Backtracking

### Key Concepts
- **Recursion**: Function calls itself
- **Backtracking**: Try all possibilities, undo choices
- **Base Case**: Termination condition

### Templates

#### 10.1 Basic Recursion
```java
void recursion(int n) {
    // Base case
    if (n == 0) return;
    
    // Recursive case
    // Do something
    recursion(n - 1);
}
```

#### 10.2 Backtracking Template
```java
void backtrack(List<Integer> current, List<List<Integer>> result) {
    // Base case - found solution
    if (isValid(current)) {
        result.add(new ArrayList<>(current));
        return;
    }
    
    // Try all possibilities
    for (int choice : choices) {
        // Make choice
        current.add(choice);
        
        // Recurse
        backtrack(current, result);
        
        // Undo choice
        current.remove(current.size() - 1);
    }
}
```

#### 10.3 Permutations
```java
void permute(int[] nums, List<Integer> current, List<List<Integer>> result, boolean[] used) {
    if (current.size() == nums.length) {
        result.add(new ArrayList<>(current));
        return;
    }
    
    for (int i = 0; i < nums.length; i++) {
        if (used[i]) continue;
        
        current.add(nums[i]);
        used[i] = true;
        
        permute(nums, current, result, used);
        
        current.remove(current.size() - 1);
        used[i] = false;
    }
}
```

#### 10.4 Combinations/Subsets
```java
void combine(int[] nums, int start, List<Integer> current, List<List<Integer>> result) {
    result.add(new ArrayList<>(current));
    
    for (int i = start; i < nums.length; i++) {
        current.add(nums[i]);
        combine(nums, i + 1, current, result);
        current.remove(current.size() - 1);
    }
}
```

**Practice Problems:**
- Subsets (78)
- Permutations (46)
- Combinations (77)
- Letter Combinations of Phone Number (17)
- Generate Parentheses (22)
- N-Queens (51)
- Word Search (79)
- Sudoku Solver (37)

---

## 11. Dynamic Programming

### Key Concepts
- **Memoization**: Top-down with caching
- **Tabulation**: Bottom-up with table
- **Optimal Substructure**: Optimal solution contains optimal sub-solutions
- **Overlapping Subproblems**: Same subproblems computed multiple times

### Templates

#### 11.1 1D DP
```java
// Memoization
int solve(int n, int[] memo) {
    if (n <= 1) return n;
    if (memo[n] != -1) return memo[n];
    
    memo[n] = solve(n - 1, memo) + solve(n - 2, memo);
    return memo[n];
}

// Tabulation
int solve(int n) {
    int[] dp = new int[n + 1];
    dp[0] = 0; dp[1] = 1;
    
    for (int i = 2; i <= n; i++) {
        dp[i] = dp[i - 1] + dp[i - 2];
    }
    return dp[n];
}
```

#### 11.2 2D DP
```java
int solve(String s1, String s2) {
    int m = s1.length(), n = s2.length();
    int[][] dp = new int[m + 1][n + 1];
    
    // Base cases
    for (int i = 0; i <= m; i++) dp[i][0] = i;
    for (int j = 0; j <= n; j++) dp[0][j] = j;
    
    // Fill table
    for (int i = 1; i <= m; i++) {
        for (int j = 1; j <= n; j++) {
            if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
                dp[i][j] = dp[i - 1][j - 1];
            } else {
                dp[i][j] = 1 + Math.min(dp[i - 1][j], 
                                         Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
            }
        }
    }
    return dp[m][n];
}
```

### Important Patterns

#### 11.3 Fibonacci-like
- Climbing Stairs (70)
- House Robber (198)
- Min Cost Climbing Stairs (746)

#### 11.4 Knapsack
- 0/1 Knapsack (classic)
- Partition Equal Subset Sum (416)
- Target Sum (494)

#### 11.5 Longest Common Subsequence (LCS)
- Longest Common Subsequence (1143)
- Edit Distance (72)
- Longest Palindromic Subsequence (516)

#### 11.6 Longest Increasing Subsequence (LIS)
- Longest Increasing Subsequence (300)
- Russian Doll Envelopes (354)

#### 11.7 Grid DP
- Unique Paths (62)
- Minimum Path Sum (64)
- Dungeon Game (174)

**Top DP Problems:**
- Climbing Stairs (70)
- House Robber (198)
- Coin Change (322)
- Longest Increasing Subsequence (300)
- Longest Common Subsequence (1143)
- Edit Distance (72)
- Word Break (139)
- Palindromic Substrings (647)
- Regular Expression Matching (10)

---

## 12. Greedy Algorithms

### Key Concepts
- **Greedy Choice Property**: Local optimal → Global optimal
- **Use Cases**: Optimization problems with greedy choice

### Important Patterns

#### 12.1 Interval Problems
```java
// Meeting rooms - sort by end time
Arrays.sort(intervals, (a, b) -> a[1] - b[1]);
```

**Practice Problems:**
- Jump Game (55)
- Jump Game II (45)
- Meeting Rooms II (253)
- Non-overlapping Intervals (435)
- Minimum Number of Arrows (452)
- Task Scheduler (621)

---

## 13. Tries (Prefix Trees)

### Implementation
```java
class TrieNode {
    TrieNode[] children = new TrieNode[26];
    boolean isEnd = false;
}

class Trie {
    private TrieNode root = new TrieNode();
    
    public void insert(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int idx = c - 'a';
            if (node.children[idx] == null) {
                node.children[idx] = new TrieNode();
            }
            node = node.children[idx];
        }
        node.isEnd = true;
    }
    
    public boolean search(String word) {
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            int idx = c - 'a';
            if (node.children[idx] == null) return false;
            node = node.children[idx];
        }
        return node.isEnd;
    }
}
```

**Practice Problems:**
- Implement Trie (208)
- Design Add and Search Words Data Structure (211)
- Word Search II (212)

---

## Problem-Solving Framework

### Step-by-Step Approach:
1. **Understand the problem**
   - Read carefully
   - Identify inputs/outputs
   - Clarify constraints

2. **Examples & Edge Cases**
   - Walk through examples
   - Think of edge cases (empty, single element, duplicates)

3. **Brute Force**
   - Think of simplest solution
   - State time/space complexity

4. **Optimize**
   - Identify bottlenecks
   - Use appropriate data structure/algorithm
   - Think of patterns

5. **Code**
   - Write clean code
   - Use meaningful names
   - Handle edge cases

6. **Test**
   - Test with examples
   - Test edge cases
   - Dry run code

7. **Analyze**
   - Time complexity
   - Space complexity
   - Optimize if needed

---

## Problem Count by Difficulty

**Target Distribution:**
- **Easy**: 100 problems (foundation)
- **Medium**: 250 problems (interview core)
- **Hard**: 50 problems (senior level)
- **Total**: 400 problems

---

## Study Tips

1. **Solve without IDE first** - Use pen & paper
2. **Time yourself** - 45 mins per problem
3. **Review patterns** - Not memorize solutions
4. **Space/Time analysis** - Always analyze
5. **Multiple approaches** - Think of 2-3 ways
6. **Explain out loud** - Practice communication
7. **Revisit problems** - Review after 1 week, 1 month

---

## Next Steps
- Follow the roadmap in `02_PREPARATION_ROADMAP.md`
- Practice on LeetCode/HackerRank
- Check `07_RESOURCES_AND_PRACTICE.md` for problem lists

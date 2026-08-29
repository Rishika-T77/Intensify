package com.intensify.config;

import com.intensify.entity.InterviewQuestion;
import com.intensify.entity.InterviewQuestion.Difficulty;
import com.intensify.repository.InterviewQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the question bank on startup if the database is empty.
 * 15 questions per category = 45 total (PRD §10).
 * Expected_concepts are internal rubric grounding — never shown to users.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuestionSeeder implements CommandLineRunner {

    private final InterviewQuestionRepository questionRepository;

    @Override
    public void run(String... args) {
        if (questionRepository.count() > 0) {
            log.info("Question bank already seeded ({} questions). Skipping.", questionRepository.count());
            return;
        }
        log.info("Seeding question bank...");
        questionRepository.saveAll(questions());
        log.info("Seeded {} questions.", questionRepository.count());
    }

    private List<InterviewQuestion> questions() {
        return List.of(

            // ── DSA ───────────────────────────────────────────────────────────────

            InterviewQuestion.builder().category("DSA").difficulty(Difficulty.EASY).title("Two Sum")
                .promptText("Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target. You may assume each input has exactly one solution.")
                .expectedConcepts(List.of("hash map for O(n) lookup", "brute force O(n²) alternative", "space vs time tradeoff", "single-pass vs two-pass"))
                .build(),

            InterviewQuestion.builder().category("DSA").difficulty(Difficulty.EASY).title("Valid Parentheses")
                .promptText("Given a string s containing just the characters '(', ')', '{', '}', '[', ']', determine if the input string is valid. A string is valid if brackets are closed in the correct order.")
                .expectedConcepts(List.of("stack usage", "LIFO property", "empty stack check at end", "character matching logic"))
                .build(),

            InterviewQuestion.builder().category("DSA").difficulty(Difficulty.EASY).title("Reverse a Linked List")
                .promptText("Given the head of a singly linked list, reverse the list and return the reversed list.")
                .expectedConcepts(List.of("prev/current/next pointer approach", "O(n) time O(1) space", "iterative vs recursive", "in-place reversal"))
                .build(),

            InterviewQuestion.builder().category("DSA").difficulty(Difficulty.MEDIUM).title("Longest Substring Without Repeating Characters")
                .promptText("Given a string s, find the length of the longest substring without repeating characters.")
                .expectedConcepts(List.of("sliding window", "hash set for O(1) lookup", "two pointers", "shrink window on duplicate", "O(n) time"))
                .build(),

            InterviewQuestion.builder().category("DSA").difficulty(Difficulty.MEDIUM).title("Binary Search")
                .promptText("Given an array of integers nums sorted in ascending order and an integer target, write a function to search target in nums. Return the index if found, otherwise return -1.")
                .expectedConcepts(List.of("divide and conquer", "O(log n) time", "mid calculation overflow", "left/right boundary conditions", "iterative vs recursive"))
                .build(),

            InterviewQuestion.builder().category("DSA").difficulty(Difficulty.MEDIUM).title("Maximum Subarray (Kadane's Algorithm)")
                .promptText("Given an integer array nums, find the subarray with the largest sum, and return its sum.")
                .expectedConcepts(List.of("Kadane's algorithm", "local vs global max", "O(n) time O(1) space", "why greedy works here", "negative number edge case"))
                .build(),

            InterviewQuestion.builder().category("DSA").difficulty(Difficulty.MEDIUM).title("Merge Two Sorted Lists")
                .promptText("You are given the heads of two sorted linked lists. Merge the two lists into one sorted list. Return the head of the merged linked list.")
                .expectedConcepts(List.of("dummy node technique", "pointer comparison", "O(m+n) time", "tail handling"))
                .build(),

            InterviewQuestion.builder().category("DSA").difficulty(Difficulty.MEDIUM).title("Number of Islands")
                .promptText("Given an m×n 2D binary grid where '1' represents land and '0' represents water, return the number of islands.")
                .expectedConcepts(List.of("DFS/BFS traversal", "visited marking", "O(m*n) time and space", "4-directional vs 8-directional", "flood fill"))
                .build(),

            InterviewQuestion.builder().category("DSA").difficulty(Difficulty.MEDIUM).title("Coin Change")
                .promptText("Given an integer array coins representing coins of different denominations and an integer amount, return the fewest number of coins needed to make up that amount. Return -1 if it cannot be made up.")
                .expectedConcepts(List.of("dynamic programming", "bottom-up DP", "recurrence relation", "greedy failure case", "O(amount × coins) time"))
                .build(),

            InterviewQuestion.builder().category("DSA").difficulty(Difficulty.MEDIUM).title("Product of Array Except Self")
                .promptText("Given an integer array nums, return an array answer such that answer[i] is equal to the product of all elements of nums except nums[i]. You must write an algorithm that runs in O(n) time and without using the division operation.")
                .expectedConcepts(List.of("prefix product", "suffix product", "O(n) time O(1) extra space", "why no division", "two-pass approach"))
                .build(),

            InterviewQuestion.builder().category("DSA").difficulty(Difficulty.HARD).title("Trapping Rain Water")
                .promptText("Given n non-negative integers representing an elevation map where the width of each bar is 1, compute how much water it can trap after raining.")
                .expectedConcepts(List.of("two pointer approach", "prefix max / suffix max", "O(n) time O(1) space", "why water height is min of two walls", "naive O(n²) tradeoff"))
                .build(),

            InterviewQuestion.builder().category("DSA").difficulty(Difficulty.HARD).title("Merge K Sorted Lists")
                .promptText("You are given an array of k linked-lists, each sorted in ascending order. Merge all the linked lists into one sorted linked list and return it.")
                .expectedConcepts(List.of("min-heap / priority queue", "O(n log k) time", "divide and conquer alternative", "heap invariant maintenance"))
                .build(),

            InterviewQuestion.builder().category("DSA").difficulty(Difficulty.HARD).title("Word Search II")
                .promptText("Given an m×n board of characters and a list of strings words, return all words on the board. Each word must be constructed from letters of sequentially adjacent cells.")
                .expectedConcepts(List.of("Trie construction", "DFS backtracking", "pruning with Trie", "O(m*n*4^L) time", "marking visited cells"))
                .build(),

            InterviewQuestion.builder().category("DSA").difficulty(Difficulty.MEDIUM).title("Clone Graph")
                .promptText("Given a reference of a node in a connected undirected graph, return a deep copy (clone) of the graph.")
                .expectedConcepts(List.of("DFS or BFS traversal", "hash map node mapping (old→new)", "cycle handling via visited map", "O(V+E) time and space"))
                .build(),

            InterviewQuestion.builder().category("DSA").difficulty(Difficulty.EASY).title("Climbing Stairs")
                .promptText("You are climbing a staircase. It takes n steps to reach the top. Each time you can either climb 1 or 2 steps. In how many distinct ways can you climb to the top?")
                .expectedConcepts(List.of("Fibonacci-like recurrence", "DP bottom-up", "O(n) time O(1) space", "base cases 1 and 2", "top-down memoization alternative"))
                .build(),

            // ── SYSTEM DESIGN ─────────────────────────────────────────────────────

            InterviewQuestion.builder().category("SYSTEM_DESIGN").difficulty(Difficulty.MEDIUM).title("Design a URL Shortener (TinyURL)")
                .promptText("Design a URL shortening service like TinyURL. The system should be able to generate short unique URLs for long URLs and redirect users to original URLs when the short URL is accessed.")
                .expectedConcepts(List.of("key generation strategy (hash vs counter)", "database choice", "redirection (301 vs 302)", "scalability and caching", "collision handling", "read-heavy workload"))
                .build(),

            InterviewQuestion.builder().category("SYSTEM_DESIGN").difficulty(Difficulty.MEDIUM).title("Design a Rate Limiter")
                .promptText("Design a rate limiter that limits the number of requests a user can send to an API within a time window.")
                .expectedConcepts(List.of("token bucket vs leaky bucket vs sliding window", "in-memory (Redis) vs DB storage", "distributed rate limiting", "consistency tradeoffs", "headers and error responses"))
                .build(),

            InterviewQuestion.builder().category("SYSTEM_DESIGN").difficulty(Difficulty.HARD).title("Design Twitter/X News Feed")
                .promptText("Design the news feed feature of Twitter. A user can follow other users and should see a feed of tweets from users they follow, ranked by recency.")
                .expectedConcepts(List.of("push vs pull model", "fanout on write vs read", "celebrity problem", "timeline storage (Redis sorted set)", "pagination", "eventual consistency"))
                .build(),

            InterviewQuestion.builder().category("SYSTEM_DESIGN").difficulty(Difficulty.HARD).title("Design a Distributed Cache")
                .promptText("Design a distributed cache system (similar to Memcached or Redis). The system should support get, set, and delete operations with high throughput and low latency.")
                .expectedConcepts(List.of("consistent hashing", "eviction policies (LRU/LFU)", "replication for availability", "TTL management", "cache invalidation strategies", "hotspot handling"))
                .build(),

            InterviewQuestion.builder().category("SYSTEM_DESIGN").difficulty(Difficulty.MEDIUM).title("Design a Notification System")
                .promptText("Design a notification system that can send push notifications, emails, and SMS to millions of users. The system must be reliable and support different notification priorities.")
                .expectedConcepts(List.of("message queue (Kafka/RabbitMQ)", "retry with exponential backoff", "deduplication", "user preference storage", "rate limiting per channel", "delivery guarantees"))
                .build(),

            InterviewQuestion.builder().category("SYSTEM_DESIGN").difficulty(Difficulty.MEDIUM).title("Design a Key-Value Store")
                .promptText("Design a key-value store that supports get, put, and delete operations. The store should be highly available and handle millions of requests per second.")
                .expectedConcepts(List.of("LSM tree vs B-tree storage", "WAL for durability", "replication strategy", "partitioning/sharding", "consistency model (eventual vs strong)", "CAP theorem tradeoffs"))
                .build(),

            InterviewQuestion.builder().category("SYSTEM_DESIGN").difficulty(Difficulty.HARD).title("Design a Search Autocomplete System")
                .promptText("Design a search autocomplete system that suggests the top 5 search queries as users type. The system should handle billions of queries and return suggestions in real-time.")
                .expectedConcepts(List.of("Trie vs prefix index", "popularity scoring", "real-time vs batch aggregation", "CDN for edge delivery", "caching top suggestions", "data aggregation pipeline"))
                .build(),

            InterviewQuestion.builder().category("SYSTEM_DESIGN").difficulty(Difficulty.MEDIUM).title("Design a File Storage System (like Google Drive)")
                .promptText("Design a file storage system where users can upload, download, share, and sync files across devices.")
                .expectedConcepts(List.of("chunking large files", "deduplication (content-addressed storage)", "metadata vs blob storage separation", "sync conflict resolution", "CDN for downloads", "versioning"))
                .build(),

            InterviewQuestion.builder().category("SYSTEM_DESIGN").difficulty(Difficulty.HARD).title("Design a Real-Time Chat Application")
                .promptText("Design a real-time messaging system like WhatsApp or Slack supporting 1-on-1 and group chats with message delivery guarantees.")
                .expectedConcepts(List.of("WebSocket vs long polling", "message queue for delivery", "online presence tracking", "message ordering", "group chat fan-out", "offline message storage"))
                .build(),

            InterviewQuestion.builder().category("SYSTEM_DESIGN").difficulty(Difficulty.MEDIUM).title("Design a Ride-Sharing Service (like Uber)")
                .promptText("Design the core of a ride-sharing application like Uber that matches riders with nearby drivers in real-time.")
                .expectedConcepts(List.of("geospatial indexing (geohash/quadtree)", "location update frequency", "matching algorithm", "ETA calculation", "surge pricing architecture", "trip state machine"))
                .build(),

            InterviewQuestion.builder().category("SYSTEM_DESIGN").difficulty(Difficulty.MEDIUM).title("Design a Job Scheduler")
                .promptText("Design a distributed job scheduler that can schedule and execute millions of tasks (one-time and recurring). The system must be fault-tolerant and guarantee at-least-once execution.")
                .expectedConcepts(List.of("cron expression parsing", "distributed locking", "worker pool", "at-least-once vs exactly-once delivery", "dead-letter queue", "priority queuing"))
                .build(),

            InterviewQuestion.builder().category("SYSTEM_DESIGN").difficulty(Difficulty.HARD).title("Design YouTube/Video Streaming")
                .promptText("Design a video streaming platform like YouTube that handles video uploads, processing, and streaming to millions of concurrent users.")
                .expectedConcepts(List.of("video transcoding pipeline", "CDN for streaming", "adaptive bitrate streaming (HLS/DASH)", "blob storage for videos", "metadata DB", "recommendation system separation"))
                .build(),

            InterviewQuestion.builder().category("SYSTEM_DESIGN").difficulty(Difficulty.MEDIUM).title("Design an API Gateway")
                .promptText("Design an API gateway for a microservices architecture that handles authentication, rate limiting, request routing, and logging.")
                .expectedConcepts(List.of("reverse proxy pattern", "JWT validation at gateway", "rate limiting per client", "circuit breaker", "service discovery", "request/response transformation"))
                .build(),

            InterviewQuestion.builder().category("SYSTEM_DESIGN").difficulty(Difficulty.MEDIUM).title("Design a Leaderboard System")
                .promptText("Design a real-time leaderboard for a gaming platform that shows the top 100 players globally and a user's rank among millions of players.")
                .expectedConcepts(List.of("Redis sorted set for O(log n) rank updates", "score update strategy", "pagination", "eventual consistency tradeoff", "sharding for scale", "batch vs real-time updates"))
                .build(),

            InterviewQuestion.builder().category("SYSTEM_DESIGN").difficulty(Difficulty.EASY).title("Design a Simple Authentication System")
                .promptText("Design the authentication system for a web application supporting email/password login, JWT tokens, and password reset.")
                .expectedConcepts(List.of("stateless JWT vs stateful sessions", "token expiry and refresh tokens", "password hashing (bcrypt)", "secure password reset flow", "brute force protection"))
                .build(),

            // ── CONCEPTUAL ────────────────────────────────────────────────────────

            InterviewQuestion.builder().category("CONCEPTUAL").difficulty(Difficulty.EASY).title("Explain the difference between a stack and a queue")
                .promptText("Explain the difference between a stack and a queue. Describe their underlying principles, typical use cases, and the operations each supports.")
                .expectedConcepts(List.of("LIFO vs FIFO", "push/pop vs enqueue/dequeue", "call stack use case", "BFS use case", "array vs linked list implementation"))
                .build(),

            InterviewQuestion.builder().category("CONCEPTUAL").difficulty(Difficulty.EASY).title("What is Big-O notation? Why does it matter?")
                .promptText("Explain Big-O notation. Why is it used, and what are its limitations? Give examples of O(1), O(log n), O(n), O(n log n), and O(n²) operations.")
                .expectedConcepts(List.of("worst-case analysis", "asymptotic growth rate", "constant factor ignored", "space vs time complexity", "practical relevance at scale"))
                .build(),

            InterviewQuestion.builder().category("CONCEPTUAL").difficulty(Difficulty.MEDIUM).title("Explain how a HashMap works internally")
                .promptText("Explain how a HashMap (or hash table) works internally. Cover hashing, collision handling, load factor, and resizing.")
                .expectedConcepts(List.of("hash function", "array of buckets", "collision — chaining vs open addressing", "load factor threshold", "rehashing/resizing", "O(1) average vs O(n) worst case"))
                .build(),

            InterviewQuestion.builder().category("CONCEPTUAL").difficulty(Difficulty.MEDIUM).title("What is the difference between TCP and UDP?")
                .promptText("Explain the difference between TCP and UDP. When would you choose one over the other? Give concrete examples of each.")
                .expectedConcepts(List.of("connection-oriented vs connectionless", "reliability guarantees", "ordering", "flow control and congestion control", "latency tradeoff", "use cases (HTTP vs gaming/video)"))
                .build(),

            InterviewQuestion.builder().category("CONCEPTUAL").difficulty(Difficulty.MEDIUM).title("Explain database indexing")
                .promptText("Explain what a database index is, how it works, and the tradeoffs involved in adding indexes. When should you not add an index?")
                .expectedConcepts(List.of("B-tree index structure", "read speedup", "write overhead", "index selectivity", "composite index", "covering index", "when not to index"))
                .build(),

            InterviewQuestion.builder().category("CONCEPTUAL").difficulty(Difficulty.MEDIUM).title("What is the difference between process and thread?")
                .promptText("Explain the difference between a process and a thread. Describe memory isolation, context switching, and when to use one over the other.")
                .expectedConcepts(List.of("separate memory space vs shared", "context switch cost", "IPC mechanisms", "thread synchronization (locks, semaphores)", "race conditions", "use cases"))
                .build(),

            InterviewQuestion.builder().category("CONCEPTUAL").difficulty(Difficulty.EASY).title("Explain REST vs GraphQL")
                .promptText("Compare REST and GraphQL APIs. When would you choose GraphQL over REST and vice versa? What are the tradeoffs?")
                .expectedConcepts(List.of("over-fetching vs under-fetching", "schema-based queries", "N+1 problem in GraphQL", "caching difficulty in GraphQL", "REST's simplicity", "versioning"))
                .build(),

            InterviewQuestion.builder().category("CONCEPTUAL").difficulty(Difficulty.HARD).title("Explain CAP Theorem")
                .promptText("Explain the CAP theorem. What do Consistency, Availability, and Partition Tolerance mean? Give real-world examples of CA, CP, and AP systems.")
                .expectedConcepts(List.of("partition tolerance is non-negotiable in distributed systems", "CA not truly achievable", "CP systems (ZooKeeper, HBase)", "AP systems (DynamoDB, Cassandra)", "eventual consistency"))
                .build(),

            InterviewQuestion.builder().category("CONCEPTUAL").difficulty(Difficulty.MEDIUM).title("What is eventual consistency?")
                .promptText("Explain eventual consistency. How does it differ from strong consistency? In what systems is it acceptable, and what are the risks?")
                .expectedConcepts(List.of("replica convergence", "strong vs eventual consistency", "read-your-writes", "monotonic reads", "acceptable use cases (shopping cart, DNS)", "conflict resolution"))
                .build(),

            InterviewQuestion.builder().category("CONCEPTUAL").difficulty(Difficulty.MEDIUM).title("Explain SOLID principles")
                .promptText("Explain the SOLID principles of object-oriented design. Give a brief example of each and explain why violating each principle causes problems.")
                .expectedConcepts(List.of("Single Responsibility", "Open/Closed", "Liskov Substitution", "Interface Segregation", "Dependency Inversion", "practical tradeoffs — SOLID isn't absolute"))
                .build(),

            InterviewQuestion.builder().category("CONCEPTUAL").difficulty(Difficulty.MEDIUM).title("What is a deadlock? How do you prevent it?")
                .promptText("Explain what a deadlock is in concurrent programming. What conditions lead to a deadlock? How can you detect and prevent it?")
                .expectedConcepts(List.of("four Coffman conditions", "mutual exclusion", "hold and wait", "no preemption", "circular wait", "prevention strategies", "deadlock detection algorithms"))
                .build(),

            InterviewQuestion.builder().category("CONCEPTUAL").difficulty(Difficulty.EASY).title("Explain the difference between SQL and NoSQL databases")
                .promptText("Compare SQL and NoSQL databases. When would you choose each? Cover data modeling, scalability, consistency, and query flexibility.")
                .expectedConcepts(List.of("structured vs flexible schema", "ACID vs BASE", "horizontal vs vertical scaling", "query flexibility", "specific NoSQL types (document, key-value, columnar, graph)", "use case examples"))
                .build(),

            InterviewQuestion.builder().category("CONCEPTUAL").difficulty(Difficulty.HARD).title("How does garbage collection work in Java/JVM?")
                .promptText("Explain how garbage collection works in the JVM. Cover the heap structure, generational GC, common GC algorithms, and the impact of GC on application latency.")
                .expectedConcepts(List.of("heap regions (young/old/metaspace)", "generational hypothesis", "minor vs major GC", "mark-sweep-compact", "G1 vs ZGC", "GC pauses and throughput tradeoff"))
                .build(),

            InterviewQuestion.builder().category("CONCEPTUAL").difficulty(Difficulty.MEDIUM).title("Explain HTTP caching")
                .promptText("Explain how HTTP caching works. Cover cache-control headers, ETags, and the differences between browser caching, CDN caching, and server-side caching.")
                .expectedConcepts(List.of("Cache-Control directives (max-age, no-cache, no-store)", "ETag and conditional requests (If-None-Match)", "Last-Modified", "CDN edge caching", "cache invalidation strategies"))
                .build(),

            InterviewQuestion.builder().category("CONCEPTUAL").difficulty(Difficulty.HARD).title("Explain consistent hashing")
                .promptText("Explain consistent hashing. Why is it used in distributed systems, and how does it solve the problems of traditional modulo-based hashing when nodes are added or removed?")
                .expectedConcepts(List.of("hash ring", "node placement", "key assignment", "minimal key remapping on change", "virtual nodes for even distribution", "use cases (distributed cache, DHT)"))
                .build()
        );
    }
}

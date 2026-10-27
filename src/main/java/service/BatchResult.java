package service;

import java.util.List;
import java.util.Map;

/** What happened when a batch of commands was run on the thread pool. */
public record BatchResult(int submitted, int succeeded, List<String> failures,
                          Map<String, Integer> tasksPerThread, long millis) {
}

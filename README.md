# MST Edge Replacement Demo

This project demonstrates how to:

- Build a minimum spanning tree (MST) from a weighted, undirected graph.
- Remove an edge from the MST and inspect the resulting forest.
- Efficiently find the lightest replacement edge that reconnects the components while keeping the tree minimum.

## Build & Run

Clone the repository and run the following commands from the project root:

```bash
mvn clean package
java -jar target/mst-edge-replacement-1.0.0.jar
```

### Sample Output

The default run uses `src/main/resources/graph.txt`:

```
Input graph edges (u v w):
  A -- B (w=4)
  A -- C (w=3)
  ...
Replacement edge selected: B -- E (w=5)
Updated MST edges:
  ...
Total weight: 18
```

### Custom Graph

You can provide your own graph file via `--graph`:

```bash
java -jar target/mst-edge-replacement-1.0.0.jar --graph /absolute/path/to/graph.txt
```

Each non-empty, non-comment line in the graph file must contain three tokens: `<u> <v> <weight>`.

Example:

```
# u v w
1 2 4
1 3 2
2 3 5
```

### Selecting an Edge to Remove

By default, the program removes the heaviest edge in the MST. To target a specific edge, pass `--remove` followed by the two vertices and the expected weight:

```bash
java -jar target/mst-edge-replacement-1.0.0.jar --remove A C 3
```

If the edge is part of the MST, it will be removed before computing the replacement.

## Project Structure

- `src/main/java/com/example/mst/Main.java` — entry point and MST logic
- `src/main/resources/graph.txt` — sample graph
- `pom.xml` — Maven build configuration

## Notes

- The program reports when the input graph is disconnected or when no replacement edge exists.
- Outputs are deterministic for a given input graph.


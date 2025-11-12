package com.example.mst;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException {
        ProgramArguments programArguments = ProgramArguments.parse(args);
        Graph graph = GraphLoader.loadGraph(programArguments.graphPath());

        System.out.println("Input graph edges (u v w):");
        graph.edges().forEach(edge -> System.out.printf("  %s%n", edge));
        System.out.println();

        MinimumSpanningTree mst = MinimumSpanningTree.kruskal(graph);
        List<Edge> mstEdges = new ArrayList<>(mst.edges());

        System.out.println("Initial MST edges:");
        mstEdges.forEach(edge -> System.out.printf("  %s%n", edge));
        System.out.printf("Total weight: %d%n%n", totalWeight(mstEdges));

        Edge edgeToRemove = determineEdgeToRemove(programArguments, mstEdges);
        if (edgeToRemove == null) {
            System.out.println("No edge selected for removal. Exiting.");
            return;
        }

        System.out.printf("Removing edge: %s%n%n", edgeToRemove);

        List<Edge> remainingEdges = new ArrayList<>(mstEdges);
        if (!remainingEdges.remove(edgeToRemove)) {
            System.out.println("Selected edge is not part of the MST. Exiting.");
            return;
        }

        List<Set<String>> components = identifyComponents(remainingEdges, mst.vertices());
        System.out.println("Components after removal:");
        for (int i = 0; i < components.size(); i++) {
            System.out.printf("  Component %d: %s%n", i + 1, components.get(i));
        }
        System.out.println();

        Optional<Edge> replacement = findReplacementEdge(graph.edges(), components);
        if (replacement.isEmpty()) {
            System.out.println("No replacement edge found. The graph cannot remain connected.");
            return;
        }

        Edge replacementEdge = replacement.get();
        System.out.printf("Replacement edge selected: %s%n%n", replacementEdge);

        remainingEdges.add(replacementEdge);
        remainingEdges.sort(Comparator.naturalOrder());

        System.out.println("Updated MST edges:");
        remainingEdges.forEach(edge -> System.out.printf("  %s%n", edge));
        System.out.printf("Total weight: %d%n", totalWeight(remainingEdges));
    }

    private static long totalWeight(Collection<Edge> edges) {
        return edges.stream().mapToLong(Edge::weight).sum();
    }

    private static Edge determineEdgeToRemove(ProgramArguments arguments, List<Edge> mstEdges) {
        if (arguments.edgeToRemove().isPresent()) {
            Edge candidate = arguments.edgeToRemove().get();
            for (Edge edge : mstEdges) {
                if (edge.connectsSameVertices(candidate)) {
                    return edge;
                }
            }
            System.out.println("Requested edge to remove was not found in the MST.");
            return null;
        }

        return mstEdges.stream().max(Comparator.comparingInt(Edge::weight)).orElse(null);
    }

    private static List<Set<String>> identifyComponents(List<Edge> edges, Set<String> vertices) {
        Map<String, Set<String>> adjacency = new HashMap<>();
        for (String vertex : vertices) {
            adjacency.put(vertex, new HashSet<>());
        }
        for (Edge edge : edges) {
            adjacency.get(edge.u()).add(edge.v());
            adjacency.get(edge.v()).add(edge.u());
        }

        List<Set<String>> components = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (String vertex : vertices) {
            if (visited.contains(vertex)) {
                continue;
            }
            Set<String> component = new TreeSet<>();
            Deque<String> stack = new ArrayDeque<>();
            stack.push(vertex);
            visited.add(vertex);

            while (!stack.isEmpty()) {
                String current = stack.pop();
                component.add(current);
                for (String neighbor : adjacency.getOrDefault(current, Set.of())) {
                    if (visited.add(neighbor)) {
                        stack.push(neighbor);
                    }
                }
            }
            components.add(component);
        }

        components.sort(Comparator.comparingInt((Set<String> set) -> set.size()).reversed());
        return components;
    }

    private static Optional<Edge> findReplacementEdge(List<Edge> edges, List<Set<String>> components) {
        if (components.size() < 2) {
            return Optional.empty();
        }
        Set<String> componentA = components.get(0);
        Set<String> componentB = components.get(1);

        return edges.stream()
                .filter(edge -> crossesComponents(edge, componentA, componentB))
                .min(Comparator.comparingInt(Edge::weight)
                        .thenComparing(Edge::u)
                        .thenComparing(Edge::v));
    }

    private static boolean crossesComponents(Edge edge, Set<String> componentA, Set<String> componentB) {
        boolean uInA = componentA.contains(edge.u());
        boolean vInA = componentA.contains(edge.v());
        boolean uInB = componentB.contains(edge.u());
        boolean vInB = componentB.contains(edge.v());
        return (uInA && vInB) || (vInA && uInB);
    }

    private record ProgramArguments(Path graphPath, Optional<Edge> edgeToRemove) {
        static ProgramArguments parse(String[] args) {
            Path graphPath = Path.of("src/main/resources/graph.txt");
            Optional<Edge> edgeToRemove = Optional.empty();

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--graph" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("--graph requires a path");
                        }
                        graphPath = Path.of(args[++i]);
                    }
                    case "--remove" -> {
                        if (i + 3 >= args.length) {
                            throw new IllegalArgumentException("--remove requires three arguments: <u> <v> <weight>");
                        }
                        String u = args[++i];
                        String v = args[++i];
                        int weight = Integer.parseInt(args[++i]);
                        edgeToRemove = Optional.of(new Edge(u, v, weight));
                    }
                    default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
                }
            }

            return new ProgramArguments(graphPath, edgeToRemove);
        }
    }

    private record Graph(List<Edge> edges, Set<String> vertices) {
    }

    private static final class GraphLoader {
        private GraphLoader() {
        }

        static Graph loadGraph(Path path) throws IOException {
            List<Edge> edges = new ArrayList<>();
            Set<String> vertices = new TreeSet<>();

            List<String> lines = Files.readAllLines(path);
            for (String rawLine : lines) {
                String line = rawLine.strip();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                if (parts.length != 3) {
                    throw new IllegalArgumentException("Invalid edge line: " + line);
                }
                String u = parts[0];
                String v = parts[1];
                int weight = Integer.parseInt(parts[2]);
                edges.add(new Edge(u, v, weight));
                vertices.add(u);
                vertices.add(v);
            }

            return new Graph(edges, vertices);
        }
    }

    private record MinimumSpanningTree(List<Edge> edges, Set<String> vertices) {
        static MinimumSpanningTree kruskal(Graph graph) {
            List<Edge> sortedEdges = new ArrayList<>(graph.edges());
            sortedEdges.sort(Comparator.naturalOrder());

            DisjointSet<String> disjointSet = new DisjointSet<>(graph.vertices());
            List<Edge> mstEdges = new ArrayList<>();

            for (Edge edge : sortedEdges) {
                if (disjointSet.union(edge.u(), edge.v())) {
                    mstEdges.add(edge);
                }
            }

            if (mstEdges.size() != graph.vertices().size() - 1) {
                throw new IllegalStateException("Graph is not connected. MST could not be formed.");
            }

            return new MinimumSpanningTree(mstEdges, new TreeSet<>(graph.vertices()));
        }
    }

    private record Edge(String u, String v, int weight) implements Comparable<Edge> {
        @Override
        public int compareTo(Edge other) {
            int weightCompare = Integer.compare(this.weight, other.weight);
            if (weightCompare != 0) {
                return weightCompare;
            }
            int uCompare = this.u.compareTo(other.u);
            if (uCompare != 0) {
                return uCompare;
            }
            return this.v.compareTo(other.v);
        }

        boolean connectsSameVertices(Edge other) {
            return (u.equals(other.u) && v.equals(other.v) || u.equals(other.v) && v.equals(other.u))
                    && weight == other.weight;
        }

        @Override
        public String toString() {
            return "%s -- %s (w=%d)".formatted(u, v, weight);
        }
    }

    private static final class DisjointSet<T> {
        private final Map<T, T> parent = new HashMap<>();
        private final Map<T, Integer> rank = new HashMap<>();

        DisjointSet(Collection<T> elements) {
            for (T element : elements) {
                parent.put(element, element);
                rank.put(element, 0);
            }
        }

        T find(T element) {
            T parentElement = parent.get(element);
            if (!parentElement.equals(element)) {
                parent.put(element, find(parentElement));
            }
            return parent.get(element);
        }

        boolean union(T first, T second) {
            T rootFirst = find(first);
            T rootSecond = find(second);
            if (rootFirst.equals(rootSecond)) {
                return false;
            }

            int rankFirst = rank.get(rootFirst);
            int rankSecond = rank.get(rootSecond);

            if (rankFirst < rankSecond) {
                parent.put(rootFirst, rootSecond);
            } else if (rankFirst > rankSecond) {
                parent.put(rootSecond, rootFirst);
            } else {
                parent.put(rootSecond, rootFirst);
                rank.put(rootFirst, rankFirst + 1);
            }
            return true;
        }
    }
}


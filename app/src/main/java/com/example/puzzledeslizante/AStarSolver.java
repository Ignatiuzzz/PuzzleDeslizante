package com.example.puzzledeslizante;

import java.util.*;

public class AStarSolver {

    private static class Node {
        int[][] board;
        int emptyRow, emptyCol, g, h;
        Node parent;

        Node(int[][] board, int emptyRow, int emptyCol, int g, Node parent) {
            this.board = board;
            this.emptyRow = emptyRow;
            this.emptyCol = emptyCol;
            this.g = g;
            this.h = calculateHeuristic(board);
            this.parent = parent;
        }

        int calculateHeuristic(int[][] board) {
            int h = 0;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    int value = board[i][j];
                    if (value != 0) {
                        int targetRow = (value - 1) / 3;
                        int targetCol = (value - 1) % 3;
                        h += Math.abs(i - targetRow) + Math.abs(j - targetCol);
                    }
                }
            }
            return h;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Node) {
                Node other = (Node) obj;
                return Arrays.deepEquals(this.board, other.board);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(board);
        }
    }

    public List<int[][]> solve(int[][] initialBoard) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(n -> n.g + n.h));
        Set<Node> closedSet = new HashSet<>();

        Node startNode = new Node(initialBoard, findEmptyTile(initialBoard)[0], findEmptyTile(initialBoard)[1], 0, null);
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            if (current.h == 0) {
                return constructPath(current);
            }

            closedSet.add(current);
            for (Node neighbor : getNeighbors(current)) {
                if (closedSet.contains(neighbor)) continue;

                if (!openSet.contains(neighbor)) {
                    openSet.add(neighbor);
                }
            }
        }

        return new ArrayList<>();
    }

    private List<int[][]> constructPath(Node node) {
        List<int[][]> path = new ArrayList<>();
        while (node != null) {
            path.add(node.board);
            node = node.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        for (int[] dir : directions) {
            int newRow = node.emptyRow + dir[0];
            int newCol = node.emptyCol + dir[1];

            if (newRow >= 0 && newRow < 3 && newCol >= 0 && newCol < 3) {
                int[][] newBoard = copyBoard(node.board);
                newBoard[node.emptyRow][node.emptyCol] = newBoard[newRow][newCol];
                newBoard[newRow][newCol] = 0;
                neighbors.add(new Node(newBoard, newRow, newCol, node.g + 1, node));
            }
        }

        return neighbors;
    }

    private int[] findEmptyTile(int[][] board) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == 0) {
                    return new int[]{i, j};
                }
            }
        }
        return null;
    }

    private int[][] copyBoard(int[][] board) {
        int[][] newBoard = new int[3][3];
        for (int i = 0; i < 3; i++) {
            newBoard[i] = board[i].clone();
        }
        return newBoard;
    }
}

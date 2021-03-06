import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class MazeSolverWithPower implements IMazeSolverWithPower {
	private static final int NORTH = 0, SOUTH = 1, EAST = 2, WEST = 3;
	private static int[][] DELTAS = new int[][] {
		{ -1, 0 }, // North
		{ 1, 0 }, // South
		{ 0, 1 }, // East
		{ 0, -1 } // West
	};

	private Maze maze;
	private boolean[][] visited;
	private boolean solved = false;

	private int startRow;
	private int startCol;
	private int superpowers;
	private boolean pathSearchPower;


	//For numReachable
	//Array with number of rooms reachable with index steps
	//Index 2 will contain number of rooms reachable with 2 steps
	private ArrayList<Integer> numRoomSteps = new ArrayList<Integer>();


	public MazeSolverWithPower() {
		solved = false;
		maze = null;
	}

	@Override
	public void initialize(Maze maze) {
		this.maze = maze;
		visited = new boolean[maze.getRows()][maze.getColumns()];
		solved = false;
	}

	@Override
	public Integer pathSearch(int startRow, int startCol, int endRow, int endCol) throws Exception {
		if (maze == null) {
			throw new Exception("Oh no! You cannot call me without initializing the maze!");
		}

		if (startRow < 0 || startCol < 0 || startRow >= maze.getRows() || startCol >= maze.getColumns() ||
				endRow < 0 || endCol < 0 || endRow >= maze.getRows() || endCol >= maze.getColumns()) {
			throw new IllegalArgumentException("Invalid start/end coordinate");
		}

		this.startRow = startRow;
		this.startCol = startCol;
		this.pathSearchPower = false;

		//Reset numRoomSteps each time pathSearch is called
		this.numRoomSteps = new ArrayList<Integer>();

		//Reset room fields before search
		for (int i = 0; i < maze.getRows(); ++i) {
			for (int j = 0; j < maze.getColumns(); ++j) {
				this.visited[i][j] = false;
				maze.getRoom(i, j).onPath = false;
			}
		}

		//Hashmap for room parent when traversing maze
		//Key is a room
		//Value is an int[] array of size 2, index 0 for row, index 1 for col
		HashMap<Room, int[]> pathMap = new HashMap<>();

		//Number of steps required to reach end point
		int steps = 0;

		//Frontier
		//Each element will be an int array of size 2
		//0 index - row coordinate
		//1 index - column coordinate
		ArrayList<int []> frontier = new ArrayList<>();

		//Add start point to frontier
		this.visited[startRow][startCol] = true;
		frontier.add(new int[] {startRow, startCol});

		//Set starting room onPath to true
		//Set to false later if no path is found
		maze.getRoom(startRow, startCol).onPath = true;

		//BFS
		while (!frontier.isEmpty()) {
			ArrayList<int[]> nextFrontier = new ArrayList<>();

			for (int[] rc : frontier) {
				int row = rc[0];
				int col = rc[1];

				//Testing for end point
				if (row == endRow && col == endCol) {
					this.solved = true;
					Room room = maze.getRoom(row, col);

					//Set rooms traversed to onPath = true
					while(pathMap.containsKey(room)) {
						room.onPath = true;
						room = maze.getRoom(pathMap.get(room)[0], pathMap.get(room)[1]);
					}

					return steps;
				}

				Room r = maze.getRoom(row, col);

				for (int direction = 0; direction < 4; direction++) {
					int newRow = row + DELTAS[direction][0];
					int newCol = col + DELTAS[direction][1];

					if (canGo(row, col, direction)) {
						if (!visited[newRow][newCol]) {
							visited[newRow][newCol] = true;
							nextFrontier.add(new int[] {newRow, newCol});
							pathMap.put(maze.getRoom(newRow, newCol), new int[] {row, col});
						}
					}
				}
			}

			steps++;
			frontier = nextFrontier;
		}

		maze.getRoom(startRow, startCol).onPath = false;
		return null;
	}

	@Override
	public Integer numReachable(int k) throws Exception {

		if (numRoomSteps.isEmpty()) {

			//Reset visited
			for (int i = 0; i < maze.getRows(); ++i) {
				for (int j = 0; j < maze.getColumns(); ++j) {
					this.visited[i][j] = false;
				}
			}

			//BFS without power
			if (!pathSearchPower) {
				ArrayList<int []> frontier = new ArrayList<>();

				this.visited[this.startRow][this.startCol] = true;
				frontier.add(new int[] {startRow, startCol});

				while (!frontier.isEmpty()) {
					ArrayList<int[]> nextFrontier = new ArrayList<>();

					//Adding number of rooms reachable within index steps
					numRoomSteps.add(frontier.size());

					//Checking which paths are available
					for (int[] rc : frontier) {
						int row = rc[0];
						int col = rc[1];

						for (int direction = 0; direction < 4; direction++) {
							int newRow = row + DELTAS[direction][0];
							int newCol = col + DELTAS[direction][1];

							if (canGo(row, col, direction)) {
								if (!visited[newRow][newCol]) {
									visited[newRow][newCol] = true;
									nextFrontier.add(new int[] {newRow, newCol});
								}
							}
						}
					}

					frontier = nextFrontier;
				}

			//BFS with powers
			} else {

				ArrayList<int []> frontier = new ArrayList<>();
				boolean[][][] powVisited = new boolean[maze.getRows()][maze.getColumns()][superpowers + 1];


				this.visited[this.startRow][this.startCol] = true;
				frontier.add(new int[] {startRow, startCol, superpowers});

				//You can only reach 1 room with 0 steps
				int forNumRoomSteps = 1;

				while (!frontier.isEmpty()) {

					ArrayList<int[]> nextFrontier = new ArrayList<>();


					//Adding number of rooms reachable within index steps
					numRoomSteps.add(forNumRoomSteps);
					forNumRoomSteps = 0; //Reset for each frontier update

					for (int[] rcp : frontier) {
						int row = rcp[0];
						int col = rcp[1];
						int power = rcp[2];


						for (int direction = 0; direction < 4; direction++) {
							int newRow = row + DELTAS[direction][0];
							int newCol = col + DELTAS[direction][1];

							if (newRow < 0 || newRow > maze.getRows() - 1 || newCol < 0 || newCol > maze.getColumns() - 1) {
								continue;
							}

							if (canGo(row, col, direction)) {
								if (!powVisited[newRow][newCol][power]) {
									powVisited[newRow][newCol][power] = true;
									nextFrontier.add(new int[] {newRow, newCol, power});

									if (!visited[newRow][newCol]) {
										forNumRoomSteps++;
										visited[newRow][newCol] = true;
									}
								}

							} else {
								if (power > 0 && !powVisited[newRow][newCol][power - 1]) {
									powVisited[newRow][newCol][power - 1] = true;
									nextFrontier.add(new int[] {newRow, newCol, power - 1});

									if (!visited[newRow][newCol]) {
										forNumRoomSteps++;
										visited[newRow][newCol] = true;
									}
								}
							}
						}
					}
					frontier = nextFrontier;
				}
			}
		}

		if (k > numRoomSteps.size() - 1) {
			return 0;
		}

		return numRoomSteps.get(k);
	}

	private boolean canGo(int row, int col, int dir) {

		if (dir == NORTH) {
			return !maze.getRoom(row, col).hasNorthWall();
		}

		if (dir == SOUTH) {
			return !maze.getRoom(row, col).hasSouthWall();
		}

		if (dir == EAST) {
			return !maze.getRoom(row, col).hasEastWall();
		}

		if (dir == WEST) {
			return !maze.getRoom(row, col).hasWestWall();
		}

		return false;
	}

	@Override
	public Integer pathSearch(int startRow, int startCol, int endRow,
							  int endCol, int superpowers) throws Exception {

		if (superpowers <= 0) {
			return pathSearch(startRow, startCol, endRow, endCol);
		}

		if (maze == null) {
			throw new Exception("Oh no! You cannot call me without initializing the maze!");
		}

		if (startRow < 0 || startCol < 0 || startRow >= maze.getRows() || startCol >= maze.getColumns() ||
				endRow < 0 || endCol < 0 || endRow >= maze.getRows() || endCol >= maze.getColumns()) {
			throw new IllegalArgumentException("Invalid start/end coordinate");
		}

		//Updating fields
		this.startRow = startRow;
		this.startCol = startCol;
		this.superpowers = superpowers;
		this.pathSearchPower = true;

		//Reset numRoomSteps
		this.numRoomSteps = new ArrayList<>();


		//Array that stores room coordinates and power
		//[row][col][pow]{row, col, pow}
		int[][][][] rowcolpow = new int[maze.getRows()][maze.getColumns()][superpowers + 1][3];

		//Key is current room, Value is parent room of key
		//To be used with rowcolpow
		HashMap<int[], int[]> pathMap = new HashMap<>();

		//Reset room fields before search
		for (int i = 0; i < maze.getRows(); ++i) {
			for (int j = 0; j < maze.getColumns(); ++j) {
				this.visited[i][j] = false;
				maze.getRoom(i, j).onPath = false;
			}
		}


		//Mark start room as visited
		boolean[][][] visited = new boolean[maze.getRows()][maze.getColumns()][superpowers + 1];
		visited[startRow][startCol][superpowers] = true;

		//Add start room to frontier
		ArrayList<int[]> frontier = new ArrayList<>();
		frontier.add(new int[] {startRow, startCol, superpowers});
		maze.getRoom(startRow, startCol).onPath = true;

		int steps = 0;

		while (!frontier.isEmpty()) {

			ArrayList<int[]> nextFrontier = new ArrayList<>();

			for (int[] rcp : frontier) {
				int row = rcp[0];
				int col = rcp[1];
				int power = rcp[2];

				if (row == endRow && col == endCol) {
					this.solved = true;
					int[] coords = rowcolpow[row][col][power];
					while (pathMap.containsKey(coords)) {
						maze.getRoom(coords[0], coords[1]).onPath = true;
						coords = pathMap.get(coords);
					}

					return steps;
				}


				for (int direction = 0; direction < 4; direction++) {
					int newRow = row + DELTAS[direction][0];
					int newCol = col + DELTAS[direction][1];

					if (newRow < 0 || newRow > maze.getRows() - 1 || newCol < 0 || newCol > maze.getColumns() - 1) {
						continue;
					}

					//Directions without walls
					if (canGo(row, col, direction)) {
						if (!visited[newRow][newCol][power]) {
							visited[newRow][newCol][power] = true;
							nextFrontier.add(new int[] {newRow, newCol, power});

							rowcolpow[newRow][newCol][power] = new int[] {newRow, newCol, power};
							pathMap.put(rowcolpow[newRow][newCol][power], rowcolpow[row][col][power]);
						}

					//Directions with walls
					} else {
						if (power > 0 && !visited[newRow][newCol][power - 1]) {
							visited[newRow][newCol][power - 1] = true;
							nextFrontier.add(new int[] {newRow, newCol, power - 1});

							rowcolpow[newRow][newCol][power - 1] = new int[] {newRow, newCol, power - 1};
							pathMap.put(rowcolpow[newRow][newCol][power - 1], rowcolpow[row][col][power]);
						}
					}
				}
			}
			steps++;
			frontier = nextFrontier;
		}

		maze.getRoom(startRow, startCol).onPath = false;
		return null;
	}





	public static void main(String[] args) {
		try {
			Maze maze = Maze.readMaze("maze-sample.txt");
			IMazeSolverWithPower solver = new MazeSolverWithPower();
			solver.initialize(maze);

			System.out.println(solver.pathSearch(0, 0, 1, 0, 2));
			MazePrinter.printMaze(maze);

			for (int i = 0; i <= 9; ++i) {
				System.out.println("Steps " + i + " Rooms: " + solver.numReachable(i));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
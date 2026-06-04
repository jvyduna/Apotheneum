package apotheneum.doved.patterns;

import apotheneum.Apotheneum;
import apotheneum.ApotheneumPattern;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import java.util.ArrayList;
import java.util.List;

@LXCategory("Apotheneum/doved")
public class EdgeTracer extends ApotheneumPattern {
    
    public enum Surface {
        CYLINDER("Cylinder"),
        CUBE("Cube"),
        BOTH("Both");
        
        private final String label;
        Surface(String label) { this.label = label; }
        @Override
        public String toString() { return label; }
    }
    
    public enum CubeMode {
        PER_FACE("Per Face"),
        BOTTOM("Bottom"),
        TOP("Top"),
        FRONT_FLAT("Front Flat");
        
        private final String label;
        CubeMode(String label) { this.label = label; }
        @Override
        public String toString() { return label; }
    }
    
    public final EnumParameter<Surface> surface = 
        new EnumParameter<Surface>("Surface", Surface.CYLINDER)
        .setDescription("Which surface to trace");
    
    public final EnumParameter<CubeMode> cubeMode = 
        new EnumParameter<CubeMode>("Cube Mode", CubeMode.BOTTOM)
        .setDescription("How to trace the cube edges");
    
    public final CompoundParameter position = 
        new CompoundParameter("Position", 0, 0, 1)
        .setDescription("Position along the edge path (0-1)");
    
    public final CompoundParameter width = 
        new CompoundParameter("Width", 5, 0.5, 50)
        .setDescription("Thickness of the traced line (in LEDs)");
    
    public final CompoundParameter length = 
        new CompoundParameter("Length", 0.05, 0.01, 0.3)
        .setDescription("Length of the traced line");
    
    public final CompoundParameter brightness = 
        new CompoundParameter("Brightness", 1, 0, 1)
        .setDescription("Brightness of the trace");
    
    // Pre-allocated collections for performance
    private final List<LXPoint> activePointsBuffer = new ArrayList<>();
    
    private List<LXPoint> cylinderBottomPath;
    private List<LXPoint> cylinderBottomPathInterior;
    private List<LXPoint> cubeBottomPath;
    private List<LXPoint> cubeBottomPathInterior;
    private List<LXPoint> cubeTopPath;
    private List<LXPoint> cubeTopPathInterior;
    private List<LXPoint> cubeFrontFlatPath;
    private List<LXPoint> cubeFrontFlatPathInterior;
    private List<List<LXPoint>> cubeFacePaths;
    private List<List<LXPoint>> cubeFacePathsInterior;
    
    public EdgeTracer(LX lx) {
        super(lx);
        addParameter("surface", surface);
        addParameter("cubeMode", cubeMode);
        addParameter("position", position);
        addParameter("width", width);
        addParameter("length", length);
        addParameter("brightness", brightness);
        
        buildPaths();
    }
    
    private void buildPaths() {
        buildCylinderBottomPath();
        buildCylinderBottomPathInterior();
        buildCubeBottomPath();
        buildCubeBottomPathInterior();
        buildCubeTopPath();
        buildCubeTopPathInterior();
        buildCubeFrontFlatPath();
        buildCubeFrontFlatPathInterior();
        buildCubeFacePaths();
        buildCubeFacePathsInterior();
    }
    
    // Door handling utility methods
    private static class DoorTraversal {
        
        static void addPointsWithDoorHandling(List<LXPoint> path, Apotheneum.Column[] columns,
                                             Apotheneum.Orientation orientation, int faceOffset, 
                                             int heightConstant, int doorHeight) {
            for (int col = 0; col < columns.length; col++) {
                int globalCol = faceOffset + col;
                int available = orientation.available(globalCol);
                Apotheneum.Column column = columns[col];
                
                boolean isInDoor = available < heightConstant;
                boolean nextInDoor = (col < columns.length - 1) && 
                                   (orientation.available(faceOffset + col + 1) < heightConstant);
                
                if (!isInDoor) {
                    // Normal bottom edge - add the bottom point
                    path.add(column.points[column.points.length - 1]);
                    
                    // If next column is a door, add the vertical going up
                    if (nextInDoor) {
                        addVerticalTransition(path, column, heightConstant, doorHeight);
                    }
                } else {
                    // We're in a door column
                    if (available > 0) {
                        // Add the bottom-most available point (top of door opening)
                        path.add(column.points[available - 1]);
                    }
                    
                    // If this is the last door column, add the descent
                    if (!nextInDoor && col < columns.length - 1) {
                        Apotheneum.Column nextColumn = columns[col + 1];
                        addDescentTransition(path, nextColumn, heightConstant, doorHeight);
                    }
                }
            }
        }
        
        static void addVerticalTransition(List<LXPoint> path, Apotheneum.Column column, int heightConstant, int doorHeight) {
            for (int y = heightConstant - 2; y >= heightConstant - doorHeight; y--) {
                if (y >= 0 && y < column.points.length) {
                    path.add(column.points[y]);
                }
            }
        }
        
        static void addDescentTransition(List<LXPoint> path, Apotheneum.Column nextColumn, int heightConstant, int doorHeight) {
            for (int y = heightConstant - doorHeight; y < heightConstant; y++) {
                if (y >= 0 && y < nextColumn.points.length) {
                    path.add(nextColumn.points[y]);
                }
            }
        }
        
        static void addCubeFacePerimeterWithDoors(List<LXPoint> facePath, Apotheneum.Cube.Face cubeFace,
                                                 Apotheneum.Orientation orientation, int face) {
            // Bottom edge with door handling
            DoorTraversal.addPointsWithDoorHandling(facePath, cubeFace.columns, orientation, 
                                                   face * Apotheneum.GRID_WIDTH, 
                                                   Apotheneum.GRID_HEIGHT, Apotheneum.DOOR_HEIGHT);
            
            // Right edge (bottom to top)
            Apotheneum.Column rightColumn = cubeFace.columns[cubeFace.columns.length - 1];
            for (int y = rightColumn.points.length - 2; y >= 0; y--) {
                facePath.add(rightColumn.points[y]);
            }
            
            // Top edge (right to left)
            for (int col = cubeFace.columns.length - 2; col >= 0; col--) {
                Apotheneum.Column column = cubeFace.columns[col];
                if (column.points.length > 0) {
                    facePath.add(column.points[0]);
                }
            }
            
            // Left edge (top to bottom) - but don't duplicate the starting point
            Apotheneum.Column leftColumn = cubeFace.columns[0];
            int leftAvailable = orientation.available(face * Apotheneum.GRID_WIDTH);
            int startY = (leftAvailable < Apotheneum.GRID_HEIGHT) ? leftAvailable - 1 : leftColumn.points.length - 2;
            
            for (int y = 1; y <= startY; y++) {
                facePath.add(leftColumn.points[y]);
            }
        }
        
        static void addCubeFacePerimeterWithDoorsInterior(List<LXPoint> facePath, 
                                                         Apotheneum.Orientation orientation, int face) {
            // Bottom edge with door handling
            for (int col = 0; col < Apotheneum.GRID_WIDTH; col++) {
                int globalCol = face * Apotheneum.GRID_WIDTH + col;
                int available = orientation.available(globalCol);
                Apotheneum.Column column = orientation.columns()[globalCol];
                
                boolean isInDoor = available < Apotheneum.GRID_HEIGHT;
                boolean nextInDoor = (col < Apotheneum.GRID_WIDTH - 1) && 
                                   (orientation.available(face * Apotheneum.GRID_WIDTH + col + 1) < Apotheneum.GRID_HEIGHT);
                
                if (!isInDoor) {
                    facePath.add(column.points[column.points.length - 1]);
                    if (nextInDoor) {
                        addVerticalTransition(facePath, column, Apotheneum.GRID_HEIGHT, Apotheneum.DOOR_HEIGHT);
                    }
                } else {
                    if (available > 0) {
                        facePath.add(column.points[available - 1]);
                    }
                    if (!nextInDoor && col < Apotheneum.GRID_WIDTH - 1) {
                        int nextGlobalCol = face * Apotheneum.GRID_WIDTH + col + 1;
                        Apotheneum.Column nextColumn = orientation.columns()[nextGlobalCol];
                        addDescentTransition(facePath, nextColumn, Apotheneum.GRID_HEIGHT, Apotheneum.DOOR_HEIGHT);
                    }
                }
            }
            
            // Right edge (bottom to top)
            int rightGlobalCol = face * Apotheneum.GRID_WIDTH + (Apotheneum.GRID_WIDTH - 1);
            Apotheneum.Column rightColumn = orientation.columns()[rightGlobalCol];
            for (int y = rightColumn.points.length - 2; y >= 0; y--) {
                facePath.add(rightColumn.points[y]);
            }
            
            // Top edge (right to left)
            for (int col = Apotheneum.GRID_WIDTH - 2; col >= 0; col--) {
                int globalCol = face * Apotheneum.GRID_WIDTH + col;
                Apotheneum.Column column = orientation.columns()[globalCol];
                if (column.points.length > 0) {
                    facePath.add(column.points[0]);
                }
            }
            
            // Left edge (top to bottom) - but don't duplicate the starting point
            int leftGlobalCol = face * Apotheneum.GRID_WIDTH;
            Apotheneum.Column leftColumn = orientation.columns()[leftGlobalCol];
            int leftAvailable = orientation.available(leftGlobalCol);
            int startY = (leftAvailable < Apotheneum.GRID_HEIGHT) ? leftAvailable - 1 : leftColumn.points.length - 2;
            
            for (int y = 1; y <= startY; y++) {
                facePath.add(leftColumn.points[y]);
            }
        }
    }
    
    private void buildCylinderBottomPath() {
        cylinderBottomPath = new ArrayList<>();
        
        if (!Apotheneum.exists) return;
        
        Apotheneum.Cylinder cylinder = Apotheneum.cylinder;
        DoorTraversal.addPointsWithDoorHandling(cylinderBottomPath, cylinder.exterior.columns,
                                               cylinder.exterior, 0, 
                                               Apotheneum.CYLINDER_HEIGHT, Apotheneum.DOOR_HEIGHT);
    }
    
    private void buildCylinderBottomPathInterior() {
        cylinderBottomPathInterior = new ArrayList<>();
        
        if (!Apotheneum.exists) return;
        
        Apotheneum.Cylinder cylinder = Apotheneum.cylinder;
        DoorTraversal.addPointsWithDoorHandling(cylinderBottomPathInterior, cylinder.interior.columns,
                                               cylinder.interior, 0, 
                                               Apotheneum.CYLINDER_HEIGHT, Apotheneum.DOOR_HEIGHT);
    }
    
    private void buildCubeBottomPath() {
        cubeBottomPath = new ArrayList<>();
        
        if (!Apotheneum.exists) return;
        
        Apotheneum.Cube cube = Apotheneum.cube;
        
        // Traverse around all 4 faces bottom edge, going up and around doors
        for (int face = 0; face < 4; face++) {
            Apotheneum.Cube.Face cubeFace = cube.faces[face];
            DoorTraversal.addPointsWithDoorHandling(cubeBottomPath, cubeFace.columns,
                                                   cube.exterior, face * Apotheneum.GRID_WIDTH, 
                                                   Apotheneum.GRID_HEIGHT, Apotheneum.DOOR_HEIGHT);
        }
    }
    
    private void buildCubeTopPath() {
        cubeTopPath = new ArrayList<>();
        
        if (!Apotheneum.exists) return;
        
        Apotheneum.Cube cube = Apotheneum.cube;
        
        // Simple top edge - no doors to worry about
        for (int face = 0; face < 4; face++) {
            Apotheneum.Cube.Face cubeFace = cube.faces[face];
            for (Apotheneum.Column column : cubeFace.columns) {
                if (column.points.length > 0) {
                    cubeTopPath.add(column.points[0]);
                }
            }
        }
    }
    
    private void buildCubeFacePaths() {
        cubeFacePaths = new ArrayList<>();
        
        if (!Apotheneum.exists) return;
        
        Apotheneum.Cube cube = Apotheneum.cube;
        
        // Build a separate path for each face
        for (int face = 0; face < 4; face++) {
            List<LXPoint> facePath = new ArrayList<>();
            Apotheneum.Cube.Face cubeFace = cube.faces[face];
            
            DoorTraversal.addCubeFacePerimeterWithDoors(facePath, cubeFace, cube.exterior, face);
            cubeFacePaths.add(facePath);
        }
    }
    
    private void buildCubeBottomPathInterior() {
        cubeBottomPathInterior = new ArrayList<>();
        
        if (!Apotheneum.exists) return;
        
        Apotheneum.Cube cube = Apotheneum.cube;
        
        // Build bottom path for interior using special handling for interior columns
        for (int face = 0; face < 4; face++) {
            for (int col = 0; col < Apotheneum.GRID_WIDTH; col++) {
                int globalCol = face * Apotheneum.GRID_WIDTH + col;
                int available = cube.interior.available(globalCol);
                Apotheneum.Column column = cube.interior.columns()[globalCol];
                
                boolean isInDoor = available < Apotheneum.GRID_HEIGHT;
                boolean nextInDoor = (col < Apotheneum.GRID_WIDTH - 1) && 
                                   (cube.interior.available(face * Apotheneum.GRID_WIDTH + col + 1) < Apotheneum.GRID_HEIGHT);
                
                if (!isInDoor) {
                    cubeBottomPathInterior.add(column.points[column.points.length - 1]);
                    if (nextInDoor) {
                        DoorTraversal.addVerticalTransition(cubeBottomPathInterior, column, Apotheneum.GRID_HEIGHT, Apotheneum.DOOR_HEIGHT);
                    }
                } else {
                    if (available > 0) {
                        cubeBottomPathInterior.add(column.points[available - 1]);
                    }
                    if (!nextInDoor && col < Apotheneum.GRID_WIDTH - 1) {
                        int nextGlobalCol = face * Apotheneum.GRID_WIDTH + col + 1;
                        Apotheneum.Column nextColumn = cube.interior.columns()[nextGlobalCol];
                        DoorTraversal.addDescentTransition(cubeBottomPathInterior, nextColumn, Apotheneum.GRID_HEIGHT, Apotheneum.DOOR_HEIGHT);
                    }
                }
            }
        }
    }
    
    private void buildCubeTopPathInterior() {
        cubeTopPathInterior = new ArrayList<>();
        
        if (!Apotheneum.exists) return;
        
        Apotheneum.Cube cube = Apotheneum.cube;
        
        // Simple top edge interior - no doors to worry about
        for (int face = 0; face < 4; face++) {
            for (int col = 0; col < Apotheneum.GRID_WIDTH; col++) {
                int globalCol = face * Apotheneum.GRID_WIDTH + col;
                Apotheneum.Column column = cube.interior.columns[globalCol];
                if (column.points.length > 0) {
                    cubeTopPathInterior.add(column.points[0]);
                }
            }
        }
    }
    
    private void buildCubeFacePathsInterior() {
        cubeFacePathsInterior = new ArrayList<>();
        
        if (!Apotheneum.exists) return;
        
        Apotheneum.Cube cube = Apotheneum.cube;
        
        // Build a separate path for each face interior
        for (int face = 0; face < 4; face++) {
            List<LXPoint> facePath = new ArrayList<>();
            
            DoorTraversal.addCubeFacePerimeterWithDoorsInterior(facePath, cube.interior, face);
            cubeFacePathsInterior.add(facePath);
        }
    }
    
    private void buildCubeFrontFlatPath() {
        cubeFrontFlatPath = buildFlatPath(Apotheneum.cube.exterior, 0, Apotheneum.cube.faces[0]);
    }
    
    private void buildCubeFrontFlatPathInterior() {
        cubeFrontFlatPathInterior = buildFlatPathInterior(Apotheneum.cube.interior, 0);
    }
    
    // Generic helper methods for building flat paths
    private List<LXPoint> buildFlatPath(Apotheneum.Orientation orientation, int frontFace, Apotheneum.Cube.Face cubeFace) {
        List<LXPoint> path = new ArrayList<>();
        
        if (!Apotheneum.exists) return path;
        
        // For a truly flat line, use the bottom-most available LED from each column
        // This ensures all LEDs are at the same physical height (door level)
        for (int col = 0; col < cubeFace.columns.length; col++) {
            int globalCol = frontFace * Apotheneum.GRID_WIDTH + col;
            int available = orientation.available(globalCol);
            Apotheneum.Column column = cubeFace.columns[col];
            
            // Always use the last available LED (bottom-most) from each column
            if (available > 0 && (available - 1) < column.points.length) {
                path.add(column.points[available - 1]);
            }
        }
        
        return path;
    }
    
    private List<LXPoint> buildFlatPathInterior(Apotheneum.Orientation orientation, int frontFace) {
        List<LXPoint> path = new ArrayList<>();
        
        if (!Apotheneum.exists) return path;
        
        // For a truly flat line, use the bottom-most available LED from each column
        // This ensures all LEDs are at the same physical height (door level)
        for (int col = 0; col < Apotheneum.GRID_WIDTH; col++) {
            int globalCol = frontFace * Apotheneum.GRID_WIDTH + col;
            int available = orientation.available(globalCol);
            Apotheneum.Column column = orientation.columns()[globalCol];
            
            // Always use the last available LED (bottom-most) from each column
            if (available > 0 && (available - 1) < column.points.length) {
                path.add(column.points[available - 1]);
            }
        }
        
        return path;
    }
    
    @Override
    public void render(double deltaMs) {
        if (!Apotheneum.exists) return;
        
        // Clear all colors
        setColors(LXColor.BLACK);
        
        Surface surf = surface.getEnum();
        
        // Draw cylinder - both exterior and interior
        if (surf == Surface.CYLINDER || surf == Surface.BOTH) {
            drawPath(cylinderBottomPath, position.getValue());
            drawPath(cylinderBottomPathInterior, position.getValue());
        }
        
        // Draw cube based on mode - both exterior and interior
        if (surf == Surface.CUBE || surf == Surface.BOTH) {
            CubeMode mode = cubeMode.getEnum();
            
            switch (mode) {
                case BOTTOM:
                    drawPath(cubeBottomPath, position.getValue());
                    drawPath(cubeBottomPathInterior, position.getValue());
                    break;
                    
                case TOP:
                    drawPath(cubeTopPath, position.getValue());
                    drawPath(cubeTopPathInterior, position.getValue());
                    break;
                    
                case PER_FACE:
                    // Draw all 4 face paths - exterior and interior
                    if (cubeFacePaths != null) {
                        for (List<LXPoint> facePath : cubeFacePaths) {
                            drawPath(facePath, position.getValue());
                        }
                    }
                    if (cubeFacePathsInterior != null) {
                        for (List<LXPoint> facePath : cubeFacePathsInterior) {
                            drawPath(facePath, position.getValue());
                        }
                    }
                    break;
                    
                case FRONT_FLAT:
                    // Draw straight front edge - exterior and interior
                    drawPath(cubeFrontFlatPath, position.getValue());
                    drawPath(cubeFrontFlatPathInterior, position.getValue());
                    break;
            }
        }
        
    }
    
    private void drawPath(List<LXPoint> path, double pos) {
        if (path == null || path.isEmpty()) return;
        
        int pathLength = path.size();
        double lengthInPoints = length.getValue() * pathLength;
        double centerPoint = pos * pathLength;
        double thicknessRadiusSquared = width.getValue() * width.getValue(); // Use squared distance to avoid sqrt
        
        // Clear and reuse pre-allocated buffer instead of creating new ArrayList
        activePointsBuffer.clear();
        
        // Get the points along the path that should be lit (trailing behavior)
        // Trail extends backwards from current position, clipped at start
        // Add small offset so position 0 starts after the first corner, not at it
        int currentIndex = (int) Math.floor(centerPoint + 1) % pathLength;
        
        for (int i = 0; i < lengthInPoints && i < pathLength; i++) {
            // Calculate the index starting from current position and going backwards
            // i=0 is the current position, i=1 is one step behind, etc.
            int trailIndex = currentIndex - i;
            
            // Clip at the beginning - don't wrap around
            if (trailIndex < 0) {
                break; // Stop adding points when we reach the beginning
            }
            
            activePointsBuffer.add(path.get(trailIndex));
        }
        
        // Pre-calculate color once
        int color = LXColor.gray((float)(brightness.getValue() * 100));
        
        // Now find all nearby points within thickness radius for each active point
        for (LXPoint pathPoint : activePointsBuffer) {
            for (LXPoint point : model.points) {
                // Use squared distance comparison to avoid expensive sqrt calculation
                double dx = point.x - pathPoint.x;
                double dy = point.y - pathPoint.y;
                double dz = point.z - pathPoint.z;
                double distSquared = dx*dx + dy*dy + dz*dz;
                
                if (distSquared <= thicknessRadiusSquared) {
                    colors[point.index] = LXColor.add(colors[point.index], color);
                }
            }
        }
    }
}
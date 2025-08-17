package apotheneum.doved.patterns;

import apotheneum.Apotheneum;
import apotheneum.ApotheneumPattern;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.model.LXModel;
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
    
    private void buildCylinderBottomPath() {
        cylinderBottomPath = new ArrayList<>();
        
        if (!Apotheneum.exists) return;
        
        Apotheneum.Cylinder cylinder = Apotheneum.cylinder;
        
        // Traverse around the cylinder bottom, going up and around doors
        for (int col = 0; col < cylinder.exterior.columns.length; col++) {
            int available = cylinder.exterior.available(col);
            LXModel column = cylinder.exterior.columns[col];
            
            // Check if this column is part of a door
            boolean isInDoor = available < Apotheneum.CYLINDER_HEIGHT;
            boolean prevInDoor = (col > 0) && (cylinder.exterior.available(col - 1) < Apotheneum.CYLINDER_HEIGHT);
            boolean nextInDoor = (col < cylinder.exterior.columns.length - 1) && (cylinder.exterior.available(col + 1) < Apotheneum.CYLINDER_HEIGHT);
            
            if (!isInDoor) {
                // Normal bottom edge - add the bottom point
                cylinderBottomPath.add(column.points[column.points.length - 1]);
                
                // If next column is a door, add the vertical going up
                if (nextInDoor) {
                    // Travel up the right edge of this column to the door height
                    for (int y = Apotheneum.CYLINDER_HEIGHT - 2; y >= Apotheneum.CYLINDER_HEIGHT - Apotheneum.DOOR_HEIGHT; y--) {
                        if (y >= 0 && y < column.points.length) {
                            cylinderBottomPath.add(column.points[y]);
                        }
                    }
                }
            } else {
                // We're in a door column
                // The door columns have fewer points (only above the door)
                if (available > 0) {
                    // Add the bottom-most available point (top of door opening)
                    cylinderBottomPath.add(column.points[available - 1]);
                }
                
                // If this is the last door column, add the descent
                if (!nextInDoor && col < cylinder.exterior.columns.length - 1) {
                    // Next column is not a door, so travel down on it
                    LXModel nextColumn = cylinder.exterior.columns[col + 1];
                    // Travel down from door height to bottom
                    for (int y = Apotheneum.CYLINDER_HEIGHT - Apotheneum.DOOR_HEIGHT; y < Apotheneum.CYLINDER_HEIGHT; y++) {
                        if (y >= 0 && y < nextColumn.points.length) {
                            cylinderBottomPath.add(nextColumn.points[y]);
                        }
                    }
                }
            }
        }
    }
    
    private void buildCylinderBottomPathInterior() {
        cylinderBottomPathInterior = new ArrayList<>();
        
        if (!Apotheneum.exists) return;
        
        Apotheneum.Cylinder cylinder = Apotheneum.cylinder;
        
        // Traverse around the cylinder bottom interior, going up and around doors
        for (int col = 0; col < cylinder.interior.columns.length; col++) {
            int available = cylinder.interior.available(col);
            LXModel column = cylinder.interior.columns[col];
            
            // Check if this column is part of a door
            boolean isInDoor = available < Apotheneum.CYLINDER_HEIGHT;
            boolean prevInDoor = (col > 0) && (cylinder.interior.available(col - 1) < Apotheneum.CYLINDER_HEIGHT);
            boolean nextInDoor = (col < cylinder.interior.columns.length - 1) && (cylinder.interior.available(col + 1) < Apotheneum.CYLINDER_HEIGHT);
            
            if (!isInDoor) {
                // Normal bottom edge - add the bottom point
                cylinderBottomPathInterior.add(column.points[column.points.length - 1]);
                
                // If next column is a door, add the vertical going up
                if (nextInDoor) {
                    // Travel up the right edge of this column to the door height
                    for (int y = Apotheneum.CYLINDER_HEIGHT - 2; y >= Apotheneum.CYLINDER_HEIGHT - Apotheneum.DOOR_HEIGHT; y--) {
                        if (y >= 0 && y < column.points.length) {
                            cylinderBottomPathInterior.add(column.points[y]);
                        }
                    }
                }
            } else {
                // We're in a door column
                // The door columns have fewer points (only above the door)
                if (available > 0) {
                    // Add the bottom-most available point (top of door opening)
                    cylinderBottomPathInterior.add(column.points[available - 1]);
                }
                
                // If this is the last door column, add the descent
                if (!nextInDoor && col < cylinder.interior.columns.length - 1) {
                    // Next column is not a door, so travel down on it
                    LXModel nextColumn = cylinder.interior.columns[col + 1];
                    // Travel down from door height to bottom
                    for (int y = Apotheneum.CYLINDER_HEIGHT - Apotheneum.DOOR_HEIGHT; y < Apotheneum.CYLINDER_HEIGHT; y++) {
                        if (y >= 0 && y < nextColumn.points.length) {
                            cylinderBottomPathInterior.add(nextColumn.points[y]);
                        }
                    }
                }
            }
        }
    }
    
    private void buildCubeBottomPath() {
        cubeBottomPath = new ArrayList<>();
        
        if (!Apotheneum.exists) return;
        
        Apotheneum.Cube cube = Apotheneum.cube;
        
        // Traverse around all 4 faces bottom edge, going up and around doors
        for (int face = 0; face < 4; face++) {
            Apotheneum.Cube.Face cubeFace = cube.faces[face];
            
            for (int col = 0; col < cubeFace.columns.length; col++) {
                int globalCol = face * Apotheneum.GRID_WIDTH + col;
                int available = cube.exterior.available(globalCol);
                LXModel column = cubeFace.columns[col];
                
                // Check if this column is part of a door
                boolean isInDoor = available < Apotheneum.GRID_HEIGHT;
                boolean prevInDoor = (col > 0) && (cube.exterior.available(face * Apotheneum.GRID_WIDTH + col - 1) < Apotheneum.GRID_HEIGHT);
                boolean nextInDoor = (col < cubeFace.columns.length - 1) && (cube.exterior.available(face * Apotheneum.GRID_WIDTH + col + 1) < Apotheneum.GRID_HEIGHT);
                
                if (!isInDoor) {
                    // Normal bottom edge - add the bottom point
                    cubeBottomPath.add(column.points[column.points.length - 1]);
                    
                    // If next column is a door, add the vertical going up
                    if (nextInDoor) {
                        // Travel up the right edge of this column to the door height
                        for (int y = Apotheneum.GRID_HEIGHT - 2; y >= Apotheneum.GRID_HEIGHT - Apotheneum.DOOR_HEIGHT; y--) {
                            if (y >= 0 && y < column.points.length) {
                                cubeBottomPath.add(column.points[y]);
                            }
                        }
                    }
                } else {
                    // We're in a door column
                    // The door columns have fewer points (only above the door)
                    if (available > 0) {
                        // Add the bottom-most available point (top of door opening)
                        cubeBottomPath.add(column.points[available - 1]);
                    }
                    
                    // If this is the last door column, add the descent
                    if (!nextInDoor && col < cubeFace.columns.length - 1) {
                        // Next column is not a door, so travel down on it
                        LXModel nextColumn = cubeFace.columns[col + 1];
                        // Travel down from door height to bottom
                        for (int y = Apotheneum.GRID_HEIGHT - Apotheneum.DOOR_HEIGHT; y < Apotheneum.GRID_HEIGHT; y++) {
                            if (y >= 0 && y < nextColumn.points.length) {
                                cubeBottomPath.add(nextColumn.points[y]);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void buildCubeTopPath() {
        cubeTopPath = new ArrayList<>();
        
        if (!Apotheneum.exists) return;
        
        Apotheneum.Cube cube = Apotheneum.cube;
        
        // Simple top edge - no doors to worry about
        for (int face = 0; face < 4; face++) {
            Apotheneum.Cube.Face cubeFace = cube.faces[face];
            for (LXModel column : cubeFace.columns) {
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
            
            // Bottom edge (left to right) - handling doors
            for (int col = 0; col < cubeFace.columns.length; col++) {
                int globalCol = face * Apotheneum.GRID_WIDTH + col;
                int available = cube.exterior.available(globalCol);
                LXModel column = cubeFace.columns[col];
                
                boolean isInDoor = available < Apotheneum.GRID_HEIGHT;
                boolean prevInDoor = (col > 0) && (cube.exterior.available(face * Apotheneum.GRID_WIDTH + col - 1) < Apotheneum.GRID_HEIGHT);
                boolean nextInDoor = (col < cubeFace.columns.length - 1) && (cube.exterior.available(face * Apotheneum.GRID_WIDTH + col + 1) < Apotheneum.GRID_HEIGHT);
                
                if (!isInDoor) {
                    // Normal bottom edge
                    facePath.add(column.points[column.points.length - 1]);
                    
                    // If next column is a door, add the vertical going up
                    if (nextInDoor) {
                        for (int y = Apotheneum.GRID_HEIGHT - 2; y >= Apotheneum.GRID_HEIGHT - Apotheneum.DOOR_HEIGHT; y--) {
                            if (y >= 0 && y < column.points.length) {
                                facePath.add(column.points[y]);
                            }
                        }
                    }
                } else {
                    // Door column
                    if (available > 0) {
                        facePath.add(column.points[available - 1]);
                    }
                    
                    // If this is the last door column, add the descent
                    if (!nextInDoor && col < cubeFace.columns.length - 1) {
                        LXModel nextColumn = cubeFace.columns[col + 1];
                        for (int y = Apotheneum.GRID_HEIGHT - Apotheneum.DOOR_HEIGHT; y < Apotheneum.GRID_HEIGHT; y++) {
                            if (y >= 0 && y < nextColumn.points.length) {
                                facePath.add(nextColumn.points[y]);
                            }
                        }
                    }
                }
            }
            
            // Right edge (bottom to top)
            LXModel rightColumn = cubeFace.columns[cubeFace.columns.length - 1];
            for (int y = rightColumn.points.length - 2; y >= 0; y--) {
                facePath.add(rightColumn.points[y]);
            }
            
            // Top edge (right to left)
            for (int col = cubeFace.columns.length - 2; col >= 0; col--) {
                LXModel column = cubeFace.columns[col];
                if (column.points.length > 0) {
                    facePath.add(column.points[0]);
                }
            }
            
            // Left edge (top to bottom) - but don't duplicate the starting point
            LXModel leftColumn = cubeFace.columns[0];
            // Check if the left column is affected by a door
            int leftAvailable = cube.exterior.available(face * Apotheneum.GRID_WIDTH);
            int startY = (leftAvailable < Apotheneum.GRID_HEIGHT) ? leftAvailable - 1 : leftColumn.points.length - 2;
            
            for (int y = 1; y <= startY; y++) {
                facePath.add(leftColumn.points[y]);
            }
            
            cubeFacePaths.add(facePath);
        }
    }
    
    private void buildCubeBottomPathInterior() {
        cubeBottomPathInterior = new ArrayList<>();
        
        if (!Apotheneum.exists) return;
        
        Apotheneum.Cube cube = Apotheneum.cube;
        
        // Traverse around all 4 faces bottom edge interior, going up and around doors
        for (int face = 0; face < 4; face++) {
            Apotheneum.Cube.Face cubeFace = cube.faces[face];
            
            for (int col = 0; col < cubeFace.columns.length; col++) {
                int globalCol = face * Apotheneum.GRID_WIDTH + col;
                int available = cube.interior.available(globalCol);
                LXModel column = cube.interior.columns[globalCol];
                
                // Check if this column is part of a door
                boolean isInDoor = available < Apotheneum.GRID_HEIGHT;
                boolean nextInDoor = (col < cubeFace.columns.length - 1) && (cube.interior.available(face * Apotheneum.GRID_WIDTH + col + 1) < Apotheneum.GRID_HEIGHT);
                
                if (!isInDoor) {
                    // Normal bottom edge - add the bottom point
                    cubeBottomPathInterior.add(column.points[column.points.length - 1]);
                    
                    // If next column is a door, add the vertical going up
                    if (nextInDoor) {
                        // Travel up the right edge of this column to the door height
                        for (int y = Apotheneum.GRID_HEIGHT - 2; y >= Apotheneum.GRID_HEIGHT - Apotheneum.DOOR_HEIGHT; y--) {
                            if (y >= 0 && y < column.points.length) {
                                cubeBottomPathInterior.add(column.points[y]);
                            }
                        }
                    }
                } else {
                    // We're in a door column
                    // The door columns have fewer points (only above the door)
                    if (available > 0) {
                        // Add the bottom-most available point (top of door opening)
                        cubeBottomPathInterior.add(column.points[available - 1]);
                    }
                    
                    // If this is the last door column, add the descent
                    if (!nextInDoor && col < cubeFace.columns.length - 1) {
                        // Next column is not a door, so travel down on it
                        int nextGlobalCol = face * Apotheneum.GRID_WIDTH + col + 1;
                        LXModel nextColumn = cube.interior.columns[nextGlobalCol];
                        // Travel down from door height to bottom
                        for (int y = Apotheneum.GRID_HEIGHT - Apotheneum.DOOR_HEIGHT; y < Apotheneum.GRID_HEIGHT; y++) {
                            if (y >= 0 && y < nextColumn.points.length) {
                                cubeBottomPathInterior.add(nextColumn.points[y]);
                            }
                        }
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
                LXModel column = cube.interior.columns[globalCol];
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
            
            // Bottom edge (left to right) - handling doors
            for (int col = 0; col < Apotheneum.GRID_WIDTH; col++) {
                int globalCol = face * Apotheneum.GRID_WIDTH + col;
                int available = cube.interior.available(globalCol);
                LXModel column = cube.interior.columns[globalCol];
                
                boolean isInDoor = available < Apotheneum.GRID_HEIGHT;
                boolean nextInDoor = (col < Apotheneum.GRID_WIDTH - 1) && (cube.interior.available(face * Apotheneum.GRID_WIDTH + col + 1) < Apotheneum.GRID_HEIGHT);
                
                if (!isInDoor) {
                    // Normal bottom edge
                    facePath.add(column.points[column.points.length - 1]);
                    
                    // If next column is a door, add the vertical going up
                    if (nextInDoor) {
                        for (int y = Apotheneum.GRID_HEIGHT - 2; y >= Apotheneum.GRID_HEIGHT - Apotheneum.DOOR_HEIGHT; y--) {
                            if (y >= 0 && y < column.points.length) {
                                facePath.add(column.points[y]);
                            }
                        }
                    }
                } else {
                    // Door column
                    if (available > 0) {
                        facePath.add(column.points[available - 1]);
                    }
                    
                    // If this is the last door column, add the descent
                    if (!nextInDoor && col < Apotheneum.GRID_WIDTH - 1) {
                        int nextGlobalCol = face * Apotheneum.GRID_WIDTH + col + 1;
                        LXModel nextColumn = cube.interior.columns[nextGlobalCol];
                        for (int y = Apotheneum.GRID_HEIGHT - Apotheneum.DOOR_HEIGHT; y < Apotheneum.GRID_HEIGHT; y++) {
                            if (y >= 0 && y < nextColumn.points.length) {
                                facePath.add(nextColumn.points[y]);
                            }
                        }
                    }
                }
            }
            
            // Right edge (bottom to top)
            int rightGlobalCol = face * Apotheneum.GRID_WIDTH + (Apotheneum.GRID_WIDTH - 1);
            LXModel rightColumn = cube.interior.columns[rightGlobalCol];
            for (int y = rightColumn.points.length - 2; y >= 0; y--) {
                facePath.add(rightColumn.points[y]);
            }
            
            // Top edge (right to left)
            for (int col = Apotheneum.GRID_WIDTH - 2; col >= 0; col--) {
                int globalCol = face * Apotheneum.GRID_WIDTH + col;
                LXModel column = cube.interior.columns[globalCol];
                if (column.points.length > 0) {
                    facePath.add(column.points[0]);
                }
            }
            
            // Left edge (top to bottom) - but don't duplicate the starting point
            int leftGlobalCol = face * Apotheneum.GRID_WIDTH;
            LXModel leftColumn = cube.interior.columns[leftGlobalCol];
            // Check if the left column is affected by a door
            int leftAvailable = cube.interior.available(leftGlobalCol);
            int startY = (leftAvailable < Apotheneum.GRID_HEIGHT) ? leftAvailable - 1 : leftColumn.points.length - 2;
            
            for (int y = 1; y <= startY; y++) {
                facePath.add(leftColumn.points[y]);
            }
            
            cubeFacePathsInterior.add(facePath);
        }
    }
    
    private void buildCubeFrontFlatPath() {
        cubeFrontFlatPath = new ArrayList<>();
        
        if (!Apotheneum.exists) return;
        
        Apotheneum.Cube cube = Apotheneum.cube;
        
        // Front face is face 0 - create a straight horizontal line across the bottom
        int frontFace = 0;
        Apotheneum.Cube.Face cubeFace = cube.faces[frontFace];
        
        for (int col = 0; col < cubeFace.columns.length; col++) {
            int globalCol = frontFace * Apotheneum.GRID_WIDTH + col;
            int available = cube.exterior.available(globalCol);
            LXModel column = cubeFace.columns[col];
            
            // Check if this column is part of a door
            boolean isInDoor = available < Apotheneum.GRID_HEIGHT;
            
            if (!isInDoor) {
                // Normal bottom edge - add the bottom point
                cubeFrontFlatPath.add(column.points[column.points.length - 1]);
            } else {
                // Door column - add the bottom-most available point (top of door opening)
                if (available > 0) {
                    cubeFrontFlatPath.add(column.points[available - 1]);
                }
            }
        }
    }
    
    private void buildCubeFrontFlatPathInterior() {
        cubeFrontFlatPathInterior = new ArrayList<>();
        
        if (!Apotheneum.exists) return;
        
        Apotheneum.Cube cube = Apotheneum.cube;
        
        // Front face is face 0 - create a straight horizontal line across the bottom interior
        int frontFace = 0;
        
        for (int col = 0; col < Apotheneum.GRID_WIDTH; col++) {
            int globalCol = frontFace * Apotheneum.GRID_WIDTH + col;
            int available = cube.interior.available(globalCol);
            LXModel column = cube.interior.columns[globalCol];
            
            // Check if this column is part of a door
            boolean isInDoor = available < Apotheneum.GRID_HEIGHT;
            
            if (!isInDoor) {
                // Normal bottom edge - add the bottom point
                cubeFrontFlatPathInterior.add(column.points[column.points.length - 1]);
            } else {
                // Door column - add the bottom-most available point (top of door opening)
                if (available > 0) {
                    cubeFrontFlatPathInterior.add(column.points[available - 1]);
                }
            }
        }
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
        double thicknessRadius = width.getValue();
        
        // Get the points along the path that should be lit (length control)
        List<LXPoint> activePoints = new ArrayList<>();
        for (int i = 0; i < pathLength; i++) {
            // Calculate distance considering wrap-around
            double distance = Math.min(
                Math.abs(i - centerPoint),
                Math.min(Math.abs(i - centerPoint + pathLength),
                         Math.abs(i - centerPoint - pathLength))
            );
            
            if (distance < lengthInPoints / 2) {
                LXPoint pathPoint = path.get(i);
                activePoints.add(pathPoint);
                
                // Now find all nearby points within thickness radius
                for (LXPoint point : model.points) {
                    double dist3D = Math.sqrt(
                        Math.pow(point.x - pathPoint.x, 2) +
                        Math.pow(point.y - pathPoint.y, 2) +
                        Math.pow(point.z - pathPoint.z, 2)
                    );
                    
                    if (dist3D <= thicknessRadius) {
                        // Solid white - no color, just brightness
                        int color = LXColor.gray((float)(brightness.getValue() * 100));
                        
                        colors[point.index] = LXColor.add(colors[point.index], color);
                    }
                }
            }
        }
    }
}
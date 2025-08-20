package apotheneum.patterns;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.pattern.LXPattern;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@LXCategory("Apotheneum")
@LXComponentName("Hyperspace")
public class Hyperspace extends LXPattern implements UIDeviceControls<Hyperspace> {
  
  // Star particle in 3D space
  private static class Star {
    float x, y, z;  // Position in model space (0-1)
    float vx, vy, vz;  // Velocity
    float speed;    // Individual star speed multiplier
    int color;      // Star color
    double lifespan; // Total lifespan in milliseconds
    double age;     // Current age in milliseconds
    
    
    Star(double maxLifespan, LXPoint[] allPoints) {
      this.lifespan = Math.random() * maxLifespan + maxLifespan * 0.5; // 50%-150% of max
      reset();
    }
    
    void reset() {
      // Reset is called during initialization and when stars die
      // We'll set a default position here, but actual spawning position
      // will be set by the main pattern based on motion direction
      x = (float)Math.random();
      y = (float)Math.random(); 
      z = (float)Math.random();
      
      
      // Stars don't have individual velocities - they're static
      // Only the motion controls move them
      vx = 0;
      vy = 0;
      vz = 0;
      
      speed = 0.5f + (float)Math.random() * 1.0f; // Individual speed variation
      age = 0; // Reset age
      
      // Pure white stars with brightness variation
      float brightness = 0.8f + (float)Math.random() * 0.2f;
      color = LXColor.rgb(
        (int)(brightness * 255),
        (int)(brightness * 255),
        (int)(brightness * 255)
      );
    }
    
    void update(double deltaMs, float baseSpeed, double maxLifespan, int axis, float direction) {
      // Age the star
      age += deltaMs;
      
      // Update lifespan if parameter changed
      if (lifespan > maxLifespan * 1.5 || lifespan < maxLifespan * 0.5) {
        lifespan = Math.random() * maxLifespan + maxLifespan * 0.5;
      }
      
      float currentSpeed = baseSpeed * speed;
      
      // Motion control moves the entire star field in one axis
      // Stars themselves are static - only the field moves
      float movement = direction * currentSpeed;
      
      switch (axis) {
        case 0: // X axis
          x += movement;
          break;
        case 1: // Y axis
          y += movement;
          break;
        case 2: // Z axis
          z += movement;
          break;
      }
      
      
      // Reset if out of bounds - spawn behind based on motion direction
      // This creates a natural flow where stars come from behind and exit in front
      boolean outOfBounds = false;
      
      // Check if star has exited the extended boundaries
      if (x < -0.2f || x > 1.2f || y < -0.2f || y > 1.2f || z < -0.2f || z > 1.2f) {
        // Only respawn if the star is moving away from the installation
        // This prevents stars from immediately respawning when they should be traveling through
        switch (axis) {
          case 0: // X-axis motion
            if ((direction > 0 && x > 1.2f) || (direction < 0 && x < -0.2f)) {
              outOfBounds = true;
            }
            break;
          case 1: // Y-axis motion
            if ((direction > 0 && y > 1.2f) || (direction < 0 && y < -0.2f)) {
              outOfBounds = true;
            }
            break;
          case 2: // Z-axis motion
            if ((direction > 0 && z > 1.2f) || (direction < 0 && z < -0.2f)) {
              outOfBounds = true;
            }
            break;
        }
        
        if (outOfBounds) {
          spawnBehind(axis, direction);
          return; // Exit early, don't check lifespan
        }
      }
      
      if (age >= lifespan) {
        spawnBehind(axis, direction);
      }
    }
    
    // Spawn star behind the installation based on motion direction
    void spawnBehind(int axis, float direction) {
      // Random position in the two perpendicular axes
      float rand1 = (float)Math.random();
      float rand2 = (float)Math.random();
      
      switch (axis) {
        case 0: // X-axis motion
          y = rand1;
          z = rand2;
          if (direction > 0) {
            x = -0.1f; // Spawn just behind negative X
          } else {
            x = 1.1f; // Spawn just behind positive X
          }
          break;
          
        case 1: // Y-axis motion
          x = rand1;
          z = rand2;
          if (direction > 0) {
            y = -0.1f; // Spawn just behind negative Y
          } else {
            y = 1.1f; // Spawn just behind positive Y
          }
          break;
          
        case 2: // Z-axis motion
          x = rand1;
          y = rand2;
          if (direction > 0) {
            z = -0.1f; // Spawn just behind negative Z
          } else {
            z = 1.1f; // Spawn just behind positive Z
          }
          break;
      }
      
      
      // Reset age
      age = 0;
    }
    
    float getBrightness() {
      // Smooth fade in/out
      double lifeFraction = age / lifespan;
      
      if (lifeFraction < 0.1) {
        // Fade in over first 10% of life
        return (float)(lifeFraction / 0.1);
      } else if (lifeFraction > 0.9) {
        // Fade out over last 10% of life
        return (float)((1.0 - lifeFraction) / 0.1);
      } else {
        // Full brightness in middle
        return 1.0f;
      }
    }
  }
  
  // LED Spatial grid for efficient nearest-neighbor search
  private static class LEDSpatialGrid {
    private final float cellSize;
    private final Map<Long, List<LXPoint>> grid;
    private final int gridWidth, gridHeight, gridDepth;
    private final float xMin, yMin, zMin;
    private final float xMax, yMax, zMax;
    
    LEDSpatialGrid(LXPoint[] points, float cellSize) {
      this.cellSize = cellSize;
      this.grid = new HashMap<>();
      
      // Find bounds
      float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
      float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE, maxZ = Float.MIN_VALUE;
      
      for (LXPoint p : points) {
        minX = Math.min(minX, p.x);
        minY = Math.min(minY, p.y);
        minZ = Math.min(minZ, p.z);
        maxX = Math.max(maxX, p.x);
        maxY = Math.max(maxY, p.y);
        maxZ = Math.max(maxZ, p.z);
      }
      
      // Store bounds with some padding
      this.xMin = minX - cellSize;
      this.yMin = minY - cellSize;
      this.zMin = minZ - cellSize;
      this.xMax = maxX + cellSize;
      this.yMax = maxY + cellSize;
      this.zMax = maxZ + cellSize;
      
      // Calculate grid dimensions
      this.gridWidth = (int) Math.ceil((xMax - xMin + 2 * cellSize) / cellSize);
      this.gridHeight = (int) Math.ceil((yMax - yMin + 2 * cellSize) / cellSize);
      this.gridDepth = (int) Math.ceil((zMax - zMin + 2 * cellSize) / cellSize);
      
      // Add all points to grid
      for (LXPoint p : points) {
        long key = getCellKey(p.x, p.y, p.z);
        List<LXPoint> cell = grid.computeIfAbsent(key, k -> new ArrayList<>());
        cell.add(p);
      }
      
      LX.log(String.format("LED Spatial Grid initialized: %d cells, %d LEDs, grid size %dx%dx%d", 
        grid.size(), points.length, gridWidth, gridHeight, gridDepth));
    }
    
    List<LXPoint> getNearbyLEDs(float x, float y, float z, float radius) {
      List<LXPoint> nearbyLEDs = new ArrayList<>();
      
      // Calculate cell range to search
      int minGridX = Math.max(0, (int) Math.floor((x - radius - xMin) / cellSize));
      int maxGridX = Math.min(gridWidth - 1, (int) Math.ceil((x + radius - xMin) / cellSize));
      int minGridY = Math.max(0, (int) Math.floor((y - radius - yMin) / cellSize));
      int maxGridY = Math.min(gridHeight - 1, (int) Math.ceil((y + radius - yMin) / cellSize));
      int minGridZ = Math.max(0, (int) Math.floor((z - radius - zMin) / cellSize));
      int maxGridZ = Math.min(gridDepth - 1, (int) Math.ceil((z + radius - zMin) / cellSize));
      
      // Search cells in range
      for (int gx = minGridX; gx <= maxGridX; gx++) {
        for (int gy = minGridY; gy <= maxGridY; gy++) {
          for (int gz = minGridZ; gz <= maxGridZ; gz++) {
            long key = getKey(gx, gy, gz);
            List<LXPoint> cell = grid.get(key);
            if (cell != null) {
              nearbyLEDs.addAll(cell);
            }
          }
        }
      }
      
      return nearbyLEDs;
    }
    
    private long getCellKey(float x, float y, float z) {
      int gx = Math.max(0, Math.min(gridWidth - 1, (int) Math.floor((x - xMin) / cellSize)));
      int gy = Math.max(0, Math.min(gridHeight - 1, (int) Math.floor((y - yMin) / cellSize)));
      int gz = Math.max(0, Math.min(gridDepth - 1, (int) Math.floor((z - zMin) / cellSize)));
      return getKey(gx, gy, gz);
    }
    
    private long getKey(int gx, int gy, int gz) {
      return ((long) gx << 42) | ((long) gy << 21) | gz;
    }
  }
  
  private final List<Star> stars = new ArrayList<>();
  private LXPoint[] allPoints; // Cache of all LED points for targeting
  private LEDSpatialGrid ledGrid; // Spatial grid for efficient LED search
  
  // Performance monitoring
  private long frameCount = 0;
  private long totalUpdateTime = 0;
  private long totalRenderTime = 0;
  private long totalFindLEDTime = 0;
  private long totalLEDsSearched = 0;
  private long totalStarsRendered = 0;
  
  public final CompoundParameter speed = new CompoundParameter("Speed", 0.5, 0.1, 50.0)
    .setDescription("Speed of hyperspace travel");
    
  public final CompoundParameter density = new CompoundParameter("Density", 100, 10, 3000)
    .setDescription("Number of stars");
    
  public final CompoundParameter starSize = new CompoundParameter("Star Size", 0.1, 0.05, 0.3)
    .setDescription("Size of stars and trails");
    
  public final CompoundParameter duration = new CompoundParameter("Duration", 3000, 1000, 8000)
    .setDescription("How long stars live (milliseconds)");
    
  public final CompoundParameter brightness = new CompoundParameter("Bright", 1.0, 0.1, 2.0)
    .setDescription("Overall brightness");
    
    
  public final BooleanParameter pulse = new BooleanParameter("Pulse", false)
    .setDescription("Pulsing speed effect");
    
  public final CompoundParameter motionAxis = new CompoundParameter("Axis", 0, 0, 2)
    .setDescription("Motion axis: 0=X, 1=Y, 2=Z");
    
  public final CompoundParameter motionDirection = new CompoundParameter("Direction", 1, -1, 1)
    .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
    .setPolarity(CompoundParameter.Polarity.BIPOLAR)
    .setDescription("Motion direction: -1=Negative, +1=Positive");
    
  
  private double pulsePhase = 0;
  
  public Hyperspace(LX lx) {
    super(lx);
    addParameter("speed", this.speed);
    addParameter("density", this.density);
    addParameter("starSize", this.starSize);
    addParameter("duration", this.duration);
    addParameter("brightness", this.brightness);
    addParameter("pulse", this.pulse);
    addParameter("motionAxis", this.motionAxis);
    addParameter("motionDirection", this.motionDirection);
    
    // Cache all LED points for star targeting
    allPoints = model.points;
    
    // Initialize LED spatial grid for efficient nearest-neighbor search
    // Cell size of 10 units seems reasonable for the installation scale
    ledGrid = new LEDSpatialGrid(allPoints, 10.0f);
    
    updateStarCount();
  }
  
  private void updateStarCount() {
    int targetCount = (int)density.getValue();
    double maxLifespan = duration.getValue();
    int axis = (int)motionAxis.getValue();
    float direction = (float)motionDirection.getValue();
    
    while (stars.size() < targetCount) {
      Star newStar = new Star(maxLifespan, allPoints);
      // Spawn new stars behind the installation for natural flow
      newStar.spawnBehind(axis, direction);
      stars.add(newStar);
    }
    while (stars.size() > targetCount) {
      stars.remove(stars.size() - 1);
    }
  }
  
  @Override
  public void onParameterChanged(heronarts.lx.parameter.LXParameter parameter) {
    if (parameter == density) {
      updateStarCount();
    }
    super.onParameterChanged(parameter);
  }
  
  @Override
  protected void run(double deltaMs) {
    long frameStartTime = System.nanoTime();
    
    // Update pulse phase
    if (pulse.isOn()) {
      pulsePhase += deltaMs * 0.003;
    }
    
    
    // Calculate current speed with optional pulse
    float currentSpeed = (float)(speed.getValue() * deltaMs * 0.0001);
    if (pulse.isOn()) {
      currentSpeed *= 1.0f + (float)Math.sin(pulsePhase) * 0.5f;
    }
    
    // Update all stars
    long updateStartTime = System.nanoTime();
    double maxLifespan = duration.getValue();
    int axis = (int)motionAxis.getValue();
    float direction = (float)motionDirection.getValue();
    
    for (Star star : stars) {
      star.update(deltaMs, currentSpeed, maxLifespan, axis, direction);
    }
    long updateEndTime = System.nanoTime();
    totalUpdateTime += (updateEndTime - updateStartTime);
    
    // Clear all points first
    for (LXPoint p : model.points) {
      colors[p.index] = 0;
    }
    
    // Now render each star as a sharp point
    long renderStartTime = System.nanoTime();
    float brightnessMult = (float)brightness.getValue();
    int starsRendered = 0;
    
    for (Star star : stars) {
      // Only render stars that are reasonably close to the visible cube
      // This allows stars outside the cube to exist but not waste computation
      if (star.x >= -0.2f && star.x <= 1.2f && 
          star.y >= -0.2f && star.y <= 1.2f && 
          star.z >= -0.2f && star.z <= 1.2f) {
        
        // Render the star
        float starBrightness = star.getBrightness() * brightnessMult;
        renderStarAtPoint(star.x, star.y, star.z, star.color, starBrightness);
        starsRendered++;
      }
    }
    long renderEndTime = System.nanoTime();
    totalRenderTime += (renderEndTime - renderStartTime);
    
    frameCount++;
    
    // Print performance stats every 60 frames (roughly 1 second at 60fps)
    if (frameCount % 60 == 0) {
      double avgUpdateMs = (totalUpdateTime / (double)frameCount) / 1_000_000.0;
      double avgRenderMs = (totalRenderTime / (double)frameCount) / 1_000_000.0;
      double avgFindLEDMs = (totalFindLEDTime / (double)frameCount) / 1_000_000.0;
      double totalFrameMs = avgUpdateMs + avgRenderMs;
      double avgLEDsPerStar = totalStarsRendered > 0 ? (double)totalLEDsSearched / totalStarsRendered : 0;
      
      LX.log(String.format(
        "Hyperspace Performance - Stars: %d (rendered: %d) | Update: %.2fms | Render: %.2fms (LED search: %.2fms, avg %.0f LEDs/star) | Total: %.2fms | FPS potential: %.0f",
        stars.size(), starsRendered, avgUpdateMs, avgRenderMs, avgFindLEDMs, avgLEDsPerStar, totalFrameMs, 1000.0 / totalFrameMs
      ));
      
      // Reset counters
      frameCount = 0;
      totalUpdateTime = 0;
      totalRenderTime = 0;
      totalFindLEDTime = 0;
      totalLEDsSearched = 0;
      totalStarsRendered = 0;
    }
  }
  
  // Efficient star rendering - finds closest LED in 3D space using spatial grid
  private void renderStarAtPoint(float x, float y, float z, int color, float brightness) {
    // Quick bounds check
    if (x < 0 || x > 1 || y < 0 || y > 1 || z < 0 || z > 1) return;
    
    // Convert star position from normalized space to model space
    float starX = x * (model.xMax - model.xMin) + model.xMin;
    float starY = y * (model.yMax - model.yMin) + model.yMin;
    float starZ = z * (model.zMax - model.zMin) + model.zMin;
    
    long ledSearchStart = System.nanoTime();
    float minDistanceSquared = Float.MAX_VALUE;
    int closestIndex = -1;
    
    // Use spatial grid to find nearby LEDs only
    // Search radius of 30 units should be enough to find closest LED
    List<LXPoint> nearbyLEDs = ledGrid.getNearbyLEDs(starX, starY, starZ, 30.0f);
    totalLEDsSearched += nearbyLEDs.size();
    totalStarsRendered++;
    
    // Find closest LED among nearby candidates
    for (LXPoint p : nearbyLEDs) {
      float dx = p.x - starX;
      float dy = p.y - starY;
      float dz = p.z - starZ;
      float distanceSquared = dx*dx + dy*dy + dz*dz;
      
      if (distanceSquared < minDistanceSquared) {
        minDistanceSquared = distanceSquared;
        closestIndex = p.index;
      }
    }
    long ledSearchEnd = System.nanoTime();
    totalFindLEDTime += (ledSearchEnd - ledSearchStart);
    
    // Always render to the closest LED
    if (closestIndex >= 0) {
      int finalColor = LXColor.scaleBrightness(color, brightness);
      colors[closestIndex] = LXColor.blend(colors[closestIndex], finalColor, LXColor.Blend.ADD);
    }
  }
  
  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, Hyperspace pattern) {
    uiDevice.setLayout(UIDevice.Layout.HORIZONTAL, 4);
    
    // Movement controls
    addColumn(uiDevice, "Movement",
      newKnob(pattern.speed),
      newKnob(pattern.density),
      newKnob(pattern.duration)).setChildSpacing(6);
    
    addVerticalBreak(ui, uiDevice);
    
    // Motion controls
    addColumn(uiDevice, "Motion",
      newKnob(pattern.motionAxis),
      newKnob(pattern.motionDirection)).setChildSpacing(6);
    
    addVerticalBreak(ui, uiDevice);
    
    // Visual controls
    addColumn(uiDevice, "Visual",
      newKnob(pattern.brightness)).setChildSpacing(6);
      
    addVerticalBreak(ui, uiDevice);
    
    // Additional controls
    addColumn(uiDevice, "Effects",
      newButton(pattern.pulse).setTriggerable(true)).setChildSpacing(6);
  }
}
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
import java.util.List;

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
  
  private final List<Star> stars = new ArrayList<>();
  private LXPoint[] allPoints; // Cache of all LED points for targeting
  
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
    double maxLifespan = duration.getValue();
    int axis = (int)motionAxis.getValue();
    float direction = (float)motionDirection.getValue();
    
    for (Star star : stars) {
      star.update(deltaMs, currentSpeed, maxLifespan, axis, direction);
    }
    
    // Clear all points first
    for (LXPoint p : model.points) {
      colors[p.index] = 0;
    }
    
    // Now render each star as a sharp point
    float brightnessMult = (float)brightness.getValue();
    
    for (Star star : stars) {
      // Only render stars that are reasonably close to the visible cube
      // This allows stars outside the cube to exist but not waste computation
      if (star.x >= -0.2f && star.x <= 1.2f && 
          star.y >= -0.2f && star.y <= 1.2f && 
          star.z >= -0.2f && star.z <= 1.2f) {
        
        // Render the star
        float starBrightness = star.getBrightness() * brightnessMult;
        renderStarAtPoint(star.x, star.y, star.z, star.color, starBrightness);
      }
    }
  }
  
  // Efficient star rendering - finds closest LED in 3D space with distance threshold
  private void renderStarAtPoint(float x, float y, float z, int color, float brightness) {
    // Quick bounds check
    if (x < 0 || x > 1 || y < 0 || y > 1 || z < 0 || z > 1) return;
    
    // Convert star position from normalized space to model space
    float starX = x * (model.xMax - model.xMin) + model.xMin;
    float starY = y * (model.yMax - model.yMin) + model.yMin;
    float starZ = z * (model.zMax - model.zMin) + model.zMin;
    
    float minDistanceSquared = Float.MAX_VALUE;
    int closestIndex = -1;
    
    // Find closest LED using actual 3D coordinates - no distance limit
    for (LXPoint p : model.points) {
      float dx = p.x - starX;
      float dy = p.y - starY;
      float dz = p.z - starZ;
      float distanceSquared = dx*dx + dy*dy + dz*dz;
      
      if (distanceSquared < minDistanceSquared) {
        minDistanceSquared = distanceSquared;
        closestIndex = p.index;
      }
    }
    
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
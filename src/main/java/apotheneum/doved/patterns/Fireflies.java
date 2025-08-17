package apotheneum.doved.patterns;

import apotheneum.Apotheneum;
import apotheneum.ApotheneumPattern;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.TriggerParameter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@LXCategory("Apotheneum/doved")
@LXComponentName("Fireflies")
public class Fireflies extends ApotheneumPattern implements UIDeviceControls<Fireflies> {

  // Control parameters
  public final CompoundParameter maxFireflies = new CompoundParameter("Quantity", 50, 10, 200)
      .setDescription("Number of fireflies");

  public final CompoundParameter glowRadius = new CompoundParameter("Glow Size", 3.0, 1.0, 6.0)
      .setDescription("Size of firefly glow");

  public final CompoundParameter glowCurve = new CompoundParameter("Glow Focus", 2.0, 1.0, 5.0)
      .setDescription("How focused the glow is (higher = sharper)");

  // Fixed internal parameters
  private static final double SPAWN_RATE = 0.12; // Higher spawn rate for more fireflies
  private static final float MOVE_SPEED = 0.8f; // Base movement speed
  private static final float SPEED_VARIATION = 1.0f; // Moderate speed variation with lower max
  private static final float GLOW_INTENSITY = 1.0f; // Full intensity glow
  private static final double LIFESPAN_MIN = 1.5; // seconds - shorter, more dynamic
  private static final double LIFESPAN_MAX = 4.0; // seconds - much shorter max
  private static final float PULSE_RATE = 3.0f; // Much faster flashing
  private static final float PULSE_DEPTH = 0.8f; // Deeper oscillation
  private static final float WANDER_STRENGTH = 0.7f;
  private static final int SHAPE_BOTH = 2; // Both cube and cylinder
  private static final int SURFACE_BOTH = 2; // Both exterior and interior

  public final TriggerParameter clearAll = new TriggerParameter("Clear", this::clearAllFireflies)
      .setDescription("Clear all fireflies");

  // Internal state
  private final List<Firefly> fireflies = new ArrayList<>();
  private double currentTime = 0;

  // Firefly inner class
  private class Firefly {
    // Position in ring coordinates
    float ringX, ringY;

    // Movement
    float direction;
    float speed;

    // Lifecycle
    double birthTime;
    double lifespan;

    // Glow animation
    double glowPhase;
    float glowSpeed;
    float baseIntensity; // Individual firefly brightness

    // Which shape this firefly is on (0=cube, 1=cylinder)
    int shapeType;

    Firefly(double currentTime) {
      this.birthTime = currentTime;

      // Lifespan is now calculated above based on speed

      // Choose shape based on current setting
      // Always use both shapes
      this.shapeType = Math.random() < 0.5 ? 0 : 1; // Randomly choose cube or cylinder

      // Get dimensions for chosen shape
      int ringLength = (shapeType == 0) ? Apotheneum.Cube.Ring.LENGTH : Apotheneum.Cylinder.Ring.LENGTH;
      int ringHeight = (shapeType == 0) ? Apotheneum.GRID_HEIGHT : Apotheneum.CYLINDER_HEIGHT;

      // Random starting position, avoiding door areas
      do {
        this.ringX = (float) (Math.random() * ringLength);
        this.ringY = (float) (Math.random() * ringHeight);
      } while (isInDoorArea(ringX, ringY, shapeType));

      // Random initial direction - favor horizontal movement
      // Bias toward horizontal angles (±30 degrees from horizontal)
      float baseAngle = (Math.random() < 0.5) ? 0 : (float) Math.PI; // Left or right
      this.direction = baseAngle + (float) ((Math.random() - 0.5) * Math.PI / 3); // ±30 degrees

      // Speed with individual variation - slower max speeds
      // Use exponential distribution for more interesting speed variety
      float speedMultiplier = (float) Math.pow(Math.random(), 0.5); // Bias toward slower speeds
      speedMultiplier = speedMultiplier * SPEED_VARIATION + 0.3f; // Range: 0.3 to 1.3x base speed
      this.speed = MOVE_SPEED * speedMultiplier;
      
      // Faster fireflies live shorter lives (more realistic)
      float speedLifespanFactor = 1.5f - speedMultiplier; // Faster = shorter life
      double minLife = LIFESPAN_MIN * 1000 * speedLifespanFactor; // Convert to ms
      double maxLife = LIFESPAN_MAX * 1000 * speedLifespanFactor;
      this.lifespan = minLife + Math.random() * (maxLife - minLife);

      // Random glow phase and speed
      this.glowPhase = Math.random() * 2 * Math.PI;
      this.glowSpeed = 0.5f + (float) (Math.random() * 2.5); // 50-300% of base rate for rapid flash changes

      // Individual brightness variation (40-100% of base intensity)
      this.baseIntensity = 0.4f + (float) (Math.random() * 0.6);
    }

    void update(double deltaMs) {
      // Update glow phase
      glowPhase += deltaMs * 0.001 * PULSE_RATE * glowSpeed;

      // Update position with wandering movement - realistic speed for 40ft cube
      float moveAmount = (float) (speed * deltaMs * 0.025); // Faster, more dynamic movement

      // Add random wandering to direction - keep horizontal bias
      float wander = (float) (WANDER_STRENGTH * (Math.random() - 0.5) * 0.3);
      direction += wander;

      // Gently bias back toward horizontal if getting too vertical
      float verticalComponent = (float) Math.sin(direction);
      if (Math.abs(verticalComponent) > 0.6f) { // If angle is more than ~35 degrees from horizontal
        // Nudge back toward horizontal
        float horizontalBias = -verticalComponent * 0.02f;
        direction += horizontalBias;
      }

      // Calculate new position
      float newX = ringX + (float) Math.cos(direction) * moveAmount;
      float newY = ringY + (float) Math.sin(direction) * moveAmount;

      // Get dimensions for this firefly's shape
      int ringLength = (shapeType == 0) ? Apotheneum.Cube.Ring.LENGTH : Apotheneum.Cylinder.Ring.LENGTH;
      int ringHeight = (shapeType == 0) ? Apotheneum.GRID_HEIGHT : Apotheneum.CYLINDER_HEIGHT;

      // Wrap X coordinate around ring
      newX = (newX + ringLength) % ringLength;

      // Bounce Y at boundaries
      if (newY < 0 || newY >= ringHeight) {
        direction = -direction; // Reverse vertical component
        newY = Math.max(0, Math.min(ringHeight - 1, newY));
      }

      // Check for door collision and redirect if needed
      if (isInDoorArea(newX, newY, shapeType)) {
        // Redirect upward to avoid door
        direction = (float) (-Math.PI / 2 + (Math.random() - 0.5) * 0.5);
      } else {
        // Update position if valid
        ringX = newX;
        ringY = newY;
      }
    }

    float getBrightness() {
      // Calculate age-based fade
      double age = currentTime - birthTime;
      float ageFade;

      if (age < 1000) {
        // Fade in during first second
        ageFade = (float) (age / 1000.0);
      } else if (age > lifespan - 2000) {
        // Fade out during last 2 seconds
        ageFade = (float) ((lifespan - age) / 2000.0);
        ageFade = Math.max(0, ageFade);
      } else {
        // Full brightness in between
        ageFade = 1.0f;
      }

      // Calculate pulse with more dramatic oscillation (oscillates between 0 and 1)
      float pulseValue = (float) (0.5 + 0.5 * Math.sin(glowPhase));

      // Add secondary slower oscillation for more organic feel
      float slowPulse = (float) (0.5 + 0.5 * Math.sin(glowPhase * 0.3));
      float combinedPulse = (pulseValue + slowPulse) * 0.5f;

      // Apply pulse depth (0 = no pulse, 1 = full pulse) - now with combined
      // oscillation
      float pulsedBrightness = 1.0f - PULSE_DEPTH + PULSE_DEPTH * combinedPulse;

      // Combine age fade, pulse, base intensity, and global intensity
      return ageFade * pulsedBrightness * baseIntensity * GLOW_INTENSITY;
    }

    boolean isDead() {
      return (currentTime - birthTime) > lifespan;
    }
  }

  public Fireflies(LX lx) {
    super(lx);
    addParameter("maxFireflies", this.maxFireflies);
    addParameter("glowRadius", this.glowRadius);
    addParameter("glowCurve", this.glowCurve);
    addParameter("clearAll", this.clearAll);
  }

  @Override
  protected void render(double deltaMs) {
    // Clear the display
    setApotheneumColor(0);

    // Update time
    currentTime += deltaMs;

    // Spawn new fireflies
    spawnFireflies();

    // Update and render fireflies
    updateAndRenderFireflies(deltaMs);
  }

  private void spawnFireflies() {
    int targetCount = (int) maxFireflies.getValue();
    int currentCount = fireflies.size();

    // If we're significantly under target, spawn multiple fireflies
    if (currentCount < targetCount) {
      int deficit = targetCount - currentCount;

      // Spawn aggressively to reach target population
      // Spawn up to deficit * spawn rate, but at least try to spawn some
      int spawnAttempts = Math.max(1, (int) (deficit * SPAWN_RATE));

      for (int i = 0; i < spawnAttempts && fireflies.size() < targetCount; i++) {
        if (Math.random() < 0.8) { // 80% chance per attempt to actually spawn
          fireflies.add(new Firefly(currentTime));
        }
      }
    }
  }

  private void updateAndRenderFireflies(double deltaMs) {
    // Use iterator for safe removal while iterating
    Iterator<Firefly> it = fireflies.iterator();
    while (it.hasNext()) {
      Firefly firefly = it.next();

      // Remove dead fireflies
      if (firefly.isDead()) {
        it.remove();
        continue;
      }

      // Update firefly
      firefly.update(deltaMs);

      // Render firefly
      renderFirefly(firefly);
    }
  }

  private void renderFirefly(Firefly firefly) {
    float brightness = firefly.getBrightness();
    if (brightness <= 0)
      return;

    // Calculate color based on brightness (monochrome white)
    // Ensure we get intermediate values, not just 0 or 255
    int colorValue = Math.min(255, Math.max(0, (int) (255.0f * brightness)));
    int color = 0xFF000000 | (colorValue << 16) | (colorValue << 8) | colorValue;

    // Render the glow
    float radius = (float) glowRadius.getValue();
    renderGlow(firefly.ringX, firefly.ringY, color, radius, firefly.shapeType);
  }

  private void renderGlow(float centerX, float centerY, int centerColor, float radius, int shapeType) {
    int radiusInt = (int) Math.ceil(radius);

    // Extract brightness from color for falloff calculation
    float centerBrightness = (centerColor & 0xFF) / 255.0f;

    // Render bright center pixel - extra bright for focus
    int brightCenter = Math.min(255, (int) (centerBrightness * 255 * 1.2f)); // 20% brighter center
    int brightCenterColor = 0xFF000000 | (brightCenter << 16) | (brightCenter << 8) | brightCenter;
    setPixelOnShape(centerX, centerY, brightCenterColor, shapeType);

    // Render surrounding pixels with falloff
    for (int dx = -radiusInt; dx <= radiusInt; dx++) {
      for (int dy = -radiusInt; dy <= radiusInt; dy++) {
        if (dx == 0 && dy == 0)
          continue;

        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance <= radius) {
          // Calculate falloff using adjustable curve
          float falloff = 1.0f - (distance / radius);
          float curve = (float) glowCurve.getValue();
          falloff = (float) Math.pow(falloff, curve); // Adjustable curve power

          // Apply falloff to brightness
          float fadedBrightness = centerBrightness * falloff;
          int brightness = Math.min(255, Math.max(0, (int) (255.0f * fadedBrightness)));
          int pixelColor = 0xFF000000 | (brightness << 16) | (brightness << 8) | brightness;

          setPixelOnShape(centerX + dx, centerY + dy, pixelColor, shapeType);
        }
      }
    }
  }

  private void setPixelOnShape(float ringX, float ringY, int color, int shapeType) {
    int ringIndex = (int) Math.round(ringY);
    int pointIndex = (int) Math.round(ringX);

    int surfaceMode = SURFACE_BOTH; // Always render on both surfaces

    if (shapeType == 0) {
      // Cube rendering
      if (ringIndex >= 0 && ringIndex < Apotheneum.GRID_HEIGHT) {
        // 0=Exterior, 1=Interior, 2=Both
        if (surfaceMode == 0 || surfaceMode == 2) {
          setPixelOnRing(Apotheneum.cube.exterior.ring(ringIndex), pointIndex, color);
        }
        if (surfaceMode == 1 || surfaceMode == 2) {
          setPixelOnRing(Apotheneum.cube.interior.ring(ringIndex), pointIndex, color);
        }
      }
    } else {
      // Cylinder rendering
      if (ringIndex >= 0 && ringIndex < Apotheneum.CYLINDER_HEIGHT) {
        // 0=Exterior, 1=Interior, 2=Both
        if (surfaceMode == 0 || surfaceMode == 2) {
          setPixelOnRing(Apotheneum.cylinder.exterior.ring(ringIndex), pointIndex, color);
        }
        if (surfaceMode == 1 || surfaceMode == 2) {
          setPixelOnRing(Apotheneum.cylinder.interior.ring(ringIndex), pointIndex, color);
        }
      }
    }
  }

  private void setPixelOnRing(Apotheneum.Ring ring, int pointIndex, int color) {
    if (ring != null && ring.points.length > 0) {
      int wrappedIndex = ((pointIndex % ring.points.length) + ring.points.length) % ring.points.length;
      // Use additive blending for overlapping glows
      int existingColor = colors[ring.points[wrappedIndex].index];
      colors[ring.points[wrappedIndex].index] = blendColors(existingColor, color);
    }
  }

  private int blendColors(int existing, int newColor) {
    // Extract components
    int existingR = (existing >> 16) & 0xFF;
    int existingG = (existing >> 8) & 0xFF;
    int existingB = existing & 0xFF;

    int newR = (newColor >> 16) & 0xFF;
    int newG = (newColor >> 8) & 0xFF;
    int newB = newColor & 0xFF;

    // Additive blend with clamping
    int blendedR = Math.min(255, existingR + newR);
    int blendedG = Math.min(255, existingG + newG);
    int blendedB = Math.min(255, existingB + newB);

    return 0xFF000000 | (blendedR << 16) | (blendedG << 8) | blendedB;
  }

  private boolean isInDoorArea(float ringX, float ringY, int shapeType) {
    int ringIndex = (int) Math.round(ringY);
    int ringHeight = (shapeType == 0) ? Apotheneum.GRID_HEIGHT : Apotheneum.CYLINDER_HEIGHT;
    int ringLength = (shapeType == 0) ? Apotheneum.Cube.Ring.LENGTH : Apotheneum.Cylinder.Ring.LENGTH;

    if (ringIndex < 0 || ringIndex >= ringHeight) {
      return false;
    }

    // Check if at the bottom where doors are
    if (ringIndex >= ringHeight - Apotheneum.DOOR_HEIGHT) {
      int ringPos = (int) Math.round(ringX);
      int wrappedPos = ((ringPos % ringLength) + ringLength) % ringLength;

      if (shapeType == 0) {
        // Cube door detection
        for (int face = 0; face < 4; face++) {
          int faceStart = face * Apotheneum.GRID_WIDTH;
          int doorStart = faceStart + Apotheneum.Cube.DOOR_START_COLUMN;
          int doorEnd = doorStart + Apotheneum.DOOR_WIDTH - 1;

          if (wrappedPos >= doorStart && wrappedPos <= doorEnd) {
            return true;
          }
        }
      } else {
        // Cylinder door detection
        int doorStart = Apotheneum.Cylinder.DOOR_START_COLUMN;
        int doorEnd = doorStart + Apotheneum.DOOR_WIDTH - 1;

        // Doors repeat every 30 pixels
        int posInCycle = wrappedPos % 30;
        if (posInCycle >= doorStart && posInCycle <= doorEnd) {
          return true;
        }
      }
    }

    return false;
  }

  private void clearAllFireflies() {
    fireflies.clear();
  }

  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, Fireflies fireflies) {
    uiDevice.setLayout(UIDevice.Layout.HORIZONTAL, 2);

    // Quantity and clear
    addColumn(uiDevice, "Fireflies",
        newKnob(fireflies.maxFireflies),
        newButton(fireflies.clearAll).setTriggerable(true)).setChildSpacing(6);

    addVerticalBreak(ui, uiDevice);

    // Glow controls
    addColumn(uiDevice, "Glow",
        newKnob(fireflies.glowRadius),
        newKnob(fireflies.glowCurve)).setChildSpacing(6);
  }
}
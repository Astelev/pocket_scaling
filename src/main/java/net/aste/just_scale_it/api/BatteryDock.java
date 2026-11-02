package net.aste.just_scale_it.api;
/** An item-backed battery dock can scale its fallback buffer separately from the inserted item. */
public interface BatteryDock {
    void limitFallback(double factor);
}

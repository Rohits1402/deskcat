package com.deskcat;

/**
 * Damped spring driving the squash-and-stretch scale. Pure math so it can be
 * unit-tested without GL.
 */
public class Spring {

    private final float stiffness, damping, min, max;
    private float value = 1f, vel;

    public Spring(float stiffness, float damping, float min, float max) {
        this.stiffness = stiffness;
        this.damping = damping;
        this.min = min;
        this.max = max;
    }

    /** Add an impulse (hop, landing squash, jelly wobble). */
    public void kick(float impulse) {
        vel += impulse;
    }

    public float update(float target, float dt) {
        vel += (target - value) * stiffness * dt;
        vel -= vel * damping * dt;
        value += vel * dt;
        value = Math.max(min, Math.min(max, value));
        return value;
    }

    public float value() {
        return value;
    }
}

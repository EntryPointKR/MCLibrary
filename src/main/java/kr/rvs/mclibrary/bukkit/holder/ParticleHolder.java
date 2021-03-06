package kr.rvs.mclibrary.bukkit.holder;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * Created by Junhyeong Lim on 2017-10-10.
 */
public class ParticleHolder<D> {
    private Particle particle;
    private int count;
    private double offsetX;
    private double offsetY;
    private double offsetZ;
    private double extra;
    private D data;

    public ParticleHolder(Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra, D data) {
        this.particle = particle;
        this.count = count;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.extra = extra;
        this.data = data;
    }

    public ParticleHolder(Particle particle, int count, double extra, D data) {
        this(particle, count, 0, 0, 0, extra, data);
    }

    public ParticleHolder(Particle particle, int count, double extra) {
        this(particle, count, extra, null);
    }

    public ParticleHolder(Particle particle, int count) {
        this(particle, count, 1.0);
    }

    public void spawn(Location location) {
        location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, data);
    }

    public void spawn(Location location, Iterable<Player> players) {
        for (Player player : players) {
            player.spawnParticle(particle, location.add(0.5, 0, 0.5), count, offsetX, offsetY, offsetZ, extra, data);
        }
    }

    public void spawn(Location location, Player... players) {
        spawn(location, Arrays.asList(players));
    }

    public void spawn(Iterable<Player> players) {
        for (Player player : players) {
            spawn(player.getLocation(), player);
        }
    }

    public void spawn(Player... players) {
        spawn(Arrays.asList(players));
    }

    public Particle getParticle() {
        return particle;
    }

    public void setParticle(Particle particle) {
        this.particle = particle;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }

    public double getOffsetZ() {
        return offsetZ;
    }

    public void setOffsetZ(double offsetZ) {
        this.offsetZ = offsetZ;
    }

    public double getExtra() {
        return extra;
    }

    public void setExtra(double extra) {
        this.extra = extra;
    }

    public D getData() {
        return data;
    }

    public void setData(D data) {
        this.data = data;
    }
}

package me.deecaad.weaponmechanics.weapon.explode;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.core.mechanics.CastData;
import me.deecaad.core.mechanics.Mechanics;
import me.deecaad.core.utils.VectorUtil;
import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.Projectile;
import me.deecaad.weaponmechanics.weapon.projectile.weaponprojectile.WeaponProjectile;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import static me.deecaad.weaponmechanics.WeaponMechanics.getConfigurations;

public class ClusterBomb implements Serializer<ClusterBomb> {

    private Projectile projectile;
    private double speed;
    private int splits;
    private int bombs;
    private Detonation detonation;
    private Mechanics mechanics;

    /**
     * Default constructor for serializer
     */
    public ClusterBomb() {
    }

    public ClusterBomb(Projectile projectile, double speed, int splits, int bombs, Detonation detonation, Mechanics mechanics) {
        this.projectile = projectile;
        this.speed = speed;
        this.splits = splits;
        this.bombs = bombs;
        this.detonation = detonation;
        this.mechanics = mechanics;
    }

    public Projectile getProjectile() {
        return projectile;
    }

    public double getSpeed() {
        return speed;
    }

    public int getSplits() {
        return splits;
    }

    public int getBombs() {
        return bombs;
    }

    public Detonation getDetonation() {
        return detonation;
    }

    public void trigger(WeaponProjectile projectile, LivingEntity shooter, Location splitLocation) {

        int currentDepth = projectile.getIntTag("cluster-split-level");

        // Checking to see if we have split the proper number of times
        if (currentDepth >= splits)
            return;

        if (mechanics != null) {
            CastData cast = new CastData(shooter, projectile.getWeaponTitle(), projectile.getWeaponStack());
            cast.setTargetLocation(projectile.getLocation().toLocation(projectile.getWorld()));
            mechanics.use(cast);
        }

        for (int i = 0; i < bombs; i++) {
            Vector vector = VectorUtil.onUnitSphere().multiply(speed);
            vector.setY(Math.abs(vector.getY()));

            // Either use the projectile settings from the "parent" projectile,
            // or use the projectile settings for this cluster bomb
            Projectile projectileHandler = getProjectile() != null ? getProjectile() : getConfigurations().getObject(projectile.getWeaponTitle() + ".Projectile", Projectile.class);
            if (projectileHandler != null) {
                WeaponProjectile newProjectile = getProjectile() != null ? projectileHandler.create(shooter, splitLocation, vector, projectile.getWeaponStack(), projectile.getWeaponTitle(), projectile.getHand())
                        : projectile.clone(splitLocation, vector);
                newProjectile.setIntTag("cluster-split-level", currentDepth + 1);
                projectileHandler.shoot(newProjectile, splitLocation);
            }

        }

        // Remove the parent split
        projectile.remove();
    }

    @Override
    @NotNull
    public ClusterBomb serialize(@NotNull SerializeData data) throws SerializerException {
        int bombs = data.of("Number_Of_Bombs").assertExists().assertPositive().getInt();
        Projectile projectileSettings = data.of("Split_Projectile").serialize(Projectile.class);
        double speed = data.of("Projectile_Speed").assertPositive().getDouble(30.0) / 20.0;
        int splits = data.of("Number_Of_Splits").assertPositive().getInt(1);
        Detonation detonation = data.of("Detonation").serialize(Detonation.class);
        Mechanics mechanics = data.of("Mechanics").serialize(Mechanics.class);

        return new ClusterBomb(projectileSettings, speed, splits, bombs, detonation, mechanics);
    }
}

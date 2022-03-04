package me.deecaad.weaponmechanics.weapon.damage;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DamageDropoff implements Serializer<DamageDropoff> {

    private TreeMap<Double, Double> distances;

    /**
     * Default constructor for serializer
     */
    public DamageDropoff() {
    }

    public DamageDropoff(TreeMap<Double, Double> distances) {
        this.distances = distances;
    }

    /**
     * Gets the damage modifier for given distance. If no damage
     * modifier exists, the damage modifier is 0.0
     *
     * @param distance How far away
     * @return Damage modifier
     */
    public double getDamage(double distance) {
        Map.Entry<Double, Double> floor = distances.floorEntry(distance);

        return floor == null ? 0.0 : floor.getValue();
    }

    @Override
    public String getKeyword() {
        return "Dropoff";
    }

    @Override
    @Nonnull
    public DamageDropoff serialize(SerializeData data) throws SerializerException {

        List<String[]> list = data.ofList()
                .addArgument(double.class, true)
                .addArgument(double.class, true)
                .assertExists().assertList().get();

        TreeMap<Double, Double> distances = new TreeMap<>();

        for (String[] split : list) {
            Double blocks = Double.valueOf(split[0]);
            Double damage = Double.valueOf(split[1]);
            distances.put(blocks, damage);
        }

        return new DamageDropoff(distances);
    }
}
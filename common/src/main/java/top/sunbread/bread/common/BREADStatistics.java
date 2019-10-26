/*
 * Copyright (C) 2019 Sunbread.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package top.sunbread.bread.common;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class BREADStatistics {

    public static final class Point {

        public int x, y, z, w;

        public Point(int x, int y, int z, int w) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = w;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (this == o) return true;
            if (!(o instanceof Point)) return false;
            return this.x == ((Point) o).x && this.y == ((Point) o).y && this.z == ((Point) o).z && this.w == ((Point) o).w;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.x, this.y, this.z, this.w);
        }

    }

    public static final class ClusterStatistics {

        public Set<Point> raw;
        public double eventsPerTick;
        public double[] eventCentroidLocation; // has 3 elements
        public double distanceFromCentroid;

        ClusterStatistics(Set<Point> raw, double eventsPerTick,
                          double[] eventCentroidLocation, double distanceFromCentroid) {
            this.raw = raw;
            this.eventsPerTick = eventsPerTick;
            this.eventCentroidLocation = eventCentroidLocation;
            this.distanceFromCentroid = distanceFromCentroid;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (this == o) return true;
            if (!(o instanceof ClusterStatistics)) return false;
            return this.raw.equals(((ClusterStatistics) o).raw) &&
                    this.eventsPerTick == ((ClusterStatistics) o).eventsPerTick &&
                    this.eventCentroidLocation == ((ClusterStatistics) o).eventCentroidLocation &&
                    this.distanceFromCentroid == ((ClusterStatistics) o).distanceFromCentroid;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.raw, this.eventsPerTick, this.eventCentroidLocation, this.distanceFromCentroid);
        }

    }

    public static final class NoiseStatistics {

        public Set<Point> raw;
        public double eventsPerTick;

        NoiseStatistics(Set<Point> raw, double eventsPerTick) {
            this.raw = raw;
            this.eventsPerTick = eventsPerTick;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (this == o) return true;
            if (!(o instanceof NoiseStatistics)) return false;
            return this.raw.equals(((NoiseStatistics) o).raw) && this.eventsPerTick == ((NoiseStatistics) o).eventsPerTick;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.raw, this.eventsPerTick);
        }

    }

    public static final class WorldStatistics {

        public List<ClusterStatistics> clusters;
        public NoiseStatistics noise;

        WorldStatistics(List<ClusterStatistics> clusters, NoiseStatistics noise) {
            this.clusters = clusters;
            this.noise = noise;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (this == o) return true;
            if (!(o instanceof WorldStatistics)) return false;
            return this.clusters.equals(((WorldStatistics) o).clusters) && this.noise.equals(((WorldStatistics) o).noise);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.clusters, this.noise);
        }

    }

}

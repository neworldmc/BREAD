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

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BREADAnalysisTest {

    @Test
    void clusterAnalysisLostOrDuplicateTest() {
        for (int i = 1; i <= 100; ++i) {
            Set<BREADStatistics.Point> points = getRandomPoints();
            List<Set<BREADStatistics.Point>> result =
                    BREADAnalysis.clusterAnalysis(points, i % 2 == 0 ? 1 : 4);
            assertEquals(points.size(), result.parallelStream().
                            mapToLong(Set::size).sum(),
                    "Round #" + i);
            assertEquals(points.size(), result.parallelStream().
                            map(Set::parallelStream).
                            mapToLong(stream -> stream.filter(points::contains).count()).sum(),
                    "Round #" + i);
        }
    }

    private Set<BREADStatistics.Point> getRandomPoints() {
        return getRandomPoints(new Random());
    }

    private Set<BREADStatistics.Point> getRandomPoints(Random rand) {
        Set<BREADStatistics.Point> points = new HashSet<>();
        int clusters = 5 + (rand.nextInt(10) + 1);
        int clusterPointsNum = 700 + (rand.nextInt(200) + 1);
        int noisePointsNum = 100 + (rand.nextInt(200) + 1);
        List<BREADStatistics.Point> clusterCenters = new ArrayList<>();
        for (int i = 0; i < clusters; ++i)
            clusterCenters.add(new BREADStatistics.Point(rand.nextInt(2000 + 1) - 1000,
                    rand.nextInt(2000 + 1) - 1000,
                    rand.nextInt(2000 + 1) - 1000,
                    0));
        Set<BREADStatistics.Point> marked = new HashSet<>();
        for (int i = 0; i < clusterPointsNum; ++i) {
            BREADStatistics.Point randomCenter = clusterCenters.get(rand.nextInt(clusterCenters.size()));
            BREADStatistics.Point location;
            do {
                location = new BREADStatistics.Point(randomCenter.x + rand.nextInt(64 + 1) - 128,
                        randomCenter.y + rand.nextInt(64 + 1) - 128,
                        randomCenter.z + rand.nextInt(64 + 1) - 128,
                        0);
            }
            while (marked.contains(location));
            marked.add(location);
            points.add(new BREADStatistics.Point(location.x, location.y, location.z,
                    10 + (rand.nextInt(90) + 1)));
        }
        clusterCenters.clear();
        for (int i = 0; i < noisePointsNum; ++i) {
            BREADStatistics.Point location;
            do {
                location = new BREADStatistics.Point(rand.nextInt(2000 + 1) - 1000,
                        rand.nextInt(2000 + 1) - 1000,
                        rand.nextInt(2000 + 1) - 1000,
                        0);
            }
            while (marked.contains(location));
            marked.add(location);
            points.add(new BREADStatistics.Point(location.x, location.y, location.z,
                    rand.nextInt(10) + 1));
        }
        marked.clear();
        return points;
    }

}
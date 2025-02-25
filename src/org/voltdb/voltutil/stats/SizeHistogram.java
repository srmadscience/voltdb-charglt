/*
 * Copyright (C) 2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package org.voltdb.voltutil.stats;


/**
 * Stores a histogram of 'something', where buckets can be incremented by
 * arbitrary amounts.
 */
public class SizeHistogram {

    String description = "";
    String name = "";

    int[] theHistogram = new int[0];
    String[] theHistogramComment = new String[0];

    public SizeHistogram(String name, int size) {
        this.name = name;
        theHistogram = new int[size];
        theHistogramComment = new String[size];
    }

    public void inc(int size, String comment) {

        if (size >= 0 && size < theHistogram.length) {
            theHistogram[size]++;
            theHistogramComment[size] = comment;
        } else if (size >= theHistogram.length) {
            theHistogram[theHistogram.length - 1]++;
            theHistogramComment[theHistogram.length - 1] = comment;
        }
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer(name);
        b.append(" ");
        b.append(description);
        b.append(" ");

        for (int i = 0; i < theHistogram.length; i++) {
            if (theHistogram[i] > 0) {
                b.append(System.lineSeparator());
                b.append(i);
                b.append(' ');
                b.append(theHistogram[i]);

            }
        }

        return b.toString();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}

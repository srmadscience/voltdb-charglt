/*
 * Copyright (C) 2025 Volt Active Data Inc.
 *
 * Use of this source code is governed by an MIT
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */

package org.voltdb.voltutil.stats;

/**
 * Stores a histogram of latency stats
 *
 */
public class LatencyHistogram {

    final String NUMFORMAT_DECIMAL = "% ,16.0f";
    final String NUMFORMAT_INTEGER = "%16d";

    /**
     * Number of slots in histogram. 1000 = 0 to 999 ms...
     */
    int maxSize = 1000;

    /**
     * Store of values, generally incremented by 1 for each ms
     */
    double[] latencyHistogram = new double[0];

    /**
     * Store of optional comments for specific readings
     */
    String[] latencyComment = new String[0];

    /**
     * Whether we have 'rolled over' - reached Integer.MAX_VALUE for any counter.
     *
     * @see resetLatency
     */
    boolean isRolledOver = false;

    /**
     * Optional name
     */
    String name = "";

    /**
     * Optional Description
     */
    String description = "";

    /**
     * Number of reports since start or rollover
     */
    long reports = 0;

    /**
     * Highest value seen
     */
    int maxUsedSize = 0;

    long underReports = 0;

    long overReports = 0;

    /**
     * Create a histogram of up to maxSize
     *
     * @param maxSize
     */
    public LatencyHistogram(int maxSize) {
        init("", maxSize);
    }

    /**
     * Create a named histogram of up to maxSize
     *
     * @param maxSize
     */
    public LatencyHistogram(String name, int maxSize) {
        init(name, maxSize);
    }

    /**
     * Initialize histogram elements
     *
     * @param name
     * @param maxSize
     */
    public void init(String name, int maxSize) {

        this.name = name;
        this.maxSize = maxSize;

        latencyHistogram = new double[maxSize];
        latencyComment = new String[maxSize];

        resetLatency();

    }

    /**
     * reset latency stats to zero. Called at start, on demand, and on rollover.
     */
    public void resetLatency() {
        
        for (int i = 0; i < maxSize; i++) {
            latencyHistogram[i] = 0;
        }

        for (int i = 0; i < latencyComment.length; i++) {
            latencyComment[i] = "";
        }

        reports = 0;
        maxUsedSize = 0;
        underReports = 0;
        overReports = 0;
    }

    /**
     * Report a latency measurement. If it's >= maxSize it goes into the last
     * element. Negative values are forced to zero.
     *
     * @param latency
     * @param comment
     */
    public void report(int latency, String comment) {
        report(latency, comment, 1);
    }

    /**
     * Report a latency measurement. If it's >= maxSize it goes into the last
     * element. Negative values are forced to zero.
     *
     * @param latency
     * @param comment
     * @param howmany
     */
    public void report(int latency, String comment, int howMany) {

        reports += howMany;

        if (latency < 0) {
            underReports++;
            latency = 0;
        } else if (latency >= maxSize) {
            overReports++;
            latency = maxSize - 1;
        }

        // Can we actually add howMany to the value?
        if ((latencyHistogram[latency] + howMany) < Integer.MAX_VALUE) {

            latencyHistogram[latency] += howMany;

            if (maxUsedSize < latency) {
                maxUsedSize = latency;
            }

            // We can't - time for a rollover!
        } else {
            isRolledOver = true;
            resetLatency();
        }

        // Update comment, if it makes sense to do so
        if (comment != null && comment.length() > 0 && latencyComment[latency].length() == 0) {
            latencyComment[latency] = comment;

        }

    }

    /**
     * Report latency, assuming startTime is relative to now...
     *
     * @param startTime
     * @param comment
     */
    public void reportLatency(long startTime, String comment) {

        int latency = (int) (System.currentTimeMillis() - startTime);

        report(latency, comment);

    }

    /**
     * Inspect a specific latency value
     *
     * @param idx
     * @return
     */
    public double peekValue(int idx) {

        if (idx < maxSize) {
            return latencyHistogram[idx];
        }

        return 0.0;

    }

    /**
     * Change a specific latency value.
     *
     * @param idx
     * @param value
     */
    public void pokeValue(int idx, double value) {

        int actualIdx = idx;

        if (actualIdx > maxSize - 1) {
            actualIdx = maxSize - 1;
        }

        if (actualIdx >= 0) {
            reports -= latencyHistogram[actualIdx];
            reports += value;
            latencyHistogram[actualIdx] = value;

            if (maxUsedSize < actualIdx) {
                maxUsedSize = actualIdx;
            }

        }

    }

    /**
     * Change reports number. This could break things.
     *
     * @param reports
     */
    public void pokeReports(long reports) {

        this.reports = reports;
    }

    /**
     * @return a histogram
     */
    public double[] getLatencyHistogram() {
        return latencyHistogram;
    }

    /**
     * @return comments for histogram
     */
    public String[] getLatencyComment() {
        return latencyComment;
    }

    /**
     * @return largest element used
     */
    public int getMaxUsedSize() {

        return maxUsedSize;
    }

    /**
     * Assuming this is, in fact latency, return how may milliseconds are needed to
     * 'cover' a given percentage.
     *
     * @param pct
     * @return how many reports are <= pct
     */
    public int getLatencyPct(double pct) {

        final double target = getEventTotal() * (pct / 100);
        double runningTotal = latencyHistogram[0];
        int matchValue = 0;

        for (int i = 1; i < latencyHistogram.length; i++) {

            if (runningTotal >= target) {
                break;
            }

            matchValue = i;
            runningTotal = runningTotal + latencyHistogram[i];

        }

        return matchValue;
    }

    /**
     * @return total amount of time we have tracked. Note that if we have events
     *         that are higher than maxSize this number will be short.
     */
    public double getLatencyTotal() {

        double runningTotal = 0.0;

        for (int i = 0; i < latencyHistogram.length; i++) {
            runningTotal = runningTotal + (i * latencyHistogram[i]);
        }

        return runningTotal;
    }

    /**
     * @return average latency.
     */
    public double getLatencyAverage() {

        return getLatencyTotal() / reports;
    }

    /**
     * @return total number of events seen, not how much time they collectively
     *         took.
     */
    public double getEventTotal() {

        double runningTotal = 0.0;

        for (double element : latencyHistogram) {
            runningTotal += element;
        }

        return runningTotal;
    }

    /**
     * @return terse one line summary.
     */
    public String toStringShort() {
        StringBuffer b = new StringBuffer(name);

        b.append(" ");
        b.append(description);
        b.append(System.lineSeparator());

        b.append(" Reports=");
        b.append(String.format(NUMFORMAT_INTEGER, reports));
        b.append(" Average=");
        b.append(String.format(NUMFORMAT_DECIMAL, getLatencyAverage()));
        b.append(", Total=");
        b.append(String.format(NUMFORMAT_DECIMAL, getLatencyTotal()));
        b.append(", 50%=");
        b.append(String.format(NUMFORMAT_INTEGER, getLatencyPct(50)));
        b.append(", 95%=");
        b.append(String.format(NUMFORMAT_INTEGER, getLatencyPct(95)));
        b.append(", 99%=");
        b.append(String.format(NUMFORMAT_INTEGER, getLatencyPct(99)));
        b.append(", 99.5%=");
        b.append(String.format(NUMFORMAT_INTEGER, getLatencyPct(99.5)));
        b.append(", 99.95%=");
        b.append(String.format(NUMFORMAT_INTEGER, getLatencyPct(99.95)));
        b.append(", Max=");
        b.append(String.format(NUMFORMAT_INTEGER, maxUsedSize));

        if (isRolledOver) {
            b.append(" ROLLED OVER");
        }

        if (underReports > 0) {
            b.append(" Reports <0 ");
            b.append(underReports);
        }

        if (overReports > 0) {
            b.append(" Reports <= maxsize ");
            b.append(overReports);
        }

        return b.toString();
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer(toStringShort());

        b.append("\n");

        for (int i = 0; i < latencyHistogram.length; i++) {
            if (latencyHistogram[i] != 0) {
                if (i == (latencyHistogram.length - 1)) {
                    b.append(">= ");
                }
                b.append(i);
                b.append("\t");
                b.append(latencyHistogram[i]);
                b.append("\t");
                b.append(latencyComment[i]);
                b.append("\n");
            }
        }

        return b.toString();
    }

    /**
     * @return whether we have ever reached Integer.MAX_VALUE...
     */
    public boolean isHasRolledOver() {
        return isRolledOver;
    }

    /**
     * Method for when you need to subtract two histograms. This happens when you
     * are tracking different parts of a lifecycle and want to break down latency
     * into steps.
     * <p>
     * Say you have a lifecycle that goes a->b->c->d. You have histograms for a->d
     * and b->c. By subtracting them you can create a->b+c->d
     *
     * @param name       Name for your new histogram
     * @param bigThing   thing that has the bigger values
     * @param smallThing thing that has the smaller values
     * @return a new LatencyHistogram that is bigThing - smallThing
     */
    public static LatencyHistogram subtract(String name, LatencyHistogram bigThing, LatencyHistogram smallThing) {
        int size = bigThing.getMaxUsedSize();

        if (smallThing.getMaxUsedSize() > size) {
            size = smallThing.getMaxUsedSize();
        }

        LatencyHistogram newHist = new LatencyHistogram(name, size);

        for (int i = 0; i < size; i++) {
            double bigVal = bigThing.peekValue(i);
            double smallVal = smallThing.peekValue(i);
            newHist.pokeValue(i, (bigVal - smallVal));
        }

        newHist.pokeReports(bigThing.reports);

        return newHist;
    }

    /**
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Update description
     *
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return true if this histogram has been used
     */
    public boolean hasReports() {

        if (reports > 0) {
            return true;
        }

        return false;
    }

    /**
     * @return the nUMFORMAT_DECIMAL
     */
    public String getNUMFORMAT_DECIMAL() {
        return NUMFORMAT_DECIMAL;
    }

    /**
     * @return the nUMFORMAT_INTEGER
     */
    public String getNUMFORMAT_INTEGER() {
        return NUMFORMAT_INTEGER;
    }

    /**
     * @return the maxSize
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * @return the isRolledOver
     */
    public boolean isRolledOver() {
        return isRolledOver;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the reports
     */
    public long getReports() {
        return reports;
    }

    /**
     * @return the underReports
     */
    public long getUnderReports() {
        return underReports;
    }

    /**
     * @return the overReports
     */
    public long getOverReports() {
        return overReports;
    }

}

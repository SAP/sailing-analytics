package com.sap.sailing.gwt.ui.shared.controls.slider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TimeTicksCalculator {

    public TimeTicksCalculator() {
    }

    /**
     * Get a normalized tick interval for dates. Returns a configuration object with unit range (interval), count and
     * name. Used to prepare data for getTimeTicks. Previously this logic was part of getTimeTicks, but as getTimeTicks
     * now runs of segments in stock charts, the normalizing logic was extracted in order to prevent it for running over
     * again for each segment having the same interval.
     */
    public NormalizedInterval normalizeTimeTickInterval(long tickInterval) {
        TimeUnits[] units = TimeUnits.values();

        TimeUnits unit = units[units.length - 1]; // default unit is years
        long interval = unit.unitInMs;
        int[] multiples = unit.allowedMultiples;
        int count;

        // loop through the units to find the one that best fits the tickInterval
        for (int i = 0; i < units.length; i++) {
            unit = units[i];
            interval = unit.unitInMs;
            multiples = unit.allowedMultiples;

            if (i + 1 < units.length) {
                // lessThan is in the middle between the highest multiple and the next unit.
                long lessThan = (interval * multiples[multiples.length - 1] + units[i + 1].unitInMs) / 2;

                // break and keep the current unit
                if (tickInterval <= lessThan) {
                    break;
                }
            }
        }

        // prevent 2.5 years intervals, though 25, 250 etc. are allowed
        if (interval == TimeUnits.YEAR.unitInMs && tickInterval < 5 * interval) {
            multiples = new int[] { 1, 2, 5 };
        }

        // get the count
        count = (int) normalizeTickInterval(tickInterval / interval, multiples, 1);

        return new NormalizedInterval(unit.name(), interval, count);
    }

    /**
     * Take an interval and normalize it to multiples of 1, 2, 2.5 and 5
     * 
     * @param {Number} interval
     * @param {Array} multiples
     * @param {Number} magnitude
     * @param {Object} options
     */
    public long normalizeTickInterval(long interval, int[] multiples, int magnitude) {
        long normalized;

        // round to a tenfold of 1, 2, 2.5 or 5
        normalized = interval / magnitude;

        // multiples for a linear scale
        if (magnitude == 1) {
            multiples = new int[] { 1, 2, 5, 10 };
        }

        // normalize the interval to the nearest multiple
        for (int i = 0; i < multiples.length; i++) {
            interval = multiples[i];
            int addedValue = i + 1 < multiples.length ? multiples[i + 1] : multiples[i];
            if (normalized <= (multiples[i] + addedValue) / 2) {
                break;
            }
        }

        // multiply back to the correct magnitude
        interval *= magnitude;

        return interval;
    }

    private long makeTime(int year, int month) {
        return makeTime(year, month, 1, 0, 0, 0);
    }

    private long makeTime(int year, int month, int date) {
        return makeTime(year, month, date, 0, 0, 0);
    }

    @SuppressWarnings("deprecation")
    private long makeTime(int year, int month, int date, int hours, int minutes, int seconds) {
        return new Date(year, month, date, hours, minutes, seconds).getTime();
    }

    /**
     * Set the tick positions to a time unit that makes sense, for example on the first of each month or on every
     * Monday. Return an array with the time positions. Used in datetime axes as well as for grouping data on a datetime
     * axis.
     * 
     * @param {Object} normalizedInterval The interval in axis values (ms) and the count
     * @param {Number} min The minimum in axis values
     * @param {Number} max The maximum in axis values
     * @param {Number} startOfWeek
     */
    @SuppressWarnings("deprecation")
    public List<TickPosition> calculateTimeTicks(NormalizedInterval normalizedInterval, long min, long max,
            long startOfWeek) {
        List<TickPosition> tickPositions = new ArrayList<TickPosition>();
        int i;
        boolean useUTC = true;
        int minYear = 0; // used in months and years as a basis for Date.UTC()
        Date minDate = new Date(min);
        long interval = normalizedInterval.unitRange;
        int count = (int) normalizedInterval.count;


        if (interval >= TimeUnits.SECOND.unitInMs) { // second
            minDate.setTime(min - (min % 1000));
            minDate.setSeconds(interval >= TimeUnits.MINUTE.unitInMs ? 0 : count
                    * Math.round(minDate.getSeconds() / count));
        }

        if (interval >= TimeUnits.MINUTE.unitInMs) { // minute
            minDate.setMinutes(interval >= TimeUnits.HOUR.unitInMs ? 0 : count
                    * Math.round(minDate.getMinutes() / count));
        }

        if (interval >= TimeUnits.HOUR.unitInMs) { // hour
            minDate.setHours(interval >= TimeUnits.DAY.unitInMs ? 0 : count * Math.round(minDate.getHours() / count));
        }

        if (interval >= TimeUnits.DAY.unitInMs) { // day
            minDate.setDate(interval >= TimeUnits.MONTH.unitInMs ? 1 : count * Math.round(minDate.getDate() / count));
        }

        if (interval >= TimeUnits.MONTH.unitInMs) { // month
            minDate.setMonth(interval >= TimeUnits.YEAR.unitInMs ? 0 : count * Math.round(minDate.getMonth() + 1 / count) - 1);
            minYear = minDate.getYear() + 1900;
        }

        if (interval >= TimeUnits.YEAR.unitInMs) { // year
            minYear -= minYear % count;
            minDate.setYear(minYear - 1900);
        }

        // week is a special case that runs outside the hierarchy
        if (interval == TimeUnits.WEEK.unitInMs) {
            // get start of current week, independent of count
            long value = minDate.getDate() - minDate.getDay() + startOfWeek;
            minDate.setDate((int) value);
        }

        // get tick positions
        i = 1;
        minYear = minDate.getYear() + 1900;
        int minMonth = minDate.getMonth() + 1;
        int minDateDate = minDate.getDate();
        long time = minDate.getTime();

        // iterate and add tick positions at appropriate values
        while (time < max) {
            if(time > min)
                tickPositions.add(new TickPosition(time));

            // if the interval is years, use Date.UTC to increase years
            if (interval == TimeUnits.YEAR.unitInMs) {
                time = makeTime(minYear + i * count, 0);

                // if the interval is months, use Date.UTC to increase months
            } else if (interval == TimeUnits.MONTH.unitInMs) {
                time = makeTime(minYear, minMonth + i * count);

                // if we're using global time, the interval is not fixed as it jumps
                // one hour at the DST crossover
            } else if (!useUTC && (interval == TimeUnits.DAY.unitInMs || interval == TimeUnits.WEEK.unitInMs)) {
                time = makeTime(minYear, minMonth, minDateDate + i * count * (interval == TimeUnits.DAY.unitInMs ? 1 : 7));

                // else, the interval is fixed and we use simple addition
            } else {
                time += interval * count;
            }

            i++;
        }

        // push the last time
        if(time < max)
            tickPositions.add(new TickPosition(time));

        // record information on the chosen unit - for dynamic label formatter
        // tickPositions.info = extend(normalizedInterval, {
        // totalRange: interval * count
        // });

        return tickPositions;
    }

    public class NormalizedInterval {
        public String unitName;
        public long unitRange;
        public int count;

        public NormalizedInterval(String unitName, long unitRange, int count) {
            this.unitName = unitName;
            this.unitRange = unitRange;
            this.count = count;
        }
    }

    public class TickPosition {
        public TickPosition(long time) {
            this.time = new Date(time);
        }

        Date time;
    }

    enum TimeUnits {
        MILLISECOND(1, new int[] { 1, 2, 5, 10, 20, 25, 50, 100, 200, 500 }), 
        SECOND(1000, new int[] { 1, 2, 5, 10, 15, 30 }),
        MINUTE(60 * 1000, new int[] { 1, 2, 5, 10, 15, 30 }),
        HOUR(60 * 60 * 1000, new int[] { 1, 2, 3, 4, 6, 8, 12 }),
        DAY(24 * 3600000l, new int[] { 1, 2 }), 
        WEEK(7 * 24 * 3600000l, new int[] { 1, 2 }), 
        MONTH(30 * 24 * 3600000l, new int[] { 1, 2, 3, 4, 6 }),
        YEAR(31556952000l, new int[] {});

        int[] allowedMultiples;

        long unitInMs;

        TimeUnits(long unitInMs, int[] allowedMultiples) {
            this.unitInMs = unitInMs;
            this.allowedMultiples = allowedMultiples;
        }
    }
}

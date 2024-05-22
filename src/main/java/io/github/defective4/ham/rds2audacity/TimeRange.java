package io.github.defective4.ham.rds2audacity;

public class TimeRange {
    private long start, end;

    public TimeRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public TimeRange() {
        start = Long.MAX_VALUE;
        end = Long.MIN_VALUE;
    }

    public long getDuration() {
        return end - start;
    }

    @Override
    public String toString() {
        return "TimeRange{" + "start=" + start + ", end=" + end + '}';
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }
}

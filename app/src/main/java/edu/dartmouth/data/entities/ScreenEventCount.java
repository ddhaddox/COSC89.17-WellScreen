package edu.dartmouth.data.entities;

public class ScreenEventCount {
    public long date;
    public int count;

    public ScreenEventCount() {}

    public ScreenEventCount(long date, int count) {
        this.date = date;
        this.count = count;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}

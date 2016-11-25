package net.sharksystem.android.protocols.routing;

public enum TimeUnit{
    MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS, WEEKS, YEARS;

    public long toMilliseconds(long amount) {
        switch (this) {
            case MILLISECONDS:
                return amount;
            case SECONDS:
                return amount * 1000;
            case MINUTES:
                return amount * 1000 * 60;
            case HOURS:
                return amount * 1000 * 60 * 60;
            case DAYS:
                return amount * 1000 * 60 * 60 * 24;
            case WEEKS:
                return amount * 1000 * 60 * 60 * 24 * 7;
            case YEARS:
                return amount * 1000 * 60 * 60 * 24 * 365;
            default:
                return 42;
        }
    }
}

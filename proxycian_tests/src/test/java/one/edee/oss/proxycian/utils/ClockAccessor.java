package one.edee.oss.proxycian.utils;

import javax.annotation.Nullable;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Serves as general current date and time accessor. Can be tweaked in tests.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2014
 */
public class ClockAccessor {
	private static final ClockAccessor INSTANCE = new ClockAccessor();
	private Clock currentClock;

	/**
	 * ClockAccessor should not be instantiated to ensure there is single instance in application to allow
	 * system wide control of current date and time.
	 */
	private ClockAccessor() {}

	/**
	 * Returns main instance of the ClockAccessor.
	 * @return
	 */
	public static ClockAccessor getInstance() {
		return INSTANCE;
	}

	/**
	 * Returns current date and time clock. Usually clock represent system date and time but can be altered in
	 * tests (and only in tests!).
	 *
	 * @return
	 */
	public Clock getCurrentClock() {
		return currentClock == null ? Clock.systemDefaultZone() : currentClock;
	}

	/**
	 * Allows to override current date and time. Should be used only in tests.
	 * Remember to always call {@link #setSystemDateTimeClock()} in final block of the test or tear down.
	 * Or you can use more safer {@link #doWithClock}
	 *
	 * @param clock
	 */
	public void setCurrentClock(Clock clock) {
		this.currentClock = clock;
	}

	/**
	 * Resets previously overriden current date and time to system time.
	 */
	public void setSystemDateTimeClock() {
		setCurrentClock(Clock.systemDefaultZone());
	}

	/**
	 * Executes certain logic with fixed clock time. Clocks are restored to system date and time after lambda
	 * is finished.
	 *
	 * @param clock
	 * @param logic
	 */
	public void doWithClock(Clock clock, Runnable logic) {
		try {
			setCurrentClock(clock);
			logic.run();
		} finally {
			setSystemDateTimeClock();
		}
	}

	/**
	 * Returns current date for current clock.
	 * @return
	 */
	public LocalDate today() {
		return LocalDate.now(getCurrentClock());
	}

	/**
	 * Returns current date for current clock and time zone.
	 * @param zoneId
	 * @return
	 */
	public LocalDate today(@Nullable ZoneId zoneId) {
		return currentClock == null && zoneId != null ? LocalDate.now(zoneId) : today();
	}

	/**
	 * Returns current date and time for current clock.
	 * @return
	 */
	public LocalDateTime now() {
		return LocalDateTime.now(getCurrentClock());
	}

	/**
	 * Returns current date and time for current clock and time zone.
	 * @param zoneId
	 * @return
	 */
	public LocalDateTime now(@Nullable ZoneId zoneId) {
		return currentClock == null && zoneId != null ? LocalDateTime.now(zoneId) : now();
	}

	/**
	 * Returns date and time aligned to next whole minute, zero seconds.
	 * @return
	 */
	public LocalDateTime nextMinute() {
		return LocalDateTime.now(getCurrentClock()).withSecond(0).withNano(0).plusMinutes(1);
	}

	/**
	 * Returns date and time aligned to last passed whole minute, zero seconds.
	 * @return
	 */
	public LocalDateTime lastMinute() {
		return LocalDateTime.now(getCurrentClock()).withSecond(0).withNano(0);
	}

}
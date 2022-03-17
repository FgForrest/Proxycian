package one.edee.oss.proxycian.model.traits;

import one.edee.oss.proxycian.utils.ClockAccessor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;


public interface AgingPerson {

	LocalDate getBirthDate();
	void setBirthDate(LocalDate birthDate);

	default int getAge() {
		return (int)ChronoUnit.YEARS.between(getBirthDate(), ClockAccessor.getInstance().now());
	}

}

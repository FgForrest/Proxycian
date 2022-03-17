package one.edee.oss.proxycian.model.composite;

import lombok.Data;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;


@Data
public class CustomizedPersonImpl implements CustomizedPerson {
	private String firstName;
	private String lastName;
	private LocalDate birthDate;

	@Override
	public Object getProperty(String name) {
		try {
			Field field = this.getClass().getField(name);
			return field.get(this);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void setProperty(String name, Object value) {
		try {
			Field field = this.getClass().getField(name);
			field.set(this, value);
		} catch (Exception e) {
			return;
		}
	}

	@Override
	public Map<String, Object> getProperties() {
		final Map<String, Object> props = new HashMap<>(3);
		props.put("firstName", firstName);
		props.put("lastName", lastName);
		props.put("birthDate", birthDate);
		return props;
	}

	@Override
	public void doWork() {
		// do nothing :)
	}
}

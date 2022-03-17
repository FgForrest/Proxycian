package one.edee.oss.proxycian.model.composite;

import static java.util.Optional.ofNullable;


public abstract class CustomizedPersonAbstract implements CustomizedPerson {

    public String getCompleteName() {
        return ofNullable(getFirstName()).map(it -> it + " ").orElse("") +
                ofNullable(getLastName()).orElse("");
    }

}

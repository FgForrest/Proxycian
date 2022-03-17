package one.edee.oss.proxycian.model.composite;

import one.edee.oss.proxycian.model.traits.AgingPerson;
import one.edee.oss.proxycian.model.traits.Person;
import one.edee.oss.proxycian.model.traits.PropertyAccessor;
import one.edee.oss.proxycian.model.traits.Worker;

public interface CustomizedPerson extends Person, AgingPerson, Worker, PropertyAccessor {
}

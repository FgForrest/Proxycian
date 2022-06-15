package one.edee.oss.proxycian.exception;

/**
 * Exception is thrown when class generator is not able to found appropriate constructor on a class the proxy should
 * extend from. If proxy instance is generated without constructor arguments, it means that there is no default
 * (non-parametrized) constructor on super class. If proxy instance is generated using specific constructor arguments,
 * it means that there is mismatch in those arguments (their count or their types).
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class SuperConstructorNotFoundException extends IllegalArgumentException {
	private static final long serialVersionUID = -5828817139099056141L;

	public SuperConstructorNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

}

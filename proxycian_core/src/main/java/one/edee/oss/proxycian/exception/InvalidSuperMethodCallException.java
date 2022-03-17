package one.edee.oss.proxycian.exception;

/**
 * This exception is thrown when super class method cannot be called.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class InvalidSuperMethodCallException extends IllegalStateException {
	private static final long serialVersionUID = 3324938940250637059L;

	public InvalidSuperMethodCallException(Throwable cause) {
		super(cause);
	}

}

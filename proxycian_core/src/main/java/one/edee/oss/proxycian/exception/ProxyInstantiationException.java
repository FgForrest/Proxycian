package one.edee.oss.proxycian.exception;

/**
 * Exception is thrown when the proxy instantiation fails. The original exception is propagated. This usually means
 * there was error in {@link one.edee.oss.proxycian.OnInstantiationCallback} or constructor logic of the superclass.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public class ProxyInstantiationException extends IllegalArgumentException {
	private static final long serialVersionUID = 2590708900779741768L;

	public ProxyInstantiationException(String message, Throwable cause) {
		super(message, cause);
	}

}

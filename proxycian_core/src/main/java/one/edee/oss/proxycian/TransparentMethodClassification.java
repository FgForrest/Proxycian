package one.edee.oss.proxycian;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * {@link TransparentMethodClassification} annotation allows you to change behaviour of `invokeSuper` parameter of the
 * method {@link CurriedMethodContextInvocationHandler#invoke(Object, Method, Object[], Object, Callable)}. Instead
 * of calling original implementation directly it allows executing additional MethodInvocations that were targeting
 * the same method. This allows creating a logic, that inject your logic before and/or after the original method call
 * and let other advices do their work.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
public interface TransparentMethodClassification {
}

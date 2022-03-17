package one.edee.oss.proxycian.trait.groovy;

import groovy.lang.MetaClass;
import groovy.lang.MetaClassImpl;
import one.edee.oss.proxycian.MethodClassification;
import one.edee.oss.proxycian.PredicateMethodClassification;
import one.edee.oss.proxycian.recipe.IntroductionAdvice;
import one.edee.oss.proxycian.util.ReflectionUtils;

import java.util.Collections;
import java.util.List;

import static one.edee.oss.proxycian.MethodClassification.noContext;


/**
 * GroovyObjectAdvice returns properly initialized metaClass object for generated proxies in order to avoid
 * ClassCastExceptions.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2021
 */
public class GroovyObjectAdvice implements IntroductionAdvice<GroovyStateProvider> {
	public static GroovyObjectAdvice INSTANCE = new GroovyObjectAdvice();
	private static final long serialVersionUID = -4511766610737064209L;

	private GroovyObjectAdvice() { }

	@Override
	public Class<GroovyStateProvider> getRequestedStateContract() {
		return GroovyStateProvider.class;
	}

	@Override
	public List<Class<?>> getInterfacesToImplement() {
		return Collections.singletonList(GroovyStateProvider.class);
	}

	@Override
	public List<MethodClassification<?, GroovyStateProvider>> getMethodClassification() {
		return Collections.singletonList(
			new PredicateMethodClassification<>(
				/* description */   "GroovyStateProvider.getMetaClass()",
				/* matcher */       (method, proxyState) -> ReflectionUtils.isMethodDeclaredOn(method, GroovyStateProvider.class, "getMetaClass"),
				/* methodContext */ noContext(),
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> {
					final MetaClass metaClass = proxyState.getMetaClass();
					if (metaClass == null) {
						final MetaClass newMetaClass = new MetaClassImpl(proxyState.getBaseClass());
						proxyState.setMetaClass(newMetaClass);
						newMetaClass.initialize();
						return newMetaClass;
					} else {
						return metaClass;
					}
				}
			)
		);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || !(obj == null || getClass() != obj.getClass());
	}
    
}

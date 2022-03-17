package one.edee.oss.proxycian.model.traits;

import one.edee.oss.proxycian.ByteBuddyDispatcherInvocationHandler;
import one.edee.oss.proxycian.ByteBuddyProxyGenerator;
import one.edee.oss.proxycian.JavassistDispatcherInvocationHandler;
import one.edee.oss.proxycian.JavassistProxyGenerator;
import one.edee.oss.proxycian.PredicateMethodClassification;
import one.edee.oss.proxycian.bytebuddy.ByteBuddyDeserializationProxyFactory;
import one.edee.oss.proxycian.javassist.JavassistDeserializationProxyFactory;
import one.edee.oss.proxycian.trait.SerializableProxy;

import java.io.Serializable;
import java.lang.reflect.Modifier;

import static one.edee.oss.proxycian.util.ReflectionUtils.isMethodDeclaredOn;
import static org.apache.commons.lang.StringUtils.uncapitalize;

public interface GenericBucketProxyGenerator {
    String GET = "get";
    String SET = "set";
    String GET_PROPERTY = "getProperty";
    String SET_PROPERTY = "setProperty";
    String GET_PROPERTIES = "getProperties";

	/** METHOD CONTRACT: Object getSomething() **/
    static PredicateMethodClassification<PropertyAccessor, String, GenericBucket> getterInvoker() {
        return new PredicateMethodClassification<>(
        /* description */   "getter",
        /* matcher */       (method, proxyState) -> Modifier.isAbstract(method.getModifiers()) && method.getName().startsWith(GET) && method.getParameterCount() == 0,
        /* methodContext */ (method, proxyState) -> uncapitalize(method.getName().substring(GET.length())),
        /* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> proxyState.get(methodContext)
        );
    }

	/** METHOD CONTRACT: void setSomething(Object value) **/
    static PredicateMethodClassification<PropertyAccessor, String, GenericBucket> setterInvoker() {
        return new PredicateMethodClassification<>(
        /* description */   "setter",
        /* matcher */       (method, proxyState) -> Modifier.isAbstract(method.getModifiers()) && method.getName().startsWith(SET) && method.getParameterCount() == 1,
        /* methodContext */ (method, proxyState) -> uncapitalize(method.getName().substring(SET.length())),
        /* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> {
        	proxyState.set(methodContext, (Serializable) args[0]);
			return null;
		});
    }

	/** METHOD CONTRACT: Map<String,Object> getProperties() **/
    static PredicateMethodClassification<PropertyAccessor, Void, GenericBucket> getPropertiesInvoker() {
        return new PredicateMethodClassification<>(
        /* description */   "getProperties",
        /* matcher */       (method, proxyState) -> isMethodDeclaredOn(method, PropertyAccessor.class, GET_PROPERTIES),
        /* methodContext */ (method, proxyState) -> null,
        /* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> proxyState.getOrCreateLocalDataStore()
        );
    }

	/** METHOD CONTRACT: Object getProperty(String propertyName) **/
    static PredicateMethodClassification<PropertyAccessor, Void, GenericBucket> getPropertyInvoker() {
        return new PredicateMethodClassification<>(
        /* description */   "getProperty",
        /* matcher */       (method, proxyState) -> isMethodDeclaredOn(method, PropertyAccessor.class, GET_PROPERTY, String.class),
        /* methodContext */ (method, proxyState) -> null,
        /* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> {
        	proxyState.get(String.valueOf(args[0]));
        	return null;
		});
    }

	/** METHOD CONTRACT: void setProperty(String propertyName, Object propertyValue) **/
    static PredicateMethodClassification<PropertyAccessor, Void, GenericBucket> setPropertyInvoker() {
        return new PredicateMethodClassification<>(
        /* description */   "setProperty",
        /* matcher */       (method, proxyState) -> isMethodDeclaredOn(method, PropertyAccessor.class, SET_PROPERTY, String.class, Object.class),
        /* methodContext */ (method, proxyState) -> null,
        /* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> {
        	proxyState.set(String.valueOf(args[0]), (Serializable) args[1]);
		    return null;
		});
    }

    static <T> T instantiateJavassistProxy(Class<T> contract) {
        return JavassistProxyGenerator.instantiate(
				// create invocation handler delegating calls to "classifications" - ie atomic features of the proxy
                new JavassistDispatcherInvocationHandler<>(
						// proxy state
                        new GenericBucket(),
						// list of features - order is important
                        getPropertiesInvoker(),
                        getPropertyInvoker(),
                        setPropertyInvoker(),
                        getterInvoker(),
                        setterInvoker(),
                        SerializableProxy.getWriteReplaceMethodInvoker(JavassistDeserializationProxyFactory.INSTANCE)
                ),
				// interfaces to implement
                contract, SerializableProxy.class);
    }

    static <T> T instantiateByteBuddyProxy(Class<T> contract) {
        return ByteBuddyProxyGenerator.instantiate(
				// create invocation handler delegating calls to "classifications" - ie atomic features of the proxy
                new ByteBuddyDispatcherInvocationHandler<>(
						// proxy state
                        new GenericBucket(),
						// list of features - order is important
                        getPropertiesInvoker(),
                        getPropertyInvoker(),
                        setPropertyInvoker(),
                        getterInvoker(),
                        setterInvoker(),
                        SerializableProxy.getWriteReplaceMethodInvoker(ByteBuddyDeserializationProxyFactory.INSTANCE)
                ),
				// interfaces to implement
                contract,
	            SerializableProxy.class
        );
    }

}

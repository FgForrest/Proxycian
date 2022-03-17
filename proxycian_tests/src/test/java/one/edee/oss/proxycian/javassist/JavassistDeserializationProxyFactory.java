package one.edee.oss.proxycian.javassist;


import javassist.util.proxy.ProxyObject;
import one.edee.oss.proxycian.JavassistDispatcherInvocationHandler;
import one.edee.oss.proxycian.JavassistProxyGenerator;
import one.edee.oss.proxycian.ProxyStateWithConstructorArgs;
import one.edee.oss.proxycian.model.traits.GenericBucketProxyGenerator;
import one.edee.oss.proxycian.trait.ProxyStateAccessor;
import one.edee.oss.proxycian.trait.SerializableProxy;
import one.edee.oss.proxycian.trait.SerializableProxy.DeserializationProxyFactory;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JavassistDeserializationProxyFactory implements DeserializationProxyFactory {
	public static final SerializableProxy.DeserializationProxyFactory INSTANCE = new JavassistDeserializationProxyFactory();
	// LIST OF "SYSTEM" INTERFACES THAT ARE ADDED TO OUR PROXIES AUTOMATICALLY EITHER BY US OR BY THE BYTECODE LIBRARY
	private static final Set<Class<?>> EXCLUDED_CLASSES = new HashSet<>(
		Arrays.asList(
			ProxyObject.class,
			ProxyStateAccessor.class
		)
	);
	private static final long serialVersionUID = 3573491785842144918L;

	private JavassistDeserializationProxyFactory() {
        //singleton
    }

	@Nonnull
	@Override
	public Set<Class<?>> getExcludedInterfaces() {
		return EXCLUDED_CLASSES;
	}

	@Override
	public Object deserialize(@Nonnull Serializable proxyState, @Nonnull Class<?>[] interfaces) {
		return JavassistProxyGenerator.instantiate(
			new JavassistDispatcherInvocationHandler<>(
				proxyState,
				GenericBucketProxyGenerator.getPropertiesInvoker(),
				GenericBucketProxyGenerator.getterInvoker(),
				GenericBucketProxyGenerator.setterInvoker(),
				SerializableProxy.getWriteReplaceMethodInvoker(INSTANCE)
			),
			interfaces
		);
	}

	@Override
	public Object deserialize(@Nonnull ProxyStateWithConstructorArgs proxyState, @Nonnull Class<?>[] interfaces, @Nonnull Class<?>[] constructorTypes, @Nonnull Object[] constructorArgs) {
		return JavassistProxyGenerator.instantiate(
			new JavassistDispatcherInvocationHandler<>(
				proxyState,
				GenericBucketProxyGenerator.getPropertiesInvoker(),
				GenericBucketProxyGenerator.getterInvoker(),
				GenericBucketProxyGenerator.setterInvoker(),
				SerializableProxy.getWriteReplaceMethodInvoker(INSTANCE)
			),
			interfaces,
			constructorTypes,
			constructorArgs
		);
	}

}

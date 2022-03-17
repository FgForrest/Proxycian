package one.edee.oss.proxycian.bytebuddy;


import one.edee.oss.proxycian.ByteBuddyDispatcherInvocationHandler;
import one.edee.oss.proxycian.ByteBuddyProxyGenerator;
import one.edee.oss.proxycian.ProxyStateWithConstructorArgs;
import one.edee.oss.proxycian.trait.ProxyStateAccessor;
import one.edee.oss.proxycian.trait.SerializableProxy;
import one.edee.oss.proxycian.trait.SerializableProxy.DeserializationProxyFactory;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static one.edee.oss.proxycian.model.traits.GenericBucketProxyGenerator.*;

public class ByteBuddyDeserializationProxyFactory extends AbstractByteBuddyProxycianTest implements DeserializationProxyFactory {
	public static final DeserializationProxyFactory INSTANCE = new ByteBuddyDeserializationProxyFactory();
	// LIST OF "SYSTEM" INTERFACES THAT ARE ADDED TO OUR PROXIES AUTOMATICALLY EITHER BY US OR BY THE BYTECODE LIBRARY
	private static final Set<Class<?>> EXCLUDED_CLASSES = new HashSet<>(
		Collections.singletonList(
			ProxyStateAccessor.class
		)
	);
	private static final long serialVersionUID = -9030607883489527280L;

	private ByteBuddyDeserializationProxyFactory() {
        // singleton
    }

	@Nonnull
	@Override
	public Set<Class<?>> getExcludedInterfaces() {
		return EXCLUDED_CLASSES;
	}

	@Override
	public Object deserialize(@Nonnull Serializable proxyState, @Nonnull Class<?>[] interfaces) {
		return ByteBuddyProxyGenerator.instantiate(
			new ByteBuddyDispatcherInvocationHandler<>(
				proxyState,
				getPropertiesInvoker(),
				getPropertyInvoker(),
				setPropertyInvoker(),
				getterInvoker(),
				setterInvoker(),
				SerializableProxy.getWriteReplaceMethodInvoker(INSTANCE)
			),
			interfaces
		);
	}

	@Override
	public Object deserialize(@Nonnull ProxyStateWithConstructorArgs proxyState, @Nonnull Class<?>[] interfaces, @Nonnull Class<?>[] constructorTypes, @Nonnull Object[] constructorArgs) {
		return ByteBuddyProxyGenerator.instantiate(
			new ByteBuddyDispatcherInvocationHandler<>(
				proxyState,
				getPropertiesInvoker(),
				getPropertyInvoker(),
				setPropertyInvoker(),
				getterInvoker(),
				setterInvoker(),
				SerializableProxy.getWriteReplaceMethodInvoker(INSTANCE)
			),
			interfaces,
			constructorTypes,
			constructorArgs
		);
	}
}

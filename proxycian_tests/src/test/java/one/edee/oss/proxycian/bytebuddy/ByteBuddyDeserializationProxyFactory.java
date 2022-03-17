package one.edee.oss.proxycian.bytebuddy;


import one.edee.oss.proxycian.ProxyStateWithConstructorArgs;
import one.edee.oss.proxycian.trait.SerializableProxy;
import one.edee.oss.proxycian.trait.SerializableProxy.DeserializationProxyFactory;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Set;

import static one.edee.oss.proxycian.model.traits.GenericBucketProxyGenerator.*;

public class ByteBuddyDeserializationProxyFactory extends AbstractByteBuddyProxycianTest implements DeserializationProxyFactory {
	public static final DeserializationProxyFactory INSTANCE = new ByteBuddyDeserializationProxyFactory();
	private static final long serialVersionUID = -9030607883489527280L;

	private ByteBuddyDeserializationProxyFactory() {
        // singleton
    }

	@Nonnull
	@Override
	public Set<Class<?>> getExcludedInterfaces() {
		return ByteBuddyProxyGenerator.EXCLUDED_CLASSES;
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

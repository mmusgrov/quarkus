package io.quarkus.arc;

import javax.enterprise.context.spi.CreationalContext;

/**
 * 
 * 
 * @param <T>
 */
public class ContextInstanceHandleImpl<T> extends InstanceHandleImpl<T> implements ContextInstanceHandle<T> {

    public ContextInstanceHandleImpl(InjectableBean<T> bean, T instance, CreationalContext<T> creationalContext) {
        super(bean, instance, creationalContext);
    }

    @Override
    public void destroy() {
        destroyInternal();
    }

}

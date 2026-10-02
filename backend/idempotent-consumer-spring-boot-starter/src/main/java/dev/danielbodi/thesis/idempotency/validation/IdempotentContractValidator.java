package dev.danielbodi.thesis.idempotency.validation;

import dev.danielbodi.thesis.idempotency.annotation.Idempotent;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * @author danielbodi
 */
public class IdempotentContractValidator implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        final Class<?> targetClass = AopUtils.getTargetClass(bean);
        final ReflectionUtils.MethodFilter filter =
                method -> AnnotationUtils.findAnnotation(method, Idempotent.class) != null;
        final Set<Method> methods = MethodIntrospector.selectMethods(targetClass, filter);
        for (Method method : methods) {
            validate(method);
        }
        return bean;
    }

    private void validate(Method method) {
        if (!method.isAnnotationPresent(Idempotent.class)) {
            throw new BeanInitializationException(
                    "@Idempotent must be declared on the implementing method, not inherited: " + method);
        }
        if (method.getReturnType() != void.class) {
            throw new BeanInitializationException("@Idempotent method must be void: " + method);
        }
    }
}

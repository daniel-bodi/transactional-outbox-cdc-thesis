package dev.danielbodi.thesis.inbox.validation;

import dev.danielbodi.thesis.inbox.annotation.Idempotent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanInitializationException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author danielbodi
 */
class IdempotentContractValidatorTest {

    private final IdempotentContractValidator validator = new IdempotentContractValidator();

    @Test
    void acceptsVoidMethod() {
        assertThatCode(() -> validator.postProcessAfterInitialization(new ValidBean(), "bean"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNonVoidReturnType() {
        assertThatThrownBy(() -> validator.postProcessAfterInitialization(new NonVoidBean(), "bean"))
                .isInstanceOf(BeanInitializationException.class)
                .hasMessageContaining("must be void");
    }

    @Test
    void rejectsIdempotentInheritedFromInterface() {
        assertThatThrownBy(() -> validator.postProcessAfterInitialization(new InheritedIdempotentBean(), "bean"))
                .isInstanceOf(BeanInitializationException.class)
                .hasMessageContaining("not inherited");
    }

    static class ValidBean {
        @Idempotent
        public void on(String payload) {
        }
    }

    static class NonVoidBean {
        @Idempotent
        public String on() {
            return "no";
        }
    }

    interface IdempotentListener {
        @Idempotent
        void on();
    }

    static class InheritedIdempotentBean implements IdempotentListener {
        @Override
        public void on() {
        }
    }
}

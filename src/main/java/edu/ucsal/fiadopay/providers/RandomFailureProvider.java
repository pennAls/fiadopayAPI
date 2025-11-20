package edu.ucsal.fiadopay.providers;
import edu.ucsal.fiadopay.annotations.RandomicFailureRate;
import edu.ucsal.fiadopay.decorators.RandomFailureDecorator;
import edu.ucsal.fiadopay.strategies.PaymentStategy;
import org.springframework.stereotype.Component;


@Component
public class RandomFailureProvider implements AnnotationProvider {

    @Override
    public PaymentStategy apply(PaymentStategy current, Class<?> strategyClass) {
        if (strategyClass.isAnnotationPresent(RandomicFailureRate.class)) {
            RandomicFailureRate annotation = strategyClass.getAnnotation(RandomicFailureRate.class);
            return new RandomFailureDecorator(annotation.failureRate());
        } else {
            return current;
        }
    }
}
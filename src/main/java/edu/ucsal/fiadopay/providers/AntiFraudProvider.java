package edu.ucsal.fiadopay.providers;
import edu.ucsal.fiadopay.annotations.AntiFraud;
import edu.ucsal.fiadopay.decorators.AntiFraudDecorator;
import edu.ucsal.fiadopay.strategies.PaymentStategy;
import org.springframework.stereotype.Component;

@Component
public class AntiFraudProvider implements AnnotationProvider {

    @Override
    public PaymentStategy apply(PaymentStategy current, Class<?> strategyClass) {
        if(strategyClass.isAnnotationPresent(AntiFraud.class)){
            AntiFraud annotation = strategyClass.getAnnotation(AntiFraud.class);
            return new AntiFraudDecorator(annotation.threshold());
        } else {
            return current;
        }
    }
}
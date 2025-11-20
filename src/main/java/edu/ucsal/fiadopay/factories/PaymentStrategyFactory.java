package edu.ucsal.fiadopay.factories;

import edu.ucsal.fiadopay.annotations.AntiFraud;
import edu.ucsal.fiadopay.annotations.PaymentMethod;
import edu.ucsal.fiadopay.annotations.RandomicFailureRate;
import edu.ucsal.fiadopay.decorators.RandomFailureDecorator;
import edu.ucsal.fiadopay.providers.AnnotationProvider;
import edu.ucsal.fiadopay.strategies.PaymentStategy;
import edu.ucsal.fiadopay.decorators.AntiFraudDecorator;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PaymentStrategyFactory {
    private final Map<String, PaymentStategy> strategies = new HashMap<String, PaymentStategy>();

    public PaymentStrategyFactory(List<PaymentStategy> stategyList, List<AnnotationProvider> annotationProviderList) {
        for (PaymentStategy strategy : stategyList) {
            Class<?> classe = strategy.getClass();
            if (classe.isAnnotationPresent(PaymentMethod.class)) {
                PaymentMethod paymentMethod = classe.getAnnotation(PaymentMethod.class);
                String method = paymentMethod.type();
                PaymentStategy selectedStrategy = strategy;
                for (AnnotationProvider annotationProvider : annotationProviderList) {
                    selectedStrategy = annotationProvider.apply(selectedStrategy, classe);
                }
                strategies.put(method, selectedStrategy);
            }
        }
    }

    public PaymentStategy getStrategy(String method) {
        PaymentStategy stategy = strategies.get(method);
        if (stategy == null) {
            throw new Error("Método de pagamento não suportado");
        }
        return stategy;
    }
}

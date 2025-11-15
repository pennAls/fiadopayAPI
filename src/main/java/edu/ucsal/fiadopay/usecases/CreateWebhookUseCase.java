package edu.ucsal.fiadopay.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsal.fiadopay.domain.Payment;
import edu.ucsal.fiadopay.domain.WebhookDelivery;
import edu.ucsal.fiadopay.repo.MerchantRepository;
import edu.ucsal.fiadopay.repo.WebhookDeliveryRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Component
public class CreateWebhookUseCase {
    private final MerchantRepository merchants;
    private final ObjectMapper objectMapper;
    private final WebhookDeliveryRepository deliveries;

    @Value("${fiadopay.webhook-secret}") String secret;


    public CreateWebhookUseCase(MerchantRepository merchants, ObjectMapper objectMapper, WebhookDeliveryRepository deliveries) {
        this.merchants = merchants;
        this.objectMapper = objectMapper;
        this.deliveries = deliveries;
    }

    @Transactional
    public WebhookDelivery execute(Payment payment) {
            var merchant = merchants.findById(payment.getMerchantId()).orElse(null);
            if (merchant==null || merchant.getWebhookUrl()==null || merchant.getWebhookUrl().isBlank()) return null;

            String payload;
            try {
                var data = Map.of(
                        "paymentId", payment.getId(),
                        "status", payment.getStatus().name(),
                        "occurredAt", Instant.now().toString()
                );
                var event = Map.of(
                        "id", "evt_"+ UUID.randomUUID().toString().substring(0,8),
                        "type", "payment.updated",
                        "data", data
                );
                payload = objectMapper.writeValueAsString(event);
            } catch (Exception e) {
                // fallback mínimo: não envia webhook se falhar a serialização
                return null;
            }

            var signature = hmac(payload, secret);

           var delivery = deliveries.save(WebhookDelivery.builder()
                    .eventId("evt_"+UUID.randomUUID().toString().substring(0,8))
                    .eventType("payment.updated")
                    .paymentId(payment.getId())
                    .targetUrl(merchant.getWebhookUrl())
                    .signature(signature)
                    .payload(payload)
                    .attempts(0)
                    .delivered(false)
                    .lastAttemptAt(null)
                    .build());

           return delivery;
        }

    public String hmac(String payload, String secret){
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(payload.getBytes()));
        } catch (Exception e){ return ""; }
    }

}


package org.otus;

import java.time.Duration;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;

public class Consumer {
    public static void main(String[] args) {

        var consumer = new KafkaConsumer<String, String>(
                Utils.createConsumerConfig(m -> m.put(ConsumerConfig.GROUP_ID_CONFIG, " java-app)")));

        consumer.subscribe(List.of("test-topic"));

        while (true) {
            var records = consumer.poll(Duration.ofSeconds(10));

            for (var record : records) {
                Utils.log.warn("Message {}.{}: {} -> {}", record.topic(), record.partition(),
                        record.key(), record.value());

            }
        }
    }
}

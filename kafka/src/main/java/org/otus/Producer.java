package org.otus;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class Producer {
    public static void main(String[] args) {

        var producer = new KafkaProducer<String, String>(Utils.producerConfig);
        for (int i = 0; i < 10; i++) {
            producer.send(new ProducerRecord<>("test-topic", "key" + i, "value" + i));
        }
        producer.flush();
        producer.close();
    }
}

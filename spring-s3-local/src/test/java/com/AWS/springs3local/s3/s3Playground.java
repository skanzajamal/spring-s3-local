package com.AWS.springs3local.s3;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
public class s3Playground {

    @Autowired
    AmazonSQSAsync amazonSQSAsync;

    @Autowired
    private ObjectMapper objectMapper;

    public final String REGION = "eu-central-1";
    public final String ENDPOINT_URL = "http://localhost:4566";
    private static final String QUEUE_NAME = "testqueue";

    private AmazonSQSAsync amazonSQSAsyncClient() {
        return AmazonSQSAsyncClientBuilder.standard()
                // only needed locally for the use with localstack
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(ENDPOINT_URL, REGION))
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();
    }

    @Bean
    public QueueMessagingTemplate queueMessagingTemplate() {
        return new QueueMessagingTemplate(amazonSQSAsync);
    }

    @Test
    public void roundTrip1() {

        // this scenario simply shows how to send and receive messages

        var sqsClient  = amazonSQSAsyncClient();

        // the client can surely be used on it's own. however using the template is somewhat more comfortable
        var qmTemplate = new QueueMessagingTemplate(sqsClient);

        var payload = new MessageData("Charles Bronson", 79);
        qmTemplate.convertAndSend(QUEUE_NAME, payload);

        var loadedPayload = qmTemplate.receiveAndConvert(QUEUE_NAME, MessageData.class);
        System.err.println("Message is equal: " + payload.equals(loadedPayload));

    }

    @Test
    public void roundTrip2() throws Exception {

        // this scenario shows a simple thread receiving messages from the queue.
        // this thread only fetches messages once. in reality the thread would go
        // on until the application will be closed.

        var sqsClient  = amazonSQSAsyncClient();
        var qmTemplate = new QueueMessagingTemplate(sqsClient);

        Thread thread = new Thread(() -> receiveAndWaitForMessages(sqsClient));
        thread.start();

        for (int i = 0; i < 4; i++) {
            qmTemplate.convertAndSend(QUEUE_NAME,  new MessageData("Charles Bronson: " + i, i));
        }

        thread.join();

    }

    private void receiveAndWaitForMessages(AmazonSQSAsync sqsClient) {

        var queueUrl   = sqsClient.getQueueUrl(QUEUE_NAME);
        var request    = new ReceiveMessageRequest(queueUrl.getQueueUrl())
                .withWaitTimeSeconds(20)
                .withMaxNumberOfMessages(10)
                ;

        System.err.println("Waiting for messages...");
        var messages = sqsClient.receiveMessage(request).getMessages();
        System.err.printf("Got %d messages...\n", messages.size());

        for (var message : messages) {
            try {

                var payload = objectMapper.readValue(message.getBody(), MessageData.class);
                System.err.println("=> " + payload);

                // if we don't delete the message it will be provided next time again.
                // deletion should be omitted in an error case or there should be appropriate error
                // handling (for instance dead letter queue on aws side).
                sqsClient.deleteMessage(queueUrl.getQueueUrl(), message.getReceiptHandle());

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static class MessageData {

        private String name;
        private int age;

        public MessageData() {
            this(null, 0);
        }

        public MessageData(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public String toString() {
            return "MessageData [name=" + name + ", age=" + age + "]";
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (other == this) {
                return true;
            }
            if (other instanceof MessageData) {
                var md = (MessageData) other;
                return Objects.equals(name, md.name) && (age == md.age);
            }
            return false;
        }
    }

}

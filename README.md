# Localstack for testing AWS interfaces

# Application Features:
 This application integrate Spring boot with SQS with the use of LocalStack to provide the same functionality and APIs as the real AWS cloud environment.
 
 # Prerequisites
- Docker 
- JDK 11
- intelliJ or any IDE

 # some useful commands

 $ docker-compose up
 
 - create bucket
 
 $ aws --endpoint-url=http://localhost:4566 s3 mb s3://mytestbucket
 
  - list bucket
  
 $ aws --endpoint-url=http://localhost:4566 s3 ls
 
  - create queue
  
 $ aws --endpoint-url=http://localhost:4566 sqs create-queue --queue-name test_queue
 
  - list all bucket
  
 $ aws --endpoint-url=http://localhost:4566 --region=eu-central-1 --no-sign-request --no-paginate sqs list-queues
 
  - content of specific queue (received message)
  
 $ aws --endpoint-url=http://localhost:4566 sqs receive-message --queue http://localhost:4566/000000000000/test_queue

 
 
  
 
 
 
 
 


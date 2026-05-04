package org.otus;

import java.net.URI;


import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class Utils {

  public static S3Client createS3Client() {
    return S3Client.builder()
        .endpointOverride(URI.create("http://localhost:9010"))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create("minioadmin", "minioadmin")))
        .region(Region.US_EAST_1)
        .forcePathStyle(true)
        .build();
  }
}

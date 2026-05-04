package org.otus;

import static org.otus.Utils.createS3Client;

import java.nio.file.Path;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class Main {

    public static void main(String[] args) {
        var s3Client = createS3Client();
        putObject(s3Client, "from_java.txt", "java_test.txt");
        var response = listObjects(s3Client);
        print(response);
    }

    private static void putObject(S3Client s3, String key, String filePath) {
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket("otus-bucket")
                        .key(key)
                        .build(),
                Path.of(filePath));
    }

    private static ListObjectsV2Response listObjects(S3Client s3) {
        return s3.listObjectsV2(
                ListObjectsV2Request.builder()
                        .bucket("otus-bucket")
                        .build());
    }

    private static void print(ListObjectsV2Response response) {
        response.contents().forEach(obj -> System.out.println(obj.key() + " | " + obj.size() + " bytes"));
    }
}

package com.example.demo;

import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.S3Object;

@SpringBootApplication
public class SpringbootRekognitionApplication {

	public static void main(String[] args) {
		//		SpringApplication.run(SpringbootRekognitionApplication.class, args);

		try (ConfigurableApplicationContext ctx = SpringApplication.run(SpringbootRekognitionApplication.class, args)) {
			SpringbootRekognitionApplication app = ctx.getBean(SpringbootRekognitionApplication.class);
			app.getDetectLabels();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Amazon S3 バケットに保存されたイメージの分析
	 */
	private void getDetectLabels() {
		// S3に格納されている画像
		String keyName = "photo.png";
		String bucket = "bucket";
		S3Object s3Object = new S3Object().withName(keyName).withBucket(bucket);
		Image imageObject = new Image().withS3Object(s3Object);

		AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

		DetectLabelsRequest request = new DetectLabelsRequest()
				.withImage(imageObject)
				.withMaxLabels(10).withMinConfidence(75F);

		try {
			// 画像分析を実行する
			DetectLabelsResult result = rekognitionClient.detectLabels(request);

			// 結果を出力する
			System.out.println("Detected labels for " + s3Object.toString());
			List<Label> labels = result.getLabels();
			for (Label label : labels) {
				System.out.println(label.getName() + ": " + label.getConfidence().toString());
			}
		} catch (AmazonRekognitionException e) {
			e.printStackTrace();
		}
	}
}

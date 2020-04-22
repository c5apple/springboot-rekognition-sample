package com.example.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AgeRange;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.Attribute;
import com.amazonaws.services.rekognition.model.BoundingBox;
import com.amazonaws.services.rekognition.model.CompareFacesMatch;
import com.amazonaws.services.rekognition.model.CompareFacesRequest;
import com.amazonaws.services.rekognition.model.CompareFacesResult;
import com.amazonaws.services.rekognition.model.ComparedFace;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.DetectModerationLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectModerationLabelsResult;
import com.amazonaws.services.rekognition.model.DetectTextRequest;
import com.amazonaws.services.rekognition.model.DetectTextResult;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.ModerationLabel;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.TextDetection;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
public class SpringbootRekognitionApplication {

	public static void main(String[] args) {
		//		SpringApplication.run(SpringbootRekognitionApplication.class, args);

		try (ConfigurableApplicationContext ctx = SpringApplication.run(SpringbootRekognitionApplication.class, args)) {
			SpringbootRekognitionApplication app = ctx.getBean(SpringbootRekognitionApplication.class);
			app.getDetectLabels();
			app.getDetectLabelsForLocal();
			app.getDetectFaces();
			app.getCompareFaces();
			app.getDetectModerationLabels();
			app.getDetectText();
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

	/**
	 * ローカルファイルシステムからロードしたイメージの分析
	 */
	private void getDetectLabelsForLocal() throws FileNotFoundException, IOException {
		// ローカルファイルの画像
		String filePath = "C:\\photo.jpg";
		ByteBuffer imageBytes;
		try (InputStream inputStream = new FileInputStream(new File(filePath))) {
			imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
		}
		Image imageObject = new Image().withBytes(imageBytes);

		AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

		DetectLabelsRequest request = new DetectLabelsRequest()
				.withImage(imageObject)
				.withMaxLabels(10)
				.withMinConfidence(77F);

		try {
			// 画像分析を実行する
			DetectLabelsResult result = rekognitionClient.detectLabels(request);

			// 結果を出力する
			System.out.println("Detected labels for " + filePath);
			List<Label> labels = result.getLabels();
			for (Label label : labels) {
				System.out.println(label.getName() + ": " + label.getConfidence().toString());
			}
		} catch (AmazonRekognitionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * イメージ内の顔の検出
	 */
	private void getDetectFaces() throws FileNotFoundException, IOException {
		// ローカルファイルの画像
		String filePath = "C:\\photo.jpg";
		ByteBuffer imageBytes;
		try (InputStream inputStream = new FileInputStream(new File(filePath))) {
			imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
		}
		Image imageObject = new Image().withBytes(imageBytes);

		AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

		DetectFacesRequest request = new DetectFacesRequest()
				.withImage(imageObject)
				.withAttributes(Attribute.ALL);
		// Replace Attribute.ALL with Attribute.DEFAULT to get default values.

		try {
			// 画像分析を実行する
			DetectFacesResult result = rekognitionClient.detectFaces(request);

			// 結果を出力する
			System.out.println("Detected labels for " + filePath);
			List<FaceDetail> faceDetails = result.getFaceDetails();
			for (FaceDetail face : faceDetails) {
				if (request.getAttributes().contains("ALL")) {
					AgeRange ageRange = face.getAgeRange();
					System.out.println("The detected face is estimated to be between "
							+ ageRange.getLow().toString() + " and " + ageRange.getHigh().toString()
							+ " years old.");
					System.out.println("Here's the complete set of attributes:");
				} else { // non-default attributes have null values.
					System.out.println("Here's the default set of attributes:");
				}

				ObjectMapper objectMapper = new ObjectMapper();
				System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(face));
			}
		} catch (AmazonRekognitionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * イメージ間の顔の比較
	 */
	private void getCompareFaces() throws FileNotFoundException, IOException {
		// ローカルファイルの画像
		String sourcePath = "C:\\source.jpg"; // 一人の写真
		String targetPath = "C:\\target.jpg"; // 一人または複数人の写真

		ByteBuffer sourceImageBytes = null;
		try (InputStream inputStream = new FileInputStream(new File(sourcePath))) {
			sourceImageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
		}
		ByteBuffer targetImageBytes = null;
		try (InputStream inputStream = new FileInputStream(new File(targetPath))) {
			targetImageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
		}

		Image source = new Image().withBytes(sourceImageBytes);
		Image target = new Image().withBytes(targetImageBytes);

		AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

		CompareFacesRequest request = new CompareFacesRequest()
				.withSourceImage(source)
				.withTargetImage(target)
				.withSimilarityThreshold(70F);

		// 画像分析を実行する
		CompareFacesResult compareFacesResult = rekognitionClient.compareFaces(request);

		// 結果を出力する
		List<CompareFacesMatch> faceDetails = compareFacesResult.getFaceMatches();
		for (CompareFacesMatch match : faceDetails) {
			ComparedFace face = match.getFace();
			BoundingBox position = face.getBoundingBox();
			System.out.println("Face at " + position.getLeft().toString()
					+ " " + position.getTop()
					+ " matches with " + match.getSimilarity().toString()
					+ "% confidence.");
		}

		List<ComparedFace> uncompared = compareFacesResult.getUnmatchedFaces();
		System.out.println("There was " + uncompared.size()
				+ " face(s) that did not match");
	}

	/**
	 * 安全でないイメージの検出
	 */
	private void getDetectModerationLabels() throws FileNotFoundException, IOException {
		// ローカルファイルの画像
		String filePath = "C:\\photo.jpg";
		ByteBuffer imageBytes;
		try (InputStream inputStream = new FileInputStream(new File(filePath))) {
			imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
		}
		Image imageObject = new Image().withBytes(imageBytes);

		AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

		DetectModerationLabelsRequest request = new DetectModerationLabelsRequest()
				.withImage(imageObject)
				.withMinConfidence(60F);

		try {
			// 画像分析を実行する
			DetectModerationLabelsResult result = rekognitionClient.detectModerationLabels(request);

			// 結果を出力する
			System.out.println("Detected labels for " + filePath);
			List<ModerationLabel> labels = result.getModerationLabels();
			for (ModerationLabel label : labels) {
				System.out.println("Label: " + label.getName()
						+ "\n Confidence: " + label.getConfidence().toString() + "%"
						+ "\n Parent:" + label.getParentName());
			}
		} catch (AmazonRekognitionException e) {
			e.printStackTrace();
		}
	}

	/**
	 * イメージ内のテキストの検出
	 * <p>※日本語は対応していない</p>
	 */
	private void getDetectText() throws FileNotFoundException, IOException {
		// ローカルファイルの画像
		String filePath = "C:\\photo.jpg";
		ByteBuffer imageBytes;
		try (InputStream inputStream = new FileInputStream(new File(filePath))) {
			imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
		}
		Image imageObject = new Image().withBytes(imageBytes);

		AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();

		DetectTextRequest request = new DetectTextRequest().withImage(imageObject);

		try {
			// 画像分析を実行する
			DetectTextResult result = rekognitionClient.detectText(request);

			// 結果を出力する
			System.out.println("Detected lines and words for " + filePath);
			List<TextDetection> textDetections = result.getTextDetections();
			for (TextDetection text : textDetections) {

				System.out.println("Detected: " + text.getDetectedText());
				System.out.println("Confidence: " + text.getConfidence().toString());
				System.out.println("Id : " + text.getId());
				System.out.println("Parent Id: " + text.getParentId());
				System.out.println("Type: " + text.getType());
				System.out.println();
			}
		} catch (AmazonRekognitionException e) {
			e.printStackTrace();
		}
	}
}

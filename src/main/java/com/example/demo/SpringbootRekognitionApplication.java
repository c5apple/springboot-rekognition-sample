package com.example.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.amazonaws.auth.policy.Condition;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.SQSActions;
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
import com.amazonaws.services.rekognition.model.GetLabelDetectionRequest;
import com.amazonaws.services.rekognition.model.GetLabelDetectionResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Instance;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.LabelDetection;
import com.amazonaws.services.rekognition.model.LabelDetectionSortBy;
import com.amazonaws.services.rekognition.model.ModerationLabel;
import com.amazonaws.services.rekognition.model.NotificationChannel;
import com.amazonaws.services.rekognition.model.Parent;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.rekognition.model.StartLabelDetectionRequest;
import com.amazonaws.services.rekognition.model.StartLabelDetectionResult;
import com.amazonaws.services.rekognition.model.TextDetection;
import com.amazonaws.services.rekognition.model.Video;
import com.amazonaws.services.rekognition.model.VideoMetadata;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;
import com.amazonaws.util.IOUtils;
import com.fasterxml.jackson.databind.JsonNode;
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
			app.getDetectVideo();
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

	private static String sqsQueueName = null;
	private static String snsTopicName = null;
	private static String snsTopicArn = "arn:aws:sns:XXXXX"; // FIXME
	private static String roleArn = null;
	private static String sqsQueueUrl = null;
	private static String sqsQueueArn = null;
	private static String startJobId = null;
	private static String bucket = null;
	private static String video = null;
	private static AmazonSQS sqs = null;
	private static AmazonSNS sns = null;
	private static AmazonRekognition rek = null;

	/**
	 * 保存されたビデオの分析
	 */
	private void getDetectVideo() throws Exception {
		video = "video.mp4"; // FIXME
		bucket = "bucket"; // FIXME
		roleArn = "arn:aws:iam::XXXXX:role"; // FIXME

		sns = AmazonSNSClientBuilder.defaultClient();
		sqs = AmazonSQSClientBuilder.defaultClient();
		rek = AmazonRekognitionClientBuilder.defaultClient();

		CreateTopicandQueue();

		//=================================================

		StartLabelDetection(bucket, video);

		if (GetSQSMessageSuccess() == true)
			GetLabelDetectionResults();

		//=================================================

		DeleteTopicandQueue();
		System.out.println("Done!");
	}

	static boolean GetSQSMessageSuccess() throws Exception {
		boolean success = false;

		System.out.println("Waiting for job: " + startJobId);
		//Poll queue for messages
		List<Message> messages = null;
		int dotLine = 0;
		boolean jobFound = false;

		//loop until the job status is published. Ignore other messages in queue.
		do {
			messages = sqs.receiveMessage(sqsQueueUrl).getMessages();
			if (dotLine++ < 40) {
				System.out.print(".");
			} else {
				System.out.println();
				dotLine = 0;
			}

			if (!messages.isEmpty()) {
				//Loop through messages received.
				for (Message message : messages) {
					String notification = message.getBody();

					// Get status and job id from notification.
					ObjectMapper mapper = new ObjectMapper();
					JsonNode jsonMessageTree = mapper.readTree(notification);
					JsonNode messageBodyText = jsonMessageTree.get("Message");
					ObjectMapper operationResultMapper = new ObjectMapper();
					JsonNode jsonResultTree = operationResultMapper.readTree(messageBodyText.textValue());
					JsonNode operationJobId = jsonResultTree.get("JobId");
					JsonNode operationStatus = jsonResultTree.get("Status");
					System.out.println("Job found was " + operationJobId);
					// Found job. Get the results and display.
					if (operationJobId.asText().equals(startJobId)) {
						jobFound = true;
						System.out.println("Job id: " + operationJobId);
						System.out.println("Status : " + operationStatus.toString());
						if (operationStatus.asText().equals("SUCCEEDED")) {
							success = true;
						} else {
							System.out.println("Video analysis failed");
						}

						sqs.deleteMessage(sqsQueueUrl, message.getReceiptHandle());
					}

					else {
						System.out.println("Job received was not job " + startJobId);
						//Delete unknown message. Consider moving message to dead letter queue
						sqs.deleteMessage(sqsQueueUrl, message.getReceiptHandle());
					}
				}
			} else {
				Thread.sleep(5000);
			}
		} while (!jobFound);

		System.out.println("Finished processing video");
		return success;
	}

	private static void StartLabelDetection(String bucket, String video) throws Exception {

		NotificationChannel channel = new NotificationChannel()
				.withSNSTopicArn(snsTopicArn)
				.withRoleArn(roleArn);

		StartLabelDetectionRequest req = new StartLabelDetectionRequest()
				.withVideo(new Video()
						.withS3Object(new S3Object()
								.withBucket(bucket)
								.withName(video)))
				.withMinConfidence(50F)
				.withJobTag("DetectingLabels")
				.withNotificationChannel(channel);

		StartLabelDetectionResult startLabelDetectionResult = rek.startLabelDetection(req);
		startJobId = startLabelDetectionResult.getJobId();

	}

	private static void GetLabelDetectionResults() throws Exception {

		int maxResults = 10;
		String paginationToken = null;
		GetLabelDetectionResult labelDetectionResult = null;

		do {
			if (labelDetectionResult != null) {
				paginationToken = labelDetectionResult.getNextToken();
			}

			GetLabelDetectionRequest labelDetectionRequest = new GetLabelDetectionRequest()
					.withJobId(startJobId)
					.withSortBy(LabelDetectionSortBy.TIMESTAMP)
					.withMaxResults(maxResults)
					.withNextToken(paginationToken);

			labelDetectionResult = rek.getLabelDetection(labelDetectionRequest);

			VideoMetadata videoMetaData = labelDetectionResult.getVideoMetadata();

			System.out.println("Format: " + videoMetaData.getFormat());
			System.out.println("Codec: " + videoMetaData.getCodec());
			System.out.println("Duration: " + videoMetaData.getDurationMillis());
			System.out.println("FrameRate: " + videoMetaData.getFrameRate());

			//Show labels, confidence and detection times
			List<LabelDetection> detectedLabels = labelDetectionResult.getLabels();

			for (LabelDetection detectedLabel : detectedLabels) {
				long seconds = detectedLabel.getTimestamp();
				Label label = detectedLabel.getLabel();
				System.out.println("Millisecond: " + Long.toString(seconds) + " ");

				System.out.println("   Label:" + label.getName());
				System.out.println("   Confidence:" + detectedLabel.getLabel().getConfidence().toString());

				List<Instance> instances = label.getInstances();
				System.out.println("   Instances of " + label.getName());
				if (instances.isEmpty()) {
					System.out.println("        " + "None");
				} else {
					for (Instance instance : instances) {
						System.out.println("        Confidence: " + instance.getConfidence().toString());
						System.out.println("        Bounding box: " + instance.getBoundingBox().toString());
					}
				}
				System.out.println("   Parent labels for " + label.getName() + ":");
				List<Parent> parents = label.getParents();
				if (parents.isEmpty()) {
					System.out.println("        None");
				} else {
					for (Parent parent : parents) {
						System.out.println("        " + parent.getName());
					}
				}
				System.out.println();
			}
		} while (labelDetectionResult != null && labelDetectionResult.getNextToken() != null);

	}

	// Creates an SNS topic and SQS queue. The queue is subscribed to the topic.
	static void CreateTopicandQueue() {
		//create a new SNS topic
		snsTopicName = "AmazonRekognitionTopic" + Long.toString(System.currentTimeMillis());
		CreateTopicRequest createTopicRequest = new CreateTopicRequest(snsTopicName);
		CreateTopicResult createTopicResult = sns.createTopic(createTopicRequest);
		snsTopicArn = createTopicResult.getTopicArn();

		//Create a new SQS Queue
		sqsQueueName = "AmazonRekognitionQueue" + Long.toString(System.currentTimeMillis());
		final CreateQueueRequest createQueueRequest = new CreateQueueRequest(sqsQueueName);
		sqsQueueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();
		sqsQueueArn = sqs.getQueueAttributes(sqsQueueUrl, Arrays.asList("QueueArn")).getAttributes().get("QueueArn");

		//Subscribe SQS queue to SNS topic
		String sqsSubscriptionArn = sns.subscribe(snsTopicArn, "sqs", sqsQueueArn).getSubscriptionArn();

		// Authorize queue
		Policy policy = new Policy().withStatements(
				new Statement(Effect.Allow)
						.withPrincipals(Principal.AllUsers)
						.withActions(SQSActions.SendMessage)
						.withResources(new Resource(sqsQueueArn))
						.withConditions(new Condition().withType("ArnEquals").withConditionKey("aws:SourceArn")
								.withValues(snsTopicArn)));

		Map<String, String> queueAttributes = new HashMap<String, String>();
		queueAttributes.put(QueueAttributeName.Policy.toString(), policy.toJson());
		sqs.setQueueAttributes(new SetQueueAttributesRequest(sqsQueueUrl, queueAttributes));

		System.out.println("Topic arn: " + snsTopicArn);
		System.out.println("Queue arn: " + sqsQueueArn);
		System.out.println("Queue url: " + sqsQueueUrl);
		System.out.println("Queue sub arn: " + sqsSubscriptionArn);
	}

	static void DeleteTopicandQueue() {
		if (sqs != null) {
			sqs.deleteQueue(sqsQueueUrl);
			System.out.println("SQS queue deleted");
		}

		if (sns != null) {
			sns.deleteTopic(snsTopicArn);
			System.out.println("SNS topic deleted");
		}
		// TODO サブスクリプションが削除されていない
	}
}

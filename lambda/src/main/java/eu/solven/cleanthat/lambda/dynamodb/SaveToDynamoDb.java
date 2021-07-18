package eu.solven.cleanthat.lambda.dynamodb;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;

import eu.solven.cleanthat.lambda.step0_checkwebhook.IWebhookEvent;

/**
 * Helps saving into DynamoDB
 * 
 * @author Benoit Lacelle
 *
 */
public class SaveToDynamoDb {
	private static final Logger LOGGER = LoggerFactory.getLogger(SaveToDynamoDb.class);

	protected SaveToDynamoDb() {
		// hidden
	}

	public static final IWebhookEvent NONE = new IWebhookEvent() {

		@Override
		public Map<String, ?> getHeaders() {
			throw new IllegalArgumentException();
		}

		@Override
		public Map<String, ?> getBody() {
			throw new IllegalArgumentException();
		}
	};

	public static AmazonDynamoDB makeDynamoDbClient() {
		AmazonDynamoDB client = AmazonDynamoDBClient.builder()
				// The region is meaningless for local DynamoDb but required for client builder validation
				.withRegion(Regions.US_EAST_2)
				// .credentialsProvider( new DefaultAWSCredentialsProviderChain())
				.build();
		return client;
	}

	public static void saveToDynamoDb(String table, IWebhookEvent input, AmazonDynamoDB client) {
		LOGGER.info("Save something into DynamoDB");

		DynamoDB dynamodb = new DynamoDB(client);
		Table myTable = dynamodb.getTable(table);
		// https://stackoverflow.com/questions/31813868/aws-dynamodb-on-android-inserting-json-directly

		Map<String, Object> inputAsMap = new LinkedHashMap<>();
		inputAsMap.put("body", input.getBody());
		inputAsMap.put("headers", input.getHeaders());

		myTable.putItem(Item.fromMap(Collections.unmodifiableMap(inputAsMap)));
	}
}
